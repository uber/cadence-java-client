package com.uber.cadence.internal.dispatcher;

import com.uber.cadence.WorkflowExecutionStartedEventAttributes;

import java.util.concurrent.CancellationException;

public class WorkflowRunnable implements Runnable {
    private final SyncDecisionContext context;
    private final SyncWorkflowDefinition workflow;
    private final WorkflowExecutionStartedEventAttributes attributes;

    private Throwable failure;
    private boolean cancelRequested;
    private byte[] output;

    public WorkflowRunnable(SyncDecisionContext syncDecisionContext,
                            SyncWorkflowDefinition workflow,
                            WorkflowExecutionStartedEventAttributes attributes) {
        this.context = syncDecisionContext;
        this.workflow = workflow;
        this.attributes = attributes;
    }

    public void cancel(CancellationException e) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public Throwable getFailure() {
        return failure;
    }

    public boolean isCancelRequested() {
        return cancelRequested;
    }

    @Override
    public void run() {
        output = workflow.execute(attributes.getInput());
    }

    public byte[] getOutput() {
        return output;
    }

    public void close() {

    }
}
