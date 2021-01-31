/*
 *  Modifications Copyright (c) 2017-2020 Uber Technologies Inc.
 *  Portions of the Software are attributed to Copyright (c) 2020 Temporal Technologies Inc.
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

package com.uber.cadence.client;

import com.uber.cadence.QueryRejectCondition;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.WorkflowExecutionCloseStatus;

public final class WorkflowQueryRejectedException extends WorkflowQueryException {

  private final QueryRejectCondition queryRejectCondition;
  private final WorkflowExecutionCloseStatus workflowExecutionStatus;

  public WorkflowQueryRejectedException(
      WorkflowExecution execution,
      QueryRejectCondition queryRejectCondition,
      WorkflowExecutionCloseStatus workflowExecutionStatus) {
    super(
        execution,
        "Query invoked with "
            + queryRejectCondition
            + " reject condition. The workflow execution status is "
            + workflowExecutionStatus);
    this.queryRejectCondition = queryRejectCondition;
    this.workflowExecutionStatus = workflowExecutionStatus;
  }

  public QueryRejectCondition getQueryRejectCondition() {
    return queryRejectCondition;
  }

  public WorkflowExecutionCloseStatus getWorkflowExecutionStatus() {
    return workflowExecutionStatus;
  }
}
