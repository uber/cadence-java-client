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
package com.uber.cadence.internal.worker;

import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.internal.DecisionContext;
import com.uber.cadence.internal.AsyncWorkflowClock;
import com.uber.cadence.internal.generic.ExecuteActivityParameters;
import com.uber.cadence.workflow.ContinueAsNewWorkflowExecutionParameters;
import com.uber.cadence.workflow.StartChildWorkflowExecutionParameters;
import com.uber.cadence.workflow.WorkflowContext;
import com.uber.cadence.internal.generic.GenericAsyncActivityClient;
import com.uber.cadence.internal.generic.GenericAsyncWorkflowClient;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

class AsyncDecisionContextImpl implements DecisionContext {

    private final GenericAsyncActivityClient activityClient;

    private final GenericAsyncWorkflowClient workflowClient;

    private final AsyncWorkflowClock workflowClock;

    private final WorkflowContext workflowContext;

    AsyncDecisionContextImpl(GenericAsyncActivityClient activityClient, GenericAsyncWorkflowClient workflowClient,
                             AsyncWorkflowClock workflowClock, WorkflowContext workflowContext) {
        this.activityClient = activityClient;
        this.workflowClient = workflowClient;
        this.workflowClock = workflowClock;
        this.workflowContext = workflowContext;
    }

    @Override
    public Consumer<Throwable> scheduleActivityTask(ExecuteActivityParameters parameters,
                                                    BiConsumer<byte[], RuntimeException> callback) {
        return activityClient.scheduleActivityTask(parameters, callback);
    }

    @Override
    public Consumer<Throwable> startChildWorkflow(StartChildWorkflowExecutionParameters parameters,
                                                  Consumer<WorkflowExecution> executionCallback,
                                                  BiConsumer<byte[], RuntimeException> callback) {
        return workflowClient.startChildWorkflow(parameters, executionCallback, callback);
    }

    @Override
    public void requestCancelWorkflowExecution(WorkflowExecution execution) {
        workflowClient.requestCancelWorkflowExecution(execution);
    }

    @Override
    public void continueAsNewOnCompletion(ContinueAsNewWorkflowExecutionParameters parameters) {
        workflowClient.continueAsNewOnCompletion(parameters);
    }

    @Override
    public String generateUniqueId() {
        return workflowClient.generateUniqueId();
    }

    @Override
    public AsyncWorkflowClock getWorkflowClock() {
        return workflowClock;
    }

    @Override
    public WorkflowContext getWorkflowContext() {
        return workflowContext;
    }
}
