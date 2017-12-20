package com.amazonaws.services.simpleworkflow.flow.worker;

import com.amazonaws.services.simpleworkflow.flow.AsyncDecisionContext;
import com.amazonaws.services.simpleworkflow.flow.WorkflowException;
import com.uber.cadence.HistoryEvent;

import java.util.concurrent.CancellationException;

public interface AsyncWorkflow {
    void start(HistoryEvent event, AsyncDecisionContext context) throws Exception;

    boolean eventLoop() throws Throwable;

    /**
     *
     * @return null means no output yet
     */
    byte[] getOutput();

    void cancel(CancellationException e);

    Throwable getFailure();

    boolean isCancelRequested();

    String getAsynchronousThreadDump();

    byte[] getWorkflowState() throws WorkflowException;

    void close();

    /**
     * @return time at which workflow can make progress.
     * For example when {@link com.uber.cadence.internal.dispatcher.WorkflowThread#sleep(long)} expires.
     */
    long getNextWakeUpTime();
}
