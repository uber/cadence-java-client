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

import com.uber.cadence.ActivityType;
import com.uber.cadence.Header;
import com.uber.cadence.PollForActivityTaskResponse;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.WorkflowType;
import com.uber.cadence.internal.worker.Poller.PollTask;
import com.uber.cadence.serviceclient.IWorkflowService;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

public final class LocallyDispatchedActivityWorker extends ActivityWorker {

  private LocallyDispatchedActivityPollTask ldaPollTask;

  public LocallyDispatchedActivityWorker(
      IWorkflowService service,
      String domain,
      String taskList,
      SingleWorkerOptions options,
      ActivityTaskHandler handler) {
    super(service, domain, taskList, options, handler);
  }

  protected PollTask<PollForActivityTaskResponse> createActivityPollTask() {
    ldaPollTask = new LocallyDispatchedActivityPollTask(options);
    return ldaPollTask;
  }

  protected String GetPollThreadNamePrefix() {
    return "Locally Dispatched Activity Poller taskList=";
  }

  public Function<Task, Boolean> getLocallyDispatchedActivityTaskPoller() {
    return ldaPollTask;
  }

  public static class Task {

    // used to notify the poller the response from server is completed and the task is ready
    protected final CountDownLatch latch = new CountDownLatch(1);
    protected final ByteBuffer taskToken;
    protected final WorkflowExecution workflowExecution;
    protected final String activityId;
    protected final ActivityType activityType;
    protected final ByteBuffer input;
    protected final long scheduledTimestamp;
    protected final int scheduleToCloseTimeoutSeconds;
    protected final long startedTimestamp;
    protected final int startToCloseTimeoutSeconds;
    protected final int heartbeatTimeoutSeconds;
    protected final long scheduledTimestampOfThisAttempt;
    protected final WorkflowType workflowType;
    protected final String workflowDomain;
    protected final Header header;
    protected boolean ready;

    public Task(
        ByteBuffer taskToken,
        WorkflowExecution workflowExecution,
        String activityId,
        ActivityType activityType,
        ByteBuffer input,
        long scheduledTimestamp,
        int scheduleToCloseTimeoutSeconds,
        long startedTimestamp,
        int startToCloseTimeoutSeconds,
        int heartbeatTimeoutSeconds,
        long scheduledTimestampOfThisAttempt,
        WorkflowType workflowType,
        String workflowDomain,
        Header header) {
      this.taskToken = taskToken;
      this.workflowExecution = workflowExecution;
      this.activityId = activityId;
      this.activityType = activityType;
      this.input = input;
      this.scheduledTimestamp = scheduledTimestamp;
      this.scheduleToCloseTimeoutSeconds = scheduleToCloseTimeoutSeconds;
      this.startedTimestamp = startedTimestamp;
      this.startToCloseTimeoutSeconds = startToCloseTimeoutSeconds;
      this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
      this.scheduledTimestampOfThisAttempt = scheduledTimestampOfThisAttempt;
      this.workflowType = workflowType;
      this.workflowDomain = workflowDomain;
      this.header = header;
    }
  }
}
