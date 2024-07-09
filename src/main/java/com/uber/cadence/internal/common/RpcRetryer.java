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

package com.uber.cadence.internal.common;

import static com.uber.cadence.internal.common.CheckedExceptionWrapper.unwrap;

import com.uber.cadence.BadRequestError;
import com.uber.cadence.CancellationAlreadyRequestedError;
import com.uber.cadence.DomainAlreadyExistsError;
import com.uber.cadence.DomainNotActiveError;
import com.uber.cadence.EntityNotExistsError;
import com.uber.cadence.QueryFailedError;
import com.uber.cadence.WorkflowExecutionAlreadyCompletedError;
import com.uber.cadence.WorkflowExecutionAlreadyStartedError;
import com.uber.cadence.common.RetryOptions;
import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RpcRetryer {
  public static final RetryOptions DEFAULT_RPC_RETRY_OPTIONS;

  private static final Duration RETRY_SERVICE_OPERATION_INITIAL_INTERVAL = Duration.ofMillis(20);
  private static final Duration RETRY_SERVICE_OPERATION_EXPIRATION_INTERVAL = Duration.ofMinutes(1);
  private static final double RETRY_SERVICE_OPERATION_BACKOFF = 1.2;

  static {
    RetryOptions.Builder roBuilder =
        new RetryOptions.Builder()
            .setInitialInterval(RETRY_SERVICE_OPERATION_INITIAL_INTERVAL)
            .setExpiration(RETRY_SERVICE_OPERATION_EXPIRATION_INTERVAL)
            .setBackoffCoefficient(RETRY_SERVICE_OPERATION_BACKOFF);

    Duration maxInterval = RETRY_SERVICE_OPERATION_EXPIRATION_INTERVAL.dividedBy(10);
    if (maxInterval.compareTo(RETRY_SERVICE_OPERATION_INITIAL_INTERVAL) < 0) {
      maxInterval = RETRY_SERVICE_OPERATION_INITIAL_INTERVAL;
    }
    roBuilder.setMaximumInterval(maxInterval);
    roBuilder.setDoNotRetry(
        BadRequestError.class,
        EntityNotExistsError.class,
        WorkflowExecutionAlreadyCompletedError.class,
        WorkflowExecutionAlreadyStartedError.class,
        DomainAlreadyExistsError.class,
        QueryFailedError.class,
        DomainNotActiveError.class,
        CancellationAlreadyRequestedError.class);
    DEFAULT_RPC_RETRY_OPTIONS = roBuilder.validateBuildWithDefaults();
  }

  public interface RetryableProc<E extends Throwable> {

    void apply() throws E;
  }

  public interface RetryableFunc<R, E extends Throwable> {

    R apply() throws E;
  }

  private static final Logger log = LoggerFactory.getLogger(RpcRetryer.class);

  public static <T extends Throwable> void retry(RetryOptions options, RetryableProc<T> r)
      throws T {
    retryWithResult(
        options,
        () -> {
          r.apply();
          return null;
        });
  }

  public static <T extends Throwable> void retry(RetryableProc<T> r) throws T {
    retry(DEFAULT_RPC_RETRY_OPTIONS, r);
  }

  public static <R, T extends Throwable> R retryWithResult(
      RetryOptions options, RetryableFunc<R, T> r) throws T {
    int attempt = 0;
    long startTime = System.currentTimeMillis();
    BackoffThrottler throttler =
        new BackoffThrottler(
            options.getInitialInterval(),
            options.getMaximumInterval(),
            options.getBackoffCoefficient());
    do {
      try {
        attempt++;
        throttler.throttle();
        R result = r.apply();
        throttler.success();
        return result;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new CancellationException();
      } catch (Exception e) {
        throttler.failure();
        if (options.getDoNotRetry() != null) {
          for (Class<?> exceptionToNotRetry : options.getDoNotRetry()) {
            if (exceptionToNotRetry.isAssignableFrom(e.getClass())) {
              rethrow(e);
            }
          }
        }
        long elapsed = System.currentTimeMillis() - startTime;
        int maxAttempts = options.getMaximumAttempts();
        Duration expiration = options.getExpiration();
        if ((maxAttempts > 0 && attempt >= maxAttempts)
            || (expiration != null && elapsed >= expiration.toMillis())) {
          rethrow(e);
        }
        log.warn("Retrying after failure", e);
      }
    } while (true);
  }

  public static <R> CompletableFuture<R> retryWithResultAsync(
      RetryOptions options, Supplier<CompletableFuture<R>> function) {
    int attempt = 0;
    long startTime = System.currentTimeMillis();
    AsyncBackoffThrottler throttler =
        new AsyncBackoffThrottler(
            options.getInitialInterval(),
            options.getMaximumInterval(),
            options.getBackoffCoefficient());

    return retryWithResultAsync(options, function, attempt + 1, startTime, throttler);
  }

  private static <R> CompletableFuture<R> retryWithResultAsync(
      RetryOptions options,
      Supplier<CompletableFuture<R>> function,
      int attempt,
      long startTime,
      AsyncBackoffThrottler throttler) {
    options.validate();
    return throttler
        .throttle()
        .thenCompose(ignored -> function.get())
        // Java 12 adds exceptionallyCompose, this is the closest we can get for now
        .handle((r, e) -> failOrRetry(options, function, attempt, startTime, throttler, r, e))
        .thenCompose(x -> x);
  }

  private static <R> CompletableFuture<R> failOrRetry(
      RetryOptions options,
      Supplier<CompletableFuture<R>> function,
      int attempt,
      long startTime,
      AsyncBackoffThrottler throttler,
      R r,
      Throwable e) {
    if (e == null) {
      throttler.success();
      return CompletableFuture.completedFuture(r);
    }
    throttler.failure();
    if (e instanceof CompletionException) {
      e = e.getCause();
    }
    // Do not retry Error
    if (e instanceof Error) {
      return completedExceptionally(e);
    }
    e = unwrap((Exception) e);
    long elapsed = System.currentTimeMillis() - startTime;
    if (options.getDoNotRetry() != null) {
      for (Class<?> exceptionToNotRetry : options.getDoNotRetry()) {
        if (exceptionToNotRetry.isAssignableFrom(e.getClass())) {
          return completedExceptionally(e);
        }
      }
    }
    int maxAttempts = options.getMaximumAttempts();
    if ((maxAttempts > 0 && attempt >= maxAttempts)
        || (options.getExpiration() != null && elapsed >= options.getExpiration().toMillis())) {
      return completedExceptionally(e);
    }
    log.debug("Retrying after failure", e);
    return retryWithResultAsync(options, function, attempt + 1, startTime, throttler);
  }

  // Java hack to throw a checked exception despite it not being declared
  @SuppressWarnings("unchecked")
  private static <T extends Throwable> void rethrow(Throwable e) throws T {
    throw (T) e;
  }

  /** Prohibits instantiation. */
  private RpcRetryer() {}

  private static <T> CompletableFuture<T> completedExceptionally(Throwable throwable) {
    CompletableFuture<T> failed = new CompletableFuture<>();
    failed.completeExceptionally(throwable);
    return failed;
  }
}
