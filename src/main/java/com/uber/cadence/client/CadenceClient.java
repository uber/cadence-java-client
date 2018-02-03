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
package com.uber.cadence.client;

import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.WorkflowService;
import com.uber.cadence.internal.StartWorkflowOptions;
import com.uber.cadence.internal.dispatcher.CadenceClientInternal;
import com.uber.cadence.workflow.Functions;

public interface CadenceClient {

    static CadenceClient newClient(WorkflowService.Iface service, String domain, CadenceClientOptions options) {
        return new CadenceClientInternal(service, domain, options);
    }

    static CadenceClient newClient(WorkflowService.Iface service, String domain) {
        return new CadenceClientInternal(service, domain, null);
    }

    /**
     * Creates workflow client proxy that can be used to start a workflow.
     * After workflow is started it can be also used to send signals or queries to it.
     */
    <T> T newWorkflowStub(Class<T> workflowInterface, StartWorkflowOptions options);

    /**
     * Creates workflow client proxy for a known execution. U
     * se it to send signals or queries to a running workflow.
     */
    <T> T newWorkflowStub(Class<T> workflowInterface, WorkflowExecution execution);

    /**
     * Starts zero argument workflow with void return type
     *
     * @param workflow The only supported value is method reference to a proxy created
     *                 through {@link #newWorkflowStub(Class, StartWorkflowOptions)}.
     * @return future becomes ready upon workflow completion with null value or failure
     */
    static WorkflowExternalResult<Void> asyncStart(Functions.Proc workflow) {
        return CadenceClientInternal.asyncStart(workflow);
    }

    /**
     * Starts one argument workflow with void return type
     *
     * @param workflow The only supported value is method reference to a proxy created
     *                 through {@link #newWorkflowStub(Class, StartWorkflowOptions)}.
     * @param arg1     first workflow function parameter
     * @return future becomes ready upon workflow completion with null value or failure
     */
    static <A1> WorkflowExternalResult<Void> asyncStart(Functions.Proc1<A1> workflow, A1 arg1) {
        return CadenceClientInternal.asyncStart(workflow, arg1);
    }

    /**
     * Starts two argument workflow with void return type
     *
     * @param workflow The only supported value is method reference to a proxy created
     *                 through {@link #newWorkflowStub(Class, StartWorkflowOptions)}.
     * @param arg1     first workflow function parameter
     * @param arg2     second workflow function parameter
     * @return future becomes ready upon workflow completion with null value or failure
     */
    static <A1, A2> WorkflowExternalResult<Void> asyncStart(Functions.Proc2<A1, A2> workflow, A1 arg1, A2 arg2) {
        return CadenceClientInternal.asyncStart(workflow, arg1, arg2);
    }

    /**
     * Starts three argument workflow with void return type
     *
     * @param workflow The only supported value is method reference to a proxy created
     *                 through {@link #newWorkflowStub(Class, StartWorkflowOptions)}.
     * @param arg1     first workflow function parameter
     * @param arg2     second workflow function parameter
     * @param arg3     third workflow function parameter
     * @return future becomes ready upon workflow completion with null value or failure
     */
    static <A1, A2, A3> WorkflowExternalResult<Void> asyncStart(Functions.Proc3<A1, A2, A3> workflow, A1 arg1, A2 arg2, A3 arg3) {
        return CadenceClientInternal.asyncStart(workflow, arg1, arg2, arg3);
    }

    /**
     * Starts four argument workflow with void return type
     *
     * @param workflow The only supported value is method reference to a proxy created
     *                 through {@link #newWorkflowStub(Class, StartWorkflowOptions)}.
     * @param arg1     first workflow function parameter
     * @param arg2     second workflow function parameter
     * @param arg3     third workflow function parameter
     * @param arg4     fourth workflow function parameter
     * @return future becomes ready upon workflow completion with null value or failure
     */
    static <A1, A2, A3, A4> WorkflowExternalResult<Void> asyncStart(Functions.Proc4<A1, A2, A3, A4> workflow, A1 arg1, A2 arg2, A3 arg3, A4 arg4) {
        return CadenceClientInternal.asyncStart(workflow, arg1, arg2, arg3, arg4);
    }

    /**
     * Starts zero argument workflow with void return type
     *
     * @param workflow The only supported value is method reference to a proxy created
     *                 through {@link #newWorkflowStub(Class, StartWorkflowOptions)}.
     * @param arg1     first workflow function parameter
     * @param arg2     second workflow function parameter
     * @param arg3     third workflow function parameter
     * @param arg4     fourth workflow function parameter
     * @param arg5     fifth workflow function parameter
     * @return future becomes ready upon workflow completion with null value or failure
     */
    static <A1, A2, A3, A4, A5> WorkflowExternalResult<Void> asyncStart(Functions.Proc5<A1, A2, A3, A4, A5> workflow, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5) {
        return CadenceClientInternal.asyncStart(workflow, arg1, arg2, arg3, arg4, arg5);
    }

