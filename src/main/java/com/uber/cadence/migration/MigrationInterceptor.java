/*
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
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

package com.uber.cadence.migration;

import com.uber.cadence.*;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.internal.common.RpcRetryer;
import com.uber.cadence.internal.sync.SyncWorkflowDefinition;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.workflow.Workflow;
import com.uber.cadence.workflow.WorkflowInterceptor;
import com.uber.cadence.workflow.WorkflowInterceptorBase;
import org.apache.thrift.protocol.TField;

import java.util.UUID;

public class MigrationInterceptor extends WorkflowInterceptorBase {

  private final WorkflowInterceptor next;
  private static final String versionChangeID = "cadenceMigrationInterceptor";
  public static final int versionV1 = 1;

  public MigrationInterceptor(WorkflowInterceptor next) {
    super(next);
    this.next = next;
  }

  @Override
  public byte[] executeWorkflow(
      SyncWorkflowDefinition workflowDefinition, WorkflowExecuteInput input) {

    final IWorkflowService service = WorkflowClient.getService();

    int version = getVersion(versionChangeID, Workflow.DEFAULT_VERSION, versionV1);
    switch (version) {
      case versionV1:
        if(input.getWorkflowExecutionStartedEventAttributes().cronSchedule == "" || input.getWorkflowExecutionStartedEventAttributes().getParentWorkflowExecution().getWorkflowId() != ""){
          //TO DO: it is not a cron schedule, and we will directly send it to CustomerInterceptor's executeWorkflow
          //Customer will have option to choose to implement their own method or inform which workflows need to be migrated
          next.executeWorkflow()
                  break;
        }

        //it means it is cron schedule:
        // then call sideEffect and check for shouldMigrate
        // if yes, start an activity: startNewWorkflow in new domain
        //    // if it has been already started or completed, return cancelledError (standard cancellationException) \
        //    // if it is failed, fallback to next.executeWorkflow
        //Not required: SyncWorkflowDefinition sync = sideEffect(workflowDefinition, workflowDefinition.getClass(),input);
        if(shouldMigrate())
        {
//          startActivity();

          StartWorkflowExecutionRequest request =
                  new StartWorkflowExecutionRequest()
                          .setDomain(input.getWorkflowExecutionStartedEventAttributes().parentWorkflowDomain) //from where to get new domain name
                          .setWorkflowId(input.getWorkflowExecutionStartedEventAttributes().getIdentity())
                          .setTaskList(new TaskList().setName(input.getWorkflowExecutionStartedEventAttributes().taskList.getName()))
                          .setInput(input.getInput())
                          .setWorkflowType(new WorkflowType().setName(input.getWorkflowType().getName()))
                          .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.AllowDuplicate)
                          .setWorkflowIdReusePolicy(input.getWorkflowExecutionStartedEventAttributes().getRetryPolicy())
                          .setRequestId(UUID.randomUUID().toString())
                          .setExecutionStartToCloseTimeoutSeconds(864000)
                          .setTaskStartToCloseTimeoutSeconds(60);

          try {
            RpcRetryer.retryWithResult(
                    RpcRetryer.DEFAULT_RPC_RETRY_OPTIONS, () -> service.StartWorkflowExecution(request));
          } catch (WorkflowExecutionAlreadyStartedError e) {
            // cancelled error
            e.getMessage();
            e.getCause();
          }
          catch (Exception e)
          {
            e.getCause();
            next.executeWorkflow(workflowDefinition,input);
          }

        }


        //call workflowFunction
        next.continueAsNew(input.getWorkflowType(),input.getWorkflowExecutionStartedEventAttributes().continuedExecutionRunId, input.getWorkflowExecutionStartedEventAttributes().getFieldValue());
        //check whether the workflow is continueAsNew
          // If yes, start Activity: startNewWorkflow in new domain
                  //if already started/completed, return cancelledError
                  // if fails return error

        //return result (result mean the


      default:
        return next.executeWorkflow(workflowDefinition, input);

        //TO create a new workflow: a new instance of workflow implementation object is created.
      // call one of the methods annotated with @workflowMethod
      // no additional calls to workflow methods
      //
    }
  }

  private boolean shouldMigrate() {
    //To DO: Later if we add Flipr
    return true;
  }
}
