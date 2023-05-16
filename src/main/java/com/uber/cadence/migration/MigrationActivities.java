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

import com.uber.cadence.StartWorkflowExecutionRequest;
import com.uber.cadence.StartWorkflowExecutionResponse;

public interface MigrationActivities {

  // New classes has been created to add functionalities in future to the new request/response for
  // workflow migration
  final class StartNewWorkflowRequest {
    StartWorkflowExecutionRequest request;

    StartNewWorkflowRequest(StartWorkflowExecutionRequest request) {
      this.request = request;
    }
  }

  final class StartNewWorkflowExecutionResponse {
    StartWorkflowExecutionResponse response;

    StartNewWorkflowExecutionResponse(StartWorkflowExecutionResponse response) {
      this.response = response;
    }
  }

  StartNewWorkflowExecutionResponse startWorkflowInNewDomain(StartNewWorkflowRequest request);
}
