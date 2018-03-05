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
package com.uber.cadence.internal.replay;

import com.uber.cadence.EventType;
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.PollForDecisionTaskResponse;
import com.uber.cadence.TimerFiredEventAttributes;
import com.uber.cadence.TimerStartedEventAttributes;
import com.uber.cadence.WorkflowExecutionSignaledEventAttributes;
import com.uber.cadence.WorkflowQuery;
import com.uber.cadence.internal.common.InternalUtils;
import com.uber.cadence.internal.worker.WorkflowExecutionException;
import com.uber.cadence.workflow.Functions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implements decider that relies on replay of a worklfow code.
 * An instance of this class is created per decision.
 */
class ReplayDecider {

    private static final Log log = LogFactory.getLog(ReplayDecider.class);

    private static final int MILLION = 1000000;

    private final HistoryHelper historyHelper;

    private final DecisionsHelper decisionsHelper;

    private final DecisionContextImpl context;

    private ReplayWorkflow workflow;

    private boolean cancelRequested;

    private boolean completed;

    private WorkflowExecutionException failure;

    ReplayDecider(String domain, ReplayWorkflow workflow, HistoryHelper historyHelper, DecisionsHelper decisionsHelper) {
        this.workflow = workflow;
        this.historyHelper = historyHelper;
        this.decisionsHelper = decisionsHelper;
        PollForDecisionTaskResponse decisionTask = historyHelper.getDecisionTask();
        context = new DecisionContextImpl(decisionsHelper, domain, decisionTask, historyHelper.getWorkflowExecutionStartedEventAttributes());
    }

    public boolean isCancelRequested() {
        return cancelRequested;
    }

    private void handleWorkflowExecutionStarted(HistoryEvent event) throws Exception {
        workflow.start(event, context);
    }

    private void processEvent(HistoryEvent event, EventType eventType) throws Throwable {
        switch (eventType) {
            case ActivityTaskCanceled:
                context.handleActivityTaskCanceled(event);
                break;
            case ActivityTaskCompleted:
                context.handleActivityTaskCompleted(event);
                break;
            case ActivityTaskFailed:
                context.handleActivityTaskFailed(event);
                break;
            case ActivityTaskStarted:
                context.handleActivityTaskStarted(event.getActivityTaskStartedEventAttributes());
                break;
            case ActivityTaskTimedOut:
                context.handleActivityTaskTimedOut(event);
                break;
            case ExternalWorkflowExecutionCancelRequested:
                context.handleChildWorkflowExecutionCancelRequested(event);
                break;
            case ChildWorkflowExecutionCanceled:
                context.handleChildWorkflowExecutionCanceled(event);
                break;
            case ChildWorkflowExecutionCompleted:
                context.handleChildWorkflowExecutionCompleted(event);
                break;
            case ChildWorkflowExecutionFailed:
                context.handleChildWorkflowExecutionFailed(event);
                break;
            case ChildWorkflowExecutionStarted:
                context.handleChildWorkflowExecutionStarted(event);
                break;
            case ChildWorkflowExecutionTerminated:
                context.handleChildWorkflowExecutionTerminated(event);
                break;
            case ChildWorkflowExecutionTimedOut:
                context.handleChildWorkflowExecutionTimedOut(event);
                break;
            case DecisionTaskCompleted:
                handleDecisionTaskCompleted(event);
                break;
            case DecisionTaskScheduled:
                // NOOP
                break;
            case DecisionTaskStarted:
                handleDecisionTaskStarted(event);
                break;
            case DecisionTaskTimedOut:
                // Handled in the processEvent(event)
                break;
//        case ExternalWorkflowExecutionSignaled:
//            workflowClient.handleExternalWorkflowExecutionSignaled(event);
//            break;
            case StartChildWorkflowExecutionFailed:
                context.handleStartChildWorkflowExecutionFailed(event);
                break;
            case TimerFired:
                handleTimerFired(event);
                break;
            case WorkflowExecutionCancelRequested:
                handleWorkflowExecutionCancelRequested(event);
                break;
            case WorkflowExecutionSignaled:
                handleWorkflowExecutionSignaled(event);
                break;
            case WorkflowExecutionStarted:
                handleWorkflowExecutionStarted(event);
                break;
            case WorkflowExecutionTerminated:
                // NOOP
                break;
            case WorkflowExecutionTimedOut:
                // NOOP
                break;
            case ActivityTaskScheduled:
                decisionsHelper.handleActivityTaskScheduled(event);
                break;
            case ActivityTaskCancelRequested:
                decisionsHelper.handleActivityTaskCancelRequested(event);
                break;
            case RequestCancelActivityTaskFailed:
                decisionsHelper.handleRequestCancelActivityTaskFailed(event);
                break;
            case MarkerRecorded:
                break;
            case WorkflowExecutionCompleted:
                break;
            case WorkflowExecutionFailed:
                break;
            case WorkflowExecutionCanceled:
                break;
            case WorkflowExecutionContinuedAsNew:
                break;
            case TimerStarted:
                handleTimerStarted(event);
                break;
            case TimerCanceled:
                context.handleTimerCanceled(event);
                break;
//        case SignalExternalWorkflowExecutionInitiated:
//            decisionsHelper.handleSignalExternalWorkflowExecutionInitiated(event);
//            break;
            case RequestCancelExternalWorkflowExecutionInitiated:
                decisionsHelper.handleRequestCancelExternalWorkflowExecutionInitiated(event);
                break;
            case RequestCancelExternalWorkflowExecutionFailed:
                decisionsHelper.handleRequestCancelExternalWorkflowExecutionFailed(event);
                break;
            case StartChildWorkflowExecutionInitiated:
                decisionsHelper.handleStartChildWorkflowExecutionInitiated(event);
                break;
            case CancelTimerFailed:
                decisionsHelper.handleCancelTimerFailed(event);
        }
    }

