package com.uber.cadence.migration;

import static org.junit.Assert.assertEquals;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.testing.TestWorkflowEnvironment;
import com.uber.cadence.worker.Worker;
import java.time.Duration;
import java.util.List;

import com.uber.cadence.workflow.MetricsTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import com.uber.cadence.workflow.WorkflowMethod;

public class MigrationInterceptorTest {


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

        worker = testEnv.newWorker(SampleWorkflow.TASK_LIST, builder ->
                builder.setInterceptorFactory(
                        // ? how
                        next -> new MigrationInterceptor(next, clientInNewDomain) )
                );
        worker.registerWorkflowImplementationTypes(HelloSignal.GreetingWorkflowImpl.class);
        worker.addWorkflowImplementationFactory();
        testEnv.start();

        workflowClient = testEnv.newWorkflowClient();
    }

    @After
    public void tearDown() {
        testEnv.close();
    }

    @Test
    public void testWorkflowWithoutCron() {
        // Get a workflow stub using the same task list the worker uses.
        WorkflowOptions workflowOptions =
                new WorkflowOptions.Builder().build();
        SampleWorkflow.GreetingWorkflow workflow =
                workflowClient.newWorkflowStub(SampleWorkflow.GreetingWorkflow.class, workflowOptions);

        String greeting = workflow.getGreeting("World");
        assertEquals("Hello World!", greeting);
    }

    @Test
    public void testWorkflowWithCron() {
        // Get a workflow stub using the same task list the worker uses.
        WorkflowOptions workflowOptions =
                new WorkflowOptions.Builder()
                        .setCronSchedule("* * * * *")
                        .build();
        SampleWorkflow.GreetingWorkflow workflow =
                workflowClient.newWorkflowStub(SampleWorkflow.GreetingWorkflow.class, workflowOptions);


        // Start workflow asynchronously to not use another thread to signal.
        WorkflowClient.start(getGreeting("World");)
        assertEquals("Hello World!", greeting);
    }
}
