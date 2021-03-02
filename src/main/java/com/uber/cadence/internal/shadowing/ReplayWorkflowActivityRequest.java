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

public class ReplayWorkflowActivityRequest {
  private final String domain;
  private final List<WorkflowExecution> executions;

  public ReplayWorkflowActivityRequest(String domain, List<WorkflowExecution> executions) {
    this.domain = domain;
    this.executions = executions;
  }

  public String getDomain() {
    return domain;
  }

  public List<WorkflowExecution> getExecutions() {
    return executions;
  }
}
