package com.uber.cadence.internal;

import com.uber.cadence.workflow.Functions;
import com.uber.cadence.workflow.Promise;
import com.uber.cadence.workflow.RetryOptions;
import com.uber.cadence.workflow.Workflow;

import java.time.Duration;

/**
 * Implements operation retry logic for both synchronous and asynchronous operations.
 */
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
            long nextSleepTime = calculateSleepTime(retry, options);
            try {
                return func.apply();
            } catch (Exception e) {
                long elapsed = Workflow.currentTimeMillis() - startTime;
                if (shouldRethrow(e, options, retry, elapsed, nextSleepTime)) {
                    Workflow.throwWrapped(e);
                }
            }
            retry++;
            Workflow.sleep(nextSleepTime);
        }
    }

    public static <R> Promise<R> retryAsync(RetryOptions options, Functions.Func<Promise<R>> func) {
        long startTime = Workflow.currentTimeMillis();
        return retryAsync(options, func, startTime, 1);
    }

    private static <R> Promise<R> retryAsync(RetryOptions options, Functions.Func<Promise<R>> func, long startTime,
                                             long attempt) {
        return func.apply().handle((r, e) -> {
            if (e == null) {
                return Workflow.newPromise(r);
            }
            long elapsed = Workflow.currentTimeMillis() - startTime;
            long sleepTime = calculateSleepTime(attempt + 1, options);
            if (shouldRethrow(e, options, attempt, elapsed, sleepTime)) {
                throw e;
            }
            // newTimer runs in a separate thread, so it performs trampolining eliminating tail recursion.
            return Workflow.newTimer(Duration.ofMillis(sleepTime)).thenCompose(
                    (nil) -> retryAsync(options, func, startTime, attempt + 1));
        }).thenCompose((r) -> r);
    }

    private static boolean shouldRethrow(Exception e, RetryOptions options, long attempt, long elapsed, long sleepTime) {
        if (!options.getExceptionFilter().apply(e)) {
            return true;
        }
        // Attempt that failed.
        if (attempt >= options.getMaximumAttempts()) {
            return true;
        }
        Duration expiration = options.getExpiration();
        if (expiration != null && elapsed + sleepTime >= expiration.toMillis() && attempt > options.getMinimumAttempts()) {
            return true;
        }
        return false;
    }

    private static long calculateSleepTime(long attempt, RetryOptions options) {
        double sleepMillis = (Math.pow(options.getBackoffCoefficient(), attempt - 1)) * options.getInitialInterval().toMillis();
        Duration maximumInterval = options.getMaximumInterval();
        if (maximumInterval == null) {
            return (long) sleepMillis;
        }
        return Math.min((long) sleepMillis, maximumInterval.toMillis());
    }

    private WorkflowRetryerInternal() {
    }
}
