/*
 *  Modifications Copyright (c) 2017-2021 Uber Technologies Inc.
 *  Portions of the Software are attributed to Copyright (c) 2020 Temporal Technologies Inc.
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

package com.uber.cadence.client;

import com.uber.cadence.QueryRejectCondition;
import com.uber.cadence.WorkflowExecution;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * WorkflowStub is a client side stub to a single workflow instance. It can be used to start,
 * signal, query, wait for completion and cancel a workflow execution. Created through {@link
 * WorkflowClient#newUntypedWorkflowStub(String, WorkflowOptions)} or {@link
 * WorkflowClient#newUntypedWorkflowStub(WorkflowExecution, Optional)}.
 */
public interface WorkflowStub {

  /**
   * Extracts untyped WorkflowStub from a typed workflow stub created through {@link
   * WorkflowClient#newWorkflowStub(Class)}.
   *
   * @param typed typed workflow stub
   * @param <T> type of the workflow stub interface
   * @return untyped workflow stub for the same workflow instance.
   */
  static <T> WorkflowStub fromTyped(T typed) {
    if (!(typed instanceof Proxy)) {
      throw new IllegalArgumentException(
          "arguments must be created through WorkflowClient.newWorkflowStub");
    }

    InvocationHandler handler = Proxy.getInvocationHandler(typed);

    if (!(handler instanceof Supplier)) {
      throw new IllegalArgumentException(
          "arguments must be created through WorkflowClient.newWorkflowStub");
    }

    @SuppressWarnings("unchecked")
    Supplier<WorkflowStub> supplier = (Supplier<WorkflowStub>) handler;
    return supplier.get();
  }

  void signal(String signalName, Object... args);

  CompletableFuture<Void> signalAsync(String signalName, Object... args);

  CompletableFuture<Void> signalAsyncWithTimeout(
      long timeout, TimeUnit unit, String signalName, Object... args);

  WorkflowExecution start(Object... args);

  CompletableFuture<WorkflowExecution> startAsync(Object... args);

  CompletableFuture<WorkflowExecution> startAsyncWithTimeout(
      long timeout, TimeUnit unit, Object... args);

  WorkflowExecution signalWithStart(String signalName, Object[] signalArgs, Object[] startArgs);

  Optional<String> getWorkflowType();

  WorkflowExecution getExecution();

  /**
   * Returns workflow result potentially waiting for workflow to complete. Behind the scene this
   * call performs long poll on Cadence service waiting for workflow completion notification.
   *
   * @param resultClass class of the workflow return value
   * @param resultType type of the workflow return value. Differs from resultClass for generic
   *     types.
   * @param <R> type of the workflow return value
   * @return workflow return value
   */
  <R> R getResult(Class<R> resultClass, Type resultType);

  <R> CompletableFuture<R> getResultAsync(Class<R> resultClass, Type resultType);

  /**
   * Returns workflow result potentially waiting for workflow to complete. Behind the scene this
   * call performs long poll on Cadence service waiting for workflow completion notification.
   *
   * @param resultClass class of the workflow return value
   * @param <R> type of the workflow return value
   * @return workflow return value
   */
  <R> R getResult(Class<R> resultClass);

  <R> CompletableFuture<R> getResultAsync(Class<R> resultClass);

  /**
   * Returns workflow result potentially waiting for workflow to complete. Behind the scene this
   * call performs long poll on Cadence service waiting for workflow completion notification.
   *
   * @param timeout maximum time to wait
   * @param unit unit of timeout
   * @param resultClass class of the workflow return value
   * @param resultType type of the workflow return value. Differs from resultClass for generic
   * @param <R> type of the workflow return value
   * @return workflow return value
   * @throws TimeoutException if workflow is not completed after the timeout time.
   */
  <R> R getResult(long timeout, TimeUnit unit, Class<R> resultClass, Type resultType)
      throws TimeoutException;

  /**
   * Returns workflow result potentially waiting for workflow to complete. Behind the scene this
   * call performs long poll on Cadence service waiting for workflow completion notification.
   *
   * @param timeout maximum time to wait
   * @param unit unit of timeout
   * @param resultClass class of the workflow return value
   * @param <R> type of the workflow return value
   * @return workflow return value
   * @throws TimeoutException if workflow is not completed after the timeout time.
   */
  <R> R getResult(long timeout, TimeUnit unit, Class<R> resultClass) throws TimeoutException;

  <R> CompletableFuture<R> getResultAsync(
      long timeout, TimeUnit unit, Class<R> resultClass, Type resultType);

  <R> CompletableFuture<R> getResultAsync(long timeout, TimeUnit unit, Class<R> resultClass);

  /**
   * Query workflow by invoking its query handler. A query handler is a method annotated with {@link
   * com.uber.cadence.workflow.QueryMethod}.
   *
   * @see WorkflowClientOptions.Builder#setQueryRejectCondition(QueryRejectCondition)
   * @param queryType name of the query handler. Usually it is a method name.
   * @param resultClass class of the query result type
   * @param args optional query arguments
   * @param <R> type of the query result
   * @return query result
   * @throws WorkflowQueryException if query failed for any reason.
   */
  <R> R query(String queryType, Class<R> resultClass, Object... args);

  <R> R query(String queryType, Class<R> resultClass, Type resultType, Object... args);

  <R> R query(
      String queryType,
      Class<R> resultClass,
      QueryRejectCondition queryRejectCondition,
      Object... args);

  <R> R query(
      String queryType,
      Class<R> resultClass,
      Type resultType,
      QueryRejectCondition queryRejectCondition,
      Object... args);

  /** Request cancellation. */
  void cancel();

  Optional<WorkflowOptions> getOptions();
}
