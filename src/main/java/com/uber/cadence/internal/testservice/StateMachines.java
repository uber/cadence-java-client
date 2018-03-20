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

import static com.uber.cadence.internal.testservice.StateMachine.State.COMPLETED;
import static com.uber.cadence.internal.testservice.StateMachine.State.FAILED;
import static com.uber.cadence.internal.testservice.StateMachine.State.NONE;
import static com.uber.cadence.internal.testservice.StateMachine.State.SCHEDULED;
import static com.uber.cadence.internal.testservice.StateMachine.State.STARTED;
import static com.uber.cadence.internal.testservice.StateMachine.State.TIMED_OUT;

import com.uber.cadence.ActivityTaskCompletedEventAttributes;
import com.uber.cadence.ActivityTaskFailedEventAttributes;
import com.uber.cadence.ActivityTaskScheduledEventAttributes;
import com.uber.cadence.ActivityTaskStartedEventAttributes;
import com.uber.cadence.ActivityTaskTimedOutEventAttributes;
import com.uber.cadence.DecisionTaskCompletedEventAttributes;
import com.uber.cadence.DecisionTaskScheduledEventAttributes;
import com.uber.cadence.DecisionTaskStartedEventAttributes;
import com.uber.cadence.EntityNotExistsError;
import com.uber.cadence.EventType;
import com.uber.cadence.GetWorkflowExecutionHistoryRequest;
import com.uber.cadence.History;
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.InternalServiceError;
import com.uber.cadence.PollForActivityTaskRequest;
import com.uber.cadence.PollForActivityTaskResponse;
import com.uber.cadence.PollForDecisionTaskRequest;
import com.uber.cadence.PollForDecisionTaskResponse;
import com.uber.cadence.RecordActivityTaskHeartbeatRequest;
import com.uber.cadence.RespondActivityTaskCompletedByIDRequest;
import com.uber.cadence.RespondActivityTaskCompletedRequest;
import com.uber.cadence.RespondActivityTaskFailedByIDRequest;
import com.uber.cadence.RespondActivityTaskFailedRequest;
import com.uber.cadence.RespondDecisionTaskCompletedRequest;
import com.uber.cadence.ScheduleActivityTaskDecisionAttributes;
import com.uber.cadence.StartWorkflowExecutionRequest;
import com.uber.cadence.TimeoutType;
import com.uber.cadence.internal.testservice.TestWorkflowStore.ActivityTask;
import com.uber.cadence.internal.testservice.TestWorkflowStore.DecisionTask;
import com.uber.cadence.internal.testservice.TestWorkflowStore.TaskListId;
import java.util.List;

class StateMachines {

  static class DecisionTaskData {

    private final TestWorkflowStore store;

    private long previousStartedEventId = -1;

    private PollForDecisionTaskResponse decisionTask;

    private long scheduledEventId = -1;

    DecisionTaskData(TestWorkflowStore store) {
      this.store = store;
    }
  }

  private static class ActivityTaskData {

    PollForActivityTaskResponse activityTask;

    long scheduledEventId = -1;
    long startedEventId = -1;
    byte[] heartbeatDetails;
  }

  static StateMachine<DecisionTaskData> newDecisionStateMachine(TestWorkflowStore store) {
    StateMachine<DecisionTaskData> result = new StateMachine<>(new DecisionTaskData(store));

    result.addTransition(NONE, SCHEDULED, StateMachines::scheduleDecisionTask);
    result.addTransition(SCHEDULED, STARTED, StateMachines::startDecisionTask);
    result.addTransition(STARTED, COMPLETED, StateMachines::completeDecisionTask);
    return result;
  }

