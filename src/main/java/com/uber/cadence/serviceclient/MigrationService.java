package com.uber.cadence.serviceclient;

import com.uber.cadence.*;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import java.util.concurrent.CompletableFuture;

public class MigrationService implements IWorkflowService {

    public static enum MigrationState {
        DISABLED,
        ENABLED,
        PREFER_FROM
    }

    private final IWorkflowService from;
    private final IWorkflowService to;

    public MigrationService(IWorkflowService from, IWorkflowService to) {
        this.from = from;
        this.to = to;
    }


    @Override
    public void RegisterDomain(RegisterDomainRequest registerRequest) throws BadRequestError, DomainAlreadyExistsError, ServiceBusyError, ClientVersionNotSupportedError, TException {

    }

    @Override
    public DescribeDomainResponse DescribeDomain(DescribeDomainRequest describeRequest) throws BadRequestError, EntityNotExistsError, ServiceBusyError, ClientVersionNotSupportedError, TException {
        return null;
    }

    @Override
    public ListDomainsResponse ListDomains(ListDomainsRequest listRequest) throws BadRequestError, EntityNotExistsError, ServiceBusyError, ClientVersionNotSupportedError, TException {
        return null;
    }

    @Override
    public UpdateDomainResponse UpdateDomain(UpdateDomainRequest updateRequest) throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError, ClientVersionNotSupportedError, TException {
        return null;
    }

    @Override
    public void DeprecateDomain(DeprecateDomainRequest deprecateRequest) throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError, ClientVersionNotSupportedError, TException {

    }

    @Override
    public StartWorkflowExecutionResponse StartWorkflowExecution(StartWorkflowExecutionRequest startRequest) throws BadRequestError, WorkflowExecutionAlreadyStartedError, ServiceBusyError, DomainNotActiveError, LimitExceededError, EntityNotExistsError, ClientVersionNotSupportedError, TException {
        return null;
    }

    @Override
    public GetWorkflowExecutionHistoryResponse GetWorkflowExecutionHistory(GetWorkflowExecutionHistoryRequest getRequest) throws BadRequestError, EntityNotExistsError, ServiceBusyError, ClientVersionNotSupportedError, TException {
        return null;
    }

    @Override
    public PollForDecisionTaskResponse PollForDecisionTask(PollForDecisionTaskRequest pollRequest) throws BadRequestError, ServiceBusyError, LimitExceededError, EntityNotExistsError, DomainNotActiveError, ClientVersionNotSupportedError, TException {
        return null;
    }

    @Override
    public RespondDecisionTaskCompletedResponse RespondDecisionTaskCompleted(RespondDecisionTaskCompletedRequest completeRequest) throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError, ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError, TException {
        return null;
    }

    @Override
    public void RespondDecisionTaskFailed(RespondDecisionTaskFailedRequest failedRequest) throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError, ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError, TException {

    }

    @Override
    public PollForActivityTaskResponse PollForActivityTask(PollForActivityTaskRequest pollRequest) throws BadRequestError, ServiceBusyError, LimitExceededError, EntityNotExistsError, DomainNotActiveError, ClientVersionNotSupportedError, TException {
        return null;
    }

    @Override
    public RecordActivityTaskHeartbeatResponse RecordActivityTaskHeartbeat(RecordActivityTaskHeartbeatRequest heartbeatRequest) throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError, ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError, TException {
        return null;
    }

    @Override
    public RecordActivityTaskHeartbeatResponse RecordActivityTaskHeartbeatByID(RecordActivityTaskHeartbeatByIDRequest heartbeatRequest) throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError, ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError, TException {
        return null;
    }

    @Override
    public void RespondActivityTaskCompleted(RespondActivityTaskCompletedRequest completeRequest) throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError, ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError, TException {

    }

    @Override
    public void RespondActivityTaskCompletedByID(RespondActivityTaskCompletedByIDRequest completeRequest) throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError, ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError, TException {

    }

    @Override
    public void RespondActivityTaskFailed(RespondActivityTaskFailedRequest failRequest) throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError, ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError, TException {

    }

    @Override
    public void RespondActivityTaskFailedByID(RespondActivityTaskFailedByIDRequest failRequest) throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError, ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError, TException {

    }

    @Override
    public void RespondActivityTaskCanceled(RespondActivityTaskCanceledRequest canceledRequest) throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError, ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError, TException {

    }

    @Override
    public void RespondActivityTaskCanceledByID(RespondActivityTaskCanceledByIDRequest canceledRequest) throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError, ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError, TException {

    }

