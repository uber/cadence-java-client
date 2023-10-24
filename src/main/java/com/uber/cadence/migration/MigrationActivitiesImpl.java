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
import com.uber.cadence.WorkflowExecutionAlreadyStartedError;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.workflow.Workflow;

public class MigrationActivitiesImpl implements MigrationActivities {
  private final WorkflowClient clientInCurrDomain, clientInNewDomain;

  public MigrationActivitiesImpl(
      WorkflowClient clientInCurrDomain, WorkflowClient clientInNewDomain) {
    this.clientInCurrDomain = clientInCurrDomain;
    this.clientInNewDomain = clientInNewDomain;
  }

  @Override
  public StartWorkflowInNewResponse startWorkflowInNewDomain(
      StartWorkflowExecutionRequest request) {
    try {
      return new StartWorkflowInNewResponse(
          clientInNewDomain.getService().StartWorkflowExecution(request),
          "New workflow starting successful");
    } catch (WorkflowExecutionAlreadyStartedError e) {
      return new StartWorkflowInNewResponse(null, "Workflow already started");
    } catch (Exception e) {
      throw Workflow.wrap(e);
    }
  }

  @Override
  public void cancelWorkflowInCurrentDomain(RequestCancelWorkflowExecutionRequest request) {
    try {
      clientInCurrDomain.getService().RequestCancelWorkflowExecution(request);
    } catch (Exception e) {
      throw Workflow.wrap(e);
    }
  }
}
