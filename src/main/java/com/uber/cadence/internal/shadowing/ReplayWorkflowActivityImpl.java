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
import com.uber.cadence.activity.Activity;
import com.uber.cadence.common.WorkflowExecutionHistory;
import com.uber.cadence.internal.common.InternalUtils;
import com.uber.cadence.internal.common.RpcRetryer;
import com.uber.cadence.internal.common.WorkflowExecutionUtils;
import com.uber.cadence.internal.metrics.MetricsType;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.shadower.ReplayWorkflowActivityParams;
import com.uber.cadence.shadower.ReplayWorkflowActivityResult;
import com.uber.cadence.testing.TestWorkflowEnvironment;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.worker.WorkflowImplementationOptions;
import com.uber.cadence.workflow.Functions;
import com.uber.m3.tally.Scope;
import com.uber.m3.tally.Stopwatch;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ReplayWorkflowActivityImpl implements ReplayWorkflowActivity {

  private static final Logger log = LoggerFactory.getLogger(ReplayWorkflowActivityImpl.class);

  private final IWorkflowService serviceClient;
  private final Scope metricsScope;
  private final Worker worker;

  public ReplayWorkflowActivityImpl(
      IWorkflowService serviceClient, Scope metricsScope, String taskList) {
    this.serviceClient = Objects.requireNonNull(serviceClient);
    this.metricsScope = Objects.requireNonNull(metricsScope);
    worker = TestWorkflowEnvironment.newInstance().newWorker(taskList);
  }

  @Override
  public void registerWorkflowImplementationTypes(Class<?>... workflowImplementationClasses) {
    worker.registerWorkflowImplementationTypes(workflowImplementationClasses);
  }

  @Override
  public void registerWorkflowImplementationTypesWithOptions(
      WorkflowImplementationOptions options, Class<?>... workflowImplementationClasses) {
    worker.registerWorkflowImplementationTypes(options, workflowImplementationClasses);
  }

  @Override
  public <R> void addWorkflowImplementationFactory(
      Class<R> workflowInterface, Functions.Func<R> factory) {
    worker.addWorkflowImplementationFactory(workflowInterface, factory);
  }

  @Override
  public <R> void addWorkflowImplementationFactoryWithOptions(
      WorkflowImplementationOptions options,
      Class<R> workflowInterface,
      Functions.Func<R> factory) {
    worker.addWorkflowImplementationFactory(options, workflowInterface, factory);
  }

  @Override
  public ReplayWorkflowActivityResult replay(ReplayWorkflowActivityParams request)
      throws Exception {
    if (request == null) {
      throw new NullPointerException("Replay activity request is null.");
    }

    String domain = request.getDomain();
    int successCount = 0;
    int failedCount = 0;
    int skippedCount = 0;
    int replayIndex = 0;
    List<WorkflowExecution> executions = request.getExecutions();

    // Retrieve process from heartbeat
    Optional<HeartbeatDetail> heartbeatDetail = Activity.getHeartbeatDetails(HeartbeatDetail.class);
    if (heartbeatDetail.isPresent()) {
      ReplayWorkflowActivityResult heartbeatResult = heartbeatDetail.get().getReplayResult();
      successCount = heartbeatResult.getSucceeded();
      failedCount = heartbeatResult.getFailed();
      skippedCount = heartbeatResult.getSkipped();
      replayIndex = heartbeatDetail.get().getReplayExecutionIndex() + 1;
    }

    for (; replayIndex < executions.size(); replayIndex++) {
      WorkflowExecution execution = executions.get(replayIndex);
      ReplayWorkflowActivityResult oneReplayResult = replayHelper(request.getDomain(), execution);
      successCount += oneReplayResult.getSucceeded();
      failedCount += oneReplayResult.getFailed();
      skippedCount += oneReplayResult.getSkipped();
      ReplayWorkflowActivityResult heartbeatResult =
          new ReplayWorkflowActivityResult()
              .setSucceeded(successCount)
              .setFailed(failedCount)
              .setSkipped(skippedCount);
      Activity.heartbeat(new HeartbeatDetail(heartbeatResult, replayIndex));
    }
    return new ReplayWorkflowActivityResult()
        .setSucceeded(successCount)
        .setFailed(failedCount)
        .setSkipped(skippedCount);
  }

  protected ReplayWorkflowActivityResult replayHelper(String domain, WorkflowExecution execution) {
    WorkflowExecutionHistory workflowHistory;
    try {
      workflowHistory = getFullHistory(domain, execution);
    } catch (Exception e) {
      log.error(
          "skipped workflow execution with domain: "
              + domain
              + ". Execution: "
              + execution.toString(),
          e);
      return new ReplayWorkflowActivityResult().setSkipped(1);
    }

    try {
      boolean isSuccess = replayWorkflowHistory(domain, execution, workflowHistory);
      if (isSuccess) {
        this.metricsScope.counter(MetricsType.REPLAY_SUCCESS_COUNTER).inc(1);
        return new ReplayWorkflowActivityResult().setSucceeded(1);
      } else {
        this.metricsScope.counter(MetricsType.REPLAY_SKIPPED_COUNTER).inc(1);
        return new ReplayWorkflowActivityResult().setSkipped(1);
      }
    } catch (NonRetryableException e) {
      throw e;
    } catch (Exception e) {
      this.metricsScope.counter(MetricsType.REPLAY_FAILED_COUNTER).inc(1);
      return new ReplayWorkflowActivityResult().setFailed(1);
    }
  }

  protected WorkflowExecutionHistory getFullHistory(String domain, WorkflowExecution execution)
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

  protected boolean replayWorkflowHistory(
      String domain, WorkflowExecution execution, WorkflowExecutionHistory workflowHistory)
      throws Exception {
    Stopwatch sw = this.metricsScope.timer(MetricsType.REPLAY_LATENCY).start();
    try {
      worker.replayWorkflowExecution(workflowHistory);
    } catch (Exception e) {
      if (isNonDeterministicError(e)) {
        log.error(
            "failed to replay workflow history with domain: "
                + domain
                + ". Execution: "
                + execution.toString(),
            e);
        throw e;
      } else if (isWorkflowTypeNotRegisterError(e)) {
        log.info("replay unregistered workflow execution: {}", execution.toString(), e);
        throw new NonRetryableException(e);
      } else {
        log.info("replay workflow execution: {} skipped", execution.toString(), e);
        return false;
      }
    } finally {
      sw.stop();
    }

    log.info("replay workflow execution: {} succeed", execution.toString());
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

  private class HeartbeatDetail {
    private final ReplayWorkflowActivityResult replayResult;
    private final int replayExecutionIndex;

    public HeartbeatDetail(ReplayWorkflowActivityResult replayResult, int replayExecutionIndex) {
      this.replayResult = replayResult;
      this.replayExecutionIndex = replayExecutionIndex;
    }

    public ReplayWorkflowActivityResult getReplayResult() {
      return replayResult;
    }

    public int getReplayExecutionIndex() {
      return replayExecutionIndex;
    }
  }
}