    @Override
    public void RequestCancelWorkflowExecution(RequestCancelWorkflowExecutionRequest cancelRequest) throws BadRequestError, EntityNotExistsError, CancellationAlreadyRequestedError, ServiceBusyError, DomainNotActiveError, LimitExceededError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError, TException {

    }

    @Override
    public void SignalWorkflowExecution(SignalWorkflowExecutionRequest signalRequest) throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError, LimitExceededError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError, TException {

    }

    @Override
    public StartWorkflowExecutionResponse SignalWithStartWorkflowExecution(SignalWithStartWorkflowExecutionRequest signalWithStartRequest) throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError, LimitExceededError, WorkflowExecutionAlreadyStartedError, ClientVersionNotSupportedError, TException {
        return null;
    }

    @Override
    public ResetWorkflowExecutionResponse ResetWorkflowExecution(ResetWorkflowExecutionRequest resetRequest) throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError, LimitExceededError, ClientVersionNotSupportedError, TException {
        return null;
    }

    @Override
    public void TerminateWorkflowExecution(TerminateWorkflowExecutionRequest terminateRequest) throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError, LimitExceededError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError, TException {

    }

    @Override
    public ListOpenWorkflowExecutionsResponse ListOpenWorkflowExecutions(ListOpenWorkflowExecutionsRequest listRequest) throws BadRequestError, EntityNotExistsError, ServiceBusyError, LimitExceededError, ClientVersionNotSupportedError, TException {
        return null;
    }

    @Override
    public ListClosedWorkflowExecutionsResponse ListClosedWorkflowExecutions(ListClosedWorkflowExecutionsRequest listRequest) throws BadRequestError, EntityNotExistsError, ServiceBusyError, ClientVersionNotSupportedError, TException {
        return null;
    }

    @Override
    public ListWorkflowExecutionsResponse ListWorkflowExecutions(ListWorkflowExecutionsRequest listRequest) throws BadRequestError, EntityNotExistsError, ServiceBusyError, ClientVersionNotSupportedError, TException {
        return null;
    }

    @Override
    public ListArchivedWorkflowExecutionsResponse ListArchivedWorkflowExecutions(ListArchivedWorkflowExecutionsRequest listRequest) throws BadRequestError, EntityNotExistsError, ServiceBusyError, ClientVersionNotSupportedError, TException {
        return null;
    }

    @Override
    public ListWorkflowExecutionsResponse ScanWorkflowExecutions(ListWorkflowExecutionsRequest listRequest) throws BadRequestError, EntityNotExistsError, ServiceBusyError, ClientVersionNotSupportedError, TException {
        return null;
    }

    @Override
    public CountWorkflowExecutionsResponse CountWorkflowExecutions(CountWorkflowExecutionsRequest countRequest) throws BadRequestError, EntityNotExistsError, ServiceBusyError, ClientVersionNotSupportedError, TException {
        return null;
    }

    @Override
    public GetSearchAttributesResponse GetSearchAttributes() throws ServiceBusyError, ClientVersionNotSupportedError, TException {
        return null;
    }

    @Override
    public void RespondQueryTaskCompleted(RespondQueryTaskCompletedRequest completeRequest) throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError, DomainNotActiveError, ClientVersionNotSupportedError, TException {

    }

    @Override
    public ResetStickyTaskListResponse ResetStickyTaskList(ResetStickyTaskListRequest resetRequest) throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError, DomainNotActiveError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError, TException {
        return null;
    }

    @Override
    public QueryWorkflowResponse QueryWorkflow(QueryWorkflowRequest queryRequest) throws BadRequestError, EntityNotExistsError, QueryFailedError, LimitExceededError, ServiceBusyError, ClientVersionNotSupportedError, TException {
        return null;
    }

    @Override
    public DescribeWorkflowExecutionResponse DescribeWorkflowExecution(DescribeWorkflowExecutionRequest describeRequest) throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError, ClientVersionNotSupportedError, TException {
        return null;
    }

    @Override
    public DescribeTaskListResponse DescribeTaskList(DescribeTaskListRequest request) throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError, ClientVersionNotSupportedError, TException {
        return null;
    }

    @Override
    public ClusterInfo GetClusterInfo() throws InternalServiceError, ServiceBusyError, TException {
        return null;
    }

    @Override
    public GetTaskListsByDomainResponse GetTaskListsByDomain(GetTaskListsByDomainRequest request) throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError, ClientVersionNotSupportedError, TException {
        return null;
    }

