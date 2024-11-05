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

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class SignalExternalWorkflowParametersTest {

  @Test
  public void testSetAndGetDomain() {
    SignalExternalWorkflowParameters params = new SignalExternalWorkflowParameters();
    params.setDomain("TestDomain");
    assertEquals("TestDomain", params.getDomain());
  }

  @Test
  public void testSetAndGetInput() {
    SignalExternalWorkflowParameters params = new SignalExternalWorkflowParameters();
    byte[] input = "TestInput".getBytes(StandardCharsets.UTF_8);
    params.setInput(input);
    assertArrayEquals(input, params.getInput());
  }

  @Test
  public void testWithInputChain() {
    SignalExternalWorkflowParameters params = new SignalExternalWorkflowParameters();

    byte[] input = "ChainInput".getBytes(StandardCharsets.UTF_8);
    SignalExternalWorkflowParameters result = params.withInput(input);
    assertArrayEquals(input, params.getInput());
    assertSame(params, result);
  }

  @Test
  public void testSetAndGetRunId() {
    SignalExternalWorkflowParameters params = new SignalExternalWorkflowParameters();
    params.setRunId("TestRunId");
    assertEquals("TestRunId", params.getRunId());
  }

  @Test
  public void testWithRunIdChain() {
    SignalExternalWorkflowParameters params = new SignalExternalWorkflowParameters();

    SignalExternalWorkflowParameters result = params.withRunId("ChainRunId");
    assertEquals("ChainRunId", params.getRunId());
    assertSame(params, result);
  }

  @Test
  public void testSetAndGetSignalName() {
    SignalExternalWorkflowParameters params = new SignalExternalWorkflowParameters();

    params.setSignalName("TestSignalName");
    assertEquals("TestSignalName", params.getSignalName());
  }

  @Test
  public void testWithSignalNameChain() {
    SignalExternalWorkflowParameters params = new SignalExternalWorkflowParameters();
    SignalExternalWorkflowParameters result = params.withSignalName("ChainSignalName");
    assertEquals("ChainSignalName", params.getSignalName());
    assertSame(params, result);
  }

  @Test
  public void testSetAndGetWorkflowId() {
    SignalExternalWorkflowParameters params = new SignalExternalWorkflowParameters();
    params.setWorkflowId("TestWorkflowId");
    assertEquals("TestWorkflowId", params.getWorkflowId());
  }

  @Test
  public void testWithWorkflowIdChain() {
    SignalExternalWorkflowParameters params = new SignalExternalWorkflowParameters();
    SignalExternalWorkflowParameters result = params.withWorkflowId("ChainWorkflowId");
    assertEquals("ChainWorkflowId", params.getWorkflowId());
    assertSame(params, result);
  }

  @Test
  public void testStringification() {

    String longStr =
        "1000000000000000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000000000001";

    SignalExternalWorkflowParameters longParams =
        new SignalExternalWorkflowParameters()
            .withWorkflowId("ChainWorkflowId")
            .withSignalName("signalName")
            .withInput(longStr.getBytes())
            .withRunId("some-run-id");

    SignalExternalWorkflowParameters shortParams =
        new SignalExternalWorkflowParameters()
            .withWorkflowId("ChainWorkflowId")
            .withSignalName("signalName")
            .withInput("smol".getBytes())
            .withRunId("some-run-id");

    assertEquals(
        "{SignalName: signalName, Input: "
            + longStr.substring(0, 512)
            + ", WorkflowId: ChainWorkflowId, RunId: some-run-id, }",
        longParams.toString());

    assertEquals(
        "{SignalName: signalName, Input: "
            + "smol"
            + ", WorkflowId: ChainWorkflowId, RunId: some-run-id, }",
        shortParams.toString());

    assertEquals(
        "{SignalName: null, Input: , WorkflowId: null, RunId: null, }",
        new SignalExternalWorkflowParameters().toString());
  }

  @Test
  public void testClone() throws CloneNotSupportedException {
    SignalExternalWorkflowParameters params = new SignalExternalWorkflowParameters();
    params.setDomain("Domain");
    params.setRunId("RunId");
    params.setSignalName("SignalName");
    params.setWorkflowId("WorkflowId");
    params.setInput("Input".getBytes(StandardCharsets.UTF_8));

    SignalExternalWorkflowParameters clonedParams = params.copy();

    assertNotSame(params, clonedParams);

    assertEquals(params.getDomain(), clonedParams.getDomain());
    assertArrayEquals(params.getInput(), clonedParams.getInput());
    assertEquals(params.getRunId(), clonedParams.getRunId());
    assertEquals(params.getSignalName(), clonedParams.getSignalName());
    assertEquals(params.getWorkflowId(), clonedParams.getWorkflowId());

    // there's 5 assertions here
    // update if there's any additional properties
    assertEquals(5, getPropertyCount(params));
  }

  private static int getPropertyCount(Object obj) {
    int count = 0;
    Class<?> clazz = obj.getClass();

    while (clazz != null) {
      Field[] fields = clazz.getDeclaredFields();

      for (Field field : fields) {
        if (!field.getName().equals("$jacocoData")) {
          count++;
        }
      }
      clazz = clazz.getSuperclass();
    }

    return count;
  }
}
