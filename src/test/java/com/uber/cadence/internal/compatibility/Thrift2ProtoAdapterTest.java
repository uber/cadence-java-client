/*
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.uber.cadence.internal.compatibility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;

import com.uber.cadence.SignalWithStartWorkflowExecutionAsyncRequest;
import com.uber.cadence.StartWorkflowExecutionAsyncRequest;
import com.uber.cadence.api.v1.Header;
import com.uber.cadence.api.v1.SignalWithStartWorkflowExecutionAsyncResponse;
import com.uber.cadence.api.v1.StartWorkflowExecutionAsyncResponse;
import com.uber.cadence.api.v1.WorkflowAPIGrpc;
import com.uber.cadence.internal.compatibility.proto.RequestMapper;
import com.uber.cadence.internal.compatibility.proto.serviceclient.IGrpcServiceStubs;
import com.uber.cadence.serviceclient.ClientOptions;
import com.uber.cadence.serviceclient.IWorkflowService;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import java.util.function.BiConsumer;
import org.apache.commons.io.Charsets;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class Thrift2ProtoAdapterTest {

  @Rule public GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private final WorkflowAPIGrpc.WorkflowAPIImplBase mockApi =
      Mockito.mock(WorkflowAPIGrpc.WorkflowAPIImplBase.class);
  private final MockTracer tracer = new MockTracer();
  private IWorkflowService client;

  @Before
  public void setup() throws Exception {
    grpcCleanup.register(
        InProcessServerBuilder.forName("test")
            .directExecutor()
            .addService(mockApi)
            .build()
            .start());
    ManagedChannel channel =
        grpcCleanup.register(InProcessChannelBuilder.forName("test").directExecutor().build());
    client =
        new Thrift2ProtoAdapter(
            IGrpcServiceStubs.newInstance(
                ClientOptions.newBuilder().setTracer(tracer).setGRPCChannel(channel).build()));
  }

  @Test
  public void testStartWorkflowExecutionAsync() throws Exception {
    ArgumentCaptor<com.uber.cadence.api.v1.StartWorkflowExecutionAsyncRequest> captor =
        mockRpc(
            mockApi::startWorkflowExecutionAsync,
            StartWorkflowExecutionAsyncResponse.newBuilder().build());
    com.uber.cadence.StartWorkflowExecutionAsyncRequest thriftRequest =
        new StartWorkflowExecutionAsyncRequest()
            .setRequest(
                new com.uber.cadence.StartWorkflowExecutionRequest()
                    .setDomain("domain")
                    .setWorkflowId("workflowId")
                    .setRequestId("requestId"));
    com.uber.cadence.StartWorkflowExecutionAsyncResponse response =
        client.StartWorkflowExecutionAsync(thriftRequest);

    com.uber.cadence.api.v1.StartWorkflowExecutionAsyncRequest actual = captor.getValue();

    assertTracingHeaders(actual.getRequest().getHeader());
    // Clear header as it will have values injected into it, therefore not matching our input
    actual =
        actual
            .toBuilder()
            .setRequest(actual.getRequest().toBuilder().setHeader(Header.newBuilder()))
            .build();

    assertEquals(RequestMapper.startWorkflowExecutionAsyncRequest(thriftRequest), actual);
    assertNotNull(response);
  }

  @Test
  public void testSignalWithStartWorkflowExecutionAsync() throws Exception {
    ArgumentCaptor<com.uber.cadence.api.v1.SignalWithStartWorkflowExecutionAsyncRequest> captor =
        mockRpc(
            mockApi::signalWithStartWorkflowExecutionAsync,
            SignalWithStartWorkflowExecutionAsyncResponse.newBuilder().build());
    com.uber.cadence.SignalWithStartWorkflowExecutionAsyncRequest thriftRequest =
        new SignalWithStartWorkflowExecutionAsyncRequest()
            .setRequest(
                new com.uber.cadence.SignalWithStartWorkflowExecutionRequest()
                    .setDomain("domain")
                    .setWorkflowId("workflowId")
                    .setRequestId("requestId")
                    .setSignalName("signal"));
    com.uber.cadence.SignalWithStartWorkflowExecutionAsyncResponse response =
        client.SignalWithStartWorkflowExecutionAsync(thriftRequest);

    com.uber.cadence.api.v1.SignalWithStartWorkflowExecutionAsyncRequest actual = captor.getValue();

    assertTracingHeaders(actual.getRequest().getStartRequest().getHeader());
    // Clear header as it will have values injected into it, therefore not matching our input
    actual =
        actual
            .toBuilder()
            .setRequest(
                actual
                    .getRequest()
                    .toBuilder()
                    .setStartRequest(
                        actual
                            .getRequest()
                            .getStartRequest()
                            .toBuilder()
                            .setHeader(Header.newBuilder())))
            .build();

    assertEquals(RequestMapper.signalWithStartWorkflowExecutionAsyncRequest(thriftRequest), actual);
    assertNotNull(response);
  }

  private void assertTracingHeaders(Header header) {
    assertEquals(1, tracer.finishedSpans().size());
    MockSpan mockSpan = tracer.finishedSpans().get(0);
    assertEquals(
        mockSpan.context().toTraceId(),
        Charsets.UTF_8
            .decode(header.getFieldsMap().get("traceid").getData().asReadOnlyByteBuffer())
            .toString());
    assertEquals(
        mockSpan.context().toSpanId(),
        Charsets.UTF_8
            .decode(header.getFieldsMap().get("spanid").getData().asReadOnlyByteBuffer())
            .toString());
  }

  private <REQ, RES> ArgumentCaptor<REQ> mockRpc(
      BiConsumer<REQ, StreamObserver<RES>> method, RES value) {
    ArgumentCaptor<REQ> captor = new ArgumentCaptor<>();
    doAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              StreamObserver<RES> resultObserver =
                  (StreamObserver<RES>) invocation.getArguments()[1];
              resultObserver.onNext(value);
              resultObserver.onCompleted();
              return null;
            })
        .when(mockApi);
    method.accept(captor.capture(), any());
    return captor;
  }
}