    @Override
    public ListTaskListPartitionsResponse ListTaskListPartitions(ListTaskListPartitionsRequest request) throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError, TException {
        return null;
    }

    @Override
    public void RefreshWorkflowTasks(RefreshWorkflowTasksRequest request) throws BadRequestError, DomainNotActiveError, ServiceBusyError, EntityNotExistsError, TException {

    }

    @Override
    public void RegisterDomain(RegisterDomainRequest registerRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void DescribeDomain(DescribeDomainRequest describeRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void ListDomains(ListDomainsRequest listRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void UpdateDomain(UpdateDomainRequest updateRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void DeprecateDomain(DeprecateDomainRequest deprecateRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void StartWorkflowExecution(StartWorkflowExecutionRequest startRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void GetWorkflowExecutionHistory(GetWorkflowExecutionHistoryRequest getRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void PollForDecisionTask(PollForDecisionTaskRequest pollRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void RespondDecisionTaskCompleted(RespondDecisionTaskCompletedRequest completeRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void RespondDecisionTaskFailed(RespondDecisionTaskFailedRequest failedRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void PollForActivityTask(PollForActivityTaskRequest pollRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void RecordActivityTaskHeartbeat(RecordActivityTaskHeartbeatRequest heartbeatRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void RecordActivityTaskHeartbeatByID(RecordActivityTaskHeartbeatByIDRequest heartbeatRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void RespondActivityTaskCompleted(RespondActivityTaskCompletedRequest completeRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void RespondActivityTaskCompletedByID(RespondActivityTaskCompletedByIDRequest completeRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void RespondActivityTaskFailed(RespondActivityTaskFailedRequest failRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void RespondActivityTaskFailedByID(RespondActivityTaskFailedByIDRequest failRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void RespondActivityTaskCanceled(RespondActivityTaskCanceledRequest canceledRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void RespondActivityTaskCanceledByID(RespondActivityTaskCanceledByIDRequest canceledRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void RequestCancelWorkflowExecution(RequestCancelWorkflowExecutionRequest cancelRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void SignalWorkflowExecution(SignalWorkflowExecutionRequest signalRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void SignalWithStartWorkflowExecution(SignalWithStartWorkflowExecutionRequest signalWithStartRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void ResetWorkflowExecution(ResetWorkflowExecutionRequest resetRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void TerminateWorkflowExecution(TerminateWorkflowExecutionRequest terminateRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void ListOpenWorkflowExecutions(ListOpenWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void ListClosedWorkflowExecutions(ListClosedWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void ListWorkflowExecutions(ListWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void ListArchivedWorkflowExecutions(ListArchivedWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void ScanWorkflowExecutions(ListWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void CountWorkflowExecutions(CountWorkflowExecutionsRequest countRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void GetSearchAttributes(AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void RespondQueryTaskCompleted(RespondQueryTaskCompletedRequest completeRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void ResetStickyTaskList(ResetStickyTaskListRequest resetRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void QueryWorkflow(QueryWorkflowRequest queryRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void DescribeWorkflowExecution(DescribeWorkflowExecutionRequest describeRequest, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void DescribeTaskList(DescribeTaskListRequest request, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void GetClusterInfo(AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void GetTaskListsByDomain(GetTaskListsByDomainRequest request, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void ListTaskListPartitions(ListTaskListPartitionsRequest request, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void RefreshWorkflowTasks(RefreshWorkflowTasksRequest request, AsyncMethodCallback resultHandler) throws TException {

    }

    @Override
    public void close() {

    }

    @Override
    public void StartWorkflowExecutionWithTimeout(StartWorkflowExecutionRequest startRequest, AsyncMethodCallback resultHandler, Long timeoutInMillis) throws TException {

    }

    @Override
    public GetWorkflowExecutionHistoryResponse GetWorkflowExecutionHistoryWithTimeout(GetWorkflowExecutionHistoryRequest getRequest, Long timeoutInMillis) throws TException {
        return null;
    }

    @Override
    public void GetWorkflowExecutionHistoryWithTimeout(GetWorkflowExecutionHistoryRequest getRequest, AsyncMethodCallback resultHandler, Long timeoutInMillis) throws TException {

    }

    @Override
    public void SignalWorkflowExecutionWithTimeout(SignalWorkflowExecutionRequest signalRequest, AsyncMethodCallback resultHandler, Long timeoutInMillis) throws TException {

    }

    @Override
    public CompletableFuture<Boolean> isHealthy() {
        return null;
    }

    public MigrationState getMigrationState() {
        return MigrationState.ENABLED;
    }
}