    /**
     * Starts zero argument workflow with void return type
     *
     * @param workflow The only supported value is method reference to a proxy created
     *                 through {@link #newWorkflowStub(Class, StartWorkflowOptions)}.
     * @param arg1     first workflow function parameter
     * @param arg2     second workflow function parameter
     * @param arg3     third workflow function parameter
     * @param arg4     fourth workflow function parameter
     * @param arg5     sixth workflow function parameter
     * @param arg6     sixth workflow function parameter
     * @return future becomes ready upon workflow completion with null value or failure
     */
    static <A1, A2, A3, A4, A5, A6> WorkflowExternalResult<Void> asyncStart(Functions.Proc6<A1, A2, A3, A4, A5, A6> workflow, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5, A6 arg6) {
        return CadenceClientInternal.asyncStart(workflow, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    /**
     * Starts zero argument workflow.
     *
     * @param workflow The only supported value is method reference to a proxy created
     *                 through {@link #newWorkflowStub(Class, StartWorkflowOptions)}.
     * @return future that contains workflow result or failure
     */
    static <R> WorkflowExternalResult<R> asyncStart(Functions.Func<R> workflow) {
        return CadenceClientInternal.asyncStart(workflow);
    }

    /**
     * Invokes one argument workflow asynchronously.
     *
     * @param workflow The only supported value is method reference to a proxy created
     *                 through {@link #newWorkflowStub(Class, StartWorkflowOptions)}.
     * @param arg1     first workflow argument
     * @return future that contains workflow result or failure
     */
    static <A1, R> WorkflowExternalResult<R> asyncStart(Functions.Func1<A1, R> workflow, A1 arg1) {
        return CadenceClientInternal.asyncStart(workflow, arg1);
    }

    /**
     * Invokes two argument workflow asynchronously.
     *
     * @param workflow The only supported value is method reference to a proxy created
     *                 through {@link #newWorkflowStub(Class, StartWorkflowOptions)}.
     * @param arg1     first workflow function parameter
     * @param arg2     second workflow function parameter
     * @return future that contains workflow result or failure
     */
    static <A1, A2, R> WorkflowExternalResult<R> asyncStart(Functions.Func2<A1, A2, R> workflow, A1 arg1, A2 arg2) {
        return CadenceClientInternal.asyncStart(workflow, arg1, arg2);
    }

    /**
     * Invokes two argument workflow asynchronously.
     *
     * @param workflow The only supported value is method reference to a proxy created
     *                 through {@link #newWorkflowStub(Class, StartWorkflowOptions)}.
     * @param arg1     first workflow function parameter
     * @param arg2     second workflow function parameter
     * @param arg3     third workflow function parameter
     * @return future that contains workflow result or failure
     */
    static <A1, A2, A3, R> WorkflowExternalResult<R> asyncStart(Functions.Func3<A1, A2, A3, R> workflow, A1 arg1, A2 arg2, A3 arg3) {
        return CadenceClientInternal.asyncStart(workflow, arg1, arg2, arg3);
    }

    /**
     * Invokes two argument workflow asynchronously.
     *
     * @param workflow The only supported value is method reference to a proxy created
     *                 through {@link #newWorkflowStub(Class, StartWorkflowOptions)}.
     * @param arg1     first workflow function parameter
     * @param arg2     second workflow function parameter
     * @param arg3     third workflow function parameter
     * @param arg4     fourth workflow function parameter
     * @return future that contains workflow result or failure
     */
    static <A1, A2, A3, A4, R> WorkflowExternalResult<R> asyncStart(Functions.Func4<A1, A2, A3, A4, R> workflow, A1 arg1, A2 arg2, A3 arg3, A4 arg4) {
        return CadenceClientInternal.asyncStart(workflow, arg1, arg2, arg3, arg4);
    }

    /**
     * Invokes two argument workflow asynchronously.
     *
     * @param workflow The only supported value is method reference to a proxy created
     *                 through {@link #newWorkflowStub(Class, StartWorkflowOptions)}.
     * @param arg1     first workflow function parameter
     * @param arg2     second workflow function parameter
     * @param arg3     third workflow function parameter
     * @param arg4     fourth workflow function parameter
     * @param arg5     sixth workflow function parameter
     * @return future that contains workflow result or failure
     */
    static <A1, A2, A3, A4, A5, R> WorkflowExternalResult<R> asyncStart(Functions.Func5<A1, A2, A3, A4, A5, R> workflow, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5) {
        return CadenceClientInternal.asyncStart(workflow, arg1, arg2, arg3, arg4, arg5);
    }

    /**
     * Invokes two argument workflow asynchronously.
     *
     * @param workflow The only supported value is method reference to a proxy created
     *                 through {@link #newWorkflowStub(Class, StartWorkflowOptions)}.
     * @param arg1     first workflow argument
     * @param arg2     second workflow function parameter
     * @param arg3     third workflow function parameter
     * @param arg4     fourth workflow function parameter
     * @param arg5     sixth workflow function parameter
     * @param arg6     sixth workflow function parameter
     * @return future that contains workflow result or failure
     */
    static <A1, A2, A3, A4, A5, A6, R> WorkflowExternalResult<R> asyncStart(Functions.Func6<A1, A2, A3, A4, A5, A6, R> workflow, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5, A6 arg6) {
        return CadenceClientInternal.asyncStart(workflow, arg1, arg2, arg3, arg4, arg5, arg6);
    }
}
