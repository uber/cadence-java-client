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

import static com.uber.cadence.internal.testservice.TestWorkflowMutableStateAttrUtil.validateStartChildExecutionAttributes;

import com.uber.cadence.RetryPolicy;
import com.uber.cadence.StartChildWorkflowExecutionDecisionAttributes;
import com.uber.cadence.WorkflowType;
import java.util.Arrays;
import java.util.Collection;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TestWorkflowMutableStateAttrUtil_inheritUnsetPropertiesFromParentWorkflow
    extends TestCase {

  private final StartChildWorkflowExecutionDecisionAttributes attributes;
  private final String errorMessage;

  public TestWorkflowMutableStateAttrUtil_inheritUnsetPropertiesFromParentWorkflow(
      String testName,
      StartChildWorkflowExecutionDecisionAttributes attributes,
      String errorMessage) {
    this.attributes = attributes;
    this.errorMessage = errorMessage;
  }

  @Parameterized.Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {"valid", createAtt(), null},
          {"null", null, "StartChildWorkflowExecutionDecisionAttributes is not set on decision."},
          {
            "WorkflowId empty",
            createAtt().setWorkflowId(""),
            "Required field WorkflowID is not set on decision."
          },
          {
            "WorkflowType null",
            createAtt().setWorkflowType(null),
            "Required field WorkflowType is not set on decision."
          },
          {
            "WorkflowType name empty",
            createAtt().setWorkflowType(new WorkflowType().setName("")),
            "Required field WorkflowType is not set on decision."
          },
          {"RetryPolicy null", createAtt().setRetryPolicy(null), null},
        });
  }

  @Test
  public void testValidateScheduleActivityTask() {
    try {
      validateStartChildExecutionAttributes(attributes);
      if (errorMessage != null) {
        fail("Expected exception");
      }
    } catch (Exception e) {
      assertEquals(errorMessage, e.getMessage());
    }
  }

  private static StartChildWorkflowExecutionDecisionAttributes createAtt() {
    return new StartChildWorkflowExecutionDecisionAttributes()
        .setWorkflowId("testWorkflowId")
        .setWorkflowType(new WorkflowType().setName("testWorkflowType"))
        .setRetryPolicy(
            new RetryPolicy()
                .setInitialIntervalInSeconds(12)
                .setBackoffCoefficient(3.4)
                .setMaximumIntervalInSeconds(56)
                .setMaximumAttempts(78)
                .setExpirationIntervalInSeconds(99));
  }
}
