package com.uber.cadence;

import com.uber.cadence.serviceclient.ClientOptions;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.serviceclient.WorkflowServiceTChannel;
import com.uber.tchannel.api.ResponseCode;
import com.uber.tchannel.api.TChannel;
import com.uber.tchannel.api.handlers.ThriftRequestHandler;
import com.uber.tchannel.messages.ThriftRequest;
import com.uber.tchannel.messages.ThriftResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.rules.ExternalResource;

/**
 * FakeWorkflowServiceRule a local TChannel service which can be stubbed with fixed responses and
 * captures the requests made to it. This allows testing throw the entire TChannel stack,
 * particularly with TChannel being difficult to mock.
 */
public class FakeWorkflowServiceRule extends ExternalResource {

  private final Map<String, StubbedResponse<?>> stubbedResponses = new ConcurrentHashMap<>();
  private TChannel tChannel;
  private IWorkflowService clientConn;

  @Override
  protected void before() throws Throwable {
    tChannel = new TChannel.Builder("cadence-frontend").build();
    tChannel.setDefaultUserHandler(
        new ThriftRequestHandler<Object, Object>() {
          @Override
          public ThriftResponse<Object> handleImpl(ThriftRequest<Object> request) {
            @SuppressWarnings("rawtypes")
            StubbedResponse stub = stubbedResponses.get(request.getEndpoint());
            if (stub == null) {
              throw new IllegalStateException(
                  "Endpoint " + request.getEndpoint() + " was not stubbed");
            }
            //noinspection unchecked
            stub.future.complete(request.getBody(stub.requestType));
            return new ThriftResponse.Builder<>(request)
                .setBody(stub.body)
                .setResponseCode(stub.code)
                .build();
          }
        });
    tChannel.listen();
    clientConn =
        new WorkflowServiceTChannel(
            ClientOptions.newBuilder()
                .setHost(tChannel.getListeningHost())
                .setPort(tChannel.getListeningPort())
                .build());
  }

  @Override
  protected void after() {
    stubbedResponses.clear();
    if (clientConn != null) {
      clientConn.close();
    }
    if (tChannel != null) {
      tChannel.shutdown();
      tChannel = null;
    }
  }

  public void resetStubs() {
    stubbedResponses.clear();
  }

  public IWorkflowService getClient() {
    return clientConn;
  }

  public <V> CompletableFuture<V> stubEndpoint(
      String endpoint, Class<V> requestType, Object response) {
    CompletableFuture<V> future = new CompletableFuture<>();
    StubbedResponse<?> existingStub =
        stubbedResponses.putIfAbsent(
            endpoint, new StubbedResponse<>(response, ResponseCode.OK, future, requestType));
    if (existingStub != null) {
      throw new IllegalStateException(
          "Endpoint " + endpoint + " was already stubbed to return " + existingStub.body);
    }
    return future;
  }

  private static class StubbedResponse<V> {
    private final Object body;
    private final ResponseCode code;
    private final CompletableFuture<V> future;
    private final Class<V> requestType;

    private StubbedResponse(
        Object body, ResponseCode code, CompletableFuture<V> future, Class<V> requestType) {
      this.body = body;
      this.code = code;
      this.future = future;
      this.requestType = requestType;
    }
  }
}
