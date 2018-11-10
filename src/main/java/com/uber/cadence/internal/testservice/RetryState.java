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

import com.uber.cadence.RetryPolicy;

final class RetryState {

  private final RetryPolicy retryPolicy;
  private final long expirationTime;
  private final int attempt;

  public RetryState(RetryPolicy retryPolicy, long expirationTime, int attempt) {
    this.retryPolicy = retryPolicy;
    this.expirationTime = expirationTime;
    this.attempt = attempt;
  }

  public RetryPolicy getRetryPolicy() {
    return retryPolicy;
  }

  public long getExpirationTime() {
    return expirationTime;
  }

  public int getAttempt() {
    return attempt;
  }

  RetryState getNextAttempt() {
    return new RetryState(retryPolicy, expirationTime, attempt + 1);
  }
}
