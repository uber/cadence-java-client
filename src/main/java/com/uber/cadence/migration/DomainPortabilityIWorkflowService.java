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
import com.uber.cadence.WorkflowService.Client;
import com.uber.cadence.activity.ActivityOptions;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowClientOptions;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.common.RetryOptions;
import com.uber.cadence.internal.compatibility.Thrift2ProtoAdapter;
import com.uber.cadence.internal.compatibility.proto.serviceclient.IGrpcServiceStubs;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.worker.WorkerFactory;
import com.uber.cadence.workflow.Workflow;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import com.uber.cadence.worker.Worker;
import java.time.Duration;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TProtocol;

public class DomainPortabilityIWorkflowService implements IWorkflowService {

  static final String TASK_LIST = "Domain Porting TASK_LIST";
  @Override
  public void RegisterDomain(RegisterDomainRequest registerRequest)
      throws BadRequestError, DomainAlreadyExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, TException {}

  @Override
  public DescribeDomainResponse DescribeDomain(DescribeDomainRequest describeRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, TException {
    return null;
  }

  @Override
  public ListDomainsResponse ListDomains(ListDomainsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, TException {
    return null;
  }

  @Override
  public UpdateDomainResponse UpdateDomain(UpdateDomainRequest updateRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
          ClientVersionNotSupportedError, TException {
    return null;
  }

  @Override
  public void DeprecateDomain(DeprecateDomainRequest deprecateRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
          ClientVersionNotSupportedError, TException {}

  // StartWorkflow kicks off workflow in new cluster
  @Override
  public StartWorkflowExecutionResponse StartWorkflowExecution(
      StartWorkflowExecutionRequest startRequest)
      throws BadRequestError, WorkflowExecutionAlreadyStartedError, ServiceBusyError,
          DomainNotActiveError, LimitExceededError, EntityNotExistsError,
          ClientVersionNotSupportedError, TException {

    WorkflowClient workflowClient = WorkflowClient.newInstance(
            new Thrift2ProtoAdapter(IGrpcServiceStubs.newInstance()),
            WorkflowClientOptions.newBuilder().setDomain(startRequest.getDomain()).build());

    //Get worker to poll the task list.
    WorkerFactory factory = WorkerFactory.newInstance(workflowClient);
    Worker worker = factory.newWorker(TASK_LIST);
//    worker.registerWorkflowImplementationTypes(MigrationActivitiesImpl.class);
    worker.registerActivitiesImplementations(new MigrationActivitiesImpl(workflowClient));
    factory.start();

    WorkflowOptions workflowOptions = new WorkflowOptions.Builder()
            .setTaskList(TASK_LIST)
            .setExecutionStartToCloseTimeout(Duration.ofSeconds(startRequest.getExecutionStartToCloseTimeoutSeconds()))
            .build();

    MigrationActivities activities = workflowClient.newWorkflowStub(MigrationActivities.class,workflowOptions);
    MigrationActivities.StartNewWorkflowExecutionResponse response = activities.startWorkflowInNewDomain(new MigrationActivities.StartNewWorkflowRequest(startRequest));

    return response.response;
  }

  @Override
  public GetWorkflowExecutionHistoryResponse GetWorkflowExecutionHistory(
      GetWorkflowExecutionHistoryRequest getRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, TException {
    return null;
  }

  @Override
  public PollForDecisionTaskResponse PollForDecisionTask(PollForDecisionTaskRequest pollRequest)
      throws BadRequestError, ServiceBusyError, LimitExceededError, EntityNotExistsError,
          DomainNotActiveError, ClientVersionNotSupportedError, TException {
    return null;
  }

  @Override
  public RespondDecisionTaskCompletedResponse RespondDecisionTaskCompleted(
      RespondDecisionTaskCompletedRequest completeRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          TException {
    return null;
  }

  @Override
  public void RespondDecisionTaskFailed(RespondDecisionTaskFailedRequest failedRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          TException {}

  @Override
  public PollForActivityTaskResponse PollForActivityTask(PollForActivityTaskRequest pollRequest)
      throws BadRequestError, ServiceBusyError, LimitExceededError, EntityNotExistsError,
          DomainNotActiveError, ClientVersionNotSupportedError, TException {
    return null;
  }

  @Override
  public RecordActivityTaskHeartbeatResponse RecordActivityTaskHeartbeat(
      RecordActivityTaskHeartbeatRequest heartbeatRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          TException {
    return null;
  }

  @Override
  public RecordActivityTaskHeartbeatResponse RecordActivityTaskHeartbeatByID(
      RecordActivityTaskHeartbeatByIDRequest heartbeatRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          TException {
    return null;
  }

  @Override
  public void RespondActivityTaskCompleted(RespondActivityTaskCompletedRequest completeRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          TException {}

  @Override
  public void RespondActivityTaskCompletedByID(
      RespondActivityTaskCompletedByIDRequest completeRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          TException {}

  @Override
  public void RespondActivityTaskFailed(RespondActivityTaskFailedRequest failRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          TException {}

  @Override
  public void RespondActivityTaskFailedByID(RespondActivityTaskFailedByIDRequest failRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          TException {}

  @Override
  public void RespondActivityTaskCanceled(RespondActivityTaskCanceledRequest canceledRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          TException {}

  @Override
  public void RespondActivityTaskCanceledByID(
      RespondActivityTaskCanceledByIDRequest canceledRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          TException {}

  @Override
  public void RequestCancelWorkflowExecution(RequestCancelWorkflowExecutionRequest cancelRequest)
      throws BadRequestError, EntityNotExistsError, CancellationAlreadyRequestedError,
          ServiceBusyError, DomainNotActiveError, LimitExceededError,
          ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError, TException {}

  @Override
  public void SignalWorkflowExecution(SignalWorkflowExecutionRequest signalRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
          LimitExceededError, ClientVersionNotSupportedError,
          WorkflowExecutionAlreadyCompletedError, TException {}

  @Override
  public StartWorkflowExecutionResponse SignalWithStartWorkflowExecution(
      SignalWithStartWorkflowExecutionRequest signalWithStartRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
          LimitExceededError, WorkflowExecutionAlreadyStartedError, ClientVersionNotSupportedError,
          TException {
    return null;
  }

  @Override
  public ResetWorkflowExecutionResponse ResetWorkflowExecution(
      ResetWorkflowExecutionRequest resetRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
          LimitExceededError, ClientVersionNotSupportedError, TException {
    return null;
  }

  @Override
  public void TerminateWorkflowExecution(TerminateWorkflowExecutionRequest terminateRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
          LimitExceededError, ClientVersionNotSupportedError,
          WorkflowExecutionAlreadyCompletedError, TException {}

  @Override
  public ListOpenWorkflowExecutionsResponse ListOpenWorkflowExecutions(
      ListOpenWorkflowExecutionsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, LimitExceededError,
          ClientVersionNotSupportedError, TException {
    return null;
  }

  @Override
  public ListClosedWorkflowExecutionsResponse ListClosedWorkflowExecutions(
      ListClosedWorkflowExecutionsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, TException {
    return null;
  }

  @Override
  public ListWorkflowExecutionsResponse ListWorkflowExecutions(
      ListWorkflowExecutionsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, TException {
    return null;
  }

  @Override
  public ListArchivedWorkflowExecutionsResponse ListArchivedWorkflowExecutions(
      ListArchivedWorkflowExecutionsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, TException {
    return null;
  }

  @Override
  public ListWorkflowExecutionsResponse ScanWorkflowExecutions(
      ListWorkflowExecutionsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, TException {
    return null;
  }

  @Override
  public CountWorkflowExecutionsResponse CountWorkflowExecutions(
      CountWorkflowExecutionsRequest countRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, TException {
    return null;
  }

  @Override
  public GetSearchAttributesResponse GetSearchAttributes()
      throws ServiceBusyError, ClientVersionNotSupportedError, TException {
    return null;
  }

  @Override
  public void RespondQueryTaskCompleted(RespondQueryTaskCompletedRequest completeRequest)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
          DomainNotActiveError, ClientVersionNotSupportedError, TException {}

  @Override
  public ResetStickyTaskListResponse ResetStickyTaskList(ResetStickyTaskListRequest resetRequest)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
          DomainNotActiveError, ClientVersionNotSupportedError,
          WorkflowExecutionAlreadyCompletedError, TException {
    return null;
  }

  @Override
  public QueryWorkflowResponse QueryWorkflow(QueryWorkflowRequest queryRequest)
      throws BadRequestError, EntityNotExistsError, QueryFailedError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, TException {
    return null;
  }

  @Override
  public DescribeWorkflowExecutionResponse DescribeWorkflowExecution(
      DescribeWorkflowExecutionRequest describeRequest)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
          ClientVersionNotSupportedError, TException {
    return null;
  }

  @Override
  public DescribeTaskListResponse DescribeTaskList(DescribeTaskListRequest request)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
          ClientVersionNotSupportedError, TException {
    return null;
  }

  @Override
  public ClusterInfo GetClusterInfo() throws InternalServiceError, ServiceBusyError, TException {
    return null;
  }

  @Override
  public GetTaskListsByDomainResponse GetTaskListsByDomain(GetTaskListsByDomainRequest request)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
          ClientVersionNotSupportedError, TException {
    return null;
  }

  @Override
  public ListTaskListPartitionsResponse ListTaskListPartitions(
      ListTaskListPartitionsRequest request)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
          TException {
    return null;
  }

  @Override
  public void RefreshWorkflowTasks(RefreshWorkflowTasksRequest request)
      throws BadRequestError, DomainNotActiveError, ServiceBusyError, EntityNotExistsError,
          TException {}

  @Override
  public void RegisterDomain(
      RegisterDomainRequest registerRequest, AsyncMethodCallback resultHandler) throws TException {}

  @Override
  public void DescribeDomain(
      DescribeDomainRequest describeRequest, AsyncMethodCallback resultHandler) throws TException {}

  @Override
  public void ListDomains(ListDomainsRequest listRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void UpdateDomain(UpdateDomainRequest updateRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void DeprecateDomain(
      DeprecateDomainRequest deprecateRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void StartWorkflowExecution(
      StartWorkflowExecutionRequest startRequest, AsyncMethodCallback resultHandler)
      throws TException {
    //            MigrationDecision decision = Workflow.sideEffect(
    //                    MigrationDecision.class, () -> shouldMigrate(workflowDefinition, in)
    ActivityOptions options =
        new ActivityOptions.Builder()
            .setScheduleToCloseTimeout(Duration.ofSeconds(10))
            .setRetryOptions(new RetryOptions.Builder().build())
            .build();
    MigrationActivities activities = Workflow.newActivityStub(MigrationActivities.class, options);
    MigrationActivities.StartNewWorkflowExecutionResponse response =
        activities.startWorkflowInNewDomain(
            new MigrationActivities.StartNewWorkflowRequest(startRequest));
  }

  @Override
  public void GetWorkflowExecutionHistory(
      GetWorkflowExecutionHistoryRequest getRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void PollForDecisionTask(
      PollForDecisionTaskRequest pollRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void RespondDecisionTaskCompleted(
      RespondDecisionTaskCompletedRequest completeRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void RespondDecisionTaskFailed(
      RespondDecisionTaskFailedRequest failedRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void PollForActivityTask(
      PollForActivityTaskRequest pollRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void RecordActivityTaskHeartbeat(
      RecordActivityTaskHeartbeatRequest heartbeatRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void RecordActivityTaskHeartbeatByID(
      RecordActivityTaskHeartbeatByIDRequest heartbeatRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void RespondActivityTaskCompleted(
      RespondActivityTaskCompletedRequest completeRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void RespondActivityTaskCompletedByID(
      RespondActivityTaskCompletedByIDRequest completeRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void RespondActivityTaskFailed(
      RespondActivityTaskFailedRequest failRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void RespondActivityTaskFailedByID(
      RespondActivityTaskFailedByIDRequest failRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void RespondActivityTaskCanceled(
      RespondActivityTaskCanceledRequest canceledRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void RespondActivityTaskCanceledByID(
      RespondActivityTaskCanceledByIDRequest canceledRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void RequestCancelWorkflowExecution(
      RequestCancelWorkflowExecutionRequest cancelRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void SignalWorkflowExecution(
      SignalWorkflowExecutionRequest signalRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void SignalWithStartWorkflowExecution(
      SignalWithStartWorkflowExecutionRequest signalWithStartRequest,
      AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void ResetWorkflowExecution(
      ResetWorkflowExecutionRequest resetRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void TerminateWorkflowExecution(
      TerminateWorkflowExecutionRequest terminateRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void ListOpenWorkflowExecutions(
      ListOpenWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void ListClosedWorkflowExecutions(
      ListClosedWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void ListWorkflowExecutions(
      ListWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void ListArchivedWorkflowExecutions(
      ListArchivedWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void ScanWorkflowExecutions(
      ListWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void CountWorkflowExecutions(
      CountWorkflowExecutionsRequest countRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void GetSearchAttributes(AsyncMethodCallback resultHandler) throws TException {}

  @Override
  public void RespondQueryTaskCompleted(
      RespondQueryTaskCompletedRequest completeRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void ResetStickyTaskList(
      ResetStickyTaskListRequest resetRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void QueryWorkflow(QueryWorkflowRequest queryRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void DescribeWorkflowExecution(
      DescribeWorkflowExecutionRequest describeRequest, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void DescribeTaskList(DescribeTaskListRequest request, AsyncMethodCallback resultHandler)
      throws TException {}

  @Override
  public void GetClusterInfo(AsyncMethodCallback resultHandler) throws TException {}

  @Override
  public void GetTaskListsByDomain(
      GetTaskListsByDomainRequest request, AsyncMethodCallback resultHandler) throws TException {}

  @Override
  public void ListTaskListPartitions(
      ListTaskListPartitionsRequest request, AsyncMethodCallback resultHandler) throws TException {}

  @Override
  public void RefreshWorkflowTasks(
      RefreshWorkflowTasksRequest request, AsyncMethodCallback resultHandler) throws TException {}

  @Override
  public void close() {}

  @Override
  public void StartWorkflowExecutionWithTimeout(
      StartWorkflowExecutionRequest startRequest,
      AsyncMethodCallback resultHandler,
      Long timeoutInMillis)
      throws TException {}

  @Override
  public GetWorkflowExecutionHistoryResponse GetWorkflowExecutionHistoryWithTimeout(
      GetWorkflowExecutionHistoryRequest getRequest, Long timeoutInMillis) throws TException {
    return null;
  }

  @Override
  public void GetWorkflowExecutionHistoryWithTimeout(
      GetWorkflowExecutionHistoryRequest getRequest,
      AsyncMethodCallback resultHandler,
      Long timeoutInMillis)
      throws TException {}

  @Override
  public void SignalWorkflowExecutionWithTimeout(
      SignalWorkflowExecutionRequest signalRequest,
      AsyncMethodCallback resultHandler,
      Long timeoutInMillis)
      throws TException {}

  @Override
  public CompletableFuture<Boolean> isHealthy() {
    return null;
  }
}
