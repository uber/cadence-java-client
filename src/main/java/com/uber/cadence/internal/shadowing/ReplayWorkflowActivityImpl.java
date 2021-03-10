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

import com.google.common.collect.Lists;
import com.uber.cadence.GetWorkflowExecutionHistoryResponse;
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.common.WorkflowExecutionHistory;
import com.uber.cadence.internal.common.RpcRetryer;
import com.uber.cadence.internal.common.WorkflowExecutionUtils;
import com.uber.cadence.internal.metrics.MetricsType;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.shadower.ReplayWorkflowActivityParams;
import com.uber.cadence.shadower.ReplayWorkflowActivityResult;
import com.uber.cadence.testing.WorkflowReplayer;
import com.uber.m3.tally.Scope;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplayWorkflowActivityImpl implements ReplayWorkflowActivity {

  private static final Logger log = LoggerFactory.getLogger(ReplayWorkflowActivityImpl.class);

  private final IWorkflowService serviceClient;
  private final boolean failFast;
  private final Scope metricsScope;
  private final Class<?> workflowClass;
  private final Class<?>[] workflowClasses;

  public ReplayWorkflowActivityImpl(
      IWorkflowService serviceClient,
      boolean failFast,
      Scope metricsScope,
      Class<?> workflowClass,
      Class<?>... workflowClasses) {
    this.serviceClient = serviceClient;
    this.failFast = failFast;
    this.metricsScope = metricsScope;
    this.workflowClass = workflowClass;
    this.workflowClasses = workflowClasses;
  }

  @Override
  public ReplayWorkflowActivityResult replay(ReplayWorkflowActivityParams request)
      throws Throwable {
    if (request == null) {
      throw new NullPointerException("Replay activity request is null.");
    }

    String domain = request.getDomain();
    int successCount = 0;
    int failedCount = 0;
    int skippedCount = 0;

    for (WorkflowExecution execution : request.getExecutions()) {

      WorkflowExecutionHistory workflowHistory;
      try {
        workflowHistory = getFullHistory(domain, execution);
      } catch (Exception e) {
        log.error(
            "failed to get workflow history with domain: "
                + request.getDomain()
                + " "
                + execution.toString(),
            e);
        skippedCount++;
        continue;
      }

      try {
        WorkflowReplayer.replayWorkflowExecution(workflowHistory, workflowClass, workflowClasses);
      } catch (Exception e) {
        log.error(
            "failed to replay workflow history with domain: "
                + request.getDomain()
                + " "
                + execution.toString(),
            e);

        // Count any error with keyword 'nondeterministic' as non deterministic error.
        if (e.getCause().getMessage().contains("nondeterministic")) {
          failedCount++;
          this.metricsScope.counter(MetricsType.SHADOWING_NON_DETERMINISTIC_ERROR).inc(1);
          if (this.failFast) {
            return new ReplayWorkflowActivityResult()
                .setSucceeded(successCount)
                .setFailed(failedCount)
                .setSkipped(skippedCount);
          }
        } else {
          this.metricsScope.counter(MetricsType.SHADOWING_SKIPPED_COUNT).inc(1);
          skippedCount++;
        }
        continue;
      }
      this.metricsScope.counter(MetricsType.SHADOWING_SUCCESS_COUNT).inc(1);
      successCount++;
    }
    return new ReplayWorkflowActivityResult()
        .setSucceeded(successCount)
        .setFailed(failedCount)
        .setSkipped(skippedCount);
  }

  public WorkflowExecutionHistory getFullHistory(String domain, WorkflowExecution execution) {
    byte[] pageToken = null;
    List<HistoryEvent> histories = Lists.newArrayList();
    do {
      byte[] nextPageToken = pageToken;
      GetWorkflowExecutionHistoryResponse resp =
          RpcRetryer.retryWithResult(
              RpcRetryer.DEFAULT_RPC_RETRY_OPTIONS,
              () ->
                  WorkflowExecutionUtils.getHistoryPage(
                      nextPageToken, this.serviceClient, domain, execution));
      pageToken = resp.getNextPageToken();

      if (resp.getRawHistory() != null && resp.getRawHistory().size() > 0) {
        // TODO: handle raw history
      } else {
        histories.addAll(resp.getHistory().getEvents());
      }
    } while (pageToken != null);

    return new WorkflowExecutionHistory(histories);
  }
}
