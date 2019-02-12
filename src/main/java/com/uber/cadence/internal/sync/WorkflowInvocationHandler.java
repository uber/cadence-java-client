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

import static com.uber.cadence.internal.common.InternalUtils.getValueOrDefault;
import static com.uber.cadence.internal.common.InternalUtils.getWorkflowMethod;
import static com.uber.cadence.internal.common.InternalUtils.getWorkflowType;

import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.WorkflowIdReusePolicy;
import com.uber.cadence.client.DuplicateWorkflowException;
import com.uber.cadence.client.WorkflowClientInterceptor;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.client.WorkflowStub;
import com.uber.cadence.common.CronSchedule;
import com.uber.cadence.common.MethodRetry;
import com.uber.cadence.converter.DataConverter;
import com.uber.cadence.internal.common.InternalUtils;
import com.uber.cadence.internal.external.GenericWorkflowClientExternal;
import com.uber.cadence.workflow.QueryMethod;
import com.uber.cadence.workflow.SignalMethod;
import com.uber.cadence.workflow.WorkflowMethod;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dynamic implementation of a strongly typed workflow interface that can be used to start, signal
 * and query workflows from external processes.
 */
class WorkflowInvocationHandler implements InvocationHandler, Supplier<WorkflowStub> {

  private static final Logger log = LoggerFactory.getLogger(WorkflowInvocationHandler.class);

  public enum InvocationType {
    START,
    EXECUTE,
    BATCH,
  }

  private static class AsyncInvocation<T> {

    private final InvocationType type;

    private T value;

    AsyncInvocation(InvocationType type) {
      this.type = type;
    }

    AsyncInvocation(InvocationType type, T value) {
      this.type = type;
      this.value = value;
    }

    public T getValue() {
      return value;
    }

    public void setValue(T value) {
      this.value = value;
    }
  }

  private static final ThreadLocal<AsyncInvocation<?>> asyncInvocation = new ThreadLocal<>();

  private final WorkflowStub untyped;

  /** Must call {@link #closeAsyncInvocation()} if this one was called. */
  static void initAsyncInvocation(InvocationType type) {
    initAsyncInvocation(type, null);
  }

  /** Must call {@link #closeAsyncInvocation()} if this one was called. */
  static <T> void initAsyncInvocation(InvocationType type, T value) {
    if (asyncInvocation.get() != null) {
      throw new IllegalStateException("already in start invocation");
    }
    asyncInvocation.set(new AsyncInvocation<T>(type, value));
  }

  @SuppressWarnings("unchecked")
  static <R> R getAsyncInvocationResult(Class<R> resultClass) {
    AsyncInvocation invocation = asyncInvocation.get();
    if (invocation == null) {
      throw new IllegalStateException("initAsyncInvocation wasn't called");
    }
    Object result = invocation.getValue();
    if (result == null) {
      throw new IllegalStateException(
          "Only methods of a stub created through WorkflowClient.newWorkflowStub "
              + "can be used as a parameter to the start.");
    }
    if (invocation.type == InvocationType.START) {
      if (!resultClass.equals(WorkflowExecution.class)) {
        throw new IllegalArgumentException(
            "Only WorkflowExecution type is allowed with START " + "InvocationThype");
      }
      return (R) result;
    }
    if (invocation.type == InvocationType.EXECUTE) {
      if (!resultClass.isAssignableFrom(result.getClass())) {
        throw new IllegalArgumentException(
            "Result type \""
                + result.getClass().getName()
                + "\" "
                + "doesn't match expected type \""
                + resultClass.getName()
                + "\"");
      }
      return (R) result;
    }
    throw new Error("Unknown invocation type: " + invocation.type);
  }

  /** Closes async invocation created through {@link #initAsyncInvocation(InvocationType)} */
  static void closeAsyncInvocation() {
    asyncInvocation.remove();
  }

  WorkflowInvocationHandler(
      Class<?> workflowInterface,
      GenericWorkflowClientExternal genericClient,
      WorkflowExecution execution,
      DataConverter dataConverter,
      WorkflowClientInterceptor[] interceptors) {
    Method workflowMethod = getWorkflowMethod(workflowInterface);
    WorkflowMethod annotation = workflowMethod.getAnnotation(WorkflowMethod.class);
    String workflowType = getWorkflowType(workflowMethod, annotation);

    WorkflowStub stub =
        new WorkflowStubImpl(genericClient, dataConverter, Optional.of(workflowType), execution);
    for (WorkflowClientInterceptor i : interceptors) {
      stub = i.newUntypedWorkflowStub(execution, Optional.of(workflowType), stub);
    }
    this.untyped = stub;
  }

