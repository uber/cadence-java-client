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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Test;

public class RetryerTest {

  @Test
  public void testExpiration() throws InterruptedException {
    RetryOptions options =
        new RetryOptions.Builder()
            .setInitialInterval(Duration.ofMillis(10))
            .setMaximumInterval(Duration.ofMillis(100))
            .setExpiration(Duration.ofMillis(500))
            .setMaximumAttempts(20)
            .validateBuildWithDefaults();
    long start = System.currentTimeMillis();
    try {
      RpcRetryer.retryWithResultAsync(
              options,
              () -> {
                throw new IllegalArgumentException("simulated");
              })
          .get();
      fail("unreachable");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof IllegalArgumentException);
      assertEquals("simulated", e.getCause().getMessage());
    }
    assertTrue(System.currentTimeMillis() - start > 500);
  }

  @Test
  public void testExpirationFuture() throws InterruptedException {
    RetryOptions options =
        new RetryOptions.Builder()
            .setInitialInterval(Duration.ofMillis(10))
            .setMaximumInterval(Duration.ofMillis(100))
            .setExpiration(Duration.ofMillis(500))
            .validateBuildWithDefaults();
    long start = System.currentTimeMillis();
    try {
      RpcRetryer.retryWithResultAsync(
              options,
              () -> {
                CompletableFuture<Void> result = new CompletableFuture<>();
                result.completeExceptionally(new IllegalArgumentException("simulated"));
                return result;
              })
          .get();
      fail("unreachable");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof IllegalArgumentException);
      assertEquals("simulated", e.getCause().getMessage());
    }
    assertTrue(System.currentTimeMillis() - start > 500);
  }

  @Test
  public void testInterruptedException() throws InterruptedException {
    RetryOptions options =
        new RetryOptions.Builder()
            .setInitialInterval(Duration.ofMillis(10))
            .setMaximumInterval(Duration.ofMillis(100))
            .setExpiration(Duration.ofSeconds(100))
            .setDoNotRetry(InterruptedException.class)
            .validateBuildWithDefaults();
    long start = System.currentTimeMillis();
    try {
      RpcRetryer.retryWithResultAsync(
              options,
              () -> {
                CompletableFuture<Void> result = new CompletableFuture<>();
                result.completeExceptionally(new InterruptedException("simulated"));
                return result;
              })
          .get();
      fail("unreachable");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof InterruptedException);
      assertEquals("simulated", e.getCause().getMessage());
    }
    assertTrue(System.currentTimeMillis() - start < 100000);
  }

  @Test
  public void testAddDoNotRetry() throws InterruptedException {
    RetryOptions options =
        new RetryOptions.Builder()
            .setInitialInterval(Duration.ofMillis(10))
            .setExpiration(Duration.ofSeconds(100))
            .validateBuildWithDefaults();
    options = options.addDoNotRetry(InterruptedException.class);
    // need to use array (or an object) since we cannot change the
    // value of the variable inside the lambda function.
    int[] numberOfCalls = {0};
    try {
      RpcRetryer.retryWithResultAsync(
              options,
              () -> {
                CompletableFuture<Void> result = new CompletableFuture<>();
                result.completeExceptionally(new InterruptedException("simulated"));
                ++numberOfCalls[0];
                return result;
              })
          .get();
      fail("unreachable");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof InterruptedException);
      assertEquals("simulated", e.getCause().getMessage());
    }
    // Make sure the error wasn't retried
    assertTrue(numberOfCalls[0] == 1);
  }

  @Test
  public void testMaxAttempt() throws InterruptedException {
    RetryOptions options =
        new RetryOptions.Builder()
            .setInitialInterval(Duration.ofMillis(10))
            .setMaximumInterval(Duration.ofMillis(100))
            .setExpiration(Duration.ofMillis(500))
            .setMaximumAttempts(3)
            .validateBuildWithDefaults();
    // need to use array (or an object) since we cannot change the
    // value of the variable inside the lambda function.
    int[] numberOfCalls = {0};
    try {
      RpcRetryer.retryWithResultAsync(
              options,
              () -> {
                ++numberOfCalls[0];
                throw new IllegalArgumentException("simulated");
              })
          .get();
      fail("unreachable");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof IllegalArgumentException);
      assertEquals("simulated", e.getCause().getMessage());
    }
    // Make sure the error wasn't retried
    assertTrue(numberOfCalls[0] == 3);
  }

  @Test
  public void testNonRetriableExceptionList() {
    // Since we tested retry and no-retry logic works correctly above,
    // In this test we just ensure the default options contain the right
    // set of Exceptions as non-retriable so we can make sure that any
    // change to that list would be intentional.

    List<Class<? extends Throwable>> noRetryExceptions =
        RpcRetryer.DEFAULT_RPC_RETRY_OPTIONS.getDoNotRetry();
    List<Class<? extends Throwable>> expectedList =
        new ArrayList<>(
            Arrays.asList(
                BadRequestError.class,
                EntityNotExistsError.class,
                WorkflowExecutionAlreadyCompletedError.class,
                WorkflowExecutionAlreadyStartedError.class,
                DomainAlreadyExistsError.class,
                QueryFailedError.class,
                DomainNotActiveError.class,
                CancellationAlreadyRequestedError.class));

    assertEquals(expectedList.size(), noRetryExceptions.size());
    for (Class<? extends Throwable> exp : noRetryExceptions) {
      assertTrue("Missing no retry exception in default options", expectedList.indexOf(exp) >= 0);
    }
  }
}
