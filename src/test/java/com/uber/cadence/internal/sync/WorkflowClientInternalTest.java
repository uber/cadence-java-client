package com.uber.cadence.internal.sync;

import static org.junit.Assert.assertEquals;

import com.uber.cadence.FakeWorkflowServiceRule;
import com.uber.cadence.StartWorkflowExecutionAsyncRequest;
import com.uber.cadence.StartWorkflowExecutionAsyncResponse;
import com.uber.cadence.WorkflowService;
import com.uber.cadence.WorkflowType;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowClientOptions;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.client.WorkflowStub;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class WorkflowClientInternalTest {

  @ClassRule public static FakeWorkflowServiceRule fakeService = new FakeWorkflowServiceRule();

  private WorkflowClient client;

  @Before
  public void setup() throws Exception {
    fakeService.resetStubs();
    client =
        WorkflowClient.newInstance(
            fakeService.getClient(),
            WorkflowClientOptions.newBuilder().setDomain("domain").build());
  }

  @Test
  public void testEnqueueStart() throws Exception {
    CompletableFuture<WorkflowService.StartWorkflowExecutionAsync_args> requestFuture =
        fakeService.stubEndpoint(
            "WorkflowService::StartWorkflowExecutionAsync",
            WorkflowService.StartWorkflowExecutionAsync_args.class,
            new WorkflowService.StartWorkflowExecutionAsync_result()
                .setSuccess(new StartWorkflowExecutionAsyncResponse()));

    WorkflowStub stub =
        client.newUntypedWorkflowStub(
            "type",
            new WorkflowOptions.Builder()
                .setExecutionStartToCloseTimeout(Duration.ofSeconds(1))
                .setTaskStartToCloseTimeout(Duration.ofSeconds(2))
                .setWorkflowId("workflowId")
                .setTaskList("taskList")
                .build());
    stub.enqueueStart("input");

    StartWorkflowExecutionAsyncRequest request = requestFuture.getNow(null).getStartRequest();
    assertEquals(new WorkflowType().setName("type"), request.getRequest().getWorkflowType());
    assertEquals("workflowId", request.getRequest().getWorkflowId());
    assertEquals(1, request.getRequest().getExecutionStartToCloseTimeoutSeconds());
    assertEquals(2, request.getRequest().getTaskStartToCloseTimeoutSeconds());
    assertEquals("domain", request.getRequest().getDomain());
    assertEquals("taskList", request.getRequest().getTaskList().getName());
    assertEquals("\"input\"", StandardCharsets.UTF_8.decode(request.request.input).toString());
  }

  @Test
  public void testEnqueueStartAsync() {
    CompletableFuture<WorkflowService.StartWorkflowExecutionAsync_args> requestFuture =
        fakeService.stubEndpoint(
            "WorkflowService::StartWorkflowExecutionAsync",
            WorkflowService.StartWorkflowExecutionAsync_args.class,
            new WorkflowService.StartWorkflowExecutionAsync_result()
                .setSuccess(new StartWorkflowExecutionAsyncResponse()));

    WorkflowStub stub =
        client.newUntypedWorkflowStub(
            "type",
            new WorkflowOptions.Builder()
                .setExecutionStartToCloseTimeout(Duration.ofSeconds(1))
                .setTaskStartToCloseTimeout(Duration.ofSeconds(2))
                .setWorkflowId("workflowId")
                .setTaskList("taskList")
                .build());
    stub.enqueueStartAsync("input").join();

    StartWorkflowExecutionAsyncRequest request = requestFuture.getNow(null).getStartRequest();
    assertEquals(new WorkflowType().setName("type"), request.getRequest().getWorkflowType());
    assertEquals("workflowId", request.getRequest().getWorkflowId());
    assertEquals(1, request.getRequest().getExecutionStartToCloseTimeoutSeconds());
    assertEquals(2, request.getRequest().getTaskStartToCloseTimeoutSeconds());
    assertEquals("domain", request.getRequest().getDomain());
    assertEquals("taskList", request.getRequest().getTaskList().getName());
    assertEquals("\"input\"", StandardCharsets.UTF_8.decode(request.request.input).toString());
  }
}