  WorkflowInvocationHandler(
      Class<?> workflowInterface,
      GenericWorkflowClientExternal genericClient,
      WorkflowOptions options,
      DataConverter dataConverter,
      WorkflowClientInterceptor[] interceptors) {
    Method workflowMethod = getWorkflowMethod(workflowInterface);
    MethodRetry methodRetry = workflowMethod.getAnnotation(MethodRetry.class);
    CronSchedule cronSchedule = workflowMethod.getAnnotation(CronSchedule.class);
    WorkflowMethod annotation = workflowMethod.getAnnotation(WorkflowMethod.class);
    String workflowType = getWorkflowType(workflowMethod, annotation);
    WorkflowOptions mergedOptions =
        WorkflowOptions.merge(annotation, methodRetry, cronSchedule, options);
    WorkflowStub stub =
        new WorkflowStubImpl(genericClient, dataConverter, workflowType, mergedOptions);
    for (WorkflowClientInterceptor i : interceptors) {
      stub = i.newUntypedWorkflowStub(workflowType, mergedOptions, stub);
    }
    this.untyped = stub;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) {
    try {
      if (method.equals(Object.class.getMethod("toString"))) {
        // TODO: workflow info
        return "WorkflowInvocationHandler";
      }
    } catch (NoSuchMethodException e) {
      throw new Error("unexpected", e);
    }
    if (!method.getDeclaringClass().isInterface()) {
      throw new IllegalArgumentException(
          "Interface type is expected: " + method.getDeclaringClass());
    }
    WorkflowMethod workflowMethod = method.getAnnotation(WorkflowMethod.class);
    QueryMethod queryMethod = method.getAnnotation(QueryMethod.class);
    SignalMethod signalMethod = method.getAnnotation(SignalMethod.class);
    int count =
        (workflowMethod == null ? 0 : 1)
            + (queryMethod == null ? 0 : 1)
            + (signalMethod == null ? 0 : 1);
    if (count > 1) {
      throw new IllegalArgumentException(
          method
              + " must contain at most one annotation "
              + "from @WorkflowMethod, @QueryMethod or @SignalMethod");
    }
    Object result;
    if (workflowMethod != null) {
      result = startWorkflow(method, args);
    } else if (queryMethod != null) {
      result = queryWorkflow(method, queryMethod, args);
    } else if (signalMethod != null) {
      signalWorkflow(method, signalMethod, args);
      result = null;
    } else {
      throw new IllegalArgumentException(
          method + " is not annotated with @WorkflowMethod or @QueryMethod");
    }
    // Returning null for a built in type leads to NullPointerException
    return getValueOrDefault(result, method.getReturnType());
  }

  private void signalWorkflow(Method method, SignalMethod signalMethod, Object[] args) {
    if (method.getReturnType() != Void.TYPE) {
      throw new IllegalArgumentException("Signal method must have void return type: " + method);
    }

    String signalName = signalMethod.name();
    if (signalName.isEmpty()) {
      signalName = InternalUtils.getSimpleName(method);
    }
    untyped.signal(signalName, args);
  }

  private Object queryWorkflow(Method method, QueryMethod queryMethod, Object[] args) {
    if (method.getReturnType() == Void.TYPE) {
      throw new IllegalArgumentException("Query method cannot have void return type: " + method);
    }
    String queryType = queryMethod.name();
    if (queryType.isEmpty()) {
      queryType = InternalUtils.getSimpleName(method);
    }

    return untyped.query(queryType, method.getReturnType(), method.getGenericReturnType(), args);
  }

  private Object startWorkflow(Method method, Object[] args) {
    Optional<WorkflowOptions> options = untyped.getOptions();
    if (untyped.getExecution() == null
        || (options.isPresent()
            && options.get().getWorkflowIdReusePolicy() == WorkflowIdReusePolicy.AllowDuplicate)) {
      try {
        untyped.start(args);
      } catch (DuplicateWorkflowException e) {
        // We do allow duplicated calls if policy is not AllowDuplicate. Semantic is to wait for
        // result.
        if (options.isPresent()
            && options.get().getWorkflowIdReusePolicy() == WorkflowIdReusePolicy.AllowDuplicate) {
          throw e;
        }
      }
    }
    AsyncInvocation<?> async = asyncInvocation.get();
    if (async != null) {
      if (async.type == InvocationType.START) {
        @SuppressWarnings("unchecked")
        AsyncInvocation<WorkflowExecution> startInvocation =
            (AsyncInvocation<WorkflowExecution>) async;
        startInvocation.setValue(untyped.getExecution());
      } else if (async.type == InvocationType.EXECUTE) {
        @SuppressWarnings("unchecked")
        AsyncInvocation<CompletableFuture<?>> executeInvocation =
            (AsyncInvocation<CompletableFuture<?>>) async;
        executeInvocation.setValue(
            untyped.getResultAsync(method.getReturnType(), method.getGenericReturnType()));
      }
      return null;
    }
    return untyped.getResult(method.getReturnType(), method.getGenericReturnType());
  }

  @Override
  public WorkflowStub get() {
    return untyped;
  }
}
