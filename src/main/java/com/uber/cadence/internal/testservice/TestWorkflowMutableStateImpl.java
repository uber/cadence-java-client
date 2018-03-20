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
import com.uber.cadence.BadRequestError;
import com.uber.cadence.CompleteWorkflowExecutionDecisionAttributes;
import com.uber.cadence.Decision;
import com.uber.cadence.EventType;
import com.uber.cadence.FailWorkflowExecutionDecisionAttributes;
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.InternalServiceError;
import com.uber.cadence.PollForActivityTaskRequest;
import com.uber.cadence.PollForActivityTaskResponse;
import com.uber.cadence.PollForDecisionTaskRequest;
import com.uber.cadence.PollForDecisionTaskResponse;
import com.uber.cadence.RespondActivityTaskCompletedRequest;
import com.uber.cadence.RespondDecisionTaskCompletedRequest;
import com.uber.cadence.ScheduleActivityTaskDecisionAttributes;
import com.uber.cadence.StartWorkflowExecutionRequest;
import com.uber.cadence.WorkflowExecutionCompletedEventAttributes;
import com.uber.cadence.WorkflowExecutionFailedEventAttributes;
import com.uber.cadence.WorkflowExecutionStartedEventAttributes;
import com.uber.cadence.internal.testservice.StateMachine.State;
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

    void apply(RequestContext ctx) throws InternalServiceError, BadRequestError;
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
    log.info("Decisions: " + decisions);
    update(
        ctx -> {
          decision.complete(ctx, request);
          for (Decision d : decisions) {
            processDecision(ctx, d);
          }
          decision = null;
          if (ctx.isNeedDecision()) {
            scheduleDecision(ctx);
          }
        });
  }

  private void processDecision(RequestContext ctx, Decision d)
      throws BadRequestError, InternalServiceError {
    switch (d.getDecisionType()) {
      case CompleteWorkflowExecution:
        processCompleteWorkflowExecution(ctx, d.getCompleteWorkflowExecutionDecisionAttributes());
        break;
      case FailWorkflowExecution:
        processFailWorkflowExecution(ctx, d.getFailWorkflowExecutionDecisionAttributes());
        break;
      case ScheduleActivityTask:
        processScheduleActivityTask(ctx, d.getScheduleActivityTaskDecisionAttributes());
        break;
      case RequestCancelActivityTask:
      case StartTimer:
      case CancelTimer:
      case CancelWorkflowExecution:
      case RequestCancelExternalWorkflowExecution:
      case RecordMarker:
      case ContinueAsNewWorkflowExecution:
      case StartChildWorkflowExecution:
      case SignalExternalWorkflowExecution:
        throw new InternalServiceError(
            "Decision " + d.getDecisionType() + " is not yet " + "implemented");
    }
  }

  private void processScheduleActivityTask(
      RequestContext ctx, ScheduleActivityTaskDecisionAttributes a)
      throws BadRequestError, InternalServiceError {
    validateScheduleActivityTask(a);
    String activityId = a.getActivityId();
    StateMachine<?> activity = activities.get(activityId);
    if (activity != null) {
      throw new BadRequestError("Already open activity with " + activityId);
    }
    activity = StateMachines.newActivityStateMachine();
    activities.put(activityId, activity);
    activity.schedule(ctx, a);
  }

  private void validateScheduleActivityTask(ScheduleActivityTaskDecisionAttributes a)
      throws BadRequestError {
    if (a == null) {
      throw new BadRequestError("ScheduleActivityTaskDecisionAttributes is not set on decision.");
    }

    if (a.getTaskList() == null || a.getTaskList().getName().isEmpty()) {
      throw new BadRequestError("TaskList is not set on decision.");
    }
    if (a.getActivityId() == null || a.getActivityId().isEmpty()) {
      throw new BadRequestError("ActivityId is not set on decision.");
    }
    if (a.getActivityType() == null
        || a.getActivityType().getName() == null
        || a.getActivityType().getName().isEmpty()) {
      throw new BadRequestError("ActivityType is not set on decision.");
    }
    if (a.getStartToCloseTimeoutSeconds() <= 0) {
      throw new BadRequestError("A valid StartToCloseTimeoutSeconds is not set on decision.");
    }
    if (a.getScheduleToStartTimeoutSeconds() <= 0) {
      throw new BadRequestError("A valid ScheduleToStartTimeoutSeconds is not set on decision.");
    }
    if (a.getScheduleToCloseTimeoutSeconds() <= 0) {
      throw new BadRequestError("A valid ScheduleToCloseTimeoutSeconds is not set on decision.");
    }
    if (a.getHeartbeatTimeoutSeconds() < 0) {
      throw new BadRequestError("Ac valid HeartbeatTimeoutSeconds is not set on decision.");
    }
  }

  private void processFailWorkflowExecution(
      RequestContext ctx, FailWorkflowExecutionDecisionAttributes d) {
    WorkflowExecutionFailedEventAttributes a =
        new WorkflowExecutionFailedEventAttributes()
            .setDetails(d.getDetails())
            .setReason(d.getReason());
    ctx.addEvent(
        new HistoryEvent()
            .setEventType(EventType.WorkflowExecutionFailed)
            .setWorkflowExecutionFailedEventAttributes(a));
    ctx.completeWorkflow();
  }

  private void processCompleteWorkflowExecution(
      RequestContext ctx, CompleteWorkflowExecutionDecisionAttributes d) {
    WorkflowExecutionCompletedEventAttributes a =
        new WorkflowExecutionCompletedEventAttributes().setResult(d.getResult());
    ctx.addEvent(
        new HistoryEvent()
            .setEventType(EventType.WorkflowExecutionCompleted)
            .setWorkflowExecutionCompletedEventAttributes(a));
    ctx.completeWorkflow();
  }

  private void startWorkflow() throws InternalServiceError {
    update(
        ctx -> {
          addExecutionStartedEvent(ctx);
          scheduleDecision(ctx);
        });
  }

  private void scheduleDecision(RequestContext ctx) throws InternalServiceError {
    if (decision != null) {
      if (decision.getState() == State.SCHEDULED) {
        return; // No need to schedule again
      }
      if (decision.getState() == State.STARTED) {
        ctx.setNeedDecision(true);
        return;
      }
      if (decision.getState() == State.FAILED) {
        decision.schedule(ctx, startRequest);
        return;
      }
      throw new InternalServiceError("unexpected decision state: " + decision.getState());
    }
    this.decision = StateMachines.newDecisionStateMachine(store);
    decision.schedule(ctx, startRequest);
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
    ctx.addEvent(executionStarted);
  }

  @Override
  public void startActivityTask(
      PollForActivityTaskResponse task, PollForActivityTaskRequest pollRequest)
      throws InternalServiceError {
    update(
        ctx -> {
          StateMachine<?> activity = getActivity(task.getActivityId());
          activity.start(ctx, pollRequest);
        });
  }

  private StateMachine<?> getActivity(String activityId) throws InternalServiceError {
    StateMachine<?> activity = activities.get(activityId);
    if (activity == null) {
      throw new InternalServiceError("unknown activityId: " + activityId);
    }
    return activity;
  }

  @Override
  public void completeActivityTask(String activityId, RespondActivityTaskCompletedRequest request)
      throws InternalServiceError {
    update(
        ctx -> {
          StateMachine<?> activity = getActivity(activityId);
          activity.complete(ctx, request);
          activities.remove(activityId);
          scheduleDecision(ctx);
        });
  }
}
