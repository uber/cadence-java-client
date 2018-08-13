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

package com.uber.cadence.internal.worker;

import com.uber.cadence.PollForDecisionTaskResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class PollDecisionTaskDispatcher implements Consumer<PollForDecisionTaskResponse> {

  private final Map<String, Consumer<PollForDecisionTaskResponse>> subscribers = new HashMap<>();

  @Override
  public void accept(PollForDecisionTaskResponse t) {
    // String taskListName = t.get.getWorkflowExecution()..getWorkflowExecutionTaskList().name;
    synchronized (this) {
      if (subscribers.containsKey(t.getWorkflowExecutionTaskList().name)) {
        subscribers.get(t.getWorkflowExecutionTaskList().name).accept(t);
      }
      // what to do if we don't find the tasklist?
    }
  }

  public void Subscribe(String tasklist, Consumer<PollForDecisionTaskResponse> consumer) {
    synchronized (this) {
      subscribers.put(tasklist, consumer);
    }
  }
}
