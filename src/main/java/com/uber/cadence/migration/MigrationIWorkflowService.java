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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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

  private Boolean shouldStartInNew(String workflowID) throws TException {
    try {
      return describeWorkflowExecution(serviceNew, domainNew, workflowID)
          .thenCombine(
              describeWorkflowExecution(serviceOld, domainOld, workflowID),
              (respNew, respOld) ->
                  respNew != null // execution already in new
                      || respOld == null // execution not exist in new and not exist in old
                      || (respOld.isSetWorkflowExecutionInfo()
                          && respOld
                              .getWorkflowExecutionInfo()
                              .isSetCloseStatus()) // execution not exist in new and execution is
                                                   // closed in old
              )
          .get();
    } catch (CompletionException e) {
      throw e.getCause() instanceof TException
          ? (TException) e.getCause()
          : new TException("unknown error: " + e.getMessage());
    } catch (Exception e) {
      throw new TException("Unknown error: " + e.getMessage());
    }
  }

  private CompletableFuture<DescribeWorkflowExecutionResponse> describeWorkflowExecution(
      IWorkflowService service, String domain, String workflowID) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return service.DescribeWorkflowExecution(
                new DescribeWorkflowExecutionRequest()
                    .setDomain(domain)
                    .setExecution(new WorkflowExecution().setWorkflowId(workflowID)));
          } catch (EntityNotExistsError e) {
            return null;
          } catch (Exception e) {
            throw new CompletionException(e);
          }
        });
  }

  @Override
  public StartWorkflowExecutionResponse StartWorkflowExecution(
      StartWorkflowExecutionRequest startRequest) throws TException {

    if (shouldStartInNew(startRequest.getWorkflowId()))
      return serviceNew.StartWorkflowExecution(startRequest);

    return serviceOld.StartWorkflowExecution(startRequest);
  }

  @Override
  public StartWorkflowExecutionResponse SignalWithStartWorkflowExecution(
      SignalWithStartWorkflowExecutionRequest signalWithStartRequest) throws TException {
    if (shouldStartInNew(signalWithStartRequest.getWorkflowId()))
      return serviceNew.SignalWithStartWorkflowExecution(signalWithStartRequest);
    return serviceOld.SignalWithStartWorkflowExecution(signalWithStartRequest);
  }

  @Override
  public GetWorkflowExecutionHistoryResponse GetWorkflowExecutionHistory(
      GetWorkflowExecutionHistoryRequest getRequest) throws TException {
    if (shouldStartInNew(getRequest.execution.getWorkflowId()))
      return serviceNew.GetWorkflowExecutionHistory(getRequest);
    return serviceOld.GetWorkflowExecutionHistory(getRequest);
  }
}
