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

import com.uber.cadence.EntityNotExistsError;
import com.uber.cadence.EventType;
import com.uber.cadence.GetWorkflowExecutionHistoryRequest;
import com.uber.cadence.GetWorkflowExecutionHistoryResponse;
import com.uber.cadence.History;
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.HistoryEventFilterType;
import com.uber.cadence.PollForActivityTaskResponse;
import com.uber.cadence.PollForDecisionTaskRequest;
import com.uber.cadence.PollForDecisionTaskResponse;
import com.uber.cadence.WorkflowExecution;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class TestWorkflowStoreImpl implements TestWorkflowStore {

  private static class HistoryStore {

    private final Lock lock;
    private final Condition newEventsCondition;
    private final ExecutionId id;
    private final List<HistoryEvent> history = new ArrayList<>();
    private boolean completed;

    private HistoryStore(ExecutionId id, Lock lock) {
      this.id = id;
      this.lock = lock;
      this.newEventsCondition = lock.newCondition();
    }

    private void checkNextEventId(long nextEventId) {
      if (nextEventId != history.size()) {
        throw new IllegalStateException(
            "NextEventId=" + nextEventId + ", historySize=" + history.size() + " for " + id);
      }
    }

    void addAllLocked(List<HistoryEvent> events, boolean complete) {
      if (completed) {
        throw new IllegalStateException("Already completed");
      }
      history.addAll(events);
      if (history.isEmpty()) {
        throw new IllegalStateException("Cannot complete with empty history");
      }
      completed = complete;
      newEventsCondition.signal();
    }

    public long getNextEventIdLocked() {
      return history.size();
    }

    public List<HistoryEvent> getEventsLocked() {
      return history;
    }

    public List<HistoryEvent> waitForNewEvents(
        long expectedNextEventId, HistoryEventFilterType filterType) {
      lock.lock();
      try {
        while (true) {
          if (completed || getNextEventIdLocked() > expectedNextEventId) {
            if (filterType == HistoryEventFilterType.CLOSE_EVENT) {
              if (completed) {
                List<HistoryEvent> result = new ArrayList<>(1);
                result.add(history.get(history.size() - 1));
                return result;
              }
              expectedNextEventId = getNextEventIdLocked();
              continue;
            }
            List<HistoryEvent> result =
                new ArrayList<>(((int) (getNextEventIdLocked() - expectedNextEventId)));
            for (int i = (int) expectedNextEventId; i < getNextEventIdLocked(); i++) {
              result.add(history.get(i));
            }
            return result;
          }
          try {
            newEventsCondition.await();
          } catch (InterruptedException e) {
            return null;
          }
        }
      } finally {
        lock.unlock();
      }
    }

    public boolean isCompletedLocked() {
      return completed;
    }
  }

  private final Lock lock = new ReentrantLock();

  private final Map<ExecutionId, HistoryStore> histories = new HashMap<>();

  private final Map<TaskListId, BlockingQueue<PollForActivityTaskResponse>> activityTaskLists =
      new HashMap<>();

  private final Map<TaskListId, BlockingQueue<PollForDecisionTaskResponse>> decisionTaskLists =
      new HashMap<>();

  @Override
  public long save(
      ExecutionId executionId,
      long nextEventId,
      boolean complete,
      List<HistoryEvent> events,
      DecisionTask decisionTask,
      List<ActivityTask> activityTasks) {
    lock.lock();
    try {
      return saveLocked(executionId, nextEventId, complete, events, decisionTask, activityTasks);
    } finally {
      lock.unlock();
    }
  }

  private long saveLocked(
      ExecutionId executionId,
      long nextEventId,
      boolean complete,
      List<HistoryEvent> events,
      DecisionTask decisionTask,
      List<ActivityTask> activityTasks) {
    HistoryStore history = histories.get(executionId);
    if (history == null) {
      if (events.isEmpty() || events.get(0).getEventType() != EventType.WorkflowExecutionStarted) {
        throw new IllegalStateException("No history found for " + executionId);
      }
      history = new HistoryStore(executionId, lock);
      histories.put(executionId, history);
    }
    history.checkNextEventId(nextEventId);
    history.addAllLocked(events, complete);
    if (decisionTask != null) {
      BlockingQueue<PollForDecisionTaskResponse> decisionsQueue =
          getDecisionTaskListQueue(decisionTask.getTaskListId());
      decisionsQueue.add(decisionTask.getTask());
    }
    if (activityTasks != null) {
      for (ActivityTask activityTask : activityTasks) {
        BlockingQueue<PollForActivityTaskResponse> activitiesQueue =
            activityTaskLists.get(activityTask.getTaskListId());
        if (activitiesQueue == null) {
          activitiesQueue = new LinkedBlockingQueue<>();
          activityTaskLists.put(activityTask.getTaskListId(), activitiesQueue);
        }
        activitiesQueue.add(activityTask.getTask());
      }
    }
    return history.getNextEventIdLocked();
  }

  private BlockingQueue<PollForDecisionTaskResponse> getDecisionTaskListQueue(
      TaskListId taskListId) {
    lock.lock();
    try {
      BlockingQueue<PollForDecisionTaskResponse> decisionsQueue = decisionTaskLists.get(taskListId);
      if (decisionsQueue == null) {
        decisionsQueue = new LinkedBlockingQueue<>();
        decisionTaskLists.put(taskListId, decisionsQueue);
      }
      return decisionsQueue;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public PollForDecisionTaskResponse pollForDecisionTask(PollForDecisionTaskRequest pollRequest)
      throws InterruptedException {
    TaskListId taskListId =
        new TaskListId(pollRequest.getDomain(), pollRequest.getTaskList().getName());
    BlockingQueue<PollForDecisionTaskResponse> decisionsQueue =
        getDecisionTaskListQueue(taskListId);
    return decisionsQueue.take();
  }

  @Override
  public GetWorkflowExecutionHistoryResponse getWorkflowExecutionHistory(
      GetWorkflowExecutionHistoryRequest getRequest) throws EntityNotExistsError {
    WorkflowExecution execution = getRequest.getExecution();
    ExecutionId executionId =
        new ExecutionId(getRequest.getDomain(), execution.getWorkflowId(), execution.getRunId());
    HistoryStore history;
    // Used to eliminate the race condition on waitForNewEvents
    long expectedNextEventId;
    lock.lock();
    try {
      history = histories.get(executionId);
      if (history == null) {
        throw new EntityNotExistsError(
            String.format(
                "Workflow execution history not found.  " + "WorkflowId: %s, RunId: %s",
                execution.getWorkflowId(), execution.getRunId()));
      }
      if (!getRequest.isWaitForNewEvent()
          && getRequest.getHistoryEventFilterType() != HistoryEventFilterType.CLOSE_EVENT) {
        return new GetWorkflowExecutionHistoryResponse()
            .setHistory(new History().setEvents(history.getEventsLocked()));
      }
      expectedNextEventId = history.getNextEventIdLocked();
    } finally {
      lock.unlock();
    }
    List<HistoryEvent> events =
        history.waitForNewEvents(expectedNextEventId, getRequest.getHistoryEventFilterType());
    GetWorkflowExecutionHistoryResponse result = new GetWorkflowExecutionHistoryResponse();
    if (events != null) {
      result.setHistory(new History().setEvents(events));
    }
    return result;
  }
}
