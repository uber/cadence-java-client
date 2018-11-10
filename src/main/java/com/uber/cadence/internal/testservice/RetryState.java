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

package com.uber.cadence.internal.testservice;

import static com.uber.cadence.internal.testservice.TestWorkflowMutableStateImpl.MILLISECONDS_IN_SECOND;

import com.uber.cadence.RetryPolicy;

final class RetryState {

  private final RetryPolicy retryPolicy;
  private final long expirationTime;
  private final int attempt;

  RetryState(RetryPolicy retryPolicy, long expirationTime) {
    this(retryPolicy, expirationTime, 0);
  }

  private RetryState(RetryPolicy retryPolicy, long expirationTime, int attempt) {
    this.retryPolicy = retryPolicy;
    this.expirationTime = expirationTime;
    this.attempt = attempt;
  }

  RetryPolicy getRetryPolicy() {
    return retryPolicy;
  }

  long getExpirationTime() {
    return expirationTime;
  }

  int getAttempt() {
    return attempt;
  }

  RetryState getNextAttempt() {
    return new RetryState(retryPolicy, expirationTime, attempt + 1);
  }

  int getBackoffIntervalInSeconds(String errReason, long currentTimeMillis) {
    RetryPolicy retryPolicy = getRetryPolicy();
    long expirationTime = getExpirationTime();
    if (retryPolicy.getMaximumAttempts() == 0 && expirationTime == 0) {
      return 0;
    }

    if (retryPolicy.getMaximumAttempts() > 0
        && getAttempt() >= retryPolicy.getMaximumAttempts() - 1) {
      // currAttempt starts from 0.
      // MaximumAttempts is the total attempts, including initial (non-retry) attempt.
      return 0;
    }
    long initInterval = retryPolicy.getInitialIntervalInSeconds() * MILLISECONDS_IN_SECOND;
    long nextInterval =
        (long) (initInterval * Math.pow(retryPolicy.getBackoffCoefficient(), getAttempt()));
    long maxInterval = retryPolicy.getMaximumIntervalInSeconds() * MILLISECONDS_IN_SECOND;
    if (nextInterval <= 0) {
      // math.Pow() could overflow
      if (maxInterval > 0) {
        nextInterval = maxInterval;
      } else {
        return 0;
      }
    }

    if (maxInterval > 0 && nextInterval > maxInterval) {
      // cap next interval to MaxInterval
      nextInterval = maxInterval;
    }

    long backoffInterval = nextInterval;
    long nextScheduleTime = currentTimeMillis + backoffInterval;
    if (expirationTime != 0 && nextScheduleTime > expirationTime) {
      return 0;
    }

    // check if error is non-retriable
    for (String err : retryPolicy.getNonRetriableErrorReasons()) {
      if (errReason.equals(err)) {
        return 0;
      }
    }
    return (int) (Math.ceil((double) backoffInterval) / MILLISECONDS_IN_SECOND);
  }
}