    private void eventLoop() {
        if (completed) {
            return;
        }
        try {
            completed = workflow.eventLoop();
        } catch (Error e) {
            throw e; // errors fail decision, not a workflow
        } catch (WorkflowExecutionException e) {
            failure = e;
            completed = true;
        } catch (CancellationException e) {
            if (!cancelRequested) {
                failure = workflow.mapUnexpectedException(e);
            }
            completed = true;
        } catch (Throwable e) {
            failure = workflow.mapUnexpectedException(e);
            completed = true;
        }
    }

    private void completeWorkflow() {
        if (completed) {
            if (failure != null) {
                decisionsHelper.failWorkflowExecution(failure);
            } else if (cancelRequested) {
                decisionsHelper.cancelWorkflowExecution();
            } else {
                ContinueAsNewWorkflowExecutionParameters continueAsNewOnCompletion = context.getContinueAsNewOnCompletion();
                if (continueAsNewOnCompletion != null) {
                    decisionsHelper.continueAsNewWorkflowExecution(continueAsNewOnCompletion);
                } else {
                    byte[] workflowOutput = workflow.getOutput();
                    decisionsHelper.completeWorkflowExecution(workflowOutput);
                }
            }
        } else {
            long nextWakeUpTime = workflow.getNextWakeUpTime();
            if (nextWakeUpTime == 0) { // No time based waiting
                context.cancelAllTimers();
            }
            long delayMilliseconds = nextWakeUpTime - context.currentTimeMillis();
            if (nextWakeUpTime > context.currentTimeMillis()) {
                // Round up to the nearest second as we don't want to deliver a timer
                // earlier than requested.
                long delaySeconds = InternalUtils.roundUpToSeconds(Duration.ofMillis(delayMilliseconds)).getSeconds();
                context.createTimer(delaySeconds, (t) -> {
                    // Intentionally left empty.
                    // Timer ensures that decision is scheduled at the time workflow can make progress.
                    // But no specific timer related action is necessary.
                });
            }
        }
    }

    private void handleDecisionTaskStarted(HistoryEvent event) {
    }

    private void handleWorkflowExecutionCancelRequested(HistoryEvent event) {
        context.setCancelRequested(true);
        String cause = event.getWorkflowExecutionCancelRequestedEventAttributes().getCause();
        workflow.cancel(cause);
        cancelRequested = true;
    }

    private void handleTimerFired(HistoryEvent event) {
        TimerFiredEventAttributes attributes = event.getTimerFiredEventAttributes();
        String timerId = attributes.getTimerId();
        if (timerId.equals(DecisionsHelper.FORCE_IMMEDIATE_DECISION_TIMER)) {
            return;
        }
        context.handleTimerFired(attributes);
    }

