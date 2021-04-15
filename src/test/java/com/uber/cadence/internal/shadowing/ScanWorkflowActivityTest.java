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
package com.uber.cadence.internal.shadowing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.uber.cadence.BadRequestError;
import com.uber.cadence.ListWorkflowExecutionsRequest;
import com.uber.cadence.ListWorkflowExecutionsResponse;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.WorkflowExecutionInfo;
import com.uber.cadence.serviceclient.IWorkflowService;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Test;

public class ScanWorkflowActivityTest {
  private IWorkflowService mockServiceClient = mock(IWorkflowService.class);
  private ScanWorkflowActivityImpl activity = new ScanWorkflowActivityImpl(mockServiceClient);

  @Test
  public void testSamplingWorkflows_PointOneSampling_ExpectedOneEntity() {
    List<WorkflowExecutionInfo> executionInfoList = Lists.newArrayList();
    for (int i = 0; i < 10; i++) {
      WorkflowExecutionInfo executionInfo =
          new WorkflowExecutionInfo()
              .setExecution(new WorkflowExecution().setWorkflowId("wid").setRunId("rid"));
      executionInfoList.add(executionInfo);
    }

    List<WorkflowExecution> workflowExecutions = activity.samplingWorkflows(executionInfoList, 0.1);
    assertEquals(1, workflowExecutions.size());
  }

  @Test
  public void testSamplingWorkflows_PointNineSampling_ExpectedNineEntity() {
    List<WorkflowExecutionInfo> executionInfoList = Lists.newArrayList();
    for (int i = 0; i < 10; i++) {
      WorkflowExecutionInfo executionInfo =
          new WorkflowExecutionInfo()
              .setExecution(new WorkflowExecution().setWorkflowId("wid").setRunId("rid"));
      executionInfoList.add(executionInfo);
    }

    ScanWorkflowActivityImpl activity = new ScanWorkflowActivityImpl(mockServiceClient);
    List<WorkflowExecution> workflowExecutions = activity.samplingWorkflows(executionInfoList, 0.9);
    assertEquals(9, workflowExecutions.size());
  }

  @Test
  public void testScanWorkflows_ExpectedSuccessResponse() throws Throwable {
    ListWorkflowExecutionsResponse response = new ListWorkflowExecutionsResponse();
    when(mockServiceClient.ScanWorkflowExecutions(any())).thenReturn(response);

    ListWorkflowExecutionsResponse resp =
        activity.scanWorkflows(new ListWorkflowExecutionsRequest());
    assertEquals(response, resp);
  }

  @Test(expected = NonRetryableException.class)
  public void testScanWorkflows_ThrowBadRequestError() throws Throwable {
    ListWorkflowExecutionsResponse response = new ListWorkflowExecutionsResponse();
    when(mockServiceClient.ScanWorkflowExecutions(any())).thenThrow(new BadRequestError());

    activity.scanWorkflows(new ListWorkflowExecutionsRequest());
  }

  @Test
  public void testScan_ReturnSuccessResponse() throws Throwable {
    List<WorkflowExecutionInfo> executionInfoList = Lists.newArrayList();
    String domain = UUID.randomUUID().toString();
    String query = UUID.randomUUID().toString();
    int pageSize = 100;
    byte[] token = {1, 2, 3};
    double samplingRate = 1.0;

    ListWorkflowExecutionsRequest request =
        new ListWorkflowExecutionsRequest()
            .setQuery(query)
            .setDomain(domain)
            .setPageSize(pageSize)
            .setNextPageToken(token);
    for (int i = 0; i < 10; i++) {
      WorkflowExecutionInfo executionInfo =
          new WorkflowExecutionInfo()
              .setExecution(new WorkflowExecution().setWorkflowId("wid").setRunId("rid"));
      executionInfoList.add(executionInfo);
    }
    ListWorkflowExecutionsResponse response =
        new ListWorkflowExecutionsResponse()
            .setExecutions(executionInfoList)
            .setNextPageToken(token);
    when(mockServiceClient.ScanWorkflowExecutions(eq(request))).thenReturn(response);

    ScanWorkflowActivityParams params = new ScanWorkflowActivityParams();
    params.setDomain(domain);
    params.setWorkflowQuery(query);
    params.setPageSize(pageSize);
    params.setNextPageToken(token);
    params.setSamplingRate(samplingRate);
    ScanWorkflowActivityResult result = activity.scan(params);
    assertEquals(10, result.getExecutions().size());
    assertTrue(Arrays.equals(token, result.getNextPageToken()));
  }
}
