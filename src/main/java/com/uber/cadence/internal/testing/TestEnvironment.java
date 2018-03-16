package com.uber.cadence.internal.testing;

import com.uber.cadence.internal.sync.TestEnvironmentInternal;

public interface TestEnvironment {

  static TestEnvironment newInstance() {
    return newInstance(null);
  }

  static TestEnvironment newInstance(TestEnvironmentOptions options) {
    return new TestEnvironmentInternal(options);
  }

  void registerWorkflowImplementationTypes(Class<?>... workflowImplementationClasses);

  void registerActivitiesImplementations(Object... activityImplementations);

  <T> T newActivityStub(Class<T> activityInterface);

}
