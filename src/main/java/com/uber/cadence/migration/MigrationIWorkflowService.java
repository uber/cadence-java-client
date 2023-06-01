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
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.apache.thrift.TException;

public class MigrationIWorkflowService extends DummyIWorkflowService {

  private IWorkflowService serviceOld, serviceNew;
  private String domainOld, domainNew;
  private static final int _defaultPageSize = 10;
  private static final String _listWorkflow = "_listWorkflow";
  byte[] _marker = "to".getBytes();

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

  private ListWorkflowExecutionsResponse callOldCluster(
      ListWorkflowExecutionsRequest listWorkflowExecutionsRequest,
      int pageSizeOverride,
      String searchType)
      throws TException {

    if (pageSizeOverride != 0) {
      listWorkflowExecutionsRequest.setPageSize(pageSizeOverride);
    }
    ListWorkflowExecutionsResponse response = new ListWorkflowExecutionsResponse();
    if (searchType == _listWorkflow) {
      response = serviceNew.ListWorkflowExecutions(listWorkflowExecutionsRequest);
    }
    // TODO add scanworkflow implementation and use this method
    //    else if(searchType == _scanWorkflow) {
    //      response = serviceNew.ListWorkflowExecutions(listWorkflowExecutionsRequest)
    //    }
    return response;
  }

  private ListWorkflowExecutionsResponse appendResultsFromOldCluster(
      ListWorkflowExecutionsRequest listWorkflowExecutionsRequest,
      ListWorkflowExecutionsResponse response,
      String searchType)
      throws TException {
    int responsePageSize = response.getExecutions().size();
    int neededPageSize = listWorkflowExecutionsRequest.getPageSize() - responsePageSize;

    ListWorkflowExecutionsResponse fromResponse =
        callOldCluster(listWorkflowExecutionsRequest, neededPageSize, searchType);
    fromResponse.getExecutions().addAll(response.getExecutions());
    return fromResponse;
  }

  public boolean hasPrefix(byte[] s, byte[] prefix) {
    return s.length >= prefix.length
        && Arrays.equals(Arrays.copyOfRange(s, 0, prefix.length), prefix);
  }

  @Override
  public ListWorkflowExecutionsResponse ListWorkflowExecutions(
      ListWorkflowExecutionsRequest listRequest) throws BadRequestError, TException {
    ListWorkflowExecutionsResponse response = new ListWorkflowExecutionsResponse();
    if (listRequest == null) {
      throw new BadRequestError("List request is null");
    } else if (listRequest.getDomain() == null) {
      throw new BadRequestError("Domain is null");
    }
    if (listRequest.getPageSize() == 0) {
      listRequest.pageSize = _defaultPageSize;
    }

    if (listRequest.getNextPageToken() == null
        || hasPrefix(listRequest.getNextPageToken(), _marker)) {
      if (hasPrefix(listRequest.getNextPageToken(), _marker)) {
        listRequest.setNextPageToken(
            Arrays.copyOfRange(
                listRequest.getNextPageToken(),
                _marker.length,
                listRequest.getNextPageToken().length));
      }
      response = serviceNew.ListWorkflowExecutions(listRequest);

      if (response.getExecutions().size() < listRequest.getPageSize()) {
        appendResultsFromOldCluster(listRequest, response, _listWorkflow);
      }

      byte[] combinedNextPageToken = new byte[_marker.length + response.getNextPageToken().length];
      System.arraycopy(_marker, 0, combinedNextPageToken, 0, _marker.length);
      System.arraycopy(
          response.getNextPageToken(),
          0,
          combinedNextPageToken,
          _marker.length,
          response.getNextPageToken().length);
      response.setNextPageToken(combinedNextPageToken);
      return response;
    }
    return callOldCluster(listRequest, 0, _listWorkflow);
  }
}
