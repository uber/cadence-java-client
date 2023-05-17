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
import com.uber.cadence.serviceclient.DummyIWorkflowService;
import com.uber.cadence.serviceclient.IWorkflowService;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.thrift.TException;

public class MigrationIWorkflowService extends DummyIWorkflowService {

  private IWorkflowService serviceOld, serviceNew;
  private String domainOld, domainNew;

  MigrationIWorkflowService(
      IWorkflowService serviceOld,
      String domainOld,
      IWorkflowService serviceNew,
      String domainNew) {
    this.serviceOld = serviceOld;
    this.domainOld = domainOld;
    this.serviceNew = serviceNew;
    this.domainNew = domainNew;
  }

  @Override
  public StartWorkflowExecutionResponse StartWorkflowExecution(
      StartWorkflowExecutionRequest startRequest) throws TException {

    if (shouldStartInNew(startRequest.getWorkflowId()))
      return serviceNew.StartWorkflowExecution(startRequest);

    return serviceOld.StartWorkflowExecution(startRequest);
  }

  private boolean shouldStartInNew(String workflowID) {

    AtomicReference<DescribeWorkflowExecutionResponse> executionInNewResponse =
        new AtomicReference<>();
    AtomicReference<DescribeWorkflowExecutionResponse> executionInOldResponse =
        new AtomicReference<>();
    Thread serviceNewThread =
        new Thread(
            () -> {
              DescribeWorkflowExecutionRequest describeWorkflowExecutionRequest =
                  new DescribeWorkflowExecutionRequest();
              describeWorkflowExecutionRequest.setDomain(domainNew);
              describeWorkflowExecutionRequest.setExecution(
                  new WorkflowExecution().setWorkflowId(workflowID));
              try {
                //  DescribeWorkflowExecutionResponse executionInNewResponse = null;
                executionInNewResponse.set(
                    serviceNew.DescribeWorkflowExecution(
                        new DescribeWorkflowExecutionRequest()
                            .setDomain(domainNew)
                            .setExecution(new WorkflowExecution().setWorkflowId(workflowID))));
              } catch (EntityNotExistsError e) {
                // TODO perform any logging here
              } catch (TException e) {
                // TODO perform any logging here
              }
            });
    serviceNewThread.start();

    Thread serviceOldThread =
        new Thread(
            () -> {
              try {
                executionInOldResponse.set(
                    serviceOld.DescribeWorkflowExecution(
                        new DescribeWorkflowExecutionRequest()
                            .setDomain(domainOld)
                            .setExecution(new WorkflowExecution().setWorkflowId(workflowID))));
              } catch (EntityNotExistsError e) {
                // TODO perform any logging here
              } catch (TException e) {
                // TODO perform any logging here
              }
            });
    serviceOldThread.start();

    try {
      serviceNewThread.join();
      serviceOldThread.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // exist in both domains or exist in only new - start in new
    if (executionInNewResponse.get() != null && executionInOldResponse.get() != null) return true;
    // exist in old and workflow is still open
    if (executionInOldResponse.get() != null
        && executionInOldResponse.get().workflowExecutionInfo.closeStatus == null) return false;

    return true;
  }
}
