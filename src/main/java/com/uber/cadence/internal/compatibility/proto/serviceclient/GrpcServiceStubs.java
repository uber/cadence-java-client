/*
 *  Modifications Copyright (c) 2017-2021 Uber Technologies Inc.
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.uber.cadence.internal.compatibility.proto.serviceclient;

import com.uber.cadence.api.v1.DomainAPIGrpc;
import com.uber.cadence.api.v1.VisibilityAPIGrpc;
import com.uber.cadence.api.v1.VisibilityAPIGrpc.VisibilityAPIBlockingStub;
import com.uber.cadence.api.v1.VisibilityAPIGrpc.VisibilityAPIFutureStub;
import com.uber.cadence.api.v1.WorkerAPIGrpc;
import com.uber.cadence.api.v1.WorkerAPIGrpc.WorkerAPIBlockingStub;
import com.uber.cadence.api.v1.WorkerAPIGrpc.WorkerAPIFutureStub;
import com.uber.cadence.api.v1.WorkflowAPIGrpc;
import com.uber.cadence.api.v1.WorkflowAPIGrpc.WorkflowAPIBlockingStub;
import com.uber.cadence.api.v1.WorkflowAPIGrpc.WorkflowAPIFutureStub;
import com.uber.cadence.internal.Version;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.Deadline;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.stub.MetadataUtils;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GrpcServiceStubs implements IGrpcServiceStubs {

  private static final Logger log = LoggerFactory.getLogger(GrpcServiceStubs.class);

  private static final Metadata.Key<String> LIBRARY_VERSION_HEADER_KEY =
      Metadata.Key.of("cadence-client-library-version", Metadata.ASCII_STRING_MARSHALLER);

  private static final Metadata.Key<String> FEATURE_VERSION_HEADER_KEY =
      Metadata.Key.of("cadence-client-feature-version", Metadata.ASCII_STRING_MARSHALLER);

  private static final Metadata.Key<String> CLIENT_IMPL_HEADER_KEY =
      Metadata.Key.of("cadence-client-name", Metadata.ASCII_STRING_MARSHALLER);

  private static final String CLIENT_IMPL_HEADER_VALUE = "uber-java";

  private final ManagedChannel channel;
  private final boolean channelNeedsShutdown;
  private final AtomicBoolean shutdownRequested = new AtomicBoolean();
  private final DomainAPIGrpc.DomainAPIBlockingStub domainBlockingStub;
  private final DomainAPIGrpc.DomainAPIFutureStub domainFutureStub;
  private final VisibilityAPIGrpc.VisibilityAPIBlockingStub visibilityBlockingStub;
  private final VisibilityAPIGrpc.VisibilityAPIFutureStub visibilityFutureStub;
  private final WorkerAPIGrpc.WorkerAPIBlockingStub workerBlockingStub;
  private final WorkerAPIGrpc.WorkerAPIFutureStub workerFutureStub;
  private final WorkflowAPIGrpc.WorkflowAPIBlockingStub workflowBlockingStub;
  private final WorkflowAPIGrpc.WorkflowAPIFutureStub workflowFutureStub;

  GrpcServiceStubs(GrpcServiceStubsOptions options) {
    options = GrpcServiceStubsOptions.newBuilder(options).validateAndBuildWithDefaults();
    if (options.getChannel() != null) {
      this.channel = options.getChannel();
      channelNeedsShutdown = false;
    } else {
      this.channel =
          ManagedChannelBuilder.forTarget(options.getTarget())
              .defaultLoadBalancingPolicy("round_robin")
              .usePlaintext()
              .build();
      channelNeedsShutdown = true;
    }
    ClientInterceptor deadlineInterceptor = new GrpcDeadlineInterceptor(options);
    ClientInterceptor tracingInterceptor = newTracingInterceptor();
    Metadata headers = new Metadata();
    headers.put(LIBRARY_VERSION_HEADER_KEY, Version.LIBRARY_VERSION);
    headers.put(FEATURE_VERSION_HEADER_KEY, Version.FEATURE_VERSION);
    headers.put(CLIENT_IMPL_HEADER_KEY, CLIENT_IMPL_HEADER_VALUE);
    Channel interceptedChannel =
        ClientInterceptors.intercept(
            channel, deadlineInterceptor, MetadataUtils.newAttachHeadersInterceptor(headers));
    if (log.isTraceEnabled()) {
      interceptedChannel = ClientInterceptors.intercept(interceptedChannel, tracingInterceptor);
    }
    this.domainBlockingStub = DomainAPIGrpc.newBlockingStub(interceptedChannel);
    this.domainFutureStub = DomainAPIGrpc.newFutureStub(interceptedChannel);
    this.visibilityBlockingStub = VisibilityAPIGrpc.newBlockingStub(interceptedChannel);
    this.visibilityFutureStub = VisibilityAPIGrpc.newFutureStub(interceptedChannel);
    this.workerBlockingStub = WorkerAPIGrpc.newBlockingStub(interceptedChannel);
    this.workerFutureStub = WorkerAPIGrpc.newFutureStub(interceptedChannel);
    this.workflowBlockingStub = WorkflowAPIGrpc.newBlockingStub(interceptedChannel);
    this.workflowFutureStub = WorkflowAPIGrpc.newFutureStub(interceptedChannel);
  }

  private ClientInterceptor newTracingInterceptor() {
    return new ClientInterceptor() {

      @Override
      public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
          MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            next.newCall(method, callOptions)) {
          @Override
          public void sendMessage(ReqT message) {
            log.trace("Invoking " + method.getFullMethodName() + "with input: " + message);
            super.sendMessage(message);
          }

          @Override
          public void start(Listener<RespT> responseListener, Metadata headers) {
            Listener<RespT> listener =
                new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(
                    responseListener) {
                  @Override
                  public void onMessage(RespT message) {
                    if (method == WorkerAPIGrpc.getPollForDecisionTaskMethod()) {
                      log.trace("Returned " + method.getFullMethodName());
                    } else {
                      log.trace(
                          "Returned " + method.getFullMethodName() + " with output: " + message);
                    }
                    super.onMessage(message);
                  }
                };
            super.start(listener, headers);
          }
        };
      }
    };
  }

  public DomainAPIGrpc.DomainAPIBlockingStub domainBlockingStub() {
    return domainBlockingStub;
  }

  public DomainAPIGrpc.DomainAPIFutureStub domainFutureStub() {
    return domainFutureStub;
  }

  @Override
  public VisibilityAPIBlockingStub visibilityBlockingStub() {
    return visibilityBlockingStub;
  }

  @Override
  public VisibilityAPIFutureStub visibilityFutureStub() {
    return visibilityFutureStub;
  }

  @Override
  public WorkerAPIBlockingStub workerBlockingStub() {
    return workerBlockingStub;
  }

  @Override
  public WorkerAPIFutureStub workerFutureStub() {
    return workerFutureStub;
  }

  @Override
  public WorkflowAPIBlockingStub workflowBlockingStub() {
    return workflowBlockingStub;
  }

  @Override
  public WorkflowAPIFutureStub workflowFutureStub() {
    return workflowFutureStub;
  }

  @Override
  public void shutdown() {
    shutdownRequested.set(true);
    if (channelNeedsShutdown) {
      channel.shutdown();
    }
  }

  @Override
  public void shutdownNow() {
    shutdownRequested.set(true);
    if (channelNeedsShutdown) {
      channel.shutdownNow();
    }
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    if (channelNeedsShutdown) {
      return channel.awaitTermination(timeout, unit);
    }
    return true;
  }

  @Override
  public boolean isShutdown() {
    if (channelNeedsShutdown) {
      return channel.isShutdown();
    }
    return shutdownRequested.get();
  }

  @Override
  public boolean isTerminated() {
    if (channelNeedsShutdown) {
      return channel.isTerminated();
    }
    return shutdownRequested.get();
  }

  private static class GrpcDeadlineInterceptor implements ClientInterceptor {

    private final GrpcServiceStubsOptions options;

    public GrpcDeadlineInterceptor(GrpcServiceStubsOptions options) {
      this.options = options;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
        MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
      Deadline deadline = callOptions.getDeadline();
      long duration;
      if (deadline == null) {
        duration = options.getRpcTimeoutMillis();
      } else {
        duration = deadline.timeRemaining(TimeUnit.MILLISECONDS);
      }
      if (method == WorkflowAPIGrpc.getGetWorkflowExecutionHistoryMethod()) {
        if (deadline == null) {
          duration = options.getRpcLongPollTimeoutMillis();
        } else {
          duration = deadline.timeRemaining(TimeUnit.MILLISECONDS);
          if (duration > options.getRpcLongPollTimeoutMillis()) {
            duration = options.getRpcLongPollTimeoutMillis();
          }
        }
      } else if (method == WorkerAPIGrpc.getPollForDecisionTaskMethod()
          || method == WorkerAPIGrpc.getPollForActivityTaskMethod()) {
        duration = options.getRpcLongPollTimeoutMillis();
      } else if (method == WorkflowAPIGrpc.getQueryWorkflowMethod()) {
        duration = options.getRpcQueryTimeoutMillis();
      }
      if (log.isTraceEnabled()) {
        String name = method.getFullMethodName();
        log.trace("TimeoutInterceptor method=" + name + ", timeoutMs=" + duration);
      }
      return next.newCall(method, callOptions.withDeadlineAfter(duration, TimeUnit.MILLISECONDS));
    }
  }
}
