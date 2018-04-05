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

package com.uber.cadence.workflow;

import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.activity.ActivityOptions;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

public interface WorkflowInterceptor {

  final class WorkflowResult<R> {

    private final Promise<R> result;
    private final String runId;

    public WorkflowResult(Promise<R> result, String runId) {
      this.result = result;
      this.runId = runId;
    }

    public Promise<R> getResult() {
      return result;
    }

    public String getRunId() {
      return runId;
    }
  }

  <R> Promise<R> executeActivity(String activityName, Class<R> returnType, Object[] args,
      ActivityOptions options);

  <R> WorkflowResult<R> executeChildWorkflow(String workflowType, Class<R> returnType,
      Object[] args, ChildWorkflowOptions options);

  void signalWorkflow(WorkflowExecution execution, String signalName, Object[] args,);

  void cancelWorkflow(WorkflowExecution execution);

  void sleep(Duration duration);

  boolean await(Duration timeout, Supplier<Boolean> unblockCondition);

  boolean await(Supplier<Boolean> unblockCondition);

  Promise<Void> createTimer(Duration duration);

  void continueAsNew(Optional<String> workflowType, Optional<ContinueAsNewOptions> options,
      Object[] args);
}
