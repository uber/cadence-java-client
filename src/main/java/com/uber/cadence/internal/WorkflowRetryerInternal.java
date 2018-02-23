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
        return retryAsync(options, func, startTime, 0);
    }

    private static <R> Promise<R> retryAsync(RetryOptions options, Functions.Func<Promise<R>> func, long startTime,
                                             long retry) {
        return func.apply().handle((r, e) -> {
            if (e == null) {
                return Workflow.newPromise(r);
            }
            long elapsed = Workflow.currentTimeMillis() - startTime;
            long sleepTime = calculateSleepTime(retry, options);
            if (shouldRethrow(e, options, retry, elapsed, sleepTime)) {
                throw e;
            }
            // newTimer runs in a separate thread, so it performs trampolining eliminating tail recursion.
            return Workflow.newTimer(Duration.ofMillis(sleepTime)).thenCompose(
                    (nil) -> retryAsync(options, func, startTime, retry + 1));
        }).thenCompose((r) -> r);
    }

    private static boolean shouldRethrow(Exception e, RetryOptions options, long retry, long elapsed, long sleepTime) {
        if (!options.getExceptionFilter().apply(e)) {
            return true;
        }
        if (retry > options.getMaximumRetries()) {
            return true;
        }
        if (elapsed + sleepTime >= options.getExpiration().toMillis() && retry > options.getMinimumRetries()) {
            return true;
        }
        return false;
    }

    private static long calculateSleepTime(long retry, RetryOptions options) {
        double sleepMillis = (Math.pow(options.getBackoffCoefficient(), retry - 1)) * options.getInterval().toMillis();
        return Math.min((long) sleepMillis, options.getMaximumInterval().toMillis());
    }

    private WorkflowRetryerInternal() {
    }
}
