package com.uber.cadence.internal.dispatcher;

import com.amazonaws.services.simpleworkflow.flow.AsyncDecisionContext;
import com.amazonaws.services.simpleworkflow.flow.WorkflowException;
import com.amazonaws.services.simpleworkflow.flow.worker.AsyncWorkflow;
import com.uber.cadence.EventType;
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.WorkflowType;

import java.util.concurrent.CancellationException;
import java.util.function.Function;

/**
 * The best inheritance hierarchy :).
 * SyncWorkflow supports workflows that use blocking code.
 * <p>
 * TOOD: rename AsyncWorkflow to something more reasonable.
 */
public class SyncWorkflow implements AsyncWorkflow {

    private DeterministicRunner runner;
    private final Function<WorkflowType, SyncWorkflowDefinition> factory;
    private WorkflowRunnable runnable;

    public SyncWorkflow(Function<WorkflowType, SyncWorkflowDefinition> factory) {
        this.factory = factory;
    }

    @Override
    public void start(HistoryEvent event, AsyncDecisionContext context) {
        WorkflowType workflowType = event.getWorkflowExecutionStartedEventAttributes().getWorkflowType();
        SyncWorkflowDefinition workflow = factory.apply(workflowType);
        SyncDecisionContext syncContext = new SyncDecisionContext(context);
        if (event.getEventType() != EventType.WorkflowExecutionStarted) {
            throw new IllegalArgumentException("first event is not WorkflowExecutionStarted, but "
                    + event.getEventType());
        }

        runnable = new WorkflowRunnable(syncContext, workflow, event.getWorkflowExecutionStartedEventAttributes());
        runner = DeterministicRunner.newRunner(syncContext, ()-> context.getWorkflowClock().currentTimeMillis(), runnable);
    }

    @Override
    public boolean eventLoop() throws Throwable {
        runner.runUntilAllBlocked();
        return runner.isDone();
    }

    @Override
    public byte[] getOutput() {
        return runnable.getOutput();
    }

    @Override
    public void cancel(CancellationException e) {
        runnable.cancel(e);
    }

    @Override
    public Throwable getFailure() {
        return runnable.getFailure();
    }

    @Override
    public boolean isCancelRequested() {
        return runnable.isCancelRequested();
    }

    @Override
    public String getAsynchronousThreadDump() {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public byte[] getWorkflowState() throws WorkflowException {
        throw new UnsupportedOperationException("not supported by Cadence, use query instead");
    }

    @Override
    public void close() {
        runnable.close();
    }

    @Override
    public long getNextWakeUpTime() {
        return runner.getNextWakeUpTime();
    }
}
