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

import com.uber.cadence.BadRequestError;
import com.uber.cadence.ClientVersionNotSupportedError;
import com.uber.cadence.EntityNotExistsError;
import com.uber.cadence.ListWorkflowExecutionsRequest;
import com.uber.cadence.ListWorkflowExecutionsResponse;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.WorkflowExecutionInfo;
import com.uber.cadence.internal.common.RpcRetryer;
import com.uber.cadence.serviceclient.IWorkflowService;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ScanWorkflowActivityImpl implements ScanWorkflowActivity {

  private static final Logger log = LoggerFactory.getLogger(ScanWorkflowActivityImpl.class);

  private final IWorkflowService serviceClient;

  public ScanWorkflowActivityImpl(IWorkflowService serviceClient) {
    this.serviceClient = Objects.requireNonNull(serviceClient);
  }

  @Override
  public ScanWorkflowActivityResult scan(ScanWorkflowActivityParams params) throws Throwable {
    log.info("Starting scan with params: {} ", params);

    ListWorkflowExecutionsRequest scanRequest =
        new ListWorkflowExecutionsRequest()
            .setDomain(params.getDomain())
            .setNextPageToken(params.getNextPageToken())
            .setPageSize(params.getPageSize())
            .setQuery(params.getWorkflowQuery());

    log.debug("Created ListWorkflowExecutionsRequest: {} ", scanRequest);
    log.info("Scanning workflows with query: {}", params.getWorkflowQuery());

    ListWorkflowExecutionsResponse resp = scanWorkflows(scanRequest);

    log.info("Received response with {} executions", resp.getExecutions().size());

    List<WorkflowExecution> executions =
        samplingWorkflows(resp.getExecutions(), params.getSamplingRate());

    ScanWorkflowActivityResult result = new ScanWorkflowActivityResult();
    result.setExecutions(
        executions
            .stream()
            .map(com.uber.cadence.internal.shadowing.WorkflowExecution::new)
            .collect(Collectors.toList()));
    result.setNextPageToken(resp.getNextPageToken());
    log.info("Scan completed with {} sampled executions", executions.size());
    return result;
  }

  protected ListWorkflowExecutionsResponse scanWorkflows(ListWorkflowExecutionsRequest request)
      throws Throwable {
    log.info(
        "Scanning workflows for domain: {} with query: {}",
        request.getDomain(),
        request.getQuery());
    try {
      ListWorkflowExecutionsResponse response =
          RpcRetryer.retryWithResult(
              RpcRetryer.DEFAULT_RPC_RETRY_OPTIONS,
              () -> this.serviceClient.ScanWorkflowExecutions(request));
      log.info("Successfully scanned workflows for domain: {}", request.getDomain());
      return response;
    } catch (BadRequestError | EntityNotExistsError | ClientVersionNotSupportedError e) {
      log.error(
          "failed to scan workflow records with non-retryable error. Domain: "
              + request.getDomain()
              + "; query: "
              + request.getQuery(),
          e);
      throw new NonRetryableException(e);
    } catch (Throwable t) {
      log.error(
          "failed to scan workflow records with domain: "
              + request.getDomain()
              + "; query: "
              + request.getQuery(),
          t);
      throw t;
    }
  }

  protected List<WorkflowExecution> samplingWorkflows(
      List<WorkflowExecutionInfo> executionInfoList, double samplingRate) {
    log.info("Sampling workflows with rate: {}", samplingRate);
    int capacity = (int) (executionInfoList.size() * samplingRate);
    capacity = Math.max(capacity, 1);
    List<WorkflowExecution> sampledExecutions =
        executionInfoList
            .stream()
            .unordered()
            .map((executionInfo -> executionInfo.getExecution()))
            .limit((long) (capacity))
            .collect(Collectors.toList());
    log.info("Sampled {} workflows out of {}", sampledExecutions.size(), executionInfoList.size());
    return sampledExecutions;
  }
}
