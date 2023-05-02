package com.uber.cadence.migration;

import static org.junit.Assert.assertEquals;

import com.uber.cadence.client.*;
import com.uber.cadence.internal.sync.SyncWorkflowDefinition;
import com.uber.cadence.testing.TestWorkflowEnvironment;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.workflow.WorkflowInterceptor;
import java.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class MigrationInterceptorTest {

  public static final String TASK_LIST = "HelloMigration";
  private final SyncWorkflowDefinition syncWorkflowDefinition = null;
  private final WorkflowInterceptor.WorkflowExecuteInput input = null;

  @Rule
  public TestWatcher watchman =
      new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
          if (testEnv != null) {
            System.err.println(testEnv.getDiagnostics());
            testEnv.close();
          }
        }
      };

  private TestWorkflowEnvironment testEnv;
  private TestWorkflowEnvironment testEnvInNew;
  private Worker worker;
  private WorkflowClient workflowClient;
  private WorkflowClient workflowClientInNew;

  @Before
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    testEnvInNew = TestWorkflowEnvironment.newInstance();
    workflowClient = testEnv.newWorkflowClient();
    workflowClientInNew = testEnvInNew.newWorkflowClient();

    worker =
        testEnv.newWorker(
            TASK_LIST,
            builder ->
                builder.setInterceptorFactory(
                    // ? how
                    next -> new MigrationInterceptor(next, workflowClientInNew)));
    worker.registerWorkflowImplementationTypes(
        SampleWorkflow.GreetingWorkflowImpl.class, SampleWorkflow.GreetingChildImpl.class);
    testEnv.start();
    testEnvInNew.start();
  }

  @After
  public void tearDown() {
    testEnv.close();
    testEnvInNew.close();
  }

  @Test
  public void testWorkflowWithoutCron() {

    WorkflowOptions workflowOptions =
        new WorkflowOptions.Builder()
            .setExecutionStartToCloseTimeout(Duration.ofSeconds(10))
            .setTaskList(TASK_LIST)
            .build();

    SampleWorkflow.GreetingWorkflow workflow =
        workflowClient.newWorkflowStub(SampleWorkflow.GreetingWorkflow.class, workflowOptions);
    String res = workflow.getGreeting("Migration");

    assertEquals("Hello Migration!", res);
  }

//  @Test
//  public void testWorkflowWithCron() {
//    // Get a workflow stub using the same task list the worker uses.
//    WorkflowOptions workflowOptions =
//        new WorkflowOptions.Builder()
//            .setCronSchedule("* * * * *")
//            .setTaskStartToCloseTimeout(Duration.ofSeconds(10))
//            .build();
//    SampleWorkflow.GreetingWorkflow workflow =
//        workflowClient.newWorkflowStub(SampleWorkflow.GreetingWorkflow.class, workflowOptions);
//
//    String greeting = workflow.getGreeting("World");
//    //assertEquals("Hello World!", greeting);
//  }
}
