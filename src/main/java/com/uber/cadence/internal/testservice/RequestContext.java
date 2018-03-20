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
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.InternalServiceError;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.internal.testservice.TestWorkflowStore.ActivityTask;
import com.uber.cadence.internal.testservice.TestWorkflowStore.DecisionTask;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;

final class RequestContext {

  @FunctionalInterface
  interface CommitCallback {

    void apply() throws InternalServiceError;
  }

  static final class Timer {

    private final long delaySeconds;
    private final Runnable callback;

    Timer(long delaySeconds, Runnable callback) {
      this.delaySeconds = delaySeconds;
      this.callback = callback;
    }

    long getDelaySeconds() {
      return delaySeconds;
    }

    Runnable getCallback() {
      return callback;
    }
  }

  private static final long NANOS_PER_MILLIS = 1_000_000;

  private final LongSupplier clock;

  private final ExecutionId executionId;

  private final long initialEventId;

  private final List<HistoryEvent> events = new ArrayList<>();
  private final List<CommitCallback> commitCallbacks = new ArrayList<>();
  private DecisionTask decisionTask;
  private final List<ActivityTask> activityTasks = new ArrayList<>();
  private final List<Timer> timers = new ArrayList<>();
  private boolean workflowCompleted;
  private boolean needDecision;

  /**
   * Creates an instance of the RequestContext
   *
   * @param clock clock used to timestamp events and schedule timers.
   * @param executionId id of the execution being updated
   * @param initialEventId expected id of the next event added to the history
   */
  RequestContext(LongSupplier clock, ExecutionId executionId, long initialEventId) {
    this.clock = clock;
    this.executionId = Objects.requireNonNull(executionId);
    this.initialEventId = initialEventId;
  }

  void add(RequestContext ctx) {
    this.activityTasks.addAll(ctx.getActivityTasks());
    this.timers.addAll(ctx.getTimers());
    this.events.addAll(ctx.getEvents());
  }

  long currentTimeInNanoseconds() {
    return clock.getAsLong() * NANOS_PER_MILLIS;
  }

  /** Returns eventId of the added event; */
  long addEvent(HistoryEvent event) {
    requireNotCompleted();
    long eventId = initialEventId + events.size();
    event.setEventId(eventId);
    events.add(event);
    return eventId;
  }

  private void requireNotCompleted() {
    if (workflowCompleted) {
      throw new IllegalStateException("workflow completed");
    }
  }

  WorkflowExecution getExecution() {
    return executionId.getExecution();
  }

  String getDomain() {
    return executionId.getDomain();
  }

  public long getInitialEventId() {
    return initialEventId;
  }

  /**
   * Decision needed, but there is one already running. So initiate another one as soon as it
   * completes.
   */
  void setNeedDecision(boolean needDecision) {
    this.needDecision = needDecision;
  }

  boolean isNeedDecision() throws EntityNotExistsError {
    return needDecision;
  }

  void setDecisionTask(DecisionTask decisionTask) {
    requireNotCompleted();
    this.decisionTask = Objects.requireNonNull(decisionTask);
  }

  void addActivityTask(ActivityTask activityTask) {
    this.activityTasks.add(activityTask);
  }

  void addTimer(long delaySeconds, Runnable callback) {
    Timer timer = new Timer(delaySeconds, callback);
    this.timers.add(timer);
  }

  public List<Timer> getTimers() {
    return timers;
  }

  List<ActivityTask> getActivityTasks() {
    return activityTasks;
  }

  void completeWorkflow() {
    workflowCompleted = true;
  }

  boolean isWorkflowCompleted() {
    return workflowCompleted;
  }

  DecisionTask getDecisionTask() {
    return decisionTask;
  }

  List<HistoryEvent> getEvents() {
    return events;
  }

  void onCommit(CommitCallback callback) {
    commitCallbacks.add(callback);
  }

  /** @return nextEventId */
  long commitChanges(TestWorkflowStore store) throws InternalServiceError {
    long result = store.save(this);
    fireCallbacks();
    return result;
  }

  private void fireCallbacks() throws InternalServiceError {
    for (CommitCallback callback : commitCallbacks) {
      callback.apply();
    }
  }

  ExecutionId getExecutionId() {
    return executionId;
  }
}
