/*
 *  Modifications Copyright (c) 2017-2021 Uber Technologies Inc.
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.uber.cadence.internal.shadowing;

import com.uber.cadence.WorkflowExecution;
import java.util.List;

public class ReplayWorkflowActivityResponse {
  private final int successCount;
  private final int failureCount;
  private final int skippedCount;
  private final List<WorkflowExecution> failedWorkflowExecutions;

  public ReplayWorkflowActivityResponse(
      int successCount,
      int failureCount,
      int skippedCount,
      List<WorkflowExecution> failedWorkflowExecutions) {
    this.successCount = successCount;
    this.failureCount = failureCount;
    this.skippedCount = skippedCount;
    this.failedWorkflowExecutions = failedWorkflowExecutions;
  }

  public int getSuccessCount() {
    return successCount;
  }

  public int getFailureCount() {
    return failureCount;
  }

  public int getSkippedCount() {
    return skippedCount;
  }

  public List<WorkflowExecution> getFailedWorkflowExecutions() {
    return failedWorkflowExecutions;
  }
}
