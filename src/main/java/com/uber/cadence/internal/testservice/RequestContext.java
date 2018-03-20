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

import com.uber.cadence.HistoryEvent;
import com.uber.cadence.InternalServiceError;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.internal.testservice.TestWorkflowStore.DecisionTask;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class RequestContext {

  @FunctionalInterface
  interface CommitCallback {

    void apply() throws InternalServiceError;
  }

  private final ExecutionId executionId;

  private final long initialEventId;

  private final List<HistoryEvent> events = new ArrayList<>();
  private final List<CommitCallback> commitCallbacks = new ArrayList<>();
  private DecisionTask decisionTask;
  private boolean complete;

  RequestContext(ExecutionId executionId, long initialEventId) {
    this.executionId = Objects.requireNonNull(executionId);
    this.initialEventId = initialEventId;
  }

  /** Returns eventId of the added event; */
  long addEvents(HistoryEvent event) {
    events.add(event);
    return initialEventId + events.size() - 1;
  }

  WorkflowExecution getExecution() {
    return executionId.getExecution();
  }

  String getDomain() {
    return executionId.getDomain();
  }

  void setDecisionTask(DecisionTask decisionTask) {
    this.decisionTask = Objects.requireNonNull(decisionTask);
  }

  public boolean isComplete() {
    return complete;
  }

  public RequestContext setComplete(boolean complete) {
    this.complete = complete;
    return this;
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
    long result = store.save(executionId, initialEventId, complete, events, decisionTask, null);
    fireCallbacks();
    return result;
  }

  private void fireCallbacks() throws InternalServiceError {
    for (CommitCallback callback : commitCallbacks) {
      callback.apply();
      ;
    }
  }

  public ExecutionId getExecutionId() {
    return executionId;
  }
}
