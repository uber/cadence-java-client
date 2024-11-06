/**
 * Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * <p>Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. A copy of the License is located at
 *
 * <p>http://aws.amazon.com/apache2.0
 *
 * <p>or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.uber.cadence.internal.replay;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotSame;
import static junit.framework.TestCase.assertSame;
import static junit.framework.TestCase.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.Test;

public class ContinueAsNewWorkflowExecutionParametersTest {
  @Test
  public void testGettersAndSetters() {
    ContinueAsNewWorkflowExecutionParameters parameters =
        new ContinueAsNewWorkflowExecutionParameters();
    parameters.setWorkflowType("wf-type");
    assertEquals("wf-type", parameters.getWorkflowType());

    parameters.setExecutionStartToCloseTimeoutSeconds(1);
    assertEquals(1, parameters.getExecutionStartToCloseTimeoutSeconds());
    ContinueAsNewWorkflowExecutionParameters parameters1 =
        parameters.withExecutionStartToCloseTimeoutSeconds(2);
    assertEquals(2, parameters1.getExecutionStartToCloseTimeoutSeconds());
    assertSame(parameters, parameters1);

    parameters.setInput(new byte[] {1, 2, 3});
    assertTrue(Arrays.equals(new byte[] {1, 2, 3}, parameters.getInput()));
    ContinueAsNewWorkflowExecutionParameters parameters2 =
        parameters.withInput(new byte[] {4, 5, 6});
    assertTrue(Arrays.equals(new byte[] {4, 5, 6}, parameters2.getInput()));
    assertSame(parameters, parameters2);

    parameters.setTaskList("task-list");
    assertEquals("task-list", parameters.getTaskList());
    ContinueAsNewWorkflowExecutionParameters parameters3 = parameters.withTaskList("task-list1");
    assertEquals("task-list1", parameters3.getTaskList());
    assertSame(parameters, parameters3);

    parameters.setTaskStartToCloseTimeoutSeconds(3);
    assertEquals(3, parameters.getTaskStartToCloseTimeoutSeconds());
    ContinueAsNewWorkflowExecutionParameters parameters4 =
        parameters.withTaskStartToCloseTimeoutSeconds(4);
    assertEquals(4, parameters4.getTaskStartToCloseTimeoutSeconds());
    assertSame(parameters, parameters4);
  }

  @Test
  public void testToString() {
    ContinueAsNewWorkflowExecutionParameters params =
        new ContinueAsNewWorkflowExecutionParameters();
    byte[] input = new byte[600];
    Arrays.fill(input, (byte) 'a');
    params.setInput(input);
    params.setExecutionStartToCloseTimeoutSeconds(3600);
    params.setTaskStartToCloseTimeoutSeconds(30);
    params.setTaskList("TestTaskList");
    params.setWorkflowType("wf-type");

    String expected =
        "{Input: "
            + new String(input, 0, 512, StandardCharsets.UTF_8)
            + ", "
            + "ExecutionStartToCloseTimeout: 3600, "
            + "TaskStartToCloseTimeout: 30, "
            + "TaskList: TestTaskList, "
            + "WorkflowType: wf-type, }";
    assertEquals(expected, params.toString());
  }

  @Test
  public void testCopy() {
    ContinueAsNewWorkflowExecutionParameters params =
        new ContinueAsNewWorkflowExecutionParameters();
    params.setExecutionStartToCloseTimeoutSeconds(3600);
    params.setInput("Test input".getBytes(StandardCharsets.UTF_8));
    params.setTaskList("TestTaskList");
    params.setTaskStartToCloseTimeoutSeconds(30);
    params.setWorkflowType("TestWorkflowType");

    ContinueAsNewWorkflowExecutionParameters copiedParams = params.copy();

    assertEquals(3600, copiedParams.getExecutionStartToCloseTimeoutSeconds());
    assertTrue(
        Arrays.equals("Test input".getBytes(StandardCharsets.UTF_8), copiedParams.getInput()));
    assertEquals("TestTaskList", copiedParams.getTaskList());
    assertEquals(30, copiedParams.getTaskStartToCloseTimeoutSeconds());
    assertEquals("TestWorkflowType", copiedParams.getWorkflowType());

    // Ensure deep copy for input array
    assertNotSame(params.getInput(), copiedParams.getInput());
    assertNotSame(params, copiedParams);
  }
}
