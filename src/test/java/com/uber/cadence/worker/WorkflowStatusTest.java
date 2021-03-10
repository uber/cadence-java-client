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

import static org.junit.Assert.assertEquals;

import com.uber.cadence.WorkflowExecutionCloseStatus;
import org.junit.Test;

public class WorkflowStatusTest {

  @Test
  public void testWorkflowStatus_ToString() {
    assertEquals(WorkflowStatus.OPEN.toString(), "OPEN");
    assertEquals(WorkflowStatus.CLOSED.toString(), "CLOSED");
    assertEquals(WorkflowStatus.CANCELED.toString(), WorkflowExecutionCloseStatus.CANCELED.name());
    assertEquals(
        WorkflowStatus.CONTINUED_AS_NEW.toString(),
        WorkflowExecutionCloseStatus.CONTINUED_AS_NEW.name());
    assertEquals(
        WorkflowStatus.COMPLETED.toString(), WorkflowExecutionCloseStatus.COMPLETED.name());
    assertEquals(WorkflowStatus.FAILED.toString(), WorkflowExecutionCloseStatus.FAILED.name());
    assertEquals(
        WorkflowStatus.TERMINATED.toString(), WorkflowExecutionCloseStatus.TERMINATED.name());
    assertEquals(
        WorkflowStatus.TIMED_OUT.toString(), WorkflowExecutionCloseStatus.TIMED_OUT.name());
  }
}
