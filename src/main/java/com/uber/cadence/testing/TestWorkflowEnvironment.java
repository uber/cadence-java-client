package com.uber.cadence.testing;

public interface TestWorkflowEnvironment {

  void registerWorkflowImplementationTypes(Class<?>... workflowImplementationClasses);
}