    private void handleTimerStarted(HistoryEvent event) {
        TimerStartedEventAttributes attributes = event.getTimerStartedEventAttributes();
        String timerId = attributes.getTimerId();
        if (timerId.equals(DecisionsHelper.FORCE_IMMEDIATE_DECISION_TIMER)) {
            return;
        }
        decisionsHelper.handleTimerStarted(event);
    }

    private void handleWorkflowExecutionSignaled(HistoryEvent event) {
        assert (event.getEventType() == EventType.WorkflowExecutionSignaled);
        final WorkflowExecutionSignaledEventAttributes signalAttributes = event.getWorkflowExecutionSignaledEventAttributes();
        if (completed) {
            throw new IllegalStateException("Signal received after workflow is closed.");
        }
        this.workflow.handleSignal(signalAttributes.getSignalName(), signalAttributes.getInput(), event.getEventId());
    }

    private void handleDecisionTaskCompleted(HistoryEvent event) {
        decisionsHelper.handleDecisionCompletion(event.getDecisionTaskCompletedEventAttributes());
    }

    public void decide() throws Throwable {
        decideImpl(null);
    }

    private void decideImpl(Functions.Proc query) throws Throwable {
        try {
            long lastNonReplayedEventId = historyHelper.getLastNonReplayEventId();
            // Buffer events until the next DecisionTaskStarted and then process them
            // setting current time to the time of DecisionTaskStarted event
            HistoryHelper.EventsIterator eventsIterator = historyHelper.getEvents();
            // Processes history in batches. One batch per decision. The idea is to push all events to a workflow
            // before running event loop. This way it can make decisions based on the complete information
            // instead of a partial one. For example if workflow waits on two activities to proceed and takes
            // different action if one of them is not ready it would behave differently if both activities
            // are completed before the event loop or if the event loop runs after every activity event.
            do {
                List<HistoryEvent> decisionCompletionToStartEvents = new ArrayList<>();
                while (eventsIterator.hasNext()) {
                    HistoryEvent event = eventsIterator.next();
                    EventType eventType = event.getEventType();
                    if (eventType == EventType.DecisionTaskCompleted) {
                        decisionsHelper.setWorkflowContextData(event.getDecisionTaskCompletedEventAttributes().getExecutionContext());
                    } else if (eventType == EventType.DecisionTaskStarted || !eventsIterator.hasNext()) {
                        // Above check for the end of history is to support queries that get histories
                        // without DecisionTaskStarted being the last event.

                        decisionsHelper.handleDecisionTaskStartedEvent();
                        if (!eventsIterator.isNextDecisionFailed()) {
                            // Cadence timestamp is in nanoseconds
                            long replayCurrentTimeMilliseconds = event.getTimestamp() / MILLION;
                            context.setReplayCurrentTimeMilliseconds(replayCurrentTimeMilliseconds);
                            break;
                        }
                    } else if (eventType != EventType.DecisionTaskScheduled
                            && eventType != EventType.DecisionTaskTimedOut
                            && eventType != EventType.DecisionTaskFailed) {
                        decisionCompletionToStartEvents.add(event);
                    }
                }
                for (HistoryEvent event : decisionCompletionToStartEvents) {
                    if (event.getEventId() >= lastNonReplayedEventId) {
                        context.setReplaying(false);
                    }
                    EventType eventType = event.getEventType();
                    processEvent(event, eventType);
                }
                eventLoop();
                completeWorkflow();
            }
            while (eventsIterator.hasNext());
            //TODO (Cadence): Handle Cadence exception gracefully.
//        catch (AmazonServiceException e) {
//            // We don't want to fail workflow on service exceptions like 500 or throttling
//            // Throwing from here drops decision task which is OK as it is rescheduled after its StartToClose timeout.
//            if (e.getErrorType() == ErrorType.Client && !"ThrottlingException".equals(e.getErrorCode())) {
//                if (log.isErrorEnabled()) {
//                    log.error("Failing workflow " + workflowContext.__getWorkflowExecution(), e);
//                }
//                decisionsHelper.failWorkflowDueToUnexpectedError(e);
//            }
//            else {
//                throw e;
//            }
//        }
        } finally {
            if (query != null) {
                query.apply();
            }
            workflow.close();
        }
    }

    public DecisionsHelper getDecisionsHelper() {
        return decisionsHelper;
    }

    public byte[] query(WorkflowQuery query) throws Throwable {
        AtomicReference<byte[]> result = new AtomicReference<>();
        decideImpl(() -> result.set(workflow.query(query)));
        return result.get();
    }
}
