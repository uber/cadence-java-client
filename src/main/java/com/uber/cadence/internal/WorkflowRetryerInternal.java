/*
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package com.uber.cadence.internal;

import com.uber.cadence.workflow.CompletablePromise;
import com.uber.cadence.workflow.Functions;
import com.uber.cadence.workflow.Promise;
import com.uber.cadence.workflow.RetryOptions;
import com.uber.cadence.workflow.Workflow;

import java.time.Duration;

/**
 * Implements operation retry logic for both synchronous and asynchronous operations.
 * Internal class. Do not reference this class directly.
 * Use {@link Workflow#retry(RetryOptions, Functions.Func)} or
 * Async{@link #retry(RetryOptions, Functions.Func)}.
 */
public final class WorkflowRetryerInternal {

    /**
     * Retry procedure synchronously.
     *
     * @param options retry options.
     * @param proc    procedure to retry.
     */
    public static void retry(RetryOptions options, Functions.Proc proc) {
        retry(options, () -> {
            proc.apply();
            return null;
        });
    }

    /**
     * Retry function synchronously.
     *
     * @param options retry options.
     * @param func    procedure to retry.
     * @return result of func if ever completed successfully.
     */
    public static <R> R retry(RetryOptions options, Functions.Func<R> func) {
        int attempt = 1;
        long startTime = Workflow.currentTimeMillis();
        while (true) {
            long nextSleepTime = calculateSleepTime(attempt, options);
            try {
                return func.apply();
            } catch (Exception e) {
                long elapsed = Workflow.currentTimeMillis() - startTime;
                if (shouldRethrow(e, options, attempt, elapsed, nextSleepTime)) {
                    Workflow.throwWrapped(e);
                }
            }
            attempt++;
            Workflow.sleep(nextSleepTime);
        }
    }

    /**
     * Retry function asynchronously.
     *
     * @param options retry options.
     * @param func    procedure to retry.
     * @return result promise to the result or failure if retries stopped according to options.
     */
    public static <R> Promise<R> retryAsync(RetryOptions options, Functions.Func<Promise<R>> func) {
        long startTime = Workflow.currentTimeMillis();
        return retryAsync(options, func, startTime, 1);
    }

    private static <R> Promise<R> retryAsync(RetryOptions options, Functions.Func<Promise<R>> func, long startTime,
                                             long attempt) {
        CompletablePromise<R> funcResult = Workflow.newPromise();
        try {
            funcResult.completeFrom(func.apply());
        } catch (RuntimeException e) {
            funcResult.completeExceptionally(e);
        }
        return funcResult.handle((r, e) -> {
            if (e == null) {
                return Workflow.newPromise(r);
            }
            long elapsed = Workflow.currentTimeMillis() - startTime;
            long sleepTime = calculateSleepTime(attempt, options);
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
