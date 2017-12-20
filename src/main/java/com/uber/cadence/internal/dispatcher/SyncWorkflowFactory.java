package com.uber.cadence.internal.dispatcher;

import com.amazonaws.services.simpleworkflow.flow.worker.AsyncWorkflow;
import com.amazonaws.services.simpleworkflow.flow.worker.AsyncWorkflowFactory;
import com.uber.cadence.WorkflowType;

import java.util.function.Function;

public class SyncWorkflowFactory implements AsyncWorkflowFactory {
    private final Function<WorkflowType, SyncWorkflowDefinition> factory;

    public SyncWorkflowFactory(Function<WorkflowType, SyncWorkflowDefinition> factory) {
        this.factory = factory;
    }

    @Override
    public AsyncWorkflow getWorkflow(WorkflowType workflowType) throws Exception {
        return new SyncWorkflow(factory);
    }
}