  public static StateMachine<?> newActivityStateMachine() {
    StateMachine<ActivityTaskData> result = new StateMachine<>(new ActivityTaskData());
    result.addTransition(NONE, SCHEDULED, StateMachines::scheduleActivityTask);
    result.addTransition(SCHEDULED, STARTED, StateMachines::startActivityTask);
    result.addTransition(STARTED, COMPLETED, StateMachines::completeActivityTask);
    result.addTransition(STARTED, FAILED, StateMachines::failActivityTask);
    result.addTransition(STARTED, STARTED, StateMachines::heartbeatActivityTask);
    result.addTransition(STARTED, TIMED_OUT, StateMachines::timeoutActivityTask);
    return result;
  }

  private static void scheduleActivityTask(
      RequestContext ctx, ActivityTaskData data, ScheduleActivityTaskDecisionAttributes d) {
    ActivityTaskScheduledEventAttributes a =
        new ActivityTaskScheduledEventAttributes()
            .setInput(d.getInput())
            .setActivityId(d.getActivityId())
            .setActivityType(d.getActivityType())
            //        .setDecisionTaskCompletedEventId(d.ge)
            .setDomain(d.getDomain() == null ? ctx.getDomain() : d.getDomain())
            .setHeartbeatTimeoutSeconds(d.getHeartbeatTimeoutSeconds())
            .setScheduleToCloseTimeoutSeconds(d.getScheduleToCloseTimeoutSeconds())
            .setScheduleToStartTimeoutSeconds(d.getScheduleToStartTimeoutSeconds())
            .setStartToCloseTimeoutSeconds(d.getStartToCloseTimeoutSeconds())
            .setTaskList(d.getTaskList());
    HistoryEvent event =
        new HistoryEvent()
            .setEventType(EventType.ActivityTaskScheduled)
            .setActivityTaskScheduledEventAttributes(a);
    long scheduledEventId = ctx.addEvent(event);

    PollForActivityTaskResponse taskResponse =
        new PollForActivityTaskResponse()
            .setActivityType(d.getActivityType())
            .setWorkflowExecution(ctx.getExecution())
            .setActivityId(d.getActivityId())
            .setInput(d.getInput())
            .setHeartbeatTimeoutSeconds(d.getHeartbeatTimeoutSeconds())
            .setScheduleToCloseTimeoutSeconds(d.getScheduleToCloseTimeoutSeconds())
            .setStartToCloseTimeoutSeconds(d.getStartToCloseTimeoutSeconds())
            .setScheduledTimestamp(ctx.currentTimeInNanoseconds());

    TaskListId taskListId = new TaskListId(ctx.getDomain(), d.getTaskList().getName());
    ActivityTask activityTask = new ActivityTask(taskListId, taskResponse);
    ctx.addActivityTask(activityTask);
    ctx.onCommit(
        () -> {
          data.scheduledEventId = scheduledEventId;
          data.activityTask = taskResponse;
        });
  }

  private static void scheduleDecisionTask(
      RequestContext ctx, DecisionTaskData data, StartWorkflowExecutionRequest request) {
    DecisionTaskScheduledEventAttributes a =
        new DecisionTaskScheduledEventAttributes()
            .setStartToCloseTimeoutSeconds(request.getTaskStartToCloseTimeoutSeconds())
            .setTaskList(request.getTaskList());
    HistoryEvent event =
        new HistoryEvent()
            .setEventType(EventType.DecisionTaskScheduled)
            .setDecisionTaskScheduledEventAttributes(a);
    long scheduledEventId = ctx.addEvent(event);
    PollForDecisionTaskResponse decisionTaskResponse = new PollForDecisionTaskResponse();
    decisionTaskResponse.setPreviousStartedEventId(data.previousStartedEventId);
    decisionTaskResponse.setWorkflowExecution(ctx.getExecution());
    decisionTaskResponse.setWorkflowType(request.getWorkflowType());
    TaskListId taskListId = new TaskListId(ctx.getDomain(), request.getTaskList().getName());
    DecisionTask decisionTask = new DecisionTask(taskListId, decisionTaskResponse);
    ctx.setDecisionTask(decisionTask);
    ctx.onCommit(
        () -> {
          data.scheduledEventId = scheduledEventId;
          data.decisionTask = decisionTaskResponse;
        });
  }

