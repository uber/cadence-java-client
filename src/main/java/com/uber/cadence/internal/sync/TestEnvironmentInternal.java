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

package com.uber.cadence.internal.sync;

import com.uber.cadence.testing.TestActivityEnvironment;
import com.uber.cadence.testing.TestEnvironment;
import com.uber.cadence.testing.TestEnvironmentOptions;
import com.uber.cadence.testing.TestWorkflowEnvironment;

public class TestEnvironmentInternal implements TestEnvironment {

  private final TestEnvironmentOptions testEnvironmentOptions;

  public TestEnvironmentInternal(TestEnvironmentOptions options) {
    if (options == null) {
      this.testEnvironmentOptions = new TestEnvironmentOptions.Builder().build();
    } else {
      this.testEnvironmentOptions = options;
    }
  }

  @Override
  public TestActivityEnvironment newActivityEnvironment() {
    return new TestActivityEnvironmentInternal(testEnvironmentOptions);
  }

  @Override
  public TestWorkflowEnvironment newWorkflowEnvironment() {
    return new TestWorkflowEnvironmentInternal(testEnvironmentOptions);
  }
}
