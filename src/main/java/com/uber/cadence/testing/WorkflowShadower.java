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
package com.uber.cadence.testing;

import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.internal.shadowing.ReplayWorkflowActivity;
import com.uber.cadence.internal.shadowing.ReplayWorkflowActivityImpl;
import com.uber.cadence.internal.shadowing.ScanWorkflowActivity;
import com.uber.cadence.internal.shadowing.ScanWorkflowActivityImpl;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.shadower.Mode;
import com.uber.cadence.shadower.ReplayWorkflowActivityResult;
import com.uber.cadence.shadower.ScanWorkflowActivityParams;
import com.uber.cadence.shadower.ScanWorkflowActivityResult;
import com.uber.cadence.worker.ShadowingOptions;
import com.uber.m3.tally.NoopScope;
import com.uber.m3.tally.Scope;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;

public final class WorkflowShadower {
  private static final long SLEEP_INTERVAL = 300L;

  private final ShadowingOptions options;
  private final String query;
  private final ScanWorkflowActivity scanWorkflow;
  private final ReplayWorkflowActivity replayWorkflow;

  public WorkflowShadower(IWorkflowService service, ShadowingOptions options, String taskList) {

    this(service, options, taskList, new NoopScope());
  }

  public WorkflowShadower(
      IWorkflowService service, ShadowingOptions options, String taskList, Scope metricsScope) {
    this(
        options,
        new ScanWorkflowActivityImpl(service),
        new ReplayWorkflowActivityImpl(service, metricsScope, taskList));
  }

  public WorkflowShadower(
      ShadowingOptions options,
      ScanWorkflowActivity scanWorkflow,
      ReplayWorkflowActivity replayWorkflow) {
    this.options = validateShadowingOptions(options);
    this.query = options.getWorkflowQuery();
    this.scanWorkflow = scanWorkflow;
    this.replayWorkflow = replayWorkflow;
  }

  public void run() throws Throwable {
    byte[] nextPageToken = null;
    int replayCount = 0;

    int maxReplayCount = Integer.MAX_VALUE;
    Duration maxReplayDuration = Duration.ZERO;
    ZonedDateTime now = ZonedDateTime.now();
    if (options.getExitCondition() != null) {
      if (options.getExitCondition().getShadowCount() != 0) {
        maxReplayCount = options.getExitCondition().getShadowCount();
      }
      if (options.getExitCondition().getExpirationIntervalInSeconds() != 0) {
        maxReplayDuration =
            Duration.ofSeconds(options.getExitCondition().getExpirationIntervalInSeconds());
      }
    }
    do {
      ScanWorkflowActivityResult scanResult =
          scanWorkflow.scan(
              new ScanWorkflowActivityParams()
                  .setDomain(options.getDomain())
                  .setWorkflowQuery(query)
                  .setNextPageToken(nextPageToken)
                  .setSamplingRate(options.getSamplingRate()));
      nextPageToken = scanResult.getNextPageToken();

      for (WorkflowExecution execution : scanResult.getExecutions()) {
        ReplayWorkflowActivityResult replayResult =
            replayWorkflow.replayOneExecution(options.getDomain(), execution);

        if (replayResult.getFailed() > 0) {
          throw new Error("Replay workflow history failed with execution:" + execution.toString());
        } else if (replayResult.getSucceeded() > 0) {
          replayCount++;
        }

        // Check exit condition
        if (replayCount >= maxReplayCount) {
          return;
        }
        if (!maxReplayDuration.isZero()
            && ZonedDateTime.now().isAfter(now.plusSeconds(maxReplayDuration.getSeconds()))) {
          return;
        }
      }

      if (nextPageToken == null && options.getShadowMode() == Mode.Continuous) {
        Thread.sleep(SLEEP_INTERVAL);
      }
    } while (nextPageToken != null && options.getShadowMode() == Mode.Normal);
  }

  private ShadowingOptions validateShadowingOptions(ShadowingOptions options) {
    Objects.requireNonNull(options);

    if (options.getConcurrency() > 1) {
      throw new IllegalArgumentException("Concurrency is not supported in workflow shadower");
    }
    return options;
  }
}