  private static void startDecisionTask(
      RequestContext ctx, DecisionTaskData data, PollForDecisionTaskRequest request) {
    DecisionTaskStartedEventAttributes a =
        new DecisionTaskStartedEventAttributes()
            .setIdentity(request.getIdentity())
            .setScheduledEventId(data.scheduledEventId);
    HistoryEvent event =
        new HistoryEvent()
            .setEventType(EventType.DecisionTaskStarted)
            .setDecisionTaskStartedEventAttributes(a);
    long startedEventId = ctx.addEvent(event);
    ctx.onCommit(
        () -> {
          data.decisionTask.setStartedEventId(startedEventId);
          data.decisionTask.setTaskToken(ctx.getExecutionId().toBytes());
          GetWorkflowExecutionHistoryRequest getRequest =
              new GetWorkflowExecutionHistoryRequest()
                  .setDomain(request.getDomain())
                  .setExecution(ctx.getExecution());
          List<HistoryEvent> events = null;
          try {
            events = data.store.getWorkflowExecutionHistory(getRequest).getHistory().getEvents();
          } catch (EntityNotExistsError entityNotExistsError) {
            throw new InternalServiceError(entityNotExistsError.toString());
          }
          data.decisionTask.setHistory(new History().setEvents(events));
          data.previousStartedEventId = startedEventId;
        });
  }

  private static void startActivityTask(
      RequestContext ctx, ActivityTaskData data, PollForActivityTaskRequest request) {
    ActivityTaskStartedEventAttributes a =
        new ActivityTaskStartedEventAttributes()
            .setIdentity(request.getIdentity())
            .setScheduledEventId(data.scheduledEventId);
    HistoryEvent event =
        new HistoryEvent()
            .setEventType(EventType.ActivityTaskStarted)
            .setActivityTaskStartedEventAttributes(a);
    long startedEventId = ctx.addEvent(event);
    ctx.onCommit(
        () -> {
          data.startedEventId = startedEventId;
          data.activityTask.setTaskToken(
              new ActivityId(ctx.getExecutionId(), data.activityTask.getActivityId()).toBytes());
          data.activityTask.setStartedTimestamp(ctx.currentTimeInNanoseconds());
        });
  }

  private static void completeDecisionTask(
      RequestContext ctx, DecisionTaskData data, RespondDecisionTaskCompletedRequest request) {
    DecisionTaskCompletedEventAttributes a =
        new DecisionTaskCompletedEventAttributes()
            .setIdentity(request.getIdentity())
            .setScheduledEventId(data.scheduledEventId);
    HistoryEvent event =
        new HistoryEvent()
            .setEventType(EventType.DecisionTaskCompleted)
            .setDecisionTaskCompletedEventAttributes(a);
    ctx.addEvent(event);
    ctx.onCommit(() -> {});
  }

  private static void completeActivityTask(
      RequestContext ctx, ActivityTaskData data, Object request) {
    if (request instanceof RespondActivityTaskCompletedRequest) {
      completeActivityTaskByTaskToken(ctx, data, (RespondActivityTaskCompletedRequest) request);
    } else if (request instanceof RespondActivityTaskCompletedByIDRequest) {
      completeActivityTaskById(ctx, data, (RespondActivityTaskCompletedByIDRequest) request);
    }
  }

  private static void completeActivityTaskByTaskToken(
      RequestContext ctx, ActivityTaskData data, RespondActivityTaskCompletedRequest request) {
    ActivityTaskCompletedEventAttributes a =
        new ActivityTaskCompletedEventAttributes()
            .setIdentity(request.getIdentity())
            .setScheduledEventId(data.scheduledEventId)
            .setResult(request.getResult())
            .setIdentity(request.getIdentity())
            .setStartedEventId(data.startedEventId);
    HistoryEvent event =
        new HistoryEvent()
            .setEventType(EventType.ActivityTaskCompleted)
            .setActivityTaskCompletedEventAttributes(a);
    ctx.addEvent(event);
    ctx.onCommit(() -> {});
  }

