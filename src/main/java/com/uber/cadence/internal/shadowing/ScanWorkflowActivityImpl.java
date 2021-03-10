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

import com.uber.cadence.ListWorkflowExecutionsRequest;
import com.uber.cadence.ListWorkflowExecutionsResponse;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.WorkflowExecutionInfo;
import com.uber.cadence.internal.common.RpcRetryer;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.shadower.ScanWorkflowActivityParams;
import com.uber.cadence.shadower.ScanWorkflowActivityResult;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScanWorkflowActivityImpl implements ScanWorkflowActivity {

  private static final Logger log = LoggerFactory.getLogger(ScanWorkflowActivityImpl.class);

  private final IWorkflowService serviceClient;

  public ScanWorkflowActivityImpl(IWorkflowService serviceClient) {
    this.serviceClient = Objects.requireNonNull(serviceClient);
  }

  @Override
  public ScanWorkflowActivityResult scan(ScanWorkflowActivityParams params) throws Throwable {
    ListWorkflowExecutionsRequest scanRequest =
        new ListWorkflowExecutionsRequest()
            .setDomain(params.getDomain())
            .setNextPageToken(params.getNextPageToken())
            .setPageSize(params.getPageSize())
            .setQuery(params.getWorkflowQuery());
    ListWorkflowExecutionsResponse resp = scanWorkflows(scanRequest);

    List<WorkflowExecution> executions =
        samplingWorkflows(resp.getExecutions(), params.getSamplingRate());

    return new ScanWorkflowActivityResult()
        .setExecutions(executions)
        .setNextPageToken(resp.getNextPageToken());
  }

  public ListWorkflowExecutionsResponse scanWorkflows(ListWorkflowExecutionsRequest request)
      throws Throwable {
    try {
      return RpcRetryer.retryWithResult(
          RpcRetryer.DEFAULT_RPC_RETRY_OPTIONS,
          () -> this.serviceClient.ScanWorkflowExecutions(request));
    } catch (Throwable t) {
      // TODO: handle non retryable error
      log.error(
          "failed to scan workflow records with domain: "
              + request.getDomain()
              + "; query: "
              + request.getQuery(),
          t);
      throw t;
    }
  }

  public List<WorkflowExecution> samplingWorkflows(
      List<WorkflowExecutionInfo> executionInfoList, double samplingRate) {
    int capacity = executionInfoList.size();
    return executionInfoList
        .stream()
        .unordered()
        .map((executionInfo -> executionInfo.getExecution()))
        .limit((long) (capacity * samplingRate))
        .collect(Collectors.toList());
  }
}
