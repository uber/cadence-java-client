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
package com.uber.cadence.serviceclient.auth;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import com.uber.cadence.*;
import com.uber.cadence.internal.metrics.NoopScope;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.serviceclient.MigrationClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;

public class MigrationClientTest {

  @Mock private IWorkflowService from = Mockito.mock(IWorkflowService.class);

  @Mock private IWorkflowService to = Mockito.mock(IWorkflowService.class);

  @Rule public ExpectedException exceptionRule = ExpectedException.none();

  @Before
  public void setup() {
    from = Mockito.mock(IWorkflowService.class);
    to = Mockito.mock(IWorkflowService.class);
    exceptionRule = ExpectedException.none();
  }

  @Test
  public void testStartWorkflow() throws Exception {

    StartWorkflowExecutionResponse mockResponse = new StartWorkflowExecutionResponse();

    // Migration enabled, happy path, contact 'to' cluster
    testStartWorkflowParametrised(
        client -> {},
        client -> doReturn(mockResponse).when(client).StartWorkflowExecution(any()),
        new StartWorkflowExecutionRequest(),
        mockResponse,
        MigrationClient.MigrationState.ENABLED,
        null);

    // Migration enabled, but 'to' cluster throws exception
    testStartWorkflowParametrised(
        client -> {},
        client -> doThrow(new ServiceBusyError("test")).when(client).StartWorkflowExecution(any()),
        new StartWorkflowExecutionRequest(),
        null,
        MigrationClient.MigrationState.ENABLED,
        new ServiceBusyError("test"));

    // Migration enabled, start request with workflow id, wf not found in 'from' cluster
    StartWorkflowExecutionRequest requestWithWfId = new StartWorkflowExecutionRequest();
    requestWithWfId.setDomain("test");
    requestWithWfId.setWorkflowId("testId");

    testStartWorkflowParametrised(
        client -> doThrow(new EntityNotExistsError()).when(client).DescribeWorkflowExecution(any()),
        client -> doReturn(mockResponse).when(client).StartWorkflowExecution(any()),
        requestWithWfId,
        mockResponse,
        MigrationClient.MigrationState.ENABLED,
        null);

    // Migration enabled, start request with workflow id, wf found in 'from' cluster
    testStartWorkflowParametrised(
        client -> {
          WorkflowExecutionInfo info = new WorkflowExecutionInfo();

          DescribeWorkflowExecutionResponse describeResp = new DescribeWorkflowExecutionResponse();
          describeResp.setWorkflowExecutionInfo(info);
          doReturn(describeResp).when(client).DescribeWorkflowExecution(any());

          doReturn(mockResponse).when(client).StartWorkflowExecution(any());
        },
        client -> {},
        requestWithWfId,
        mockResponse,
        MigrationClient.MigrationState.ENABLED,
        null);

    // Migration Enabled, start request with workflow id, wf found in 'from' cluster with close
    // status, terminated
    // expect to start workflow in 'to' cluster
    testStartWorkflowParametrised(
        client -> {
          WorkflowExecutionInfo info = new WorkflowExecutionInfo();
          info.setCloseStatus(WorkflowExecutionCloseStatus.TERMINATED);

          DescribeWorkflowExecutionResponse describeResp = new DescribeWorkflowExecutionResponse();
          describeResp.setWorkflowExecutionInfo(info);
          doReturn(describeResp).when(client).DescribeWorkflowExecution(any());
        },
        client -> {
          doReturn(mockResponse).when(client).StartWorkflowExecution(any());
        },
        requestWithWfId,
        mockResponse,
        MigrationClient.MigrationState.ENABLED,
        null);

    // Migration disabled, call 'from' cluster
    testStartWorkflowParametrised(
        client -> doReturn(mockResponse).when(client).StartWorkflowExecution(any()),
        client -> {},
        new StartWorkflowExecutionRequest(),
        mockResponse,
        MigrationClient.MigrationState.DISABLED,
        null);

    // TODO: test cases for migration-preferred
  }

  private void testStartWorkflowParametrised(
      ThrowingConsumer<IWorkflowService> fromClientMock,
      ThrowingConsumer<IWorkflowService> toClientMock,
      StartWorkflowExecutionRequest request,
      StartWorkflowExecutionResponse expectedResponse,
      MigrationClient.MigrationState migrationState,
      Throwable expectedException)
      throws Exception {

    fromClientMock.acceptThrows(from);
    toClientMock.acceptThrows(to);

    MigrationClient migrationClient = new MigrationClient(from, to, NoopScope.getInstance());
    migrationClient.setMigrationState(migrationState);

    if (expectedException == null) {
      StartWorkflowExecutionResponse actualResponse =
          migrationClient.StartWorkflowExecution(request);
      Assert.assertEquals(expectedResponse, actualResponse);
    } else {
      exceptionRule.expectMessage(expectedException.getMessage());
    }
  }
}
