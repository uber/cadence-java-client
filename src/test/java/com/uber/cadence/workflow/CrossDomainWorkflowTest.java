package com.uber.cadence.workflow;

import static com.uber.cadence.testUtils.TestEnvironment.DOMAIN2;
import static org.junit.Assert.assertEquals;

import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.internal.sync.TestWorkflowEnvironmentInternal;
import com.uber.cadence.testUtils.CadenceTestRule;
import com.uber.cadence.testing.TestWorkflowEnvironment;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Rule;
import org.junit.Test;

public class CrossDomainWorkflowTest {

  // When running against the test service we need both rules to share the same one rather than each
  // creating their own.
  private final TestWorkflowEnvironmentInternal.WorkflowServiceWrapper testWorkflowService =
      new TestWorkflowEnvironmentInternal.WorkflowServiceWrapper();

  @Rule
  public CadenceTestRule firstDomain =
      CadenceTestRule.builder()
          .withWorkflowTypes(TestWorkflowCrossDomainImpl.class)
          .startWorkersAutomatically()
          .withTestEnvironmentProvider(
              options -> TestWorkflowEnvironment.newInstance(testWorkflowService, options))
          .build();

  @Rule
  public CadenceTestRule secondDomain =
      CadenceTestRule.builder()
          .withDomain(DOMAIN2)
          .withWorkflowTypes(WorkflowTest.TestWorkflowSignaledSimple.class)
          .startWorkersAutomatically()
          .withTestEnvironmentProvider(
              options -> TestWorkflowEnvironment.newInstance(testWorkflowService, options))
          .build();

  public interface TestWorkflowCrossDomain {

    @WorkflowMethod
    String execute(String workflowId);
  }

  public static class TestWorkflowCrossDomainImpl implements TestWorkflowCrossDomain {

    @Override
    @WorkflowMethod
    public String execute(String wfId) {
      ExternalWorkflowStub externalWorkflow = Workflow.newUntypedExternalWorkflowStub(wfId);
      SignalOptions options =
          SignalOptions.newBuilder().setDomain(DOMAIN2).setSignalName("testSignal").build();
      externalWorkflow.signal(options, "World");
      return "Signaled External workflow";
    }
  }

  @Test
  public void testSignalCrossDomainExternalWorkflow()
      throws ExecutionException, InterruptedException {

    WorkflowOptions.Builder options = firstDomain.workflowOptionsBuilder();

    String wfId = UUID.randomUUID().toString();
    WorkflowOptions.Builder options2 = secondDomain.workflowOptionsBuilder().setWorkflowId(wfId);

    TestWorkflowCrossDomain wf =
        firstDomain
            .getWorkflowClient()
            .newWorkflowStub(TestWorkflowCrossDomain.class, options.build());

    WorkflowTest.TestWorkflowSignaled simpleWorkflow =
        secondDomain
            .getWorkflowClient()
            .newWorkflowStub(WorkflowTest.TestWorkflowSignaled.class, options2.build());

    CompletableFuture<String> result = WorkflowClient.execute(simpleWorkflow::execute);
    assertEquals("Signaled External workflow", wf.execute(wfId));
    assertEquals("Simple workflow signaled", result.get());
  }
}
