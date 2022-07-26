/*
 *  Modifications Copyright (c) 2017-2021 Uber Technologies Inc.
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.uber.cadence.internal.compatibility;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.uber.cadence.BadRequestError;
import com.uber.cadence.CancellationAlreadyRequestedError;
import com.uber.cadence.ClientVersionNotSupportedError;
import com.uber.cadence.ClusterInfo;
import com.uber.cadence.CountWorkflowExecutionsRequest;
import com.uber.cadence.CountWorkflowExecutionsResponse;
import com.uber.cadence.DeprecateDomainRequest;
import com.uber.cadence.DescribeDomainRequest;
import com.uber.cadence.DescribeDomainResponse;
import com.uber.cadence.DescribeTaskListRequest;
import com.uber.cadence.DescribeTaskListResponse;
import com.uber.cadence.DescribeWorkflowExecutionRequest;
import com.uber.cadence.DescribeWorkflowExecutionResponse;
import com.uber.cadence.DomainAlreadyExistsError;
import com.uber.cadence.DomainNotActiveError;
import com.uber.cadence.EntityNotExistsError;
import com.uber.cadence.GetSearchAttributesResponse;
import com.uber.cadence.GetTaskListsByDomainRequest;
import com.uber.cadence.GetTaskListsByDomainResponse;
import com.uber.cadence.GetWorkflowExecutionHistoryRequest;
import com.uber.cadence.GetWorkflowExecutionHistoryResponse;
import com.uber.cadence.InternalServiceError;
import com.uber.cadence.LimitExceededError;
import com.uber.cadence.ListArchivedWorkflowExecutionsRequest;
import com.uber.cadence.ListArchivedWorkflowExecutionsResponse;
import com.uber.cadence.ListClosedWorkflowExecutionsRequest;
import com.uber.cadence.ListClosedWorkflowExecutionsResponse;
import com.uber.cadence.ListDomainsRequest;
import com.uber.cadence.ListDomainsResponse;
import com.uber.cadence.ListOpenWorkflowExecutionsRequest;
import com.uber.cadence.ListOpenWorkflowExecutionsResponse;
import com.uber.cadence.ListTaskListPartitionsRequest;
import com.uber.cadence.ListTaskListPartitionsResponse;
import com.uber.cadence.ListWorkflowExecutionsRequest;
import com.uber.cadence.ListWorkflowExecutionsResponse;
import com.uber.cadence.PollForActivityTaskRequest;
import com.uber.cadence.PollForActivityTaskResponse;
import com.uber.cadence.PollForDecisionTaskRequest;
import com.uber.cadence.PollForDecisionTaskResponse;
import com.uber.cadence.QueryFailedError;
import com.uber.cadence.QueryWorkflowRequest;
import com.uber.cadence.QueryWorkflowResponse;
import com.uber.cadence.RecordActivityTaskHeartbeatByIDRequest;
import com.uber.cadence.RecordActivityTaskHeartbeatRequest;
import com.uber.cadence.RecordActivityTaskHeartbeatResponse;
import com.uber.cadence.RefreshWorkflowTasksRequest;
import com.uber.cadence.RegisterDomainRequest;
import com.uber.cadence.RequestCancelWorkflowExecutionRequest;
import com.uber.cadence.ResetStickyTaskListRequest;
import com.uber.cadence.ResetStickyTaskListResponse;
import com.uber.cadence.ResetWorkflowExecutionRequest;
import com.uber.cadence.ResetWorkflowExecutionResponse;
import com.uber.cadence.RespondActivityTaskCanceledByIDRequest;
import com.uber.cadence.RespondActivityTaskCanceledRequest;
import com.uber.cadence.RespondActivityTaskCompletedByIDRequest;
import com.uber.cadence.RespondActivityTaskCompletedRequest;
import com.uber.cadence.RespondActivityTaskFailedByIDRequest;
import com.uber.cadence.RespondActivityTaskFailedRequest;
import com.uber.cadence.RespondDecisionTaskCompletedRequest;
import com.uber.cadence.RespondDecisionTaskCompletedResponse;
import com.uber.cadence.RespondDecisionTaskFailedRequest;
import com.uber.cadence.RespondQueryTaskCompletedRequest;
import com.uber.cadence.ServiceBusyError;
import com.uber.cadence.SignalWithStartWorkflowExecutionRequest;
import com.uber.cadence.SignalWorkflowExecutionRequest;
import com.uber.cadence.StartWorkflowExecutionRequest;
import com.uber.cadence.StartWorkflowExecutionResponse;
import com.uber.cadence.TerminateWorkflowExecutionRequest;
import com.uber.cadence.UpdateDomainRequest;
import com.uber.cadence.UpdateDomainResponse;
import com.uber.cadence.WorkflowExecutionAlreadyCompletedError;
import com.uber.cadence.WorkflowExecutionAlreadyStartedError;
import com.uber.cadence.api.v1.GetSearchAttributesRequest;
import com.uber.cadence.api.v1.HealthRequest;
import com.uber.cadence.api.v1.HealthResponse;
import com.uber.cadence.internal.compatibility.proto.RequestMapper;
import com.uber.cadence.internal.compatibility.proto.serviceclient.IGrpcServiceStubs;
import com.uber.cadence.internal.compatibility.thrift.ResponseMapper;
import com.uber.cadence.serviceclient.IWorkflowService;
import io.grpc.Deadline;
import io.grpc.StatusRuntimeException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

public class Thrift2ProtoAdapter implements IWorkflowService {

  IGrpcServiceStubs grpcServiceStubs;

  public Thrift2ProtoAdapter(IGrpcServiceStubs grpcServiceStubs) {
    this.grpcServiceStubs = grpcServiceStubs;
  }

  @Override
  public void RegisterDomain(RegisterDomainRequest registerRequest)
      throws BadRequestError, DomainAlreadyExistsError, ServiceBusyError,
      ClientVersionNotSupportedError, TException {
    grpcServiceStubs
        .domainBlockingStub()
        .registerDomain(RequestMapper.registerDomainRequest(registerRequest));
  }

  @Override
  public DescribeDomainResponse DescribeDomain(DescribeDomainRequest describeRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
      ClientVersionNotSupportedError, TException {
    com.uber.cadence.api.v1.DescribeDomainResponse response =
        grpcServiceStubs
            .domainBlockingStub()
            .describeDomain(RequestMapper.describeDomainRequest(describeRequest));
    return ResponseMapper.describeDomainResponse(response);
  }

  @Override
  public ListDomainsResponse ListDomains(ListDomainsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
      ClientVersionNotSupportedError, TException {
    com.uber.cadence.api.v1.ListDomainsResponse response =
        grpcServiceStubs
            .domainBlockingStub()
            .listDomains(RequestMapper.listDomainsRequest(listRequest));
    return ResponseMapper.listDomainsResponse(response);
  }

  @Override
  public UpdateDomainResponse UpdateDomain(UpdateDomainRequest updateRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
      ClientVersionNotSupportedError, TException {
    com.uber.cadence.api.v1.UpdateDomainResponse response =
        grpcServiceStubs
            .domainBlockingStub()
            .updateDomain(RequestMapper.updateDomainRequest(updateRequest));
    return ResponseMapper.updateDomainResponse(response);
  }

  @Override
  public void DeprecateDomain(DeprecateDomainRequest deprecateRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
      ClientVersionNotSupportedError, TException {
    grpcServiceStubs
        .domainBlockingStub()
        .deprecateDomain(RequestMapper.deprecateDomainRequest(deprecateRequest));
  }

  @Override
  public StartWorkflowExecutionResponse StartWorkflowExecution(
      StartWorkflowExecutionRequest startRequest)
      throws BadRequestError, WorkflowExecutionAlreadyStartedError, ServiceBusyError,
      DomainNotActiveError, LimitExceededError, EntityNotExistsError,
      ClientVersionNotSupportedError, TException {
    return startWorkflowExecution(startRequest);
  }

  private StartWorkflowExecutionResponse startWorkflowExecution(
      StartWorkflowExecutionRequest startRequest)
      throws BadRequestError, WorkflowExecutionAlreadyStartedError, ServiceBusyError,
      DomainNotActiveError, LimitExceededError, EntityNotExistsError,
      ClientVersionNotSupportedError, TException {
    startRequest.setRequestId(UUID.randomUUID().toString());
    try {
      com.uber.cadence.api.v1.StartWorkflowExecutionResponse response =
          grpcServiceStubs
              .workflowBlockingStub()
              .startWorkflowExecution(RequestMapper.startWorkflowExecutionRequest(startRequest));
      return ResponseMapper.startWorkflowExecutionResponse(response);
    } catch (StatusRuntimeException e) {
      convertAndThrowStatusException(e);
      throw e;
    }
  }

  private void convertAndThrowStatusException(StatusRuntimeException e)
      throws BadRequestError, WorkflowExecutionAlreadyStartedError, ServiceBusyError,
      DomainNotActiveError, LimitExceededError, EntityNotExistsError,
      ClientVersionNotSupportedError {
    // TODO handle all errors depending on status
    switch (e.getStatus().getCode()) {
      case ALREADY_EXISTS: {
        WorkflowExecutionAlreadyStartedError ex = new WorkflowExecutionAlreadyStartedError();
        ex.setMessage(e.getMessage());
        throw ex;
      }
      case NOT_FOUND: {
        throw new EntityNotExistsError(e.getMessage());
      }
      default:
        throw e;
    }
  }

  @Override
  public GetWorkflowExecutionHistoryResponse GetWorkflowExecutionHistory(
      GetWorkflowExecutionHistoryRequest getRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
      ClientVersionNotSupportedError, TException {
    com.uber.cadence.api.v1.GetWorkflowExecutionHistoryResponse response =
        grpcServiceStubs
            .workflowBlockingStub()
            .getWorkflowExecutionHistory(
                RequestMapper.getWorkflowExecutionHistoryRequest(getRequest));
    return ResponseMapper.getWorkflowExecutionHistoryResponse(response);
  }

  @Override
  public PollForDecisionTaskResponse PollForDecisionTask(PollForDecisionTaskRequest pollRequest)
      throws BadRequestError, ServiceBusyError, LimitExceededError, EntityNotExistsError,
      DomainNotActiveError, ClientVersionNotSupportedError, TException {
    try {
      com.uber.cadence.api.v1.PollForDecisionTaskResponse response =
          grpcServiceStubs
              .workerBlockingStub()
              .pollForDecisionTask(RequestMapper.pollForDecisionTaskRequest(pollRequest));
      return ResponseMapper.pollForDecisionTaskResponse(response);
    } catch (StatusRuntimeException e) {
      convertAndThrowStatusException(e);
      throw e;
    }
  }

  @Override
  public RespondDecisionTaskCompletedResponse RespondDecisionTaskCompleted(
      RespondDecisionTaskCompletedRequest completeRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
      ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
      TException {
    try {
      com.uber.cadence.api.v1.RespondDecisionTaskCompletedResponse response =
          grpcServiceStubs
              .workerBlockingStub()
              .respondDecisionTaskCompleted(
                  RequestMapper.respondDecisionTaskCompletedRequest(completeRequest));
      return ResponseMapper.respondDecisionTaskCompletedResponse(response);
    } catch (StatusRuntimeException e) {
      convertAndThrowStatusException(e);
      throw e;
    }
  }

  @Override
  public void RespondDecisionTaskFailed(RespondDecisionTaskFailedRequest failedRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
      ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
      TException {
    try {
      grpcServiceStubs
          .workerBlockingStub()
          .respondDecisionTaskFailed(RequestMapper.respondDecisionTaskFailedRequest(failedRequest));
    } catch (StatusRuntimeException e) {
      convertAndThrowStatusException(e);
      throw e;
    }
  }

  @Override
  public PollForActivityTaskResponse PollForActivityTask(PollForActivityTaskRequest pollRequest)
      throws BadRequestError, ServiceBusyError, LimitExceededError, EntityNotExistsError,
      DomainNotActiveError, ClientVersionNotSupportedError, TException {
    try {
      com.uber.cadence.api.v1.PollForActivityTaskResponse response =
          grpcServiceStubs
              .workerBlockingStub()
              .pollForActivityTask(RequestMapper.pollForActivityTaskRequest(pollRequest));
      return ResponseMapper.pollForActivityTaskResponse(response);
    } catch (StatusRuntimeException e) {
      convertAndThrowStatusException(e);
      throw e;
    }
  }

  @Override
  public RecordActivityTaskHeartbeatResponse RecordActivityTaskHeartbeat(
      RecordActivityTaskHeartbeatRequest heartbeatRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
      ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
      TException {
    com.uber.cadence.api.v1.RecordActivityTaskHeartbeatResponse response =
        grpcServiceStubs
            .workerBlockingStub()
            .recordActivityTaskHeartbeat(
                RequestMapper.recordActivityTaskHeartbeatRequest(heartbeatRequest));
    return ResponseMapper.recordActivityTaskHeartbeatResponse(response);
  }

  @Override
  public RecordActivityTaskHeartbeatResponse RecordActivityTaskHeartbeatByID(
      RecordActivityTaskHeartbeatByIDRequest heartbeatRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
      ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
      TException {
    com.uber.cadence.api.v1.RecordActivityTaskHeartbeatByIDResponse response =
        grpcServiceStubs
            .workerBlockingStub()
            .recordActivityTaskHeartbeatByID(
                RequestMapper.recordActivityTaskHeartbeatByIdRequest(heartbeatRequest));
    return ResponseMapper.recordActivityTaskHeartbeatByIdResponse(response);
  }

  @Override
  public void RespondActivityTaskCompleted(RespondActivityTaskCompletedRequest completeRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
      ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
      TException {
    grpcServiceStubs
        .workerBlockingStub()
        .respondActivityTaskCompleted(
            RequestMapper.respondActivityTaskCompletedRequest(completeRequest));
  }

  @Override
  public void RespondActivityTaskCompletedByID(
      RespondActivityTaskCompletedByIDRequest completeRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
      ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
      TException {
    grpcServiceStubs
        .workerBlockingStub()
        .respondActivityTaskCompletedByID(
            RequestMapper.respondActivityTaskCompletedByIdRequest(completeRequest));
  }

  @Override
  public void RespondActivityTaskFailed(RespondActivityTaskFailedRequest failRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
      ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
      TException {
    grpcServiceStubs
        .workerBlockingStub()
        .respondActivityTaskFailed(RequestMapper.respondActivityTaskFailedRequest(failRequest));
  }

  @Override
  public void RespondActivityTaskFailedByID(RespondActivityTaskFailedByIDRequest failRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
      ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
      TException {
    grpcServiceStubs
        .workerBlockingStub()
        .respondActivityTaskFailedByID(
            RequestMapper.respondActivityTaskFailedByIdRequest(failRequest));
  }

  @Override
  public void RespondActivityTaskCanceled(RespondActivityTaskCanceledRequest canceledRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
      ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
      TException {
    grpcServiceStubs
        .workerBlockingStub()
        .respondActivityTaskCanceled(
            RequestMapper.respondActivityTaskCanceledRequest(canceledRequest));
  }

  @Override
  public void RespondActivityTaskCanceledByID(
      RespondActivityTaskCanceledByIDRequest canceledRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
      ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
      TException {
    grpcServiceStubs
        .workerBlockingStub()
        .respondActivityTaskCanceledByID(
            RequestMapper.respondActivityTaskCanceledByIdRequest(canceledRequest));
  }

  @Override
  public void RequestCancelWorkflowExecution(RequestCancelWorkflowExecutionRequest cancelRequest)
      throws BadRequestError, EntityNotExistsError, CancellationAlreadyRequestedError,
      ServiceBusyError, DomainNotActiveError, LimitExceededError,
      ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError, TException {
    cancelRequest.setRequestId(UUID.randomUUID().toString());
    try {
      grpcServiceStubs
          .workflowBlockingStub()
          .requestCancelWorkflowExecution(
              RequestMapper.requestCancelWorkflowExecutionRequest(cancelRequest));
    } catch (StatusRuntimeException e) {
      convertAndThrowStatusException(e);
      throw e;
    }
  }

  @Override
  public void SignalWorkflowExecution(SignalWorkflowExecutionRequest signalRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
      LimitExceededError, ClientVersionNotSupportedError,
      WorkflowExecutionAlreadyCompletedError, TException {
    signalRequest.setRequestId(UUID.randomUUID().toString());
    try {
      grpcServiceStubs
          .workflowBlockingStub()
          .signalWorkflowExecution(RequestMapper.signalWorkflowExecutionRequest(signalRequest));
    } catch (StatusRuntimeException e) {
      convertAndThrowStatusException(e);
      throw e;
    }
  }

  @Override
  public StartWorkflowExecutionResponse SignalWithStartWorkflowExecution(
      SignalWithStartWorkflowExecutionRequest signalWithStartRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
      LimitExceededError, WorkflowExecutionAlreadyStartedError, ClientVersionNotSupportedError,
      TException {
    signalWithStartRequest.setRequestId(UUID.randomUUID().toString());
    com.uber.cadence.api.v1.SignalWithStartWorkflowExecutionResponse response =
        grpcServiceStubs
            .workflowBlockingStub()
            .signalWithStartWorkflowExecution(
                RequestMapper.signalWithStartWorkflowExecutionRequest(signalWithStartRequest));
    return ResponseMapper.signalWithStartWorkflowExecutionResponse(response);
  }

  @Override
  public ResetWorkflowExecutionResponse ResetWorkflowExecution(
      ResetWorkflowExecutionRequest resetRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
      LimitExceededError, ClientVersionNotSupportedError, TException {
    resetRequest.setRequestId(UUID.randomUUID().toString());
    com.uber.cadence.api.v1.ResetWorkflowExecutionResponse response =
        grpcServiceStubs
            .workflowBlockingStub()
            .resetWorkflowExecution(RequestMapper.resetWorkflowExecutionRequest(resetRequest));
    return ResponseMapper.resetWorkflowExecutionResponse(response);
  }

  @Override
  public void TerminateWorkflowExecution(TerminateWorkflowExecutionRequest terminateRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
      LimitExceededError, ClientVersionNotSupportedError,
      WorkflowExecutionAlreadyCompletedError, TException {
    grpcServiceStubs
        .workflowBlockingStub()
        .terminateWorkflowExecution(
            RequestMapper.terminateWorkflowExecutionRequest(terminateRequest));
  }

  @Override
  public ListOpenWorkflowExecutionsResponse ListOpenWorkflowExecutions(
      ListOpenWorkflowExecutionsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, LimitExceededError,
      ClientVersionNotSupportedError, TException {
    com.uber.cadence.api.v1.ListOpenWorkflowExecutionsResponse response =
        grpcServiceStubs
            .visibilityBlockingStub()
            .listOpenWorkflowExecutions(
                RequestMapper.listOpenWorkflowExecutionsRequest(listRequest));
    return ResponseMapper.listOpenWorkflowExecutionsResponse(response);
  }

  @Override
  public ListClosedWorkflowExecutionsResponse ListClosedWorkflowExecutions(
      ListClosedWorkflowExecutionsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
      ClientVersionNotSupportedError, TException {
    com.uber.cadence.api.v1.ListClosedWorkflowExecutionsResponse response =
        grpcServiceStubs
            .visibilityBlockingStub()
            .listClosedWorkflowExecutions(
                RequestMapper.listClosedWorkflowExecutionsRequest(listRequest));
    return ResponseMapper.listClosedWorkflowExecutionsResponse(response);
  }

  @Override
  public ListWorkflowExecutionsResponse ListWorkflowExecutions(
      ListWorkflowExecutionsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
      ClientVersionNotSupportedError, TException {
    com.uber.cadence.api.v1.ListWorkflowExecutionsResponse response =
        grpcServiceStubs
            .visibilityBlockingStub()
            .listWorkflowExecutions(RequestMapper.listWorkflowExecutionsRequest(listRequest));
    return ResponseMapper.listWorkflowExecutionsResponse(response);
  }

  @Override
  public ListArchivedWorkflowExecutionsResponse ListArchivedWorkflowExecutions(
      ListArchivedWorkflowExecutionsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
      ClientVersionNotSupportedError, TException {
    com.uber.cadence.api.v1.ListArchivedWorkflowExecutionsResponse response =
        grpcServiceStubs
            .visibilityBlockingStub()
            .listArchivedWorkflowExecutions(
                RequestMapper.listArchivedWorkflowExecutionsRequest(listRequest));
    return ResponseMapper.listArchivedWorkflowExecutionsResponse(response);
  }

  @Override
  public ListWorkflowExecutionsResponse ScanWorkflowExecutions(
      ListWorkflowExecutionsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
      ClientVersionNotSupportedError, TException {
    com.uber.cadence.api.v1.ScanWorkflowExecutionsResponse response =
        grpcServiceStubs
            .visibilityBlockingStub()
            .scanWorkflowExecutions(RequestMapper.scanWorkflowExecutionsRequest(listRequest));
    return ResponseMapper.scanWorkflowExecutionsResponse(response);
  }

  @Override
  public CountWorkflowExecutionsResponse CountWorkflowExecutions(
      CountWorkflowExecutionsRequest countRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
      ClientVersionNotSupportedError, TException {
    com.uber.cadence.api.v1.CountWorkflowExecutionsResponse response =
        grpcServiceStubs
            .visibilityBlockingStub()
            .countWorkflowExecutions(RequestMapper.countWorkflowExecutionsRequest(countRequest));
    return ResponseMapper.countWorkflowExecutionsResponse(response);
  }

  @Override
  public GetSearchAttributesResponse GetSearchAttributes()
      throws ServiceBusyError, ClientVersionNotSupportedError, TException {
    com.uber.cadence.api.v1.GetSearchAttributesResponse response =
        grpcServiceStubs
            .visibilityBlockingStub()
            .getSearchAttributes(GetSearchAttributesRequest.newBuilder().build());
    return ResponseMapper.getSearchAttributesResponse(response);
  }

  @Override
  public void RespondQueryTaskCompleted(RespondQueryTaskCompletedRequest completeRequest)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
      DomainNotActiveError, ClientVersionNotSupportedError, TException {
    grpcServiceStubs
        .workerBlockingStub()
        .respondQueryTaskCompleted(RequestMapper.respondQueryTaskCompletedRequest(completeRequest));
  }

  @Override
  public ResetStickyTaskListResponse ResetStickyTaskList(ResetStickyTaskListRequest resetRequest)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
      DomainNotActiveError, ClientVersionNotSupportedError,
      WorkflowExecutionAlreadyCompletedError, TException {
    com.uber.cadence.api.v1.ResetStickyTaskListResponse response =
        grpcServiceStubs
            .workerBlockingStub()
            .resetStickyTaskList(RequestMapper.resetStickyTaskListRequest(resetRequest));
    return new ResetStickyTaskListResponse();
  }

  @Override
  public QueryWorkflowResponse QueryWorkflow(QueryWorkflowRequest queryRequest)
      throws BadRequestError, EntityNotExistsError, QueryFailedError, LimitExceededError,
      ServiceBusyError, ClientVersionNotSupportedError, TException {
    com.uber.cadence.api.v1.QueryWorkflowResponse response =
        grpcServiceStubs
            .workflowBlockingStub()
            .queryWorkflow(RequestMapper.queryWorkflowRequest(queryRequest));
    return ResponseMapper.queryWorkflowResponse(response);
  }

  @Override
  public DescribeWorkflowExecutionResponse DescribeWorkflowExecution(
      DescribeWorkflowExecutionRequest describeRequest)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
      ClientVersionNotSupportedError, TException {
    com.uber.cadence.api.v1.DescribeWorkflowExecutionResponse response =
        grpcServiceStubs
            .workflowBlockingStub()
            .describeWorkflowExecution(
                RequestMapper.describeWorkflowExecutionRequest(describeRequest));
    return ResponseMapper.describeWorkflowExecutionResponse(response);
  }

  @Override
  public DescribeTaskListResponse DescribeTaskList(DescribeTaskListRequest request)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
      ClientVersionNotSupportedError, TException {
    com.uber.cadence.api.v1.DescribeTaskListResponse response =
        grpcServiceStubs
            .workflowBlockingStub()
            .describeTaskList(RequestMapper.describeTaskListRequest(request));
    return ResponseMapper.describeTaskListResponse(response);
  }

  @Override
  public ClusterInfo GetClusterInfo() throws InternalServiceError, ServiceBusyError, TException {
    com.uber.cadence.api.v1.GetClusterInfoResponse response =
        grpcServiceStubs
            .workflowBlockingStub()
            .getClusterInfo(com.uber.cadence.api.v1.GetClusterInfoRequest.newBuilder().build());
    return ResponseMapper.getClusterInfoResponse(response);
  }

  @Override
  public GetTaskListsByDomainResponse GetTaskListsByDomain(GetTaskListsByDomainRequest request) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public ListTaskListPartitionsResponse ListTaskListPartitions(
      ListTaskListPartitionsRequest request)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
      TException {
    com.uber.cadence.api.v1.ListTaskListPartitionsResponse response =
        grpcServiceStubs
            .workflowBlockingStub()
            .listTaskListPartitions(RequestMapper.listTaskListPartitionsRequest(request));
    return ResponseMapper.listTaskListPartitionsResponse(response);
  }

  @Override
  public void RefreshWorkflowTasks(RefreshWorkflowTasksRequest request)
      throws BadRequestError, DomainNotActiveError, ServiceBusyError, EntityNotExistsError,
      TException {
    grpcServiceStubs
        .workflowBlockingStub()
        .refreshWorkflowTasks(
            com.uber.cadence.api.v1.RefreshWorkflowTasksRequest.newBuilder().build());
  }

  @Override
  public void RegisterDomain(
      RegisterDomainRequest registerRequest, AsyncMethodCallback resultHandler) throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void DescribeDomain(
      DescribeDomainRequest describeRequest, AsyncMethodCallback resultHandler) throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void ListDomains(ListDomainsRequest listRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void UpdateDomain(UpdateDomainRequest updateRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void DeprecateDomain(
      DeprecateDomainRequest deprecateRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void StartWorkflowExecution(
      StartWorkflowExecutionRequest startRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void GetWorkflowExecutionHistory(
      GetWorkflowExecutionHistoryRequest getRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void PollForDecisionTask(
      PollForDecisionTaskRequest pollRequest, AsyncMethodCallback resultHandler) throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void RespondDecisionTaskCompleted(
      RespondDecisionTaskCompletedRequest completeRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void RespondDecisionTaskFailed(
      RespondDecisionTaskFailedRequest failedRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void PollForActivityTask(
      PollForActivityTaskRequest pollRequest, AsyncMethodCallback resultHandler) throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void RecordActivityTaskHeartbeat(
      RecordActivityTaskHeartbeatRequest heartbeatRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void RecordActivityTaskHeartbeatByID(
      RecordActivityTaskHeartbeatByIDRequest heartbeatRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void RespondActivityTaskCompleted(
      RespondActivityTaskCompletedRequest completeRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void RespondActivityTaskCompletedByID(
      RespondActivityTaskCompletedByIDRequest completeRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void RespondActivityTaskFailed(
      RespondActivityTaskFailedRequest failRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void RespondActivityTaskFailedByID(
      RespondActivityTaskFailedByIDRequest failRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void RespondActivityTaskCanceled(
      RespondActivityTaskCanceledRequest canceledRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void RespondActivityTaskCanceledByID(
      RespondActivityTaskCanceledByIDRequest canceledRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void RequestCancelWorkflowExecution(
      RequestCancelWorkflowExecutionRequest cancelRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void SignalWorkflowExecution(
      SignalWorkflowExecutionRequest signalRequest, AsyncMethodCallback resultHandler)
      throws TException {
    signalRequest.setRequestId(UUID.randomUUID().toString());
    ListenableFuture<com.uber.cadence.api.v1.SignalWorkflowExecutionResponse> resultFuture =
        grpcServiceStubs
            .workflowFutureStub()
            .signalWorkflowExecution(RequestMapper.signalWorkflowExecutionRequest(signalRequest));
    resultFuture.addListener(
        () -> {
          try {
            com.uber.cadence.api.v1.SignalWorkflowExecutionResponse response = resultFuture.get();
            resultHandler.onComplete(null);
          } catch (Exception e) {
            resultHandler.onError(e);
          }
        },
        ForkJoinPool.commonPool());
  }

  @Override
  public void SignalWithStartWorkflowExecution(
      SignalWithStartWorkflowExecutionRequest signalWithStartRequest,
      AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void ResetWorkflowExecution(
      ResetWorkflowExecutionRequest resetRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void TerminateWorkflowExecution(
      TerminateWorkflowExecutionRequest terminateRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void ListOpenWorkflowExecutions(
      ListOpenWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void ListClosedWorkflowExecutions(
      ListClosedWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void ListWorkflowExecutions(
      ListWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void ListArchivedWorkflowExecutions(
      ListArchivedWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void ScanWorkflowExecutions(
      ListWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void CountWorkflowExecutions(
      CountWorkflowExecutionsRequest countRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void GetSearchAttributes(AsyncMethodCallback resultHandler) throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void RespondQueryTaskCompleted(
      RespondQueryTaskCompletedRequest completeRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void ResetStickyTaskList(
      ResetStickyTaskListRequest resetRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void QueryWorkflow(QueryWorkflowRequest queryRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void DescribeWorkflowExecution(
      DescribeWorkflowExecutionRequest describeRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void DescribeTaskList(DescribeTaskListRequest request, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void GetClusterInfo(AsyncMethodCallback resultHandler) throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void GetTaskListsByDomain(
      GetTaskListsByDomainRequest request, AsyncMethodCallback resultHandler) throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void ListTaskListPartitions(
      ListTaskListPartitionsRequest request, AsyncMethodCallback resultHandler) throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void RefreshWorkflowTasks(
      RefreshWorkflowTasksRequest request, AsyncMethodCallback resultHandler) throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void close() {
    grpcServiceStubs.shutdownNow();
  }

  @Override
  public CompletableFuture<Boolean> isHealthy() {
    ListenableFuture<HealthResponse> listenableFuture =
        grpcServiceStubs.metaFutureStub().health(HealthRequest.newBuilder().build());
    CompletableFuture<Boolean> completable =
        new CompletableFuture<Boolean>() {
          @Override
          public boolean cancel(boolean mayInterruptIfRunning) {
            boolean result = listenableFuture.cancel(mayInterruptIfRunning);
            super.cancel(mayInterruptIfRunning);
            return result;
          }
        };
    Futures.addCallback(
        listenableFuture,
        new FutureCallback<HealthResponse>() {
          @Override
          public void onSuccess(HealthResponse result) {
            completable.complete(true);
          }

          @Override
          public void onFailure(Throwable t) {
            completable.completeExceptionally(t);
          }
        },
        ForkJoinPool.commonPool());
    return completable;
  }

  @Override
  public void StartWorkflowExecutionWithTimeout(
      StartWorkflowExecutionRequest startRequest,
      AsyncMethodCallback resultHandler,
      Long timeoutInMillis)
      throws TException {
    startRequest.setRequestId(UUID.randomUUID().toString());
    ListenableFuture<com.uber.cadence.api.v1.StartWorkflowExecutionResponse> resultFuture =
        grpcServiceStubs
            .workflowFutureStub()
            .withDeadline(Deadline.after(timeoutInMillis, TimeUnit.MILLISECONDS))
            .startWorkflowExecution(RequestMapper.startWorkflowExecutionRequest(startRequest));
    resultFuture.addListener(
        () -> {
          try {
            com.uber.cadence.api.v1.StartWorkflowExecutionResponse response = resultFuture.get();
            resultHandler.onComplete(ResponseMapper.startWorkflowExecutionResponse(response));
          } catch (Exception e) {
            resultHandler.onError(e);
          }
        },
        ForkJoinPool.commonPool());
  }

  @Override
  public GetWorkflowExecutionHistoryResponse GetWorkflowExecutionHistoryWithTimeout(
      GetWorkflowExecutionHistoryRequest getRequest, Long timeoutInMillis) throws TException {
    com.uber.cadence.api.v1.GetWorkflowExecutionHistoryResponse response =
        grpcServiceStubs
            .workflowBlockingStub()
            .withDeadline(Deadline.after(timeoutInMillis, TimeUnit.MILLISECONDS))
            .getWorkflowExecutionHistory(
                RequestMapper.getWorkflowExecutionHistoryRequest(getRequest));
    return ResponseMapper.getWorkflowExecutionHistoryResponse(response);
  }

  @Override
  public void GetWorkflowExecutionHistoryWithTimeout(
      GetWorkflowExecutionHistoryRequest getRequest,
      AsyncMethodCallback resultHandler,
      Long timeoutInMillis)
      throws TException {
    // TODO test
    ListenableFuture<com.uber.cadence.api.v1.GetWorkflowExecutionHistoryResponse> resultFuture =
        grpcServiceStubs
            .workflowFutureStub()
            .withDeadline(Deadline.after(timeoutInMillis, TimeUnit.MILLISECONDS))
            .getWorkflowExecutionHistory(
                RequestMapper.getWorkflowExecutionHistoryRequest(getRequest));
    resultFuture.addListener(
        () -> {
          try {
            com.uber.cadence.api.v1.GetWorkflowExecutionHistoryResponse response =
                resultFuture.get();
            resultHandler.onComplete(ResponseMapper.getWorkflowExecutionHistoryResponse(response));
          } catch (Exception e) {
            resultHandler.onError(e);
          }
        },
        ForkJoinPool.commonPool());
  }

  @Override
  public void SignalWorkflowExecutionWithTimeout(
      SignalWorkflowExecutionRequest signalRequest,
      AsyncMethodCallback resultHandler,
      Long timeoutInMillis)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }
}
