/*
 *
 *  *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *  *
 *  *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  *  use this file except in compliance with the License. A copy of the License is
 *  *  located at
 *  *
 *  *  http://aws.amazon.com/apache2.0
 *  *
 *  *  or in the "license" file accompanying this file. This file is distributed on
 *  *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  *  express or implied. See the License for the specific language governing
 *  *  permissions and limitations under the License.
 *
 */

package com.uber.cadence.internal.testservice;

import static com.uber.cadence.internal.testservice.TestWorkflowMutableStateAttrUtil.inheritUnsetPropertiesFromParentWorkflow;

import com.uber.cadence.*;
import java.util.Arrays;
import java.util.Collection;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TestWorkflowMutableStateAttrUtil_validateStartChildExecutionAttributes
    extends TestCase {

  private final StartChildWorkflowExecutionDecisionAttributes attributes;
  private final StartChildWorkflowExecutionDecisionAttributes expectedAttributes;

  public TestWorkflowMutableStateAttrUtil_validateStartChildExecutionAttributes(
      String testName,
      StartChildWorkflowExecutionDecisionAttributes attributes,
      StartChildWorkflowExecutionDecisionAttributes expectedAttributes) {
    this.attributes = attributes;
    this.expectedAttributes = expectedAttributes;
  }

  @Parameterized.Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {"all set", createAtt(), createAtt()},
          {
            "empty",
            new StartChildWorkflowExecutionDecisionAttributes(),
            createAtt()
                .setTaskList(new TaskList().setName("testTaskListFromParent"))
                .setExecutionStartToCloseTimeoutSeconds(21)
                .setTaskStartToCloseTimeoutSeconds(22)
          },
          {
            "taskList null",
            createAtt().setTaskList(null),
            createAtt().setTaskList(new TaskList().setName("testTaskListFromParent"))
          },
          {
            "taskList name empty",
            createAtt().setTaskList(new TaskList().setName("")),
            createAtt().setTaskList(new TaskList().setName("testTaskListFromParent"))
          },
          {
            "executionStartToCloseTimeoutSeconds",
            createAtt().setExecutionStartToCloseTimeoutSeconds(0),
            createAtt().setExecutionStartToCloseTimeoutSeconds(21)
          },
          {
            "taskStartToCloseTimeoutSeconds",
            createAtt().setTaskStartToCloseTimeoutSeconds(0),
            createAtt().setTaskStartToCloseTimeoutSeconds(22)
          },
        });
  }

  @Test
  public void testValidateScheduleActivityTask() {
    StartWorkflowExecutionRequest startRequest =
        new StartWorkflowExecutionRequest()
            .setTaskList(new TaskList().setName("testTaskListFromParent"))
            .setExecutionStartToCloseTimeoutSeconds(21)
            .setTaskStartToCloseTimeoutSeconds(22);

    inheritUnsetPropertiesFromParentWorkflow(startRequest, attributes);
    assertEquals(expectedAttributes, attributes);
  }

  private static StartChildWorkflowExecutionDecisionAttributes createAtt() {
    return new StartChildWorkflowExecutionDecisionAttributes()
        .setTaskList(new TaskList().setName("testTaskList"))
        .setExecutionStartToCloseTimeoutSeconds(11)
        .setTaskStartToCloseTimeoutSeconds(12);
  }
}
