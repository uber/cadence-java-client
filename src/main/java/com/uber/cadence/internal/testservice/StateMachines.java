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
import static com.uber.cadence.internal.testservice.StateMachine.State.NONE;
import static com.uber.cadence.internal.testservice.StateMachine.State.SCHEDULED;
import static com.uber.cadence.internal.testservice.StateMachine.State.STARTED;

import com.uber.cadence.DecisionTaskCompletedEventAttributes;
import com.uber.cadence.DecisionTaskScheduledEventAttributes;
import com.uber.cadence.DecisionTaskStartedEventAttributes;
import com.uber.cadence.EntityNotExistsError;
import com.uber.cadence.EventType;
import com.uber.cadence.GetWorkflowExecutionHistoryRequest;
import com.uber.cadence.History;
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.InternalServiceError;
import com.uber.cadence.PollForDecisionTaskRequest;
import com.uber.cadence.PollForDecisionTaskResponse;
import com.uber.cadence.RespondDecisionTaskCompletedRequest;
import com.uber.cadence.StartWorkflowExecutionRequest;
import com.uber.cadence.internal.testservice.TestWorkflowStore.DecisionTask;
import com.uber.cadence.internal.testservice.TestWorkflowStore.TaskListId;
import java.util.List;

class StateMachines {

  static class DecisionTaskData {
    final TestWorkflowStore store;

    long previousStartedEventId = -1;

    PollForDecisionTaskResponse decisionTask;

    long scheduledEventId = -1;

    DecisionTaskData(TestWorkflowStore store) {
      this.store = store;
    }
  }

  static StateMachine<DecisionTaskData> newDecisionStateMachine(TestWorkflowStore store) {
    StateMachine<DecisionTaskData> result = new StateMachine<>(new DecisionTaskData(store));

    result.addTransition(NONE, SCHEDULED, StateMachines::scheduleDecisionTask);
    result.addTransition(SCHEDULED, STARTED, StateMachines::startDecisionTask);
    result.addTransition(STARTED, COMPLETED, StateMachines::completeDecisionTask);
    return result;
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
    long scheduledEventId = ctx.addEvents(event);
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
    long startedEventId = ctx.addEvents(event);
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
    ctx.addEvents(event);
    ctx.onCommit(
        () -> {
          data.decisionTask = null;
          data.scheduledEventId = -1;
        });
  }
}
