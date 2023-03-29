/*
 *
 *  Modifications copyright (C) 2023 Uber Technologies, Inc.
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

package com.uber.cadence.serviceclient;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.uber.cadence.*;
import com.uber.m3.tally.Scope;
import java.util.concurrent.CompletableFuture;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

public class MigrationClient implements IWorkflowService {

  private static final String MIGRATION_API_CALL_TAG_KEY = "api-call";

  private static final String START_WORKFLOW = "startWorkflow";
  private static final String SHIM_METRIC_SUCCESS = "success";
  private static final String SHIM_METRIC_ERROR = "error";
  private static final String SHIM_METRIC_START_WORKFLOW = "start-wf";
  private static final String SHIM_METRIC_CACHE_HIT = "cache-hit";
  private static final String SHIM_METRIC_START_WORKFLOW_WITHOUT_ID = "workflow-without-id-param";
  private static final String SHIM_METRIC_PREV_WF_FOUND = "prev_wf_found";
  private static final String SHIM_METRIC_FALLING_BACK = "falling-back";
  private static final String SHIM_METRIC_FALLING_BACK_CONTINUE_AS_NEW =
      "falling-back-wf-continue-as-new";
  private static final String SHIM_METRIC_MIGRATION_AFTER_WF_COMPLETION =
      "migration-after-wf-completion";
  private static final String SHIM_METRIC_UNSUPPORTED_API_CALL = "unsupported-api-call";
  private static final String SHIM_METRIC_MIGRATING_AFTER_WF_COMPLETION =
      "migration-after-wf-completion";
  private static final String SHIM_METRIC_PREFER_FROM = "workflow-starting-with-prefer-from";

  public static enum MigrationState {
    DISABLED,
    ENABLED,
    PREFER_FROM
  }

  private final IWorkflowService from;
  private final IWorkflowService to;
  private final Scope scope;
  private final Cache<String, String> wfMigratedCache =
      CacheBuilder.newBuilder().maximumSize(1000).build();
  private MigrationState currentMigrationState;

  public MigrationClient(IWorkflowService from, IWorkflowService to, Scope scope) {
    this.from = from;
    this.to = to;
    this.scope = scope;
  }

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

  @Override
  public StartWorkflowExecutionResponse StartWorkflowExecution(
      StartWorkflowExecutionRequest startRequest)
      throws BadRequestError, WorkflowExecutionAlreadyStartedError, ServiceBusyError,
          DomainNotActiveError, LimitExceededError, EntityNotExistsError,
          ClientVersionNotSupportedError, TException {
    Scope startScope = scope.tagged(ImmutableMap.of(MIGRATION_API_CALL_TAG_KEY, START_WORKFLOW));

    MigrationState migrationState = getMigrationState();

    if (MigrationState.ENABLED.equals(migrationState)) {
      if (Strings.isNullOrEmpty(startRequest.getWorkflowId())) {
        startScope =
            startScope.tagged(
                ImmutableMap.of(SHIM_METRIC_START_WORKFLOW, SHIM_METRIC_START_WORKFLOW_WITHOUT_ID));

        StartWorkflowExecutionResponse startWorkflowExecutionResponse;
        try {
          startWorkflowExecutionResponse = to.StartWorkflowExecution(startRequest);
        } catch (Throwable t) {
          startScope.counter(SHIM_METRIC_ERROR).inc(1);
          throw t;
        }

        startScope.counter(SHIM_METRIC_SUCCESS).inc(1);
        return startWorkflowExecutionResponse;
      }

      if (wfMigratedCache.getIfPresent(startRequest.getWorkflowId()) != null) {
        startScope =
            startScope.tagged(ImmutableMap.of(SHIM_METRIC_START_WORKFLOW, SHIM_METRIC_CACHE_HIT));
        StartWorkflowExecutionResponse response;
        try {
          response = to.StartWorkflowExecution(startRequest);
        } catch (Throwable t) {
          startScope.counter(SHIM_METRIC_ERROR).inc(1);
          throw t;
        }
        startScope.counter(SHIM_METRIC_SUCCESS).inc(1);
        return response;
      }

      DescribeWorkflowExecutionRequest describeRequest = new DescribeWorkflowExecutionRequest();
      WorkflowExecution execution = new WorkflowExecution();
      describeRequest.setExecution(execution);
      describeRequest.setDomain(startRequest.getDomain());

      DescribeWorkflowExecutionResponse describeResponse;
      try {
        describeResponse = this.from.DescribeWorkflowExecution(describeRequest);
      } catch (EntityNotExistsError notExistsError) {
        startScope =
            startScope.tagged(ImmutableMap.of(SHIM_METRIC_PREV_WF_FOUND, Boolean.FALSE.toString()));
        StartWorkflowExecutionResponse response;
        try {
          response = to.StartWorkflowExecution(startRequest);
        } catch (Throwable t) {
          startScope.counter(SHIM_METRIC_ERROR).inc(1);
          throw t;
        }
        return response;
      } catch (Throwable t) {
        // Error determining workflow state in old cluster, default to starting workflow in old
        // cluster
        return from.StartWorkflowExecution(startRequest);
      }

      startScope =
          startScope.tagged(ImmutableMap.of(SHIM_METRIC_PREV_WF_FOUND, Boolean.TRUE.toString()));
      WorkflowExecutionCloseStatus closeStatus =
          describeResponse.getWorkflowExecutionInfo().getCloseStatus();

      if (closeStatus == null) {
        startScope.counter(SHIM_METRIC_FALLING_BACK).inc(1);
        return from.StartWorkflowExecution(startRequest);
      } else if (closeStatus.equals(WorkflowExecutionCloseStatus.CONTINUED_AS_NEW)) {
        startScope.counter(SHIM_METRIC_FALLING_BACK_CONTINUE_AS_NEW).inc(1);
        return from.StartWorkflowExecution(startRequest);
      } else {
        startScope =
            startScope.tagged(
                ImmutableMap.of(
                    SHIM_METRIC_START_WORKFLOW, SHIM_METRIC_MIGRATING_AFTER_WF_COMPLETION));

        try {
          StartWorkflowExecutionResponse response = to.StartWorkflowExecution(startRequest);
          startScope.counter(SHIM_METRIC_SUCCESS).inc(1);
          return response;
        } catch (Throwable t) {
          startScope.counter(SHIM_METRIC_ERROR).inc(1);
          throw t;
        }
      }
    } else if (MigrationState.PREFER_FROM.equals(migrationState)) {
      startScope =
          startScope.tagged(ImmutableMap.of(SHIM_METRIC_START_WORKFLOW, SHIM_METRIC_PREFER_FROM));

      if (Strings.isNullOrEmpty(startRequest.getWorkflowId())) {
        from.StartWorkflowExecution(startRequest);
      }

      wfMigratedCache.invalidate(startRequest.getWorkflowId());

      DescribeWorkflowExecutionRequest describeRequest = new DescribeWorkflowExecutionRequest();
      WorkflowExecution execution = new WorkflowExecution();
      describeRequest.setExecution(execution);
      describeRequest.setDomain(startRequest.getDomain());

      DescribeWorkflowExecutionResponse describeResponse;
      try {
        describeResponse = this.from.DescribeWorkflowExecution(describeRequest);
      } catch (Throwable t) {
        // Error determining workflow state in old cluster, default to starting workflow in old
        // cluster
        return from.StartWorkflowExecution(startRequest);
      }

      startScope =
          startScope.tagged(ImmutableMap.of(SHIM_METRIC_PREV_WF_FOUND, Boolean.TRUE.toString()));
      WorkflowExecutionCloseStatus closeStatus =
          describeResponse.getWorkflowExecutionInfo().getCloseStatus();

      if (closeStatus == null) {
        startScope.counter(SHIM_METRIC_FALLING_BACK).inc(1);
        return to.StartWorkflowExecution(startRequest);
      } else if (closeStatus.equals(WorkflowExecutionCloseStatus.CONTINUED_AS_NEW)) {
        startScope.counter(SHIM_METRIC_FALLING_BACK_CONTINUE_AS_NEW).inc(1);
        return to.StartWorkflowExecution(startRequest);
      } else {
        startScope =
            startScope.tagged(
                ImmutableMap.of(
                    SHIM_METRIC_START_WORKFLOW, SHIM_METRIC_MIGRATING_AFTER_WF_COMPLETION));
        try {
          StartWorkflowExecutionResponse response = from.StartWorkflowExecution(startRequest);
        } catch (Throwable t) {
          startScope.counter(SHIM_METRIC_ERROR).inc(1);
          throw t;
        }
        startScope.counter(SHIM_METRIC_SUCCESS).inc(1);
      }

    } else if (MigrationState.DISABLED.equals(migrationState)) {
      return from.StartWorkflowExecution(startRequest);
    }

    throw new IllegalStateException("please check migration config");
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
      throws TException {}

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

  //TODO: this is just for testing, needs to be replaced with some real configuration injection
  public void setMigrationState(MigrationState migrationState) {
    this.currentMigrationState = migrationState;
  }

  public MigrationState getMigrationState() {
    return currentMigrationState;
  }
}
