package com.uber.cadence.migration;

import com.uber.cadence.internal.sync.SyncWorkflowDefinition;
import com.uber.cadence.workflow.Workflow;
import com.uber.cadence.workflow.WorkflowInterceptor;
import com.uber.cadence.workflow.WorkflowInterceptorBase;

public class MigrationInterceptor extends WorkflowInterceptorBase {

    private final WorkflowInterceptor next;
    private static final String versionChangeID = "cadenceMigrationInterceptor";
    public static final int versionV1 = 1;
    public MigrationInterceptor(WorkflowInterceptor next) {
        super(next);
        this.next = next;
    }

    @Override
    public byte[] executeWorkflow(SyncWorkflowDefinition workflowDefinition, WorkflowExecuteInput input) {

        int version = getVersion(versionChangeID, Workflow.DEFAULT_VERSION, versionV1);


        return next.executeWorkflow(workflowDefinition,input);
    }

}
