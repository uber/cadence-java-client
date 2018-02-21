package com.uber.cadence.internal;

import com.uber.cadence.workflow.CompletablePromise;
import com.uber.cadence.workflow.Functions;
import com.uber.cadence.workflow.Promise;
import com.uber.cadence.workflow.RetryOptions;
import com.uber.cadence.workflow.Workflow;
import com.uber.cadence.workflow.WorkflowThread;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class WorkflowRetryerInternal {

    public static void retry(RetryOptions options, Functions.Proc proc) {
        retry(options, () -> {
            proc.apply();
            return null;
        });
    }

    public static <R> R retry(RetryOptions options, Functions.Func<R> func) {
        int retry = 0;
        long startTime = Workflow.currentTimeMillis();
        while (true) {
            try {
                return func.apply();
            } catch (Exception e) {
                long elapsed = Workflow.currentTimeMillis() - startTime;
                if (shouldRethrow(e, options, retry, elapsed)) {
                    Workflow.throwWrapped(e);
                }
            }
            retry++;
            long toSleep = calculateSleepTime(retry, options);
            WorkflowThread.sleep(toSleep);
        }
    }

    public static <R> Promise<R> retryAsync(RetryOptions options, Functions.Func<Promise<R>> func) {
        final AtomicInteger retry = new AtomicInteger();
        long startTime = Workflow.currentTimeMillis();
        CompletablePromise<R> result = Workflow.newCompletablePromise();
        retryAsync(options, func, retry, startTime);
    }

    private static <R> void retryAsyncFirst(RetryOptions options, Functions.Func<Promise<R>> func, AtomicInteger retry, long startTime, CompletablePromise<R> result) {
        func.apply().handle((resultValue, e) -> {
            if (e == null) {
                result.complete(resultValue);
            }
            long elapsed = Workflow.currentTimeMillis() - startTime;
            if (shouldRethrow(e, options, retry.get(), elapsed)) {
                result.completeExceptionally(e);
                return null;
            }
            retry.incrementAndGet();
            long toSleep = calculateSleepTime(retry.get(), options);
            Promise<Void> timer = Workflow.newTimer(Duration.ofMillis(toSleep));
            timer.thenApply((v) -> {
                retryAsync(options, func, retry, startTime, result);
                return null;
            });
            return null;
        }).get(); // get never blocks as results are always returned. It is here to throw in case of error.
    }

    private static <R> void retryAsync(RetryOptions options, Functions.Func<Promise<R>> func, AtomicInteger retry, long startTime, CompletablePromise<R> result) {
        Promise<Void> timer = func.apply().handle((resultValue, e) -> {
            if (e == null) {
                result.complete(resultValue);
            }
            long elapsed = Workflow.currentTimeMillis() - startTime;
            if (shouldRethrow(e, options, retry.get(), elapsed)) {
                result.completeExceptionally(e);
                return null;
            }
            retry.incrementAndGet();
            long toSleep = calculateSleepTime(retry.get(), options);
            Promise<Void> timer = Workflow.newTimer(Duration.ofMillis(toSleep));
            timer.thenApply((v) -> {
                retryAsync(options, func, retry, startTime, result);
                return null;
            });
            return null;
        }).get(); // get never blocks as results are always returned. It is here to throw in case of error.
    }

    private static boolean shouldRethrow(Exception e, RetryOptions options, int retry, long elapsed) {
        if (options.getExceptionFilter().apply(e)) {
            return false;
        }
        if (retry <= options.getMaximumRetries()) {
            return false;
        }
        if (elapsed < options.getExpiration().toMillis() || retry < options.getMinimumRetries()) {
            return false;
        }
        return true;
    }

    private static long calculateSleepTime(long retry, RetryOptions options) {
        double sleepMillis = (Math.pow(options.getBackoffCoefficient(), retry - 1)) * options.getInterval().toMillis();
        return Math.min((long) sleepMillis, options.getMaximumInterval().toMillis());
    }
}
