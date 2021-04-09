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

import java.util.Arrays;
import java.util.List;

/**
 * This class is the JSON serializable class of {@link
 * com.uber.cadence.shadower.ScanWorkflowActivityResult} Make sure this class is sync with auto
 * generated ScanWorkflowActivityResult
 */
public class ScanWorkflowActivityResult {
  private List<WorkflowExecution> executions;
  private byte[] nextPageToken;

  public ScanWorkflowActivityResult() {}

  public List<WorkflowExecution> getExecutions() {
    return executions;
  }

  public void setExecutions(List<WorkflowExecution> executions) {
    this.executions = executions;
  }

  public byte[] getNextPageToken() {
    return nextPageToken;
  }

  public void setNextPageToken(byte[] nextPageToken) {
    this.nextPageToken = nextPageToken;
  }

  @Override
  public String toString() {
    return "ScanWorkflowActivityResult{"
        + "executions="
        + executions
        + ", nextPageToken="
        + Arrays.toString(nextPageToken)
        + '}';
  }
}
