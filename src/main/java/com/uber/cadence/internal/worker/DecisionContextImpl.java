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
import com.uber.cadence.TimerFiredEventAttributes;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.internal.generic.ExecuteActivityParameters;
import com.uber.cadence.workflow.ContinueAsNewWorkflowExecutionParameters;
import com.uber.cadence.workflow.StartChildWorkflowExecutionParameters;
import com.uber.cadence.workflow.WorkflowContext;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

class DecisionContextImpl implements DecisionContext {

    private final ActivityDecisionContext activityClient;

    private final WorkflowDecisionContext workflowClient;

    private final ClockDecisionContext workflowClock;

    private final WorkflowContext workflowContext;

    DecisionContextImpl(DecisionsHelper decisionsHelper, WorkflowContext workflowContext) {
        this.activityClient = new ActivityDecisionContext(decisionsHelper);
        this.workflowClient = new WorkflowDecisionContext(decisionsHelper, workflowContext);
        this.workflowClock = new ClockDecisionContext(decisionsHelper);
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
    public WorkflowContext getWorkflowContext() {
        return workflowContext;
    }

    public void setReplayCurrentTimeMilliseconds(long replayCurrentTimeMilliseconds) {
        workflowClock.setReplayCurrentTimeMilliseconds(replayCurrentTimeMilliseconds);
    }

    @Override
    public boolean isReplaying() {
        return workflowClock.isReplaying();
    }

    @Override
    public IdCancellationCallbackPair createTimer(long delaySeconds, Consumer<Throwable> callback) {
        return workflowClock.createTimer(delaySeconds, callback);
    }

    @Override
    public void cancelAllTimers() {
        workflowClock.cancelAllTimers();
    }

    @Override
    public long currentTimeMillis() {
        return workflowClock.currentTimeMillis();
    }

    public void setReplaying(boolean replaying) {
        workflowClock.setReplaying(replaying);
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

    public void handleTimerFired(Long eventId, TimerFiredEventAttributes attributes) {
        workflowClock.handleTimerFired(eventId, attributes);
    }

    public void handleTimerCanceled(HistoryEvent event) {
        workflowClock.handleTimerCanceled(event);
    }
}
