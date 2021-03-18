package com.uber.cadence.testing;

import com.cronutils.utils.VisibleForTesting;
import com.google.common.collect.Lists;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.internal.shadowing.ReplayWorkflowActivity;
import com.uber.cadence.internal.shadowing.ReplayWorkflowActivityImpl;
import com.uber.cadence.internal.shadowing.ScanWorkflowActivity;
import com.uber.cadence.internal.shadowing.ScanWorkflowActivityImpl;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.shadower.ReplayWorkflowActivityParams;
import com.uber.cadence.shadower.ReplayWorkflowActivityResult;
import com.uber.cadence.shadower.ScanWorkflowActivityParams;
import com.uber.cadence.shadower.ScanWorkflowActivityResult;
import com.uber.cadence.worker.ShadowingOptions;
import com.uber.m3.tally.NoopScope;
import com.uber.m3.tally.Scope;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public final class WorkflowShadower {

    private final ShadowingOptions options;
    private final String query;
    private final ScanWorkflowActivity scanWorkflow;
    private final ReplayWorkflowActivity replayWorkflow;

    public WorkflowShadower(IWorkflowService service, ShadowingOptions options, String taskList) {

        this(service, options, taskList, new NoopScope());
    }

    public WorkflowShadower(IWorkflowService service, ShadowingOptions options, String taskList, Scope metricsScope) {
        // TODO: replace the null with taskList
        this(options, new ScanWorkflowActivityImpl(service), new ReplayWorkflowActivityImpl(service, metricsScope, null));
    }

    @VisibleForTesting
    public WorkflowShadower(
            ShadowingOptions options,
            ScanWorkflowActivity scanWorkflow,
            ReplayWorkflowActivity replayWorkflow) {
        this.options = options;
        // TODO: generate query from shadowing options
        this.query = "";
        this.scanWorkflow = scanWorkflow;
        this.replayWorkflow = replayWorkflow;
    }

    public void run() throws Throwable {
        byte[] nextPageToken = null;
        int replayCount = 0;

        int maxReplayCount = Integer.MAX_VALUE;
        Duration maxReplayDuration = ChronoUnit.FOREVER.getDuration();
        ZonedDateTime now = ZonedDateTime.now();
        if (options.getExitCondition() != null) {
            if (options.getExitCondition().getShadowingCount() != 0) {
                maxReplayCount = options.getExitCondition().getShadowingCount();
            }
            if (options.getExitCondition().getShadowingDuration() != null) {
                maxReplayDuration = options.getExitCondition().getShadowingDuration();
            }
        }
        do {
            ScanWorkflowActivityResult ScanResult = scanWorkflow.scan(new ScanWorkflowActivityParams()
            .setDomain(options.getDomain())
            .setWorkflowQuery(query)
            .setNextPageToken(nextPageToken)
            .setSamplingRate(options.getSamplingRate()));
            nextPageToken = ScanResult.getNextPageToken();

            for(WorkflowExecution execution : ScanResult.getExecutions()) {
                ReplayWorkflowActivityResult replayResult = replayWorkflow.replay(new ReplayWorkflowActivityParams()
                .setDomain(options.getDomain())
                .setExecutions(Lists.newArrayList(execution)));

                if (replayResult.getFailed() > 0) {
                    throw new Error("Replay workflow history failed with execution:" + execution.toString());
                } else if (replayResult.getSucceeded() > 0) {
                    replayCount++;
                }

                // Check exit condition
                if (replayCount >= maxReplayCount) {
                    return;
                }
                if (ZonedDateTime.now().isAfter(now.plus(maxReplayDuration))) {
                    return;
                }
            }
        } while(nextPageToken != null);
    }
}
