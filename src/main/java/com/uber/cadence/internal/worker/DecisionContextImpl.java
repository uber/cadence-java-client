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

import com.uber.cadence.ActivityTaskStartedEventAttributes;
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.internal.DecisionContext;
import com.uber.cadence.internal.AsyncWorkflowClock;
import com.uber.cadence.internal.generic.ExecuteActivityParameters;
import com.uber.cadence.workflow.ContinueAsNewWorkflowExecutionParameters;
import com.uber.cadence.workflow.StartChildWorkflowExecutionParameters;
import com.uber.cadence.workflow.WorkflowContext;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

class DecisionContextImpl implements DecisionContext {

    private final GenericAsyncActivityClient activityClient;

    private final GenericAsyncWorkflowClient workflowClient;

    private final AsyncWorkflowClock workflowClock;

    private final WorkflowContext workflowContext;

    DecisionContextImpl(DecisionsHelper decisionsHelper, AsyncWorkflowClock workflowClock, WorkflowContext workflowContext) {
        this.activityClient = new GenericAsyncActivityClient(decisionsHelper);
        this.workflowClient = new GenericAsyncWorkflowClient(decisionsHelper, workflowContext);
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

    public void handleActivityTaskStarted(ActivityTaskStartedEventAttributes attributes) {
        activityClient.handleActivityTaskStarted(attributes);
    }

    public void handleActivityTaskCanceled(HistoryEvent event) {
        activityClient.handleActivityTaskCanceled(event);
    }

    public void handleActivityTaskCompleted(HistoryEvent event) {
        activityClient.handleActivityTaskCompleted(event);
    }

    public void handleActivityTaskFailed(HistoryEvent event) {
        activityClient.handleActivityTaskFailed(event);
    }

    public void handleActivityTaskTimedOut(HistoryEvent event) {
        activityClient.handleActivityTaskTimedOut(event);
    }

    public void handleChildWorkflowExecutionCancelRequested(HistoryEvent event) {
        workflowClient.handleChildWorkflowExecutionCancelRequested(event);
    }

    public void handleChildWorkflowExecutionCanceled(HistoryEvent event) {
        workflowClient.handleChildWorkflowExecutionCanceled(event);
    }

    public void handleChildWorkflowExecutionStarted(HistoryEvent event) {
        workflowClient.handleChildWorkflowExecutionStarted(event);
    }

    public void handleChildWorkflowExecutionTimedOut(HistoryEvent event) {
        workflowClient.handleChildWorkflowExecutionTimedOut(event);
    }

    public void handleChildWorkflowExecutionTerminated(HistoryEvent event) {
        workflowClient.handleChildWorkflowExecutionTerminated(event);
    }

    public void handleStartChildWorkflowExecutionFailed(HistoryEvent event) {
        workflowClient.handleStartChildWorkflowExecutionFailed(event);
    }

    public void handleChildWorkflowExecutionFailed(HistoryEvent event) {
        workflowClient.handleChildWorkflowExecutionFailed(event);
    }

    public void handleChildWorkflowExecutionCompleted(HistoryEvent event) {
        workflowClient.handleChildWorkflowExecutionCompleted(event);
    }
}
