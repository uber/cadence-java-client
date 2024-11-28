/*
 *
 *  *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *  *
 *  *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  *  use this file except in compliance with the License. A copy of the License is
 *  *  located at
 *  *
 *  *  http://aws.amazon.com/apache2.0
 *  *
 *  *  or in the "license" file accompanying this file. This file is distributed on
 *  *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  *  express or implied. See the License for the specific language governing
 *  *  permissions and limitations under the License.
 *
 */

package com.uber.cadence.internal.testservice;

import com.uber.cadence.*;

public class TestWorkflowMutableStateAttrUtil {

  static void validateScheduleActivityTask(ScheduleActivityTaskDecisionAttributes a)
      throws BadRequestError {
    if (a == null) {
      throw new BadRequestError("ScheduleActivityTaskDecisionAttributes is not set on decision.");
    }

    if (a.getTaskList() == null || a.getTaskList().getName().isEmpty()) {
      throw new BadRequestError("TaskList is not set on decision.");
    }
    if (a.getActivityId() == null || a.getActivityId().isEmpty()) {
      throw new BadRequestError("ActivityId is not set on decision.");
    }
    if (a.getActivityType() == null
        || a.getActivityType().getName() == null
        || a.getActivityType().getName().isEmpty()) {
      throw new BadRequestError("ActivityType is not set on decision.");
    }
    if (a.getStartToCloseTimeoutSeconds() <= 0) {
      throw new BadRequestError("A valid StartToCloseTimeoutSeconds is not set on decision.");
    }
    if (a.getScheduleToStartTimeoutSeconds() <= 0) {
      throw new BadRequestError("A valid ScheduleToStartTimeoutSeconds is not set on decision.");
    }
    if (a.getScheduleToCloseTimeoutSeconds() <= 0) {
      throw new BadRequestError("A valid ScheduleToCloseTimeoutSeconds is not set on decision.");
    }
    if (a.getHeartbeatTimeoutSeconds() < 0) {
      throw new BadRequestError("Ac valid HeartbeatTimeoutSeconds is not set on decision.");
    }
  }

  static void validateStartChildExecutionAttributes(StartChildWorkflowExecutionDecisionAttributes a)
      throws BadRequestError {
    if (a == null) {
      throw new BadRequestError(
          "StartChildWorkflowExecutionDecisionAttributes is not set on decision.");
    }

    if (a.getWorkflowId().isEmpty()) {
      throw new BadRequestError("Required field WorkflowID is not set on decision.");
    }

    if (a.getWorkflowType() == null || a.getWorkflowType().getName().isEmpty()) {
      throw new BadRequestError("Required field WorkflowType is not set on decision.");
    }

    RetryPolicy retryPolicy = a.getRetryPolicy();
    if (retryPolicy != null) {
      RetryState.validateRetryPolicy(retryPolicy);
    }
  }

  public static void inheritUnsetPropertiesFromParentWorkflow(
      StartWorkflowExecutionRequest startRequest, StartChildWorkflowExecutionDecisionAttributes a) {
    // Inherit tasklist from parent workflow execution if not provided on decision
    if (a.getTaskList() == null || a.getTaskList().getName().isEmpty()) {
      a.setTaskList(startRequest.getTaskList());
    }

    // Inherit workflow timeout from parent workflow execution if not provided on decision
    if (a.getExecutionStartToCloseTimeoutSeconds() <= 0) {
      a.setExecutionStartToCloseTimeoutSeconds(
          startRequest.getExecutionStartToCloseTimeoutSeconds());
    }

    // Inherit decision task timeout from parent workflow execution if not provided on decision
    if (a.getTaskStartToCloseTimeoutSeconds() <= 0) {
      a.setTaskStartToCloseTimeoutSeconds(startRequest.getTaskStartToCloseTimeoutSeconds());
    }
  }
}
