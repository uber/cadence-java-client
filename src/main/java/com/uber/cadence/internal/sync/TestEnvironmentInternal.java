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
