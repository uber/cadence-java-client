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

package com.uber.cadence.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

import com.uber.cadence.*;
import com.uber.cadence.serviceclient.IWorkflowService;
import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MigrationIWorkflowServiceTest {

  @Mock private IWorkflowService serviceOld;

  @Mock private IWorkflowService serviceNew;

  @Captor private ArgumentCaptor<StartWorkflowExecutionRequest> startRequestCaptor;
  @Captor private ArgumentCaptor<ListWorkflowExecutionsRequest> listRequestCaptor;

  private MigrationIWorkflowService migrationService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    migrationService =
        new MigrationIWorkflowService(serviceOld, "domainOld", serviceNew, "domainNew");
  }

  // No previous workflow found - launch a workflow in new cluster
  @Test
  public void testStartWorkflowExecution_startNewWorkflow() throws TException {

    StartWorkflowExecutionRequest startRequest =
        new StartWorkflowExecutionRequest()
            .setWorkflowId("123")
            .setWorkflowType(new WorkflowType().setName("sampleWorkflow"))
            .setRequestId("123");
    DescribeWorkflowExecutionRequest describeWorkflowExecutionRequest =
        new DescribeWorkflowExecutionRequest()
            .setDomain("domainNew")
            .setExecution(new WorkflowExecution().setWorkflowId("123"));
    DescribeWorkflowExecutionRequest describeOldWorkflowExecutionRequest =
        new DescribeWorkflowExecutionRequest()
            .setDomain("domainOld")
            .setExecution(new WorkflowExecution().setWorkflowId("123"));

    // Verify DescribeWorkflowExecution calls for both services return null
    when(serviceNew.DescribeWorkflowExecution(describeWorkflowExecutionRequest)).thenReturn(null);

    when(serviceOld.DescribeWorkflowExecution(describeOldWorkflowExecutionRequest))
        .thenReturn(null);

    StartWorkflowExecutionResponse responseNew = new StartWorkflowExecutionResponse();
    when(serviceNew.StartWorkflowExecution(startRequest)).thenReturn(responseNew);

    StartWorkflowExecutionResponse response = migrationService.StartWorkflowExecution(startRequest);

    verify(serviceNew, times(1)).DescribeWorkflowExecution(describeWorkflowExecutionRequest);
    verify(serviceOld, times(1)).DescribeWorkflowExecution(describeOldWorkflowExecutionRequest);

    assertEquals(responseNew, response);

    // Verify that the StartWorkflowExecution method is only called once on serviceNew
    verify(serviceNew, times(1)).StartWorkflowExecution(startRequest);

    // Verify that no other methods are called
    verifyNoMoreInteractions(serviceNew);

    verifyNoMoreInteractions(serviceOld);
  }

  // Previous running workflow found: expected to launch a wf in the new cluster
  @Test
  public void testStartWorkflowExecution_startOldWorkflow() throws TException {

    StartWorkflowExecutionRequest startRequest =
        new StartWorkflowExecutionRequest()
            .setWorkflowId("123")
            .setWorkflowType(new WorkflowType().setName("sampleWorkflow"))
            .setRequestId("123");

    // Verify DescribeWorkflowExecution calls for both services return null
    when(serviceNew.DescribeWorkflowExecution(any())).thenReturn(null);

    DescribeWorkflowExecutionResponse describeWorkflowExecutionResponse =
        new DescribeWorkflowExecutionResponse();
    when(serviceOld.DescribeWorkflowExecution(any())).thenReturn(describeWorkflowExecutionResponse);

    StartWorkflowExecutionResponse responseOld = new StartWorkflowExecutionResponse();
    when(serviceOld.StartWorkflowExecution(any())).thenReturn(responseOld);

    StartWorkflowExecutionResponse response = migrationService.StartWorkflowExecution(startRequest);

    // Verify interactions
    verify(serviceNew, times(1)).DescribeWorkflowExecution(any());
    verify(serviceOld, times(1)).DescribeWorkflowExecution(any());
    verify(serviceOld, times(1)).StartWorkflowExecution(any());

    // Verify that no other methods are called
    verifyNoMoreInteractions(serviceNew);
    verifyNoMoreInteractions(serviceOld);

    // Assert the response
    assertEquals(responseOld, response);
  }

  @Test
  public void testStartWorkflow_noWorkflowID() throws TException {
    StartWorkflowExecutionRequest startRequest =
        new StartWorkflowExecutionRequest()
            .setWorkflowId("123")
            .setWorkflowType(new WorkflowType().setName("sampleWorkflow"))
            .setRequestId("123");

    StartWorkflowExecutionResponse mockResponse =
        new StartWorkflowExecutionResponse().setRunId("123");
    when(serviceNew.StartWorkflowExecution(any())).thenReturn(mockResponse);
    StartWorkflowExecutionResponse startWorkflowExecutionResponse =
        migrationService.StartWorkflowExecution(startRequest);
    verify(serviceNew, times(1)).StartWorkflowExecution(any());

    assertEquals(startWorkflowExecutionResponse, mockResponse);
  }

  @Test
  public void testStartWorkflow_errorInDescribeWorkflowExecution() throws TException {

    StartWorkflowExecutionRequest startRequest =
        new StartWorkflowExecutionRequest()
            .setWorkflowId("123")
            .setWorkflowType(new WorkflowType().setName("sampleWorkflow"))
            .setRequestId("123");

    when(serviceNew.DescribeWorkflowExecution(any())).thenReturn(null);
    when(serviceOld.DescribeWorkflowExecution(any())).thenReturn(null);
    StartWorkflowExecutionResponse startWorkflowExecutionResponse =
        migrationService.StartWorkflowExecution(startRequest);
    // Verify interactions
    verify(serviceNew, times(1)).DescribeWorkflowExecution(any());
    verify(serviceOld, times(1)).DescribeWorkflowExecution(any());

    assertNull(startWorkflowExecutionResponse);
  }

  //TODO: Addition of other tests
  //  @Test
  //  public void testListWorkflowExecutions_startNewWorkflow() throws TException {
  //
  //    ListWorkflowExecutionsRequest listRequest =
  //        new ListWorkflowExecutionsRequest().setDomain("domain").setPageSize(10);
  //    listRequest.setNextPageToken("sample workflow".getBytes());
  //
  //    ListWorkflowExecutionsResponse responseNew = new ListWorkflowExecutionsResponse();
  //
  //    when(serviceNew.ListWorkflowExecutions(listRequest)).thenReturn(responseNew);
  //
  //    ListWorkflowExecutionsResponse response =
  // migrationService.ListWorkflowExecutions(listRequest);
  //
  //    // assertNotNull("Response is null", response);
  //    assertEquals(responseNew, response);
  //    verify(serviceNew, times(1)).ListWorkflowExecutions(listRequestCaptor.capture());
  //    //    assertEquals(listRequest, listRequestCaptor.getValue());
  //  }
  //
  //  @Test
  //  public void testListWorkflowExecutions_startOldWorkflow() throws TException {
  //
  //    ListWorkflowExecutionsRequest listRequest =
  //        new ListWorkflowExecutionsRequest().setDomain("domain").setPageSize(10);
  //
  //    ListWorkflowExecutionsResponse responseOld = new ListWorkflowExecutionsResponse();
  //    when(serviceOld.ListWorkflowExecutions(listRequest)).thenReturn(responseOld);
  //
  //    ListWorkflowExecutionsResponse response =
  // migrationService.ListWorkflowExecutions(listRequest);
  //
  //    assertEquals(responseOld, response);
  //    verify(serviceOld, times(1)).ListWorkflowExecutions(listRequestCaptor.capture());
  //    assertEquals(listRequest, listRequestCaptor.getValue());
  //  }
}
