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
import java.util.ArrayList;
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

  @Captor private ArgumentCaptor<StartWorkflowExecutionRequest> requestCaptor;

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

  // Previous running workflow found: expected to launch a wf in the old cluster
  @Test
  public void testStartWorkflowExecution_startOldWorkflow() throws TException {

    StartWorkflowExecutionRequest startRequest =
        new StartWorkflowExecutionRequest()
            .setWorkflowId("123")
            .setWorkflowType(new WorkflowType().setName("sampleWorkflow"))
            .setRequestId("123");

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

  @Test
  public void testListWorkflows_InitialRequest() throws TException {

    String domainNew = "test";
    int one = 1;

    ListWorkflowExecutionsRequest request =
        new ListWorkflowExecutionsRequest()
            .setDomain(domainNew)
            .setPageSize(one)
            .setNextPageToken("".getBytes());

    ListWorkflowExecutionsResponse mockSingleResultResponse =
        new ListWorkflowExecutionsResponse().setExecutions(new ArrayList<>());

    WorkflowExecutionInfo executionInfo = new WorkflowExecutionInfo();
    executionInfo.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    mockSingleResultResponse.getExecutions().add(executionInfo);
    mockSingleResultResponse.setNextPageToken("testToken".getBytes());

    // fetch from new cluster for intial request
    when(serviceNew.ListWorkflowExecutions(any())).thenReturn(mockSingleResultResponse);
    ListWorkflowExecutionsResponse response = migrationService.ListWorkflowExecutions(request);
    assertEquals(mockSingleResultResponse, response);
  }

  // calling old cluster when new cluster returns empty response
  @Test
  public void testListWorkflow_OldClusterCall() throws TException {

    String domainNew = "test";
    int one = 1;

    ListWorkflowExecutionsRequest request =
        new ListWorkflowExecutionsRequest()
            .setDomain(domainNew)
            .setPageSize(one)
            .setNextPageToken("".getBytes());

    ListWorkflowExecutionsResponse mockEmptyResponse =
        new ListWorkflowExecutionsResponse()
            .setExecutions(new ArrayList<>())
            .setNextPageToken((byte[]) null);

    ListWorkflowExecutionsResponse mockSingleResultResponse =
        new ListWorkflowExecutionsResponse().setExecutions(new ArrayList<>());

    WorkflowExecutionInfo executionInfo = new WorkflowExecutionInfo();
    executionInfo.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    mockSingleResultResponse.getExecutions().add(executionInfo);
    mockSingleResultResponse.setNextPageToken("testToken".getBytes());

    when(serviceNew.ListWorkflowExecutions(any())).thenReturn(mockEmptyResponse);
    ListWorkflowExecutionsResponse response = migrationService.ListWorkflowExecutions(request);
    assertEquals(mockEmptyResponse, response);

    when(serviceOld.ListWorkflowExecutions(any())).thenReturn(mockSingleResultResponse);
    response = migrationService.ListWorkflowExecutions(request);
    assertEquals(mockSingleResultResponse, response);
  }

  // if fetching from new cluster result size is less than pageSize, fetch additional records from
  // Old Cluster
  @Test
  public void testListWorkflow_fetchFromBothCluster() throws TException {
    String domainNew = "test";
    int one = 1;
    int two = 2;

    ListWorkflowExecutionsRequest request =
        new ListWorkflowExecutionsRequest()
            .setDomain(domainNew)
            .setPageSize(one)
            .setNextPageToken("".getBytes());

    ListWorkflowExecutionsRequest requestTwoItems =
        new ListWorkflowExecutionsRequest()
            .setDomain(domainNew)
            .setPageSize(two)
            .setNextPageToken("".getBytes());

    ListWorkflowExecutionsResponse mockSingleResultResponse =
        new ListWorkflowExecutionsResponse().setExecutions(new ArrayList<>());

    WorkflowExecutionInfo executionInfo = new WorkflowExecutionInfo();
    executionInfo.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    mockSingleResultResponse.getExecutions().add(executionInfo);
    mockSingleResultResponse.setNextPageToken("testToken".getBytes());

    when(serviceOld.ListWorkflowExecutions(request)).thenReturn(mockSingleResultResponse);
    ListWorkflowExecutionsResponse response = migrationService.ListWorkflowExecutions(request);
    assertEquals(mockSingleResultResponse, response);

    when(serviceNew.ListWorkflowExecutions(requestTwoItems)).thenReturn(mockSingleResultResponse);
    response = migrationService.ListWorkflowExecutions(request);
    assertEquals(mockSingleResultResponse, response);
  }

  @Test
  public void testListWorkflows_emptyRequestTests() throws TException {

    // Test when request is null
    try {
      migrationService.ListWorkflowExecutions(null);
    } catch (BadRequestError e) {
      assertEquals("List request is null", e.getMessage());
    }

    // Test when domain is null
    try {
      migrationService.ListWorkflowExecutions(new ListWorkflowExecutionsRequest().setPageSize(10));
    } catch (BadRequestError e) {
      assertEquals("Domain is null or empty", e.getMessage());
    }
  }

  // Test when error returned from internal client, return same error
  @Test
  public void testListWorkflow_error() throws TException {
    String domainNew = "test";

    when(serviceNew.ListWorkflowExecutions(any())).thenReturn(null);
    ListWorkflowExecutionsResponse response =
        migrationService.ListWorkflowExecutions(
            new ListWorkflowExecutionsRequest().setDomain(domainNew));
    verify(serviceNew, times(1)).ListWorkflowExecutions(any());
    assertNull(response);
  }

  @Test
  public void testListWorkflow_FromClusterOnly() throws TException {

    String domain = "test";
    int one = 1;

    ListWorkflowExecutionsRequest request =
        new ListWorkflowExecutionsRequest()
            .setDomain(domain)
            .setPageSize(one)
            .setNextPageToken("".getBytes());

    ListWorkflowExecutionsResponse mockEmptyResponse =
        new ListWorkflowExecutionsResponse()
            .setExecutions(new ArrayList<>())
            .setNextPageToken("".getBytes());

    // Test fetch only from 'from' cluster
    when(serviceOld.ListWorkflowExecutions(any())).thenReturn(mockEmptyResponse);
    ListWorkflowExecutionsResponse response = migrationService.ListWorkflowExecutions(request);
    assertEquals(mockEmptyResponse, response);
  }

  @Test
  public void testListWorkflows_ResponseWithToken() throws TException {

    String domainNew = "test";
    int one = 1;
    int two = 2;

    ListWorkflowExecutionsRequest request =
        new ListWorkflowExecutionsRequest()
            .setDomain(domainNew)
            .setPageSize(one)
            .setNextPageToken("".getBytes());

    ListWorkflowExecutionsResponse expectedResponseWithToken = new ListWorkflowExecutionsResponse();
    expectedResponseWithToken.setExecutions(new ArrayList<>());
    WorkflowExecutionInfo executionInfo1 = new WorkflowExecutionInfo();
    executionInfo1.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    WorkflowExecutionInfo executionInfo2 = new WorkflowExecutionInfo();
    executionInfo2.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    expectedResponseWithToken.getExecutions().add(executionInfo1);
    expectedResponseWithToken.getExecutions().add(executionInfo2);
    expectedResponseWithToken.setNextPageToken("totestToken".getBytes());

    when(serviceNew.ListWorkflowExecutions(any())).thenReturn(expectedResponseWithToken);
    ListWorkflowExecutionsResponse response = migrationService.ListWorkflowExecutions(request);
    assertEquals(expectedResponseWithToken, response);

    ListWorkflowExecutionsRequest requestTwoItems =
        new ListWorkflowExecutionsRequest()
            .setDomain(domainNew)
            .setPageSize(two)
            .setNextPageToken("".getBytes());

    when(serviceNew.ListWorkflowExecutions(any())).thenReturn(expectedResponseWithToken);
    response = migrationService.ListWorkflowExecutions(requestTwoItems);
    assertEquals(expectedResponseWithToken, response);
  }

  @Test
  public void testScanWorkflow_InitialRequest() throws TException {

    String domainNew = "test";
    int one = 1;

    ListWorkflowExecutionsRequest request =
        new ListWorkflowExecutionsRequest()
            .setDomain(domainNew)
            .setPageSize(one)
            .setNextPageToken("".getBytes());

    ListWorkflowExecutionsResponse mockSingleResultResponse =
        new ListWorkflowExecutionsResponse().setExecutions(new ArrayList<>());

    WorkflowExecutionInfo executionInfo = new WorkflowExecutionInfo();
    executionInfo.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    mockSingleResultResponse.getExecutions().add(executionInfo);
    mockSingleResultResponse.setNextPageToken("testToken".getBytes());

    // Mock the serviceNew to return the expected response for the initial request.
    when(serviceNew.ScanWorkflowExecutions(any())).thenReturn(mockSingleResultResponse);

    // Perform the test and check if the response matches the expected result.
    ListWorkflowExecutionsResponse response = migrationService.ScanWorkflowExecutions(request);
    assertEquals(mockSingleResultResponse, response);
  }

  // Test scanWorkflow when new cluster returns an empty response and it falls back to the old
  // cluster.
  @Test
  public void testScanWorkflow_OldClusterCall() throws TException {

    String domainNew = "test";
    int one = 1;

    ListWorkflowExecutionsRequest request =
        new ListWorkflowExecutionsRequest()
            .setDomain(domainNew)
            .setPageSize(one)
            .setNextPageToken("".getBytes());

    ListWorkflowExecutionsResponse mockEmptyResponse =
        new ListWorkflowExecutionsResponse()
            .setExecutions(new ArrayList<>())
            .setNextPageToken((byte[]) null);

    ListWorkflowExecutionsResponse mockSingleResultResponse =
        new ListWorkflowExecutionsResponse().setExecutions(new ArrayList<>());

    WorkflowExecutionInfo executionInfo = new WorkflowExecutionInfo();
    executionInfo.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    mockSingleResultResponse.getExecutions().add(executionInfo);
    mockSingleResultResponse.setNextPageToken("testToken".getBytes());

    // Mock the serviceNew to return an empty response.
    when(serviceNew.ScanWorkflowExecutions(any())).thenReturn(mockEmptyResponse);

    // Perform the first test to check if the response is empty as the new cluster returned no
    // results.
    ListWorkflowExecutionsResponse response = migrationService.ScanWorkflowExecutions(request);
    assertEquals(mockEmptyResponse, response);

    // Mock the serviceOld to return the expected response.
    when(serviceOld.ScanWorkflowExecutions(any())).thenReturn(mockSingleResultResponse);

    // Perform the second test to check if the response is now populated with data from the old
    // cluster.
    response = migrationService.ScanWorkflowExecutions(request);
    assertEquals(mockSingleResultResponse, response);
  }

  @Test
  public void testScanWorkflow_FetchFromBothClusters() throws TException {

    String domainNew = "test";
    int one = 1;
    int two = 2;

    ListWorkflowExecutionsRequest request =
        new ListWorkflowExecutionsRequest()
            .setDomain(domainNew)
            .setPageSize(one)
            .setNextPageToken("".getBytes());

    ListWorkflowExecutionsResponse mockSingleResultResponse =
        new ListWorkflowExecutionsResponse().setExecutions(new ArrayList<>());

    WorkflowExecutionInfo executionInfo = new WorkflowExecutionInfo();
    executionInfo.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    mockSingleResultResponse.getExecutions().add(executionInfo);
    mockSingleResultResponse.setNextPageToken("testToken".getBytes());

    // Mock the serviceOld to return the expected response.
    when(serviceOld.ScanWorkflowExecutions(any())).thenReturn(mockSingleResultResponse);

    // Perform the first test to check if the response is populated with data from the old cluster.
    ListWorkflowExecutionsResponse response = migrationService.ScanWorkflowExecutions(request);
    assertEquals(mockSingleResultResponse, response);

    ListWorkflowExecutionsRequest requestTwoItems =
        new ListWorkflowExecutionsRequest()
            .setDomain(domainNew)
            .setPageSize(two)
            .setNextPageToken("".getBytes());

    // Mock the serviceNew to return the expected response.
    when(serviceNew.ScanWorkflowExecutions(any())).thenReturn(mockSingleResultResponse);

    // Perform the second test to check if the response is now populated with data from the new
    // cluster.
    response = migrationService.ScanWorkflowExecutions(requestTwoItems);
    assertEquals(mockSingleResultResponse, response);
  }

  @Test
  public void testScanWorkflow_EmptyRequestTests() throws TException {

    // Test when the request is null.
    try {
      migrationService.ScanWorkflowExecutions(null);
    } catch (BadRequestError e) {
      assertEquals("List request is null", e.getMessage());
    }

    // Test when the domain is null.
    try {
      migrationService.ScanWorkflowExecutions(new ListWorkflowExecutionsRequest().setPageSize(10));
    } catch (BadRequestError e) {
      assertEquals("Domain is null or empty", e.getMessage());
    }
  }

  // Test when an error is returned from the internal client, and the response is null.
  @Test
  public void testScanWorkflow_Error() throws TException {

    String domainNew = "test";

    when(serviceNew.ListWorkflowExecutions(any())).thenReturn(null);
    ListWorkflowExecutionsResponse response =
        migrationService.ScanWorkflowExecutions(
            new ListWorkflowExecutionsRequest().setDomain(domainNew));
    verify(serviceNew, times(1)).ScanWorkflowExecutions(any());
    assertNull(response);
  }

  // Test scanWorkflow when fetching only from the 'from' cluster.
  @Test
  public void testScanWorkflow_FromClusterOnly() throws TException {

    String domain = "test";
    int one = 1;

    ListWorkflowExecutionsRequest request =
        new ListWorkflowExecutionsRequest()
            .setDomain(domain)
            .setPageSize(one)
            .setNextPageToken("".getBytes());

    ListWorkflowExecutionsResponse mockEmptyResponse =
        new ListWorkflowExecutionsResponse()
            .setExecutions(new ArrayList<>())
            .setNextPageToken("".getBytes());

    // Mock the serviceOld to return an empty response.
    when(serviceOld.ScanWorkflowExecutions(any())).thenReturn(mockEmptyResponse);

    // Perform the test to check if the response is empty as the new cluster returned no results.
    ListWorkflowExecutionsResponse response = migrationService.ScanWorkflowExecutions(request);
    assertEquals(mockEmptyResponse, response);
  }

  @Test
  public void testScanWorkflows_ResponseWithToken() throws TException {

    String domainNew = "test";
    int one = 1;
    int two = 2;

    ListWorkflowExecutionsRequest request =
        new ListWorkflowExecutionsRequest()
            .setDomain(domainNew)
            .setPageSize(one)
            .setNextPageToken("".getBytes());

    ListWorkflowExecutionsResponse expectedResponseWithToken = new ListWorkflowExecutionsResponse();
    expectedResponseWithToken.setExecutions(new ArrayList<>());
    WorkflowExecutionInfo executionInfo1 = new WorkflowExecutionInfo();
    executionInfo1.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    WorkflowExecutionInfo executionInfo2 = new WorkflowExecutionInfo();
    executionInfo2.setExecution(
        new WorkflowExecution().setWorkflowId("testWfId").setRunId("testRunId"));
    expectedResponseWithToken.getExecutions().add(executionInfo1);
    expectedResponseWithToken.getExecutions().add(executionInfo2);
    expectedResponseWithToken.setNextPageToken("totestToken".getBytes());

    // Mock the serviceNew to return the expected response with a token.
    when(serviceNew.ScanWorkflowExecutions(any())).thenReturn(expectedResponseWithToken);

    // Perform the first test to check if the response contains a token from the new cluster.
    ListWorkflowExecutionsResponse response = migrationService.ScanWorkflowExecutions(request);
    assertEquals(expectedResponseWithToken, response);

    ListWorkflowExecutionsRequest requestTwoItems =
        new ListWorkflowExecutionsRequest()
            .setDomain(domainNew)
            .setPageSize(two)
            .setNextPageToken("".getBytes());

    // Perform the second test to check if the response contains a token from the new cluster for a
    // different page size.
    when(serviceNew.ScanWorkflowExecutions(any())).thenReturn(expectedResponseWithToken);
    response = migrationService.ScanWorkflowExecutions(requestTwoItems);
    assertEquals(expectedResponseWithToken, response);
  }
}
