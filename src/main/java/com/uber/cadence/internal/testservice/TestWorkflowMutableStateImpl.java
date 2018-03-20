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
import com.uber.cadence.CancelWorkflowExecutionDecisionAttributes;
import com.uber.cadence.CompleteWorkflowExecutionDecisionAttributes;
import com.uber.cadence.Decision;
import com.uber.cadence.EntityNotExistsError;
import com.uber.cadence.EventType;
import com.uber.cadence.FailWorkflowExecutionDecisionAttributes;
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.InternalServiceError;
import com.uber.cadence.PollForActivityTaskRequest;
import com.uber.cadence.PollForActivityTaskResponse;
import com.uber.cadence.PollForDecisionTaskRequest;
import com.uber.cadence.PollForDecisionTaskResponse;
import com.uber.cadence.RecordActivityTaskHeartbeatRequest;
import com.uber.cadence.RecordActivityTaskHeartbeatResponse;
import com.uber.cadence.RequestCancelActivityTaskDecisionAttributes;
import com.uber.cadence.RequestCancelWorkflowExecutionRequest;
import com.uber.cadence.RespondActivityTaskCanceledByIDRequest;
import com.uber.cadence.RespondActivityTaskCanceledRequest;
import com.uber.cadence.RespondActivityTaskCompletedByIDRequest;
import com.uber.cadence.RespondActivityTaskCompletedRequest;
import com.uber.cadence.RespondActivityTaskFailedByIDRequest;
import com.uber.cadence.RespondActivityTaskFailedRequest;
import com.uber.cadence.RespondDecisionTaskCompletedRequest;
import com.uber.cadence.ScheduleActivityTaskDecisionAttributes;
import com.uber.cadence.SignalWorkflowExecutionRequest;
import com.uber.cadence.StartWorkflowExecutionRequest;
import com.uber.cadence.TimeoutType;
import com.uber.cadence.WorkflowExecutionSignaledEventAttributes;
import com.uber.cadence.internal.testservice.StateMachine.State;
import com.uber.cadence.internal.testservice.StateMachines.ActivityTaskData;
import java.util.ArrayList;
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

  @FunctionalInterface
  private interface UpdateProcedure {

    void apply(RequestContext ctx)
        throws InternalServiceError, BadRequestError, EntityNotExistsError;
  }

  private static final Logger log = LoggerFactory.getLogger(TestWorkflowMutableStateImpl.class);

  private final Lock lock = new ReentrantLock();
  private final LongSupplier clock;
  private final ExecutionId executionId;
  private final TestWorkflowStore store;
  private final StartWorkflowExecutionRequest startRequest;
  private long nextEventId;
  private boolean cancelRequested;
  private final List<RequestContext> concurrentToDecision = new ArrayList<>();
  private final Map<String, StateMachine<?>> activities = new HashMap<>();
  private StateMachine<?> workflow;
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
    this.workflow = StateMachines.newWorkflowStateMachine();
    startWorkflow();
  }

  private void update(UpdateProcedure updater) throws InternalServiceError, EntityNotExistsError {
    update(false, updater);
  }

  private void completeDecisionUpdate(UpdateProcedure updater)
      throws InternalServiceError, EntityNotExistsError {
    update(true, updater);
  }

  private void update(boolean completeDecisionUpdate, UpdateProcedure updater)
      throws InternalServiceError, EntityNotExistsError {
    lock.lock();
    try {
      boolean concurrentDecision =
          !completeDecisionUpdate && (decision != null && decision.getState() == State.STARTED);
      RequestContext ctx = new RequestContext(clock, executionId, nextEventId);
      updater.apply(ctx);
      if (concurrentDecision) {
        concurrentToDecision.add(ctx);
        ctx.fireCallbacks();
      } else {
        nextEventId = ctx.commitChanges(store);
      }
    } catch (InternalServiceError | EntityNotExistsError e) {
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
      throws InternalServiceError, EntityNotExistsError {
    update(ctx -> decision.start(ctx, pollRequest, 0));
  }

  @Override
  public void completeDecisionTask(RespondDecisionTaskCompletedRequest request)
      throws InternalServiceError, EntityNotExistsError {
    List<Decision> decisions = request.getDecisions();
    log.info("Decisions: " + decisions);
    completeDecisionUpdate(
        ctx -> {
          decision.complete(ctx, request, 0);
          long decisionTaskCompletedId = ctx.getNextEventId() - 1;
          for (Decision d : decisions) {
            processDecision(ctx, d, decisionTaskCompletedId);
          }
          for (RequestContext deferredCtx : this.concurrentToDecision) {
            ctx.add(deferredCtx);
          }
          this.decision = null;
          boolean completed =
              workflow.getState() == State.COMPLETED
                  || workflow.getState() == State.FAILED
                  || workflow.getState() == State.CANCELED;
          if (!completed && (ctx.isNeedDecision() || !this.concurrentToDecision.isEmpty())) {
            scheduleDecision(ctx);
          }
          this.concurrentToDecision.clear();
        });
  }

  private void processDecision(RequestContext ctx, Decision d, long decisionTaskCompletedId)
      throws BadRequestError, InternalServiceError, EntityNotExistsError {
    switch (d.getDecisionType()) {
      case CompleteWorkflowExecution:
        processCompleteWorkflowExecution(
            ctx, d.getCompleteWorkflowExecutionDecisionAttributes(), decisionTaskCompletedId);
        break;
      case FailWorkflowExecution:
        processFailWorkflowExecution(
            ctx, d.getFailWorkflowExecutionDecisionAttributes(), decisionTaskCompletedId);
        break;
      case CancelWorkflowExecution:
        processCancelWorkflowExecution(
            ctx, d.getCancelWorkflowExecutionDecisionAttributes(), decisionTaskCompletedId);
        break;
      case ScheduleActivityTask:
        processScheduleActivityTask(
            ctx, d.getScheduleActivityTaskDecisionAttributes(), decisionTaskCompletedId);
        break;
      case RequestCancelActivityTask:
        processRequestCancelActivityTask(
            ctx, d.getRequestCancelActivityTaskDecisionAttributes(), decisionTaskCompletedId);
        break;
      case StartTimer:
      case CancelTimer:
      case RequestCancelExternalWorkflowExecution:
      case RecordMarker:
      case ContinueAsNewWorkflowExecution:
      case StartChildWorkflowExecution:
      case SignalExternalWorkflowExecution:
        throw new InternalServiceError(
            "Decision " + d.getDecisionType() + " is not yet " + "implemented");
    }
  }

  private void processRequestCancelActivityTask(
      RequestContext ctx,
      RequestCancelActivityTaskDecisionAttributes a,
      long decisionTaskCompletedId)
      throws EntityNotExistsError, InternalServiceError {
    String activityId = a.getActivityId();
    StateMachine<?> activity = activities.get(activityId);
    if (activity == null) {
      throw new EntityNotExistsError("ActivityId: " + activityId);
    }
    activity.requestCancellation(ctx, a, decisionTaskCompletedId);
  }

  private void processScheduleActivityTask(
      RequestContext ctx, ScheduleActivityTaskDecisionAttributes a, long decisionTaskCompletedId)
      throws BadRequestError, InternalServiceError {
    validateScheduleActivityTask(a);
    String activityId = a.getActivityId();
    StateMachine<?> activity = activities.get(activityId);
    if (activity != null) {
      throw new BadRequestError("Already open activity with " + activityId);
    }
    activity = StateMachines.newActivityStateMachine();
    activities.put(activityId, activity);
    activity.schedule(ctx, a, decisionTaskCompletedId);
    ctx.addTimer(
        a.getScheduleToCloseTimeoutSeconds(),
        () -> timeoutActivity(activityId, TimeoutType.SCHEDULE_TO_CLOSE));
    ctx.addTimer(
        a.getScheduleToStartTimeoutSeconds(),
        () -> timeoutActivity(activityId, TimeoutType.SCHEDULE_TO_START));
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
      RequestContext ctx, FailWorkflowExecutionDecisionAttributes d, long decisionTaskCompletedId)
      throws InternalServiceError {
    workflow.fail(ctx, d, decisionTaskCompletedId);
  }

  private void processCompleteWorkflowExecution(
      RequestContext ctx,
      CompleteWorkflowExecutionDecisionAttributes d,
      long decisionTaskCompletedId)
      throws InternalServiceError {
    workflow.complete(ctx, d, decisionTaskCompletedId);
  }

  private void processCancelWorkflowExecution(
      RequestContext ctx, CancelWorkflowExecutionDecisionAttributes d, long decisionTaskCompletedId)
      throws InternalServiceError {
    workflow.reportCancellation(ctx, d, decisionTaskCompletedId);
  }

  private void startWorkflow() throws InternalServiceError {
    try {
      update(
          ctx -> {
            workflow.start(ctx, startRequest, 0);
            scheduleDecision(ctx);
          });
    } catch (EntityNotExistsError entityNotExistsError) {
      throw new InternalServiceError(Throwables.getStackTraceAsString(entityNotExistsError));
    }
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
      if (decision.getState() == State.FAILED || decision.getState() == State.COMPLETED) {
        decision.schedule(ctx, startRequest, 0);
        return;
      }
      throw new InternalServiceError("unexpected decision state: " + decision.getState());
    }
    this.decision = StateMachines.newDecisionStateMachine(store);
    decision.schedule(ctx, startRequest, 0);
  }

  @Override
  public void startActivityTask(
      PollForActivityTaskResponse task, PollForActivityTaskRequest pollRequest)
      throws InternalServiceError, EntityNotExistsError {
    update(
        ctx -> {
          String activityId = task.getActivityId();
          StateMachine<ActivityTaskData> activity = getActivity(activityId);
          activity.start(ctx, pollRequest, 0);
          int timeoutSeconds = activity.getData().scheduledEvent.getStartToCloseTimeoutSeconds();
          if (timeoutSeconds > 0) {
            ctx.addTimer(
                timeoutSeconds, () -> timeoutActivity(activityId, TimeoutType.START_TO_CLOSE));
          }
        });
  }

  @Override
  public void completeActivityTask(String activityId, RespondActivityTaskCompletedRequest request)
      throws InternalServiceError, EntityNotExistsError {
    update(
        ctx -> {
          StateMachine<?> activity = getActivity(activityId);
          activity.complete(ctx, request, 0);
          activities.remove(activityId);
          scheduleDecision(ctx);
        });
  }

  @Override
  public void completeActivityTaskById(
      String activityId, RespondActivityTaskCompletedByIDRequest request)
      throws InternalServiceError, EntityNotExistsError {
    update(
        ctx -> {
          StateMachine<?> activity = getActivity(activityId);
          activity.complete(ctx, request, 0);
          activities.remove(activityId);
          scheduleDecision(ctx);
        });
  }

  @Override
  public void failActivityTask(String activityId, RespondActivityTaskFailedRequest request)
      throws InternalServiceError, EntityNotExistsError {
    update(
        ctx -> {
          StateMachine<?> activity = getActivity(activityId);
          activity.fail(ctx, request, 0);
          activities.remove(activityId);
          scheduleDecision(ctx);
        });
  }

  @Override
  public void failActivityTaskById(String activityId, RespondActivityTaskFailedByIDRequest request)
      throws EntityNotExistsError, InternalServiceError {
    update(
        ctx -> {
          StateMachine<?> activity = getActivity(activityId);
          activity.fail(ctx, request, 0);
          activities.remove(activityId);
          scheduleDecision(ctx);
        });
  }

  @Override
  public void cancelActivityTask(String activityId, RespondActivityTaskCanceledRequest request)
      throws EntityNotExistsError, InternalServiceError {
    update(
        ctx -> {
          StateMachine<?> activity = getActivity(activityId);
          activity.reportCancellation(ctx, request, 0);
          activities.remove(activityId);
          scheduleDecision(ctx);
        });
  }

  @Override
  public void cancelActivityTaskById(
      String activityId, RespondActivityTaskCanceledByIDRequest request)
      throws EntityNotExistsError, InternalServiceError {
    update(
        ctx -> {
          StateMachine<?> activity = getActivity(activityId);
          activity.reportCancellation(ctx, request, 0);
          activities.remove(activityId);
          scheduleDecision(ctx);
        });
  }

  @Override
  public RecordActivityTaskHeartbeatResponse heartbeatActivityTask(
      String activityId, RecordActivityTaskHeartbeatRequest request)
      throws InternalServiceError, EntityNotExistsError {
    RecordActivityTaskHeartbeatResponse result = new RecordActivityTaskHeartbeatResponse();
    lock.lock();
    try {
      StateMachine<?> activity = getActivity(activityId);
      activity.update(request);
      if (activity.getState() == State.CANCELLATION_REQUESTED) {
        result.setCancelRequested(true);
      }
    } catch (InternalServiceError | EntityNotExistsError e) {
      throw e;
    } catch (Exception e) {
      throw new InternalServiceError(Throwables.getStackTraceAsString(e));
    } finally {
      lock.unlock();
    }
    return result;
  }

  @Override
  public void timeoutActivity(String activityId, TimeoutType timeoutType) {
    try {
      update(
          ctx -> {
            StateMachine<?> activity = getActivity(activityId);
            switch (timeoutType) {
              case START_TO_CLOSE:
                break;
              case SCHEDULE_TO_START:
                if (activity.getState() != State.SCHEDULED) {
                  log.trace("Ignoring SCHEDULE_TO_START timeout for activityId:" + activityId);
                  return;
                }
                break;
              case SCHEDULE_TO_CLOSE:
                break;
              case HEARTBEAT:
                break;
            }
            activity.timeout(ctx, timeoutType);
            activities.remove(activityId);
            scheduleDecision(ctx);
          });
    } catch (EntityNotExistsError e) {
      if (log.isTraceEnabled()) {
        log.trace("Ignoring activity timeout for activityId:" + activityId);
      }
    } catch (Exception e) {
      // Cannot fail to timer threads
      log.error("Failure trying to timeout an activity", e);
    }
  }

  @Override
  public void signal(SignalWorkflowExecutionRequest signalRequest)
      throws EntityNotExistsError, InternalServiceError {
    update(
        ctx -> {
          addExecutionSignaledEvent(ctx, signalRequest);
          scheduleDecision(ctx);
        });
  }

  @Override
  public void requestCancelWorkflowExecution(RequestCancelWorkflowExecutionRequest cancelRequest)
      throws EntityNotExistsError, InternalServiceError {
    update(
        ctx -> {
          cancelRequested = true;
          workflow.requestCancellation(ctx, cancelRequest, 0);
          scheduleDecision(ctx);
        });
  }

  private void addExecutionSignaledEvent(
      RequestContext ctx, SignalWorkflowExecutionRequest signalRequest) {
    WorkflowExecutionSignaledEventAttributes a =
        new WorkflowExecutionSignaledEventAttributes()
            .setInput(startRequest.getInput())
            .setIdentity(startRequest.getIdentity())
            .setInput(signalRequest.getInput())
            .setSignalName(signalRequest.getSignalName());
    HistoryEvent executionSignaled =
        new HistoryEvent()
            .setEventType(EventType.WorkflowExecutionSignaled)
            .setWorkflowExecutionSignaledEventAttributes(a);
    ctx.addEvent(executionSignaled);
  }

  @SuppressWarnings("unchecked")
  private StateMachine<ActivityTaskData> getActivity(String activityId)
      throws EntityNotExistsError {
    StateMachine<?> activity = activities.get(activityId);
    if (activity == null) {
      throw new EntityNotExistsError("unknown activityId: " + activityId);
    }
    return (StateMachine<ActivityTaskData>) activity;
  }
}
