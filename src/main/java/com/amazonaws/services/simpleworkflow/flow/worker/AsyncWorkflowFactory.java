package com.amazonaws.services.simpleworkflow.flow.worker;

import com.uber.cadence.WorkflowType;

public interface AsyncWorkflowFactory {
    AsyncWorkflow getWorkflow(WorkflowType workflowType) throws Exception;
}
