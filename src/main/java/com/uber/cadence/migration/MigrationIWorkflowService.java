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
      serviceNew.StartWorkflowExecution(startRequest);

    return serviceOld.StartWorkflowExecution(startRequest);
  }

  private boolean shouldStartInNew(String workflowID) {

    Thread serviceNewThread =
        new Thread(
            () -> {
              try {
                serviceNew.DescribeWorkflowExecution(
                    new DescribeWorkflowExecutionRequest()
                        .setDomain(domainNew)
                        .setExecution(new WorkflowExecution().setWorkflowId(workflowID)));
              } catch (EntityNotExistsError e) {
                throw new RuntimeException("Entity does not exist in new domain" + domainNew, e);
              } catch (TException e) {
                throw new RuntimeException(e);
              }
            });
    serviceNewThread.start();

    Thread serviceOldThread =
        new Thread(
            () -> {
              try {
                serviceOld.DescribeWorkflowExecution(
                    new DescribeWorkflowExecutionRequest()
                        .setDomain(domainOld)
                        .setExecution(new WorkflowExecution().setWorkflowId(workflowID)));
              } catch (EntityNotExistsError e) {
                throw new RuntimeException("Entity does not exist in old domain" + domainOld, e);
              } catch (TException e) {
                throw new RuntimeException(e);
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
    if (serviceNew != null && serviceOld != null) {
      return true;
    }

    return false;
  }
}
