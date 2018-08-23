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

import com.uber.cadence.DecisionTaskFailedCause;
import com.uber.cadence.PollForDecisionTaskResponse;
import com.uber.cadence.RespondDecisionTaskFailedRequest;
import com.uber.cadence.serviceclient.IWorkflowService;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PollDecisionTaskDispatcher implements Dispatcher<String, PollForDecisionTaskResponse> {

  private static final Logger log = LoggerFactory.getLogger(PollDecisionTaskDispatcher.class);
  private final Map<String, Consumer<PollForDecisionTaskResponse>> subscribers = new HashMap<>();
  private IWorkflowService service;
  private Thread.UncaughtExceptionHandler uncaughtExceptionHandler =
      (t, e) -> log.error("uncaught exception", e);

  public PollDecisionTaskDispatcher(IWorkflowService service) {
    this.service = Objects.requireNonNull(service);
  }

  public PollDecisionTaskDispatcher(
      IWorkflowService service, Thread.UncaughtExceptionHandler exceptionHandler) {
    this.service = Objects.requireNonNull(service);
    if (exceptionHandler != null) {
      this.uncaughtExceptionHandler = exceptionHandler;
    }
  }

  @Override
  public void accept(PollForDecisionTaskResponse t) {
    synchronized (this) {
      String taskListName = t.getWorkflowExecutionTaskList().getName();
      if (subscribers.containsKey(taskListName)) {
        subscribers.get(taskListName).accept(t);
      } else {
        RespondDecisionTaskFailedRequest request = new RespondDecisionTaskFailedRequest();
        request.setTaskToken(t.taskToken);
        request.setCause(DecisionTaskFailedCause.RESET_STICKY_TASKLIST);
        String message =
            String.format(
                "No handler is subscribed for the PollForDecisionTaskResponse.WorkflowExecutionTaskList %s",
                taskListName);
        request.setDetails(message.getBytes());
        log.warn(message);

        try {
          service.RespondDecisionTaskFailed(request);

        } catch (Exception e) {
          uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), e);
        }
      }
    }
  }

  @Override
  public void subscribe(String taskList, Consumer<PollForDecisionTaskResponse> consumer) {
    synchronized (this) {
      subscribers.put(taskList, consumer);
    }
  }
}
