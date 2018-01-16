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
package com.uber.cadence.internal.dispatcher;

import com.uber.cadence.DataConverter;
import com.uber.cadence.StartWorkflowOptions;
import com.uber.cadence.WorkflowService;
import com.uber.cadence.worker.GenericWorkflowClientExternalImpl;

import java.lang.reflect.Proxy;

public class WorkflowExternal {

    private final GenericWorkflowClientExternalImpl genericClient;
    private final DataConverter dataConverter;

    public WorkflowExternal(WorkflowService.Iface service, String domain, DataConverter dataConverter) {
        this.genericClient = new GenericWorkflowClientExternalImpl(service, domain);
        this.dataConverter = dataConverter;
    }

    public <T> T newClient(Class<T> workflowInterface, StartWorkflowOptions options) {
        return (T) Proxy.newProxyInstance(Workflow.class.getClassLoader(),
                new Class<?>[]{workflowInterface},
                new WorkflowInvocationHandler(genericClient, options, dataConverter));
    }

    /**
     * Starts zero argument workflow.
     *
     * @param workflow The only supported parameter is method reference to a proxy created
     *                 through {@link #newClient(Class, StartWorkflowOptions)}.
     * @return future that contains workflow result or failure
     */
    public static <R> WorkflowExternalResult<R> executeWorkflow(Functions.Func<R> workflow) {
        WorkflowInvocationHandler.initAsyncInvocation();
        try {
            workflow.apply();
        } catch (Exception e) {
            // TODO: Appropriate exception type.
            throw new RuntimeException(e);
        } finally {
            return WorkflowInvocationHandler.getAsyncInvocationResult();
        }
    }

    /**
     * Invokes one argument workflow asynchronously.
     *
     * @param workflow The only supported parameter is method reference to a proxy created
     *                 through {@link #newClient(Class, StartWorkflowOptions)}.
     * @param arg1     first workflow argument
     * @return future that contains workflow result or failure
     */
    public static <A1, R> WorkflowExternalResult<R> executeWorkflow(Functions.Func1<A1, R> workflow, A1 arg1) {
        return executeWorkflow(() -> workflow.apply(arg1));
    }
}
