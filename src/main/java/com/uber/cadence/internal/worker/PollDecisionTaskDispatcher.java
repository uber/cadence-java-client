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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class PollDecisionTaskDispatcher implements Consumer<PollForDecisionTaskResponse> {

  private static final Logger log = LoggerFactory.getLogger(PollDecisionTaskDispatcher.class);
  private final Map<String, Consumer<PollForDecisionTaskResponse>> subscribers = new HashMap<>();

  @Override
  public void accept(PollForDecisionTaskResponse t) {
    synchronized (this) {
      String taskListName = t.getWorkflowExecutionTaskList().name;
      if (subscribers.containsKey(taskListName)) {
        subscribers.get(t.getWorkflowExecutionTaskList().name).accept(t);
      }
      else {
        log.warn(String.format("No handler is subscribed for the PollForDecisionTaskResponse.WorkflowExecutionTaskList %s", taskListName));
      }
    }
  }

  public void Subscribe(String tasklist, Consumer<PollForDecisionTaskResponse> consumer) {
    synchronized (this) {
      subscribers.put(tasklist, consumer);
    }
  }
}
