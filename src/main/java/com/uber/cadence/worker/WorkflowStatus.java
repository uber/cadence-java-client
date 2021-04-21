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
package com.uber.cadence.worker;

import com.uber.cadence.WorkflowExecutionCloseStatus;

/** The WorkflowStatus is only used in shadowing option. */
public enum WorkflowStatus {
  OPEN("OPEN"),
  CLOSED("CLOSED"),
  CANCELED(WorkflowExecutionCloseStatus.CANCELED.name()),
  CONTINUED_AS_NEW(WorkflowExecutionCloseStatus.CONTINUED_AS_NEW.name()),
  COMPLETED(WorkflowExecutionCloseStatus.COMPLETED.name()),
  FAILED(WorkflowExecutionCloseStatus.FAILED.name()),
  TERMINATED(WorkflowExecutionCloseStatus.TERMINATED.name()),
  TIMED_OUT(WorkflowExecutionCloseStatus.TIMED_OUT.name());

  private String status;

  WorkflowStatus(String status) {
    this.status = status;
  }

  public String getValue() {
    return status;
  }

  @Override
  public String toString() {
    return this.getValue();
  }
}
