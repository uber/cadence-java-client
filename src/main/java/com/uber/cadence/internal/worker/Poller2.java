package com.uber.cadence.internal.worker;

import com.google.common.base.Preconditions;
import com.uber.cadence.internal.common.BackoffThrottler;
import com.uber.cadence.internal.logging.LoggerTag;
import com.uber.cadence.internal.metrics.MetricsType;
import com.uber.m3.tally.Scope;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public final class Poller2<T> implements SuspendableWorker{

  private String identity;

  public interface Poll<TT> {
    TT poll() throws TException;
  }

  public interface PollTaskExecutor<TT> {
    void Execute(TT task) throws Exception;
  }

  private final PollTaskExecutor<T> pollTaskExecutor;
  private final Poll<T> pollService;
  private final PollerOptions pollerOptions;
  private static final Logger log = LoggerFactory.getLogger(Poller2.class);
  private ThreadPoolExecutor pollExecutor;
  private final Scope metricsScope;

  private final AtomicReference<CountDownLatch> suspendLatch = new AtomicReference<>();

  private BackoffThrottler pollBackoffThrottler;
  private Throttler pollRateThrottler;

  private Thread.UncaughtExceptionHandler uncaughtExceptionHandler =
          (t, e) -> log.error("Failure in thread " + t.getName(), e);

  Poller2(String identity, Poll<T> pollService, PollTaskExecutor<T> pollTaskExecutor, PollerOptions pollerOptions, Scope metricsScope){
    Preconditions.checkNotNull(identity, "identity cannot be null");
    Preconditions.checkNotNull(pollService, "poll service should not be null");
    Preconditions.checkNotNull(pollExecutor, "poll executor should not be null");
    Preconditions.checkNotNull(pollerOptions, "pollerOptions should not be null");

    this.identity = identity;
    this.pollService = pollService;
    this.pollTaskExecutor = pollTaskExecutor;
    this.pollerOptions = pollerOptions;
    this.metricsScope = metricsScope;
  }

  @Override
  public void start() {
    if (log.isInfoEnabled()) {
      log.info("start(): " + toString());
    }
    if (pollerOptions.getMaximumPollRatePerSecond() > 0.0) {
      pollRateThrottler =
              new Throttler(
                      "poller",
                      pollerOptions.getMaximumPollRatePerSecond(),
                      pollerOptions.getMaximumPollRateIntervalMilliseconds());
    }

    // It is important to pass blocking queue of at least options.getPollThreadCount() capacity.
    // As task enqueues next task the buffering is needed to queue task until the previous one
    // releases a thread.
    pollExecutor =
            new ThreadPoolExecutor(
                    pollerOptions.getPollThreadCount(),
                    pollerOptions.getPollThreadCount(),
                    1,
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(pollerOptions.getPollThreadCount()));
    pollExecutor.setThreadFactory(
            new ExecutorThreadFactory(
                    pollerOptions.getPollThreadNamePrefix(), pollerOptions.getUncaughtExceptionHandler()));

    pollBackoffThrottler =
            new BackoffThrottler(
                    pollerOptions.getPollBackoffInitialInterval(),
                    pollerOptions.getPollBackoffMaximumInterval(),
                    pollerOptions.getPollBackoffCoefficient());
    for (int i = 0; i < pollerOptions.getPollThreadCount(); i++) {
      pollExecutor.execute(new PollLoopTask(new PollExecutionTask()));
      metricsScope.counter(MetricsType.POLLER_START_COUNTER).inc(1);
    }
  }


  private boolean isStarted() {
    return pollExecutor != null;
  }

  @Override
  public void shutdown() {
    log.info("shutdown");
    if (!isStarted()) {
      return;
    }
    pollExecutor.shutdown();
  }

  @Override
  public void shutdownNow() {
    log.info("shutdownNow poller=" + this.pollerOptions.getPollThreadNamePrefix());
    if (!isStarted()) {
      return;
    }
    pollExecutor.shutdownNow();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    if (pollExecutor == null) {
      // not started yet.
      return true;
    }
    return pollExecutor.awaitTermination(timeout, unit);
  }

  @Override
  public boolean shutdownAndAwaitTermination(long timeout, TimeUnit unit)
          throws InterruptedException {
    if (!isStarted()) {
      return true;
    }
    pollExecutor.shutdownNow();
    return pollExecutor.awaitTermination(timeout, unit);
  }

  @Override
  public boolean isRunning() {
    return isStarted() && !pollExecutor.isTerminated();
  }

  @Override
  public void suspendPolling() {
    log.info("suspendPolling");
    suspendLatch.set(new CountDownLatch(1));
  }

  @Override
  public void resumePolling() {
    log.info("resumePolling");
    CountDownLatch existing = suspendLatch.getAndSet(null);
    if (existing != null) {
      existing.countDown();
    }
  }

  @Override
  public String toString() {
    return "Poller{" + "options=" + pollerOptions + ", identity=" + identity + '}';
  }

  private class PollLoopTask implements Runnable {

    private final Poller.ThrowingRunnable task;

    PollLoopTask(Poller.ThrowingRunnable task) {
      this.task = task;
    }

    @Override
    public void run() {
      try {
        if (pollExecutor.isTerminating()) {
          return;
        }
        pollBackoffThrottler.throttle();
        if (pollExecutor.isTerminating()) {
          return;
        }
        if (pollRateThrottler != null) {
          pollRateThrottler.throttle();
        }

        CountDownLatch suspender = Poller2.this.suspendLatch.get();
        if (suspender != null) {
          if (log.isDebugEnabled()) {
            log.debug("poll task suspending latchCount=" + suspender.getCount());
          }
          suspender.await();
        }

        if (pollExecutor.isTerminating()) {
          return;
        }
        task.run();
        pollBackoffThrottler.success();
      } catch (Throwable e) {
        pollBackoffThrottler.failure();
        if (!(e.getCause() instanceof InterruptedException)) {
          uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), e);
        }
      } finally {
        // Resubmit itself back to pollExecutor
        if (!pollExecutor.isTerminating()) {
          pollExecutor.execute(this);
        } else {
          log.info("poll loop done");
        }
      }
    }
  }

  private class PollExecutionTask implements Poller.ThrowingRunnable {
    private Semaphore pollSemaphore;

    PollExecutionTask() {
      this.pollSemaphore = new Semaphore(pollerOptions.getPollThreadCount());
    }

    @Override
    public void run() throws Exception {
      boolean synchronousSemaphoreRelease = false;
      try {
        pollSemaphore.acquire();
        // we will release the semaphore in a finally clause
        synchronousSemaphoreRelease = true;
        T task = pollService.poll();
        if (task == null) {
          return;
        }

        synchronousSemaphoreRelease = false; // released by the task
        try {
          pollTaskExecutor.Execute(task);
        } catch (Error | Exception e) {
          synchronousSemaphoreRelease = true;
          throw e;
        } finally {
          pollSemaphore.release();
        }
      } finally {
        if (synchronousSemaphoreRelease) {
          pollSemaphore.release();
        }
      }
    }
  }
}
