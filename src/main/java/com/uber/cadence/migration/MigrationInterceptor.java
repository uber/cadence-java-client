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

import com.uber.cadence.internal.sync.SyncWorkflowDefinition;
import com.uber.cadence.workflow.Workflow;
import com.uber.cadence.workflow.WorkflowInterceptor;
import com.uber.cadence.workflow.WorkflowInterceptorBase;

public class MigrationInterceptor extends WorkflowInterceptorBase {

  private final WorkflowInterceptor next;
  private static final String versionChangeID = "cadenceMigrationInterceptor";
  public static final int versionV1 = 1;

  public MigrationInterceptor(WorkflowInterceptor next) {
    super(next);
    this.next = next;
  }

  @Override
  public byte[] executeWorkflow(
      SyncWorkflowDefinition workflowDefinition, WorkflowExecuteInput input) {

    int version = getVersion(versionChangeID, Workflow.DEFAULT_VERSION, versionV1);
    switch (version) {
      case versionV1:
      default:
        return next.executeWorkflow(workflowDefinition, input);
    }
  }
}
