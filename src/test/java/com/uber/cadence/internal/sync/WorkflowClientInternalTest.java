package com.uber.cadence.internal.sync;

import static org.junit.Assert.assertEquals;

import com.uber.cadence.FakeWorkflowServiceRule;
import com.uber.cadence.SignalWithStartWorkflowExecutionAsyncResponse;
import com.uber.cadence.SignalWithStartWorkflowExecutionRequest;
import com.uber.cadence.StartWorkflowExecutionAsyncRequest;
import com.uber.cadence.StartWorkflowExecutionAsyncResponse;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.WorkflowService;
import com.uber.cadence.WorkflowType;
import com.uber.cadence.client.BatchRequest;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowClientOptions;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.client.WorkflowStub;
import com.uber.cadence.workflow.SignalMethod;
import com.uber.cadence.workflow.WorkflowMethod;
import io.opentracing.mock.MockSpan;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.io.Charsets;
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
  public void testEnqueueStart_includesTracing() {
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
    assertEquals(1, fakeService.getTracer().finishedSpans().size());
    MockSpan mockSpan = fakeService.getTracer().finishedSpans().get(0);
    assertEquals(
        mockSpan.context().toTraceId(),
        Charsets.UTF_8
            .decode(request.getRequest().getHeader().getFields().get("traceid"))
            .toString());
    assertEquals(
        mockSpan.context().toSpanId(),
        Charsets.UTF_8
            .decode(request.getRequest().getHeader().getFields().get("spanid"))
            .toString());
  }

  interface TestWorkflow {
    @WorkflowMethod(
      taskList = "taskList",
      executionStartToCloseTimeoutSeconds = 1,
      taskStartToCloseTimeoutSeconds = 2
    )
    void test(String input);
  }

  @Test
  public void testEnqueueStart_stronglyTyped() throws Exception {
    CompletableFuture<WorkflowService.StartWorkflowExecutionAsync_args> requestFuture =
        fakeService.stubEndpoint(
            "WorkflowService::StartWorkflowExecutionAsync",
            WorkflowService.StartWorkflowExecutionAsync_args.class,
            new WorkflowService.StartWorkflowExecutionAsync_result()
                .setSuccess(new StartWorkflowExecutionAsyncResponse()));

    TestWorkflow stub =
        client.newWorkflowStub(
            TestWorkflow.class,
            new WorkflowOptions.Builder()
                .setExecutionStartToCloseTimeout(Duration.ofSeconds(1))
                .setTaskStartToCloseTimeout(Duration.ofSeconds(2))
                .setWorkflowId("workflowId")
                .setTaskList("taskList")
                .build());

    WorkflowExecution execution = WorkflowClient.enqueueStart(stub::test, "input");

    assertEquals(new WorkflowExecution().setWorkflowId("workflowId"), execution);
    StartWorkflowExecutionAsyncRequest request = requestFuture.getNow(null).getStartRequest();
    assertEquals(
        new WorkflowType().setName("TestWorkflow::test"), request.getRequest().getWorkflowType());
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

  @Test
  public void testEnqueueSignalWithStart() {
    CompletableFuture<WorkflowService.SignalWithStartWorkflowExecutionAsync_args> requestFuture =
        fakeService.stubEndpoint(
            "WorkflowService::SignalWithStartWorkflowExecutionAsync",
            WorkflowService.SignalWithStartWorkflowExecutionAsync_args.class,
            new WorkflowService.SignalWithStartWorkflowExecutionAsync_result()
                .setSuccess(new SignalWithStartWorkflowExecutionAsyncResponse()));

    WorkflowStub stub =
        client.newUntypedWorkflowStub(
            "type",
            new WorkflowOptions.Builder()
                .setExecutionStartToCloseTimeout(Duration.ofSeconds(1))
                .setTaskStartToCloseTimeout(Duration.ofSeconds(2))
                .setWorkflowId("workflowId")
                .setTaskList("taskList")
                .build());
    WorkflowExecution execution =
        stub.enqueueSignalWithStart(
            "signalName", new Object[] {"signalValue"}, new Object[] {"startValue"});

    assertEquals(new WorkflowExecution().setWorkflowId("workflowId"), execution);

    SignalWithStartWorkflowExecutionRequest request =
        requestFuture.getNow(null).getSignalWithStartRequest().getRequest();
    assertEquals(new WorkflowType().setName("type"), request.getWorkflowType());
    assertEquals("workflowId", request.getWorkflowId());
    assertEquals(1, request.getExecutionStartToCloseTimeoutSeconds());
    assertEquals(2, request.getTaskStartToCloseTimeoutSeconds());
    assertEquals("domain", request.getDomain());
    assertEquals("taskList", request.getTaskList().getName());
    assertEquals(
        "\"startValue\"",
        StandardCharsets.UTF_8.decode(ByteBuffer.wrap(request.getInput())).toString());
    assertEquals("signalName", request.getSignalName());
    assertEquals(
        "\"signalValue\"",
        StandardCharsets.UTF_8.decode(ByteBuffer.wrap(request.getSignalInput())).toString());
  }

  @Test
  public void testEnqueueSignalWithStart_includesTracing() {
    CompletableFuture<WorkflowService.SignalWithStartWorkflowExecutionAsync_args> requestFuture =
        fakeService.stubEndpoint(
            "WorkflowService::SignalWithStartWorkflowExecutionAsync",
            WorkflowService.SignalWithStartWorkflowExecutionAsync_args.class,
            new WorkflowService.SignalWithStartWorkflowExecutionAsync_result()
                .setSuccess(new SignalWithStartWorkflowExecutionAsyncResponse()));

    WorkflowStub stub =
        client.newUntypedWorkflowStub(
            "type",
            new WorkflowOptions.Builder()
                .setExecutionStartToCloseTimeout(Duration.ofSeconds(1))
                .setTaskStartToCloseTimeout(Duration.ofSeconds(2))
                .setWorkflowId("workflowId")
                .setTaskList("taskList")
                .build());
    stub.enqueueSignalWithStart(
        "signalName", new Object[] {"signalValue"}, new Object[] {"startValue"});

    SignalWithStartWorkflowExecutionRequest request =
        requestFuture.getNow(null).getSignalWithStartRequest().getRequest();
    assertEquals(1, fakeService.getTracer().finishedSpans().size());
    MockSpan mockSpan = fakeService.getTracer().finishedSpans().get(0);
    assertEquals(
        mockSpan.context().toTraceId(),
        Charsets.UTF_8.decode(request.getHeader().getFields().get("traceid")).toString());
    assertEquals(
        mockSpan.context().toSpanId(),
        Charsets.UTF_8.decode(request.getHeader().getFields().get("spanid")).toString());
  }

  interface TestSignalWorkflow {
    @WorkflowMethod(
      taskList = "taskList",
      executionStartToCloseTimeoutSeconds = 1,
      taskStartToCloseTimeoutSeconds = 2
    )
    void test(String input);

    @SignalMethod
    void signal(String input);
  }

  @Test
  public void testEnqueueSignalWithStart_stronglyTyped() {
    CompletableFuture<WorkflowService.SignalWithStartWorkflowExecutionAsync_args> requestFuture =
        fakeService.stubEndpoint(
            "WorkflowService::SignalWithStartWorkflowExecutionAsync",
            WorkflowService.SignalWithStartWorkflowExecutionAsync_args.class,
            new WorkflowService.SignalWithStartWorkflowExecutionAsync_result()
                .setSuccess(new SignalWithStartWorkflowExecutionAsyncResponse()));

    TestSignalWorkflow stub =
        client.newWorkflowStub(
            TestSignalWorkflow.class,
            new WorkflowOptions.Builder()
                .setExecutionStartToCloseTimeout(Duration.ofSeconds(1))
                .setTaskStartToCloseTimeout(Duration.ofSeconds(2))
                .setWorkflowId("workflowId")
                .setTaskList("taskList")
                .build());

    BatchRequest batch = client.newSignalWithStartRequest();
    batch.add(stub::test, "startValue");
    batch.add(stub::signal, "signalValue");
    WorkflowExecution execution = client.enqueueSignalWithStart(batch);

    assertEquals(new WorkflowExecution().setWorkflowId("workflowId"), execution);

    SignalWithStartWorkflowExecutionRequest request =
        requestFuture.getNow(null).getSignalWithStartRequest().getRequest();
    assertEquals(new WorkflowType().setName("TestSignalWorkflow::test"), request.getWorkflowType());
    assertEquals("workflowId", request.getWorkflowId());
    assertEquals(1, request.getExecutionStartToCloseTimeoutSeconds());
    assertEquals(2, request.getTaskStartToCloseTimeoutSeconds());
    assertEquals("domain", request.getDomain());
    assertEquals("taskList", request.getTaskList().getName());
    assertEquals(
        "\"startValue\"",
        StandardCharsets.UTF_8.decode(ByteBuffer.wrap(request.getInput())).toString());
    assertEquals("TestSignalWorkflow::signal", request.getSignalName());
    assertEquals(
        "\"signalValue\"",
        StandardCharsets.UTF_8.decode(ByteBuffer.wrap(request.getSignalInput())).toString());
  }
}