  private static void completeActivityTaskById(
      RequestContext ctx, ActivityTaskData data, RespondActivityTaskCompletedByIDRequest request) {
    ActivityTaskCompletedEventAttributes a =
        new ActivityTaskCompletedEventAttributes()
            .setIdentity(request.getIdentity())
            .setScheduledEventId(data.scheduledEventId)
            .setResult(request.getResult())
            .setIdentity(request.getIdentity())
            .setStartedEventId(data.startedEventId);
    HistoryEvent event =
        new HistoryEvent()
            .setEventType(EventType.ActivityTaskCompleted)
            .setActivityTaskCompletedEventAttributes(a);
    ctx.addEvent(event);
    ctx.onCommit(() -> {});
  }

  private static void failActivityTask(RequestContext ctx, ActivityTaskData data, Object request) {
    if (request instanceof RespondActivityTaskFailedRequest) {
      failActivityTaskByTaskToken(ctx, data, (RespondActivityTaskFailedRequest) request);
    } else if (request instanceof RespondActivityTaskFailedByIDRequest) {
      failActivityTaskById(ctx, data, (RespondActivityTaskFailedByIDRequest) request);
    }
  }

  private static void failActivityTaskByTaskToken(
      RequestContext ctx, ActivityTaskData data, RespondActivityTaskFailedRequest request) {
    ActivityTaskFailedEventAttributes a =
        new ActivityTaskFailedEventAttributes()
            .setIdentity(request.getIdentity())
            .setScheduledEventId(data.scheduledEventId)
            .setDetails(request.getDetails())
            .setReason(request.getReason())
            .setIdentity(request.getIdentity())
            .setStartedEventId(data.startedEventId);
    HistoryEvent event =
        new HistoryEvent()
            .setEventType(EventType.ActivityTaskFailed)
            .setActivityTaskFailedEventAttributes(a);
    ctx.addEvent(event);
    ctx.onCommit(() -> {});
  }

  private static void failActivityTaskById(
      RequestContext ctx, ActivityTaskData data, RespondActivityTaskFailedByIDRequest request) {
    ActivityTaskFailedEventAttributes a =
        new ActivityTaskFailedEventAttributes()
            .setIdentity(request.getIdentity())
            .setScheduledEventId(data.scheduledEventId)
            .setDetails(request.getDetails())
            .setReason(request.getReason())
            .setIdentity(request.getIdentity())
            .setStartedEventId(data.startedEventId);
    HistoryEvent event =
        new HistoryEvent()
            .setEventType(EventType.ActivityTaskFailed)
            .setActivityTaskFailedEventAttributes(a);
    ctx.addEvent(event);
    ctx.onCommit(() -> {});
  }

  private static void timeoutActivityTask(
      RequestContext ctx, ActivityTaskData data, TimeoutType timeoutType) {
    ActivityTaskTimedOutEventAttributes a =
        new ActivityTaskTimedOutEventAttributes()
            .setScheduledEventId(data.scheduledEventId)
            .setDetails(data.heartbeatDetails)
            .setTimeoutType(timeoutType)
            .setStartedEventId(data.startedEventId);
    HistoryEvent event =
        new HistoryEvent()
            .setEventType(EventType.ActivityTaskTimedOut)
            .setActivityTaskTimedOutEventAttributes(a);
    ctx.addEvent(event);
    ctx.onCommit(() -> {});
  }

  private static void heartbeatActivityTask(
      RequestContext nullCtx, ActivityTaskData data, RecordActivityTaskHeartbeatRequest request) {
    data.heartbeatDetails = request.getDetails();
  }
}
