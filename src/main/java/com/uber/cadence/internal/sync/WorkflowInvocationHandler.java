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

import static com.uber.cadence.internal.common.InternalUtils.getWorkflowMethod;
import static com.uber.cadence.internal.common.InternalUtils.getWorkflowType;

import com.google.common.base.Defaults;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dynamic implementation of a strongly typed workflow interface that can be used to start, signal
 * and query workflows from external processes.
 */
class WorkflowInvocationHandler implements InvocationHandler {

  private static final Logger log = LoggerFactory.getLogger(WorkflowInvocationHandler.class);

  public enum InvocationType {
    SYNC,
    START,
    EXECUTE,
    BATCH,
  }

  interface SpecificInvocationHandler {
    InvocationType getInvocationType();

    void invoke(WorkflowStub untyped, Method method, Object[] args) throws Throwable;

    <R> R getResult(Class<R> resultClass);
  }

  private static final ThreadLocal<SyncWorkflowInvocationHandler> invocationContext =
      new ThreadLocal<>();

  /** Must call {@link #closeAsyncInvocation()} if this one was called. */
  static void initAsyncInvocation(InvocationType type) {
    initAsyncInvocation(type, null);
  }

  /** Must call {@link #closeAsyncInvocation()} if this one was called. */
  static <T> void initAsyncInvocation(InvocationType type, T value) {
    if (invocationContext.get() != null) {
      throw new IllegalStateException("already in start invocation");
    }
    invocationContext.set(new SyncWorkflowInvocationHandler(type));
  }

  @SuppressWarnings("unchecked")
  static <R> R getAsyncInvocationResult(Class<R> resultClass) {
    SyncWorkflowInvocationHandler invocation = invocationContext.get();
    if (invocation == null) {
      throw new IllegalStateException("initAsyncInvocation wasn't called");
    }
    return invocation.getResult(resultClass);
  }

  /** Closes async invocation created through {@link #initAsyncInvocation(InvocationType)} */
  static void closeAsyncInvocation() {
    invocationContext.remove();
  }

  private final WorkflowStub untyped;

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
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
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
    SpecificInvocationHandler handler = invocationContext.get();
    if (handler == null) {
      handler = new SyncWorkflowInvocationHandler(InvocationType.SYNC);
    }
    handler.invoke(untyped, method, args);
    if (handler.getInvocationType() == InvocationType.SYNC) {
      return handler.getResult(method.getReturnType());
    }
    return Defaults.defaultValue(method.getReturnType());
  }

  private static class SyncWorkflowInvocationHandler implements SpecificInvocationHandler {

    private final InvocationType invocationType;
    private Object result;

    private SyncWorkflowInvocationHandler(InvocationType invocationType) {
      this.invocationType = invocationType;
    }

    @Override
    public InvocationType getInvocationType() {
      return invocationType;
    }

    @Override
    public void invoke(WorkflowStub untyped, Method method, Object[] args) {
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
      if (workflowMethod != null) {
        result = startWorkflow(untyped, method, args);
      } else if (queryMethod != null) {
        result = queryWorkflow(untyped, method, queryMethod, args);
      } else if (signalMethod != null) {
        signalWorkflow(untyped, method, signalMethod, args);
        result = null;
      } else {
        throw new IllegalArgumentException(
            method + " is not annotated with @WorkflowMethod or @QueryMethod");
      }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R getResult(Class<R> resultClass) {
      return (R) result;
    }

    private void signalWorkflow(
        WorkflowStub untyped, Method method, SignalMethod signalMethod, Object[] args) {
      if (method.getReturnType() != Void.TYPE) {
        throw new IllegalArgumentException("Signal method must have void return type: " + method);
      }

      String signalName = signalMethod.name();
      if (signalName.isEmpty()) {
        signalName = InternalUtils.getSimpleName(method);
      }
      untyped.signal(signalName, args);
    }

    private Object queryWorkflow(
        WorkflowStub untyped, Method method, QueryMethod queryMethod, Object[] args) {
      if (method.getReturnType() == Void.TYPE) {
        throw new IllegalArgumentException("Query method cannot have void return type: " + method);
      }
      String queryType = queryMethod.name();
      if (queryType.isEmpty()) {
        queryType = InternalUtils.getSimpleName(method);
      }

      return untyped.query(queryType, method.getReturnType(), method.getGenericReturnType(), args);
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private Object startWorkflow(WorkflowStub untyped, Method method, Object[] args) {
      Optional<WorkflowOptions> options = untyped.getOptions();
      if (untyped.getExecution() == null
          || (options.isPresent()
              && options.get().getWorkflowIdReusePolicy()
                  == WorkflowIdReusePolicy.AllowDuplicate)) {
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
      if (invocationType == InvocationType.START) {
        return untyped.getExecution();
      } else if (invocationType == InvocationType.EXECUTE) {
        return untyped.getResultAsync(method.getReturnType(), method.getGenericReturnType());
      }
      return untyped.getResult(method.getReturnType(), method.getGenericReturnType());
    }
  }
}
