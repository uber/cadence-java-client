package com.uber.cadence.internal.sync;

import com.uber.cadence.internal.replay.ReplayDecisionTaskHandler;
import com.uber.cadence.testing.TestEnvironmentOptions;
import com.uber.cadence.testing.TestWorkflowEnvironment;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class TestWorkflowEnvironmentInternal implements TestWorkflowEnvironment {

  private static final int WORKFLOW_THREAD_POOL_SIZE = 1000;

  private final ReplayDecisionTaskHandler decisionTaskHandler;
  private final POJOWorkflowImplementationFactory workflowImplementationFactory;
  private final TestEnvironmentOptions testEnvironmentOptions;
  private final AtomicInteger idSequencer = new AtomicInteger();

  TestWorkflowEnvironmentInternal(TestEnvironmentOptions options) {
    if (options == null) {
      this.testEnvironmentOptions = new TestEnvironmentOptions.Builder().build();
    } else {
      this.testEnvironmentOptions = options;
    }
    ThreadPoolExecutor workflowThreadPool =
        new ThreadPoolExecutor(
            WORKFLOW_THREAD_POOL_SIZE,
            WORKFLOW_THREAD_POOL_SIZE,
            10,
            TimeUnit.SECONDS,
            new SynchronousQueue<>());

    workflowImplementationFactory = new POJOWorkflowImplementationFactory(
        testEnvironmentOptions.getDataConverter(), workflowThreadPool);
    decisionTaskHandler = new ReplayDecisionTaskHandler(testEnvironmentOptions.getDomain(),
        workflowImplementationFactory);
  }

  @Override
  public void registerWorkflowImplementationTypes(Class<?>... workflowImplementationClasses) {
    workflowImplementationFactory.setWorkflowImplementationTypes(workflowImplementationClasses);
  }
}
