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

import com.uber.cadence.InternalServiceError;
import com.uber.cadence.PollForActivityTaskRequest;
import com.uber.cadence.PollForActivityTaskResponse;
import com.uber.cadence.PollForDecisionTaskRequest;
import com.uber.cadence.PollForDecisionTaskResponse;
import com.uber.cadence.RespondActivityTaskCompletedRequest;
import com.uber.cadence.RespondActivityTaskFailedRequest;
import com.uber.cadence.RespondDecisionTaskCompletedRequest;

interface TestWorkflowMutableState {

  ExecutionId getExecutionId();

  void startDecisionTask(PollForDecisionTaskResponse task, PollForDecisionTaskRequest pollRequest)
      throws InternalServiceError;

  void completeDecisionTask(RespondDecisionTaskCompletedRequest request)
      throws InternalServiceError;

  void startActivityTask(PollForActivityTaskResponse task, PollForActivityTaskRequest pollRequest)
      throws InternalServiceError;

  void completeActivityTask(String activityId, RespondActivityTaskCompletedRequest request)
      throws InternalServiceError;

  void failActivityTask(String activityId, RespondActivityTaskFailedRequest request)
      throws InternalServiceError;
}
