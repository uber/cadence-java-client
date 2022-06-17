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
import com.uber.cadence.api.v1.WorkerAPIGrpc;
import com.uber.cadence.api.v1.WorkflowAPIGrpc;
import com.uber.cadence.serviceclient.ClientOptions;
import java.util.concurrent.TimeUnit;

public interface IGrpcServiceStubs {

  /** Returns gRPC stubs with default options domain service. */
  static IGrpcServiceStubs newInstance() {
    return new GrpcServiceStubs(ClientOptions.defaultInstance());
  }

  /** Returns gRPC stubs with given options domain service. */
  static IGrpcServiceStubs newInstance(ClientOptions options) {
    return new GrpcServiceStubs(options);
  }

  /** @return Blocking (synchronous) stub to domain service. */
  DomainAPIGrpc.DomainAPIBlockingStub domainBlockingStub();

  /** @return Future (asynchronous) stub to domain service. */
  DomainAPIGrpc.DomainAPIFutureStub domainFutureStub();

  /** @return Blocking (synchronous) stub to visibility service. */
  VisibilityAPIGrpc.VisibilityAPIBlockingStub visibilityBlockingStub();

  /** @return Future (asynchronous) stub to visibility service. */
  VisibilityAPIGrpc.VisibilityAPIFutureStub visibilityFutureStub();

  /** @return Blocking (synchronous) stub to worker service. */
  WorkerAPIGrpc.WorkerAPIBlockingStub workerBlockingStub();

  /** @return Future (asynchronous) stub to worker service. */
  WorkerAPIGrpc.WorkerAPIFutureStub workerFutureStub();

  /** @return Blocking (synchronous) stub to workflow service. */
  WorkflowAPIGrpc.WorkflowAPIBlockingStub workflowBlockingStub();

  /** @return Future (asynchronous) stub to workflow service. */
  WorkflowAPIGrpc.WorkflowAPIFutureStub workflowFutureStub();

  void shutdown();

  void shutdownNow();

  boolean isShutdown();

  boolean isTerminated();

  boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;
}
