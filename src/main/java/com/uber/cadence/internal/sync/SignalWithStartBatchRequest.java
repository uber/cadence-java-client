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

package com.uber.cadence.internal.sync;

import com.uber.cadence.client.BatchRequest;
import com.uber.cadence.workflow.Functions;
import java.util.concurrent.CompletableFuture;

public class SignalWithStartBatchRequest implements BatchRequest {

  private final WorkflowClientInternal workflowClient;

  public SignalWithStartBatchRequest(WorkflowClientInternal workflowClient) {
    this.workflowClient = workflowClient;
  }

  @Override
  public void invoke() {}

  @Override
  public CompletableFuture<Void> add(Functions.Proc request) {
    WorkflowInvocationHandler.initAsyncInvocation(
        WorkflowInvocationHandler.InvocationType.BATCH, this);
    try {
      request.apply();
      return new CompletableFuture<>(); // TODO
    } finally {
      WorkflowInvocationHandler.closeAsyncInvocation();
    }
  }

  @Override
  public <A1> CompletableFuture<Void> add(Functions.Proc1<A1> request, A1 arg1) {
    return null;
  }

  @Override
  public <A1, A2> CompletableFuture<Void> add(Functions.Proc2<A1, A2> request, A1 arg1, A2 arg2) {
    return null;
  }

  @Override
  public <A1, A2, A3> CompletableFuture<Void> add(
      Functions.Proc3<A1, A2, A3> request, A1 arg1, A2 arg2, A3 arg3) {
    return null;
  }

  @Override
  public <A1, A2, A3, A4> CompletableFuture<Void> add(
      Functions.Proc4<A1, A2, A3, A4> request, A1 arg1, A2 arg2, A3 arg3, A4 arg4) {
    return null;
  }

  @Override
  public <A1, A2, A3, A4, A5> CompletableFuture<Void> add(
      Functions.Proc5<A1, A2, A3, A4, A5> request, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5) {
    return null;
  }

  @Override
  public <A1, A2, A3, A4, A5, A6> CompletableFuture<Void> add(
      Functions.Proc6<A1, A2, A3, A4, A5, A6> request,
      A1 arg1,
      A2 arg2,
      A3 arg3,
      A4 arg4,
      A5 arg5,
      A6 arg6) {
    return null;
  }

  @Override
  public <R> CompletableFuture<R> add(Functions.Func<R> request) {
    return null;
  }

  @Override
  public <A1, R> CompletableFuture<R> add(Functions.Func1<A1, R> request, A1 arg1) {
    return null;
  }

  @Override
  public <A1, A2, R> CompletableFuture<R> add(
      Functions.Func2<A1, A2, R> request, A1 arg1, A2 arg2) {
    return null;
  }

  @Override
  public <A1, A2, A3, R> CompletableFuture<R> add(
      Functions.Func3<A1, A2, A3, R> request, A1 arg1, A2 arg2, A3 arg3) {
    return null;
  }

  @Override
  public <A1, A2, A3, A4, R> CompletableFuture<R> add(
      Functions.Func4<A1, A2, A3, A4, R> request, A1 arg1, A2 arg2, A3 arg3, A4 arg4) {
    return null;
  }

  @Override
  public <A1, A2, A3, A4, A5, R> CompletableFuture<R> add(
      Functions.Func5<A1, A2, A3, A4, A5, R> request, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5) {
    return null;
  }

  @Override
  public <A1, A2, A3, A4, A5, A6, R> CompletableFuture<R> add(
      Functions.Func6<A1, A2, A3, A4, A5, A6, R> request,
      A1 arg1,
      A2 arg2,
      A3 arg3,
      A4 arg4,
      A5 arg5,
      A6 arg6) {
    return null;
  }
}
