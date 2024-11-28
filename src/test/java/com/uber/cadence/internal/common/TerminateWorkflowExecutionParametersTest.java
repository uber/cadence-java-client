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

package com.uber.cadence.internal.common;

import static org.junit.Assert.*;

import com.uber.cadence.WorkflowExecution;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TerminateWorkflowExecutionParametersTest {

  private final WorkflowExecution workflowExecution;
  private final String reason;
  private final byte[] details;

  public TerminateWorkflowExecutionParametersTest(
      WorkflowExecution workflowExecution, String reason, byte[] details) {
    this.workflowExecution = workflowExecution;
    this.reason = reason;
    this.details = details;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {new WorkflowExecution(), "Test Reason 1", new byte[] {1, 2, 3}},
          {new WorkflowExecution(), "Test Reason 2", new byte[] {4, 5, 6}},
          {null, null, null}
        });
  }

  @Test
  public void testParameterizedConstructor() {
    TerminateWorkflowExecutionParameters params =
        new TerminateWorkflowExecutionParameters(workflowExecution, reason, details);

    assertEquals(workflowExecution, params.getWorkflowExecution());
    assertEquals(reason, params.getReason());
    assertArrayEquals(details, params.getDetails());
  }

  @Test
  public void testSettersAndGetters() {
    TerminateWorkflowExecutionParameters params = new TerminateWorkflowExecutionParameters();
    params.setWorkflowExecution(workflowExecution);
    params.setReason(reason);
    params.setDetails(details);

    assertEquals(workflowExecution, params.getWorkflowExecution());
    assertEquals(reason, params.getReason());
    assertArrayEquals(details, params.getDetails());
  }

  @Test
  public void testFluentSetters() {
    TerminateWorkflowExecutionParameters params =
        new TerminateWorkflowExecutionParameters()
            .withWorkflowExecution(workflowExecution)
            .withReason(reason)
            .withDetails(details);

    assertEquals(workflowExecution, params.getWorkflowExecution());
    assertEquals(reason, params.getReason());
    assertArrayEquals(details, params.getDetails());
  }
}
