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

import com.uber.cadence.EventType;
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.PollForActivityTaskResponse;
import com.uber.cadence.PollForDecisionTaskRequest;
import com.uber.cadence.PollForDecisionTaskResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class TestWorkflowStoreImpl implements TestWorkflowStore {

  private final Map<ExecutionId, List<HistoryEvent>> histories = new HashMap<>();

  private final Map<TaskListId, BlockingQueue<PollForActivityTaskResponse>> activityTaskLists =
      new HashMap<>();

  private final Map<TaskListId, BlockingQueue<PollForDecisionTaskResponse>> decisionTaskLists =
      new HashMap<>();

  @Override
  public synchronized long save(
      ExecutionId execution,
      long nextEventId,
      List<HistoryEvent> events,
      DecisionTask decisionTask,
      List<ActivityTask> activityTasks) {
    List<HistoryEvent> history = histories.get(execution);
    if (history == null) {
      if (events.isEmpty() || events.get(0).getEventType() != EventType.WorkflowExecutionStarted) {
        throw new IllegalStateException("No history found for " + execution);
      }
      history = new ArrayList<>();
      histories.put(execution, history);
    }
    if (nextEventId != history.size()) {
      throw new IllegalStateException(
          "NextEventId=" + nextEventId + ", historySize=" + history.size() + " for " + execution);
    }
    history.addAll(events);
    BlockingQueue<PollForDecisionTaskResponse> decisionsQueue =
        getDecisionTaskListQueue(decisionTask.getTaskListId());
    decisionsQueue.add(decisionTask.getTask());
    for (ActivityTask activityTask : activityTasks) {
      BlockingQueue<PollForActivityTaskResponse> activitiesQueue =
          activityTaskLists.get(activityTask.getTaskListId());
      if (activitiesQueue == null) {
        activitiesQueue = new LinkedBlockingQueue<>();
        activityTaskLists.put(activityTask.getTaskListId(), activitiesQueue);
      }
      activitiesQueue.add(activityTask.getTask());
    }
    return history.size();
  }

  private synchronized BlockingQueue<PollForDecisionTaskResponse> getDecisionTaskListQueue(
      TaskListId taskListId) {
    BlockingQueue<PollForDecisionTaskResponse> decisionsQueue = decisionTaskLists.get(taskListId);
    if (decisionsQueue == null) {
      decisionsQueue = new LinkedBlockingQueue<>();
      decisionTaskLists.put(taskListId, decisionsQueue);
    }
    return decisionsQueue;
  }

  @Override
  public PollForDecisionTaskResponse pollForDecisionTask(PollForDecisionTaskRequest pollRequest)
      throws InterruptedException {
    TaskListId taskListId =
        new TaskListId(pollRequest.getDomain(), pollRequest.getTaskList().getName());
    BlockingQueue<PollForDecisionTaskResponse> decisionsQueue =
        getDecisionTaskListQueue(taskListId);
    PollForDecisionTaskResponse result = decisionsQueue.poll(60, TimeUnit.SECONDS);
    if (result == null) {
      result = new PollForDecisionTaskResponse();
    }
    return result;
  }
}
