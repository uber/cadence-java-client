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

import static com.uber.cadence.internal.errors.ErrorType.UNKNOWN_WORKFLOW_TYPE;

import com.google.common.collect.Lists;
import com.uber.cadence.GetWorkflowExecutionHistoryResponse;
import com.uber.cadence.History;
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.HistoryEventFilterType;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.common.WorkflowExecutionHistory;
import com.uber.cadence.internal.common.InternalUtils;
import com.uber.cadence.internal.common.RpcRetryer;
import com.uber.cadence.internal.common.WorkflowExecutionUtils;
import com.uber.cadence.internal.metrics.MetricsType;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.shadower.ReplayWorkflowActivityParams;
import com.uber.cadence.shadower.ReplayWorkflowActivityResult;
import com.uber.cadence.testing.WorkflowReplayer;
import com.uber.m3.tally.Scope;
import com.uber.m3.tally.Stopwatch;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplayWorkflowActivityImpl implements ReplayWorkflowActivity {

  private static final Logger log = LoggerFactory.getLogger(ReplayWorkflowActivityImpl.class);

  private final IWorkflowService serviceClient;
  private final Scope metricsScope;
  private final Class<?> workflowClass;
  private final Class<?>[] workflowClasses;

  public ReplayWorkflowActivityImpl(
      IWorkflowService serviceClient,
      Scope metricsScope,
      Class<?> workflowClass,
      Class<?>... workflowClasses) {
    this.serviceClient = Objects.requireNonNull(serviceClient);
    this.metricsScope = Objects.requireNonNull(metricsScope);
    this.workflowClass = Objects.requireNonNull(workflowClass);
    this.workflowClasses = workflowClasses;
  }

  @Override
  public ReplayWorkflowActivityResult replay(ReplayWorkflowActivityParams request) {
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
            "skipped workflow execution with domain: "
                + request.getDomain()
                + ". Execution: "
                + execution.toString(),
            e);
        skippedCount++;
        continue;
      }

      try {
        boolean isSuccess = replayWorkflowHistory(request.getDomain(), execution, workflowHistory);
        if (isSuccess) {
          this.metricsScope.counter(MetricsType.REPLAY_SUCCESS_COUNTER).inc(1);
          successCount++;
        } else {
          this.metricsScope.counter(MetricsType.REPLAY_SKIPPED_COUNTER).inc(1);
          skippedCount++;
        }
      } catch (Exception e) {
        this.metricsScope.counter(MetricsType.REPLAY_FAILED_COUNTER).inc(1);
        failedCount++;
        if (isWorkflowTypeNotRegisterError(e)) {
          return new ReplayWorkflowActivityResult()
              .setSucceeded(successCount)
              .setFailed(failedCount)
              .setSkipped(skippedCount);
        }
      }
    }
    return new ReplayWorkflowActivityResult()
        .setSucceeded(successCount)
        .setFailed(failedCount)
        .setSkipped(skippedCount);
  }

  public WorkflowExecutionHistory getFullHistory(String domain, WorkflowExecution execution)
      throws Exception {
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

      // handle raw history
      if (resp.getRawHistory() != null && resp.getRawHistory().size() > 0) {
        History history =
            InternalUtils.DeserializeFromBlobDataToHistory(
                resp.getRawHistory(), HistoryEventFilterType.ALL_EVENT);
        if (history != null && history.getEvents() != null) {
          histories.addAll(history.getEvents());
        }
      } else {
        histories.addAll(resp.getHistory().getEvents());
      }
    } while (pageToken != null);

    return new WorkflowExecutionHistory(histories);
  }

  public boolean replayWorkflowHistory(
      String domain, WorkflowExecution execution, WorkflowExecutionHistory workflowHistory)
      throws Exception {
    Stopwatch sw = this.metricsScope.timer(MetricsType.REPLAY_LATENCY).start();
    try {
      WorkflowReplayer.replayWorkflowExecution(workflowHistory, workflowClass, workflowClasses);
    } catch (Exception e) {
      if (isNonDeterministicError(e) || isWorkflowTypeNotRegisterError(e)) {
        log.error(
            "failed to replay workflow history with domain: "
                + domain
                + ". Execution: "
                + execution.toString(),
            e);
        throw e;
      } else {
        log.info("replay workflow skipped", e);
        return false;
      }
    } finally {
      sw.stop();
    }

    log.info("replay workflow succeed");
    return true;
  }

  private boolean isNonDeterministicError(Exception e) {
    if (e != null && e.getMessage() != null && e.getMessage().contains("nondeterministic")) {
      return true;
    } else {
      return false;
    }
  }

  private boolean isWorkflowTypeNotRegisterError(Exception e) {
    if (e != null && e.getMessage() != null && e.getMessage().contains(UNKNOWN_WORKFLOW_TYPE)) {
      return true;
    } else {
      return false;
    }
  }
}
