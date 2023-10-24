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

package com.uber.cadence.migration;

import com.uber.cadence.RequestCancelWorkflowExecutionRequest;
import com.uber.cadence.StartWorkflowExecutionRequest;
import com.uber.cadence.activity.ActivityMethod;

public interface MigrationActivities {
  /**
   * Starts a new workflow execution in a new domain.
   *
   * @param request The request to start the workflow in new domain.
   * @return A response indicating the status of the operation.
   */
  @ActivityMethod
  StartWorkflowInNewResponse startWorkflowInNewDomain(StartWorkflowExecutionRequest request);

  /**
   * Cancels a workflow execution in the current domain.
   *
   * @param request The request to cancel the workflow.
   */
  @ActivityMethod
  void cancelWorkflowInCurrentDomain(RequestCancelWorkflowExecutionRequest request);
}
