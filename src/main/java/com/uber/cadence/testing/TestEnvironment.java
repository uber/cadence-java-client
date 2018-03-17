package com.uber.cadence.testing;

import com.uber.cadence.internal.sync.TestEnvironmentInternal;

public interface TestEnvironment {

  static TestEnvironment newInstance() {
    return newInstance(null);
  }

  static TestEnvironment newInstance(TestEnvironmentOptions options) {
    return new TestEnvironmentInternal(options);
  }

  TestActivityEnvironment newActivityEnvironment();

  TestWorkflowEnvironment newWorkflowEnvironment();
}
