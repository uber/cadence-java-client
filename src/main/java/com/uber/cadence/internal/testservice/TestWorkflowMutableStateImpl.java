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

package com.uber.cadence.internal.testservice;

import com.google.common.base.Throwables;
import com.uber.cadence.CompleteWorkflowExecutionDecisionAttributes;
import com.uber.cadence.Decision;
import com.uber.cadence.EventType;
import com.uber.cadence.FailWorkflowExecutionDecisionAttributes;
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.InternalServiceError;
import com.uber.cadence.PollForDecisionTaskRequest;
import com.uber.cadence.PollForDecisionTaskResponse;
import com.uber.cadence.RespondDecisionTaskCompletedRequest;
import com.uber.cadence.StartWorkflowExecutionRequest;
import com.uber.cadence.WorkflowExecutionCompletedEventAttributes;
import com.uber.cadence.WorkflowExecutionFailedEventAttributes;
import com.uber.cadence.WorkflowExecutionStartedEventAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TestWorkflowMutableStateImpl implements TestWorkflowMutableState {

  private enum DecisionTaskState {
    NONE,
    SCHEDULED,
    STARTED,
    COMPLETED,
    FAILED
  }

  @FunctionalInterface
  private interface UpdateProcedure {

    void apply(RequestContext ctx) throws InternalServiceError;
  }

  private static final Logger log = LoggerFactory.getLogger(TestWorkflowMutableStateImpl.class);
  private static final long NANOS_PER_MILLIS = 1_000_000;

  private final Lock lock = new ReentrantLock();
  private final LongSupplier clock;
  private final ExecutionId executionId;
  private final TestWorkflowStore store;
  private final StartWorkflowExecutionRequest startRequest;
  private long nextEventId;

  private final Map<String, StateMachine<?>> activities = new HashMap<>();

  private StateMachine<?> decision;

  TestWorkflowMutableStateImpl(
      StartWorkflowExecutionRequest startRequest, TestWorkflowStore store, LongSupplier clock)
      throws InternalServiceError {
    this.startRequest = startRequest;
    String runId = UUID.randomUUID().toString();
    this.executionId =
        new ExecutionId(startRequest.getDomain(), startRequest.getWorkflowId(), runId);
    this.store = store;
    this.clock = clock;
    this.decision = StateMachines.newDecisionStateMachine(store);
    startWorkflow();
  }

  private void update(UpdateProcedure updater) throws InternalServiceError {
    RequestContext ctx = new RequestContext(executionId, nextEventId);
    lock.lock();
    try {
      {
        updater.apply(ctx);
        nextEventId = ctx.commitChanges(store);
      }
    } catch (InternalServiceError e) {
      throw e;
    } catch (Exception e) {
      throw new InternalServiceError(Throwables.getStackTraceAsString(e));
    } finally {
      lock.unlock();
    }
  }

  @Override
  public ExecutionId getExecutionId() {
    return executionId;
  }

  @Override
  public void startDecisionTask(
      PollForDecisionTaskResponse task, PollForDecisionTaskRequest pollRequest)
      throws InternalServiceError {
    update(ctx -> decision.start(ctx, pollRequest));
  }

  @Override
  public void completeDecisionTask(RespondDecisionTaskCompletedRequest request)
      throws InternalServiceError {
    List<Decision> decisions = request.getDecisions();
    update(
        ctx -> {
          decision.complete(ctx, request);
          for (Decision d : decisions) {
            processDecision(ctx, d);
          }
        });
  }

  private void processDecision(RequestContext ctx, Decision d) {
    switch (d.getDecisionType()) {
      case CompleteWorkflowExecution:
        processCompleteWorkflowExecution(ctx, d.getCompleteWorkflowExecutionDecisionAttributes());
        break;
      case FailWorkflowExecution:
        processFailWorkflowExecution(ctx, d.getFailWorkflowExecutionDecisionAttributes());
        break;
      case ScheduleActivityTask:
      case RequestCancelActivityTask:
      case StartTimer:
      case CancelTimer:
      case CancelWorkflowExecution:
      case RequestCancelExternalWorkflowExecution:
      case RecordMarker:
      case ContinueAsNewWorkflowExecution:
      case StartChildWorkflowExecution:
      case SignalExternalWorkflowExecution:
        throw new UnsupportedOperationException("Decision " + d.getDecisionType() + " is not yet "
            + "implemented");
    }
  }

  private void processFailWorkflowExecution(RequestContext ctx,
      FailWorkflowExecutionDecisionAttributes d) {
    WorkflowExecutionFailedEventAttributes a = new WorkflowExecutionFailedEventAttributes()
        .setDetails(d.getDetails())
        .setReason(d.getReason());
    ctx.addEvents(new HistoryEvent().setEventType(EventType.WorkflowExecutionFailed)
        .setWorkflowExecutionFailedEventAttributes(a));
    ctx.completeWorkflow();
  }

  private void processCompleteWorkflowExecution(
      RequestContext ctx, CompleteWorkflowExecutionDecisionAttributes d) {
    WorkflowExecutionCompletedEventAttributes a =
        new WorkflowExecutionCompletedEventAttributes().setResult(d.getResult());
    ctx.addEvents(
        new HistoryEvent()
            .setEventType(EventType.WorkflowExecutionCompleted)
            .setWorkflowExecutionCompletedEventAttributes(a));
    ctx.completeWorkflow();
  }

  private void startWorkflow() throws InternalServiceError {
    update(
        ctx -> {
          addExecutionStartedEvent(ctx);
          decision.schedule(ctx, startRequest);
        });
  }

  private void addExecutionStartedEvent(RequestContext ctx) {
    WorkflowExecutionStartedEventAttributes a =
        new WorkflowExecutionStartedEventAttributes()
            .setInput(startRequest.getInput())
            .setExecutionStartToCloseTimeoutSeconds(
                startRequest.getExecutionStartToCloseTimeoutSeconds())
            .setIdentity(startRequest.getIdentity())
            .setTaskList(startRequest.getTaskList())
            .setWorkflowType(startRequest.getWorkflowType())
            .setTaskStartToCloseTimeoutSeconds(startRequest.getTaskStartToCloseTimeoutSeconds());
    HistoryEvent executionStarted =
        new HistoryEvent()
            .setEventType(EventType.WorkflowExecutionStarted)
            .setWorkflowExecutionStartedEventAttributes(a);
    ctx.addEvents(executionStarted);
  }
}
