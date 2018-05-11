package com.uber.cadence.workflow;

import static org.junit.Assert.assertEquals;
import static sun.misc.ThreadGroupUtils.getRootThreadGroup;

import com.uber.cadence.testing.TestWorkflowEnvironment;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.workflow.WorkflowTest.TestMultiargsWorkflowsImpl;
import java.time.Duration;
import org.junit.Test;

public class ExecutorShutdownTest {

  @Test
  public void test() throws InterruptedException {
    for (int i = 0; i < 10; i++) {
      TestWorkflowEnvironment environment = TestWorkflowEnvironment.newInstance();
      Worker worker = environment.newWorker("sample-task-list");
      worker.registerWorkflowImplementationTypes(TestMultiargsWorkflowsImpl.class);
      worker.registerActivitiesImplementations(new WorkflowTest.AngryChildActivityImpl());
      worker.start();
      worker.shutdown(Duration.ofMinutes(1));
      environment.close();
    }
    assertEquals(2, getRootThreadGroup().activeCount());
  }
}
