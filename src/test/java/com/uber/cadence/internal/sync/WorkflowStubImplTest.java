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

package com.uber.cadence.internal.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uber.cadence.QueryWorkflowResponse;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.WorkflowType;
import com.uber.cadence.client.DuplicateWorkflowException;
import com.uber.cadence.client.WorkflowClientOptions;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.internal.common.StartWorkflowExecutionParameters;
import com.uber.cadence.internal.external.GenericWorkflowClientExternal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class WorkflowStubImplTest {
  private static final WorkflowClientOptions CLIENT_OPTIONS =
      WorkflowClientOptions.newBuilder().setDomain("domain").build();
  private GenericWorkflowClientExternal mockClient;

  @Before
  public void setup() {
    mockClient = Mockito.mock(GenericWorkflowClientExternal.class);
  }

  @Test
  public void testEnqueueStart() throws Exception {
    WorkflowStubImpl stub =
        new WorkflowStubImpl(
            CLIENT_OPTIONS,
            mockClient,
            "workflowType",
            new WorkflowOptions.Builder()
                .setExecutionStartToCloseTimeout(Duration.ofSeconds(1))
                .setTaskStartToCloseTimeout(Duration.ofSeconds(2))
                .setWorkflowId("workflowId")
                .setTaskList("taskList")
                .build());

    WorkflowExecution execution = stub.enqueueStart("input");

    assertEquals(new WorkflowExecution().setWorkflowId("workflowId"), execution);
    assertEquals(new WorkflowExecution().setWorkflowId("workflowId"), stub.getExecution());

    ArgumentCaptor<StartWorkflowExecutionParameters> captor =
        ArgumentCaptor.forClass(StartWorkflowExecutionParameters.class);
    verify(mockClient).enqueueStartWorkflow(captor.capture());

    StartWorkflowExecutionParameters params = captor.getValue();
    assertEquals(new WorkflowType().setName("workflowType"), params.getWorkflowType());
    assertEquals("workflowId", params.getWorkflowId());
    assertEquals(1, params.getExecutionStartToCloseTimeoutSeconds());
    assertEquals(2, params.getTaskStartToCloseTimeoutSeconds());
    assertEquals("taskList", params.getTaskList());
    assertEquals(
        "\"input\"", StandardCharsets.UTF_8.decode(ByteBuffer.wrap(params.getInput())).toString());
  }

  @Test
  public void testEnqueueStart_generatesWorkflowId() throws Exception {
    WorkflowStubImpl stub =
        new WorkflowStubImpl(
            CLIENT_OPTIONS,
            mockClient,
            "workflowType",
            new WorkflowOptions.Builder()
                .setExecutionStartToCloseTimeout(Duration.ofSeconds(1))
                .setTaskStartToCloseTimeout(Duration.ofSeconds(2))
                .setTaskList("taskList")
                .build());

    WorkflowExecution execution = stub.enqueueStart("input");

    ArgumentCaptor<StartWorkflowExecutionParameters> captor =
        ArgumentCaptor.forClass(StartWorkflowExecutionParameters.class);
    verify(mockClient).enqueueStartWorkflow(captor.capture());

    StartWorkflowExecutionParameters params = captor.getValue();
    assertNotNull("workflowId", params.getWorkflowId());
    assertEquals(execution.getWorkflowId(), params.getWorkflowId());
  }

  @Test
  public void testEnqueueStart_thenQuery() {
    WorkflowStubImpl stub =
        new WorkflowStubImpl(
            CLIENT_OPTIONS,
            mockClient,
            "workflowType",
            new WorkflowOptions.Builder()
                .setExecutionStartToCloseTimeout(Duration.ofSeconds(1))
                .setTaskStartToCloseTimeout(Duration.ofSeconds(2))
                .setTaskList("taskList")
                .setWorkflowId("workflowId")
                .build());

    when(mockClient.queryWorkflow(any()))
        .thenReturn(
            new QueryWorkflowResponse().setQueryResult("\"res\"".getBytes(StandardCharsets.UTF_8)));

    stub.enqueueStart("input");
    String result = stub.query("fakeQuery", String.class);

    assertEquals("res", result);
  }

  @Test(expected = DuplicateWorkflowException.class)
  public void testEnqueueStart_twice() {
    WorkflowStubImpl stub =
        new WorkflowStubImpl(
            CLIENT_OPTIONS,
            mockClient,
            "workflowType",
            new WorkflowOptions.Builder()
                .setExecutionStartToCloseTimeout(Duration.ofSeconds(1))
                .setTaskStartToCloseTimeout(Duration.ofSeconds(2))
                .setTaskList("taskList")
                .build());

    stub.enqueueStart("input");
    stub.enqueueStart("again");
  }

  @Test(expected = IllegalStateException.class)
  public void testEnqueueStart_stubForExistingWorkflow() {
    WorkflowStubImpl stub =
        new WorkflowStubImpl(
            CLIENT_OPTIONS,
            mockClient,
            Optional.of("workflowType"),
            new WorkflowExecution().setWorkflowId("foo"));

    stub.enqueueStart("input");
  }

  @Test
  public void testEnqueueStartAsync() {
    when(mockClient.enqueueStartWorkflowAsync(any(), any()))
        .thenReturn(CompletableFuture.completedFuture(null));
    WorkflowStubImpl stub =
        new WorkflowStubImpl(
            CLIENT_OPTIONS,
            mockClient,
            "workflowType",
            new WorkflowOptions.Builder()
                .setExecutionStartToCloseTimeout(Duration.ofSeconds(1))
                .setTaskStartToCloseTimeout(Duration.ofSeconds(2))
                .setTaskList("taskList")
                .setWorkflowId("workflowId")
                .build());

    stub.enqueueStartAsync("input");

    ArgumentCaptor<StartWorkflowExecutionParameters> captor =
        ArgumentCaptor.forClass(StartWorkflowExecutionParameters.class);
    verify(mockClient).enqueueStartWorkflowAsync(captor.capture(), eq(Long.MAX_VALUE));

    StartWorkflowExecutionParameters params = captor.getValue();
    assertEquals(new WorkflowType().setName("workflowType"), params.getWorkflowType());
    assertEquals("workflowId", params.getWorkflowId());
    assertEquals(1, params.getExecutionStartToCloseTimeoutSeconds());
    assertEquals(2, params.getTaskStartToCloseTimeoutSeconds());
    assertEquals("taskList", params.getTaskList());
    assertEquals(
        "\"input\"", StandardCharsets.UTF_8.decode(ByteBuffer.wrap(params.getInput())).toString());
  }

  @Test
  public void testEnqueueStartAsyncWithTimeout() {
    when(mockClient.enqueueStartWorkflowAsync(any(), any()))
        .thenReturn(CompletableFuture.completedFuture(null));
    WorkflowStubImpl stub =
        new WorkflowStubImpl(
            CLIENT_OPTIONS,
            mockClient,
            "workflowType",
            new WorkflowOptions.Builder()
                .setExecutionStartToCloseTimeout(Duration.ofSeconds(1))
                .setTaskStartToCloseTimeout(Duration.ofSeconds(2))
                .setTaskList("taskList")
                .setWorkflowId("workflowId")
                .build());

    stub.enqueueStartAsyncWithTimeout(123, TimeUnit.MILLISECONDS, "input");

    ArgumentCaptor<StartWorkflowExecutionParameters> captor =
        ArgumentCaptor.forClass(StartWorkflowExecutionParameters.class);
    verify(mockClient).enqueueStartWorkflowAsync(captor.capture(), eq(123L));

    StartWorkflowExecutionParameters params = captor.getValue();
    assertEquals(new WorkflowType().setName("workflowType"), params.getWorkflowType());
    assertEquals("workflowId", params.getWorkflowId());
    assertEquals(1, params.getExecutionStartToCloseTimeoutSeconds());
    assertEquals(2, params.getTaskStartToCloseTimeoutSeconds());
    assertEquals("taskList", params.getTaskList());
    assertEquals(
        "\"input\"", StandardCharsets.UTF_8.decode(ByteBuffer.wrap(params.getInput())).toString());
  }
}
