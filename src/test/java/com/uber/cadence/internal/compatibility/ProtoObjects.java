package com.uber.cadence.internal.compatibility;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Duration;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Timestamp;
import com.uber.cadence.api.v1.*;
import java.util.Map;

public final class ProtoObjects {
  public static final WorkflowType WORKFLOW_TYPE =
      WorkflowType.newBuilder().setName("workflowType").build();
  public static final ActivityType ACTIVITY_TYPE =
      ActivityType.newBuilder().setName("activityName").build();
  public static final TaskList TASK_LIST =
      TaskList.newBuilder().setName("taskList").setKind(TaskListKind.TASK_LIST_KIND_NORMAL).build();
  public static final TaskListMetadata TASK_LIST_METADATA =
      TaskListMetadata.newBuilder()
          .setMaxTasksPerSecond(DoubleValue.newBuilder().setValue(10.0).build())
          .build();
  public static final RetryPolicy RETRY_POLICY =
      RetryPolicy.newBuilder()
          .setInitialInterval(seconds(11))
          .setBackoffCoefficient(0.5)
          .setMaximumInterval(seconds(12))
          .setMaximumAttempts(13)
          .addNonRetryableErrorReasons("error")
          .setExpirationInterval(seconds(14))
          .build();
  public static final WorkflowExecution WORKFLOW_EXECUTION =
      WorkflowExecution.newBuilder().setWorkflowId("workflowId").setRunId("runId").build();
  public static final Failure FAILURE =
      Failure.newBuilder().setDetails(utf8("details")).setReason("reason").build();
  public static final StickyExecutionAttributes STICKY_EXECUTION_ATTRIBUTES =
      StickyExecutionAttributes.newBuilder()
          .setWorkerTaskList(TASK_LIST)
          .setScheduleToStartTimeout(seconds(1))
          .build();
  public static final WorkflowQuery WORKFLOW_QUERY =
      WorkflowQuery.newBuilder()
          .setQueryType("queryType")
          .setQueryArgs(payload("queryArgs"))
          .build();
  public static final WorkflowQueryResult WORKFLOW_QUERY_RESULT =
      WorkflowQueryResult.newBuilder()
          .setResultType(QueryResultType.QUERY_RESULT_TYPE_ANSWERED)
          .setAnswer(payload("answer"))
          .setErrorMessage("error")
          .build();
  public static final Header HEADER =
      Header.newBuilder().putFields("key", payload("value")).build();
  public static final Memo MEMO = Memo.newBuilder().putFields("memo", payload("memoValue")).build();
  public static final SearchAttributes SEARCH_ATTRIBUTES =
      SearchAttributes.newBuilder().putIndexedFields("search", payload("attributes")).build();
  public static final Map<String, String> DATA = ImmutableMap.of("dataKey", "dataValue");

  public static final ClusterReplicationConfiguration CLUSTER_REPLICATION_CONFIGURATION =
      ClusterReplicationConfiguration.newBuilder().setClusterName("cluster").build();

  public static Decision DECISION_SCHEDULE_ACTIVITY_TASK =
      Decision.newBuilder()
          .setScheduleActivityTaskDecisionAttributes(
              ScheduleActivityTaskDecisionAttributes.newBuilder()
                  .setActivityId("activityId")
                  .setActivityType(ACTIVITY_TYPE)
                  .setTaskList(TASK_LIST)
                  .setInput(payload("input"))
                  .setScheduleToCloseTimeout(seconds(1))
                  .setScheduleToStartTimeout(seconds(2))
                  .setStartToCloseTimeout(seconds(3))
                  .setHeartbeatTimeout(seconds(4))
                  .setHeader(HEADER)
                  .setRequestLocalDispatch(true)
                  .setRetryPolicy(RETRY_POLICY)
                  .setDomain("domain"))
          .build();
  public static Decision DECISION_REQUEST_CANCEL_ACTIVITY_TASK =
      Decision.newBuilder()
          .setRequestCancelActivityTaskDecisionAttributes(
              RequestCancelActivityTaskDecisionAttributes.newBuilder().setActivityId("activityId"))
          .build();
  public static Decision DECISION_START_TIMER =
      Decision.newBuilder()
          .setStartTimerDecisionAttributes(
              StartTimerDecisionAttributes.newBuilder()
                  .setTimerId("timerId")
                  .setStartToFireTimeout(seconds(2)))
          .build();
  public static Decision DECISION_COMPLETE_WORKFLOW_EXECUTION =
      Decision.newBuilder()
          .setCompleteWorkflowExecutionDecisionAttributes(
              CompleteWorkflowExecutionDecisionAttributes.newBuilder().setResult(payload("result")))
          .build();
  public static Decision DECISION_FAIL_WORKFLOW_EXECUTION =
      Decision.newBuilder()
          .setFailWorkflowExecutionDecisionAttributes(
              FailWorkflowExecutionDecisionAttributes.newBuilder().setFailure(FAILURE))
          .build();
  public static Decision DECISION_CANCEL_TIMER =
      Decision.newBuilder()
          .setCancelTimerDecisionAttributes(
              CancelTimerDecisionAttributes.newBuilder().setTimerId("timerId"))
          .build();
  public static Decision DECISION_CANCEL_WORKFLOW =
      Decision.newBuilder()
          .setCancelWorkflowExecutionDecisionAttributes(
              CancelWorkflowExecutionDecisionAttributes.newBuilder().setDetails(payload("details")))
          .build();
  public static Decision DECISION_REQUEST_CANCEL_EXTERNAL_WORKFLOW_EXECUTION =
      Decision.newBuilder()
          .setRequestCancelExternalWorkflowExecutionDecisionAttributes(
              RequestCancelExternalWorkflowExecutionDecisionAttributes.newBuilder()
                  .setDomain("domain")
                  .setWorkflowExecution(WORKFLOW_EXECUTION)
                  .setChildWorkflowOnly(true)
                  .setControl(utf8("control")))
          .build();
  public static Decision DECISION_CONTINUE_AS_NEW_WORKFLOW_EXECUTION =
      Decision.newBuilder()
          .setContinueAsNewWorkflowExecutionDecisionAttributes(
              ContinueAsNewWorkflowExecutionDecisionAttributes.newBuilder()
                  .setWorkflowType(WORKFLOW_TYPE)
                  .setTaskList(TASK_LIST)
                  .setInput(payload("input"))
                  .setExecutionStartToCloseTimeout(seconds(1))
                  .setTaskStartToCloseTimeout(seconds(2))
                  .setBackoffStartInterval(seconds(3))
                  .setInitiator(ContinueAsNewInitiator.CONTINUE_AS_NEW_INITIATOR_DECIDER)
                  .setFailure(FAILURE)
                  .setLastCompletionResult(payload("lastCompletionResult"))
                  .setHeader(HEADER)
                  .setMemo(MEMO)
                  .setSearchAttributes(SEARCH_ATTRIBUTES)
                  .setRetryPolicy(RETRY_POLICY)
                  .setCronSchedule("cron"))
          .build();
  public static Decision DECISION_START_CHILD_WORKFLOW_EXECUTION =
      Decision.newBuilder()
          .setStartChildWorkflowExecutionDecisionAttributes(
              StartChildWorkflowExecutionDecisionAttributes.newBuilder()
                  .setDomain("domain")
                  .setWorkflowId("workflowId")
                  .setWorkflowType(WORKFLOW_TYPE)
                  .setTaskList(TASK_LIST)
                  .setInput(payload("input"))
                  .setExecutionStartToCloseTimeout(seconds(1))
                  .setTaskStartToCloseTimeout(seconds(2))
                  .setHeader(HEADER)
                  .setMemo(MEMO)
                  .setSearchAttributes(SEARCH_ATTRIBUTES)
                  .setRetryPolicy(RETRY_POLICY)
                  .setCronSchedule("cron")
                  .setControl(utf8("control"))
                  .setParentClosePolicy(ParentClosePolicy.PARENT_CLOSE_POLICY_ABANDON)
                  .setWorkflowIdReusePolicy(
                      WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE))
          .build();
  public static Decision DECISION_SIGNAL_EXTERNAL_WORKFLOW_EXECUTION =
      Decision.newBuilder()
          .setSignalExternalWorkflowExecutionDecisionAttributes(
              SignalExternalWorkflowExecutionDecisionAttributes.newBuilder()
                  .setDomain("domain")
                  .setWorkflowExecution(WORKFLOW_EXECUTION)
                  .setSignalName("signalName")
                  .setInput(payload("input"))
                  .setChildWorkflowOnly(true)
                  .setControl(utf8("control")))
          .build();
  public static Decision DECISION_UPSERT_WORKFLOW_SEARCH_ATTRIBUTES =
      Decision.newBuilder()
          .setUpsertWorkflowSearchAttributesDecisionAttributes(
              UpsertWorkflowSearchAttributesDecisionAttributes.newBuilder()
                  .setSearchAttributes(SEARCH_ATTRIBUTES))
          .build();
  public static Decision DECISION_RECORD_MARKER =
      Decision.newBuilder()
          .setRecordMarkerDecisionAttributes(
              RecordMarkerDecisionAttributes.newBuilder()
                  .setMarkerName("markerName")
                  .setDetails(payload("details"))
                  .setHeader(HEADER))
          .build();
  public static final CountWorkflowExecutionsRequest COUNT_WORKFLOW_EXECUTIONS_REQUEST =
      CountWorkflowExecutionsRequest.newBuilder().setDomain("domain").setQuery("query").build();
  public static final DescribeTaskListRequest DESCRIBE_TASK_LIST_REQUEST =
      DescribeTaskListRequest.newBuilder()
          .setDomain("domain")
          .setTaskList(TASK_LIST)
          .setTaskListType(TaskListType.TASK_LIST_TYPE_ACTIVITY)
          .setIncludeTaskListStatus(true)
          .build();
  public static final ListArchivedWorkflowExecutionsRequest
      LIST_ARCHIVED_WORKFLOW_EXECUTIONS_REQUEST =
          ListArchivedWorkflowExecutionsRequest.newBuilder()
              .setDomain("domain")
              .setPageSize(1)
              .setNextPageToken(utf8("pageToken"))
              .setQuery("query")
              .build();
  public static final RequestCancelWorkflowExecutionRequest
      REQUEST_CANCEL_WORKFLOW_EXECUTION_REQUEST =
          RequestCancelWorkflowExecutionRequest.newBuilder()
              .setDomain("domain")
              .setWorkflowExecution(WORKFLOW_EXECUTION)
              .setRequestId("requestId")
              .setIdentity("identity")
              .build();
  public static final ResetStickyTaskListRequest RESET_STICKY_TASK_LIST_REQUEST =
      ResetStickyTaskListRequest.newBuilder()
          .setDomain("domain")
          .setWorkflowExecution(WORKFLOW_EXECUTION)
          .build();
  public static final ResetWorkflowExecutionRequest RESET_WORKFLOW_EXECUTION_REQUEST =
      ResetWorkflowExecutionRequest.newBuilder()
          .setDomain("domain")
          .setWorkflowExecution(WORKFLOW_EXECUTION)
          .setReason("reason")
          .setDecisionFinishEventId(1)
          .setRequestId("requestId")
          .setSkipSignalReapply(true)
          .build();
  public static final RespondActivityTaskCanceledByIDRequest
      RESPOND_ACTIVITY_TASK_CANCELED_BY_ID_REQUEST =
          RespondActivityTaskCanceledByIDRequest.newBuilder()
              .setDomain("domain")
              .setWorkflowExecution(WORKFLOW_EXECUTION)
              .setActivityId("activityId")
              .setDetails(payload("details"))
              .setIdentity("identity")
              .build();
  public static final RespondActivityTaskCanceledRequest RESPOND_ACTIVITY_TASK_CANCELED_REQUEST =
      RespondActivityTaskCanceledRequest.newBuilder()
          .setTaskToken(utf8("taskToken"))
          .setIdentity("identity")
          .setDetails(payload("details"))
          .build();
  public static final RespondActivityTaskCompletedByIDRequest
      RESPOND_ACTIVITY_TASK_COMPLETED_BY_ID_REQUEST =
          RespondActivityTaskCompletedByIDRequest.newBuilder()
              .setDomain("domain")
              .setWorkflowExecution(WORKFLOW_EXECUTION)
              .setActivityId("activityId")
              .setResult(payload("result"))
              .setIdentity("identity")
              .build();
  public static final RespondActivityTaskCompletedRequest RESPOND_ACTIVITY_TASK_COMPLETED_REQUEST =
      RespondActivityTaskCompletedRequest.newBuilder()
          .setTaskToken(utf8("taskToken"))
          .setIdentity("identity")
          .setResult(payload("result"))
          .build();
  public static final RespondActivityTaskFailedByIDRequest
      RESPOND_ACTIVITY_TASK_FAILED_BY_ID_REQUEST =
          RespondActivityTaskFailedByIDRequest.newBuilder()
              .setDomain("domain")
              .setWorkflowExecution(WORKFLOW_EXECUTION)
              .setActivityId("activityId")
              .setFailure(FAILURE)
              .setIdentity("identity")
              .build();
  public static final RespondActivityTaskFailedRequest RESPOND_ACTIVITY_TASK_FAILED_REQUEST =
      RespondActivityTaskFailedRequest.newBuilder()
          .setTaskToken(utf8("taskToken"))
          .setFailure(FAILURE)
          .setIdentity("identity")
          .build();
  public static final RespondDecisionTaskCompletedRequest RESPOND_DECISION_TASK_COMPLETED_REQUEST =
      RespondDecisionTaskCompletedRequest.newBuilder()
          .addAllDecisions(ImmutableList.of(DECISION_COMPLETE_WORKFLOW_EXECUTION))
          .setStickyAttributes(STICKY_EXECUTION_ATTRIBUTES)
          .setReturnNewDecisionTask(true)
          .setForceCreateNewDecisionTask(false)
          .putAllQueryResults(ImmutableMap.of("query", WORKFLOW_QUERY_RESULT))
          .setExecutionContext(utf8("executionContext"))
          .setBinaryChecksum("binaryChecksum")
          .setTaskToken(utf8("taskToken"))
          .setIdentity("identity")
          .build();
  public static final RespondDecisionTaskFailedRequest RESPOND_DECISION_TASK_FAILED_REQUEST =
      RespondDecisionTaskFailedRequest.newBuilder()
          .setCause(DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_BINARY)
          .setDetails(payload("details"))
          .setBinaryChecksum("binaryChecksum")
          .setTaskToken(utf8("taskToken"))
          .setIdentity("identity")
          .build();
  public static final RespondQueryTaskCompletedRequest RESPOND_QUERY_TASK_COMPLETED_REQUEST =
      RespondQueryTaskCompletedRequest.newBuilder()
          .setResult(
              WorkflowQueryResult.newBuilder()
                  .setResultType(QueryResultType.QUERY_RESULT_TYPE_ANSWERED)
                  .setAnswer(payload("queryResult"))
                  .setErrorMessage("errorMessage"))
          .setWorkerVersionInfo(
              WorkerVersionInfo.newBuilder().setFeatureVersion("featureVersion").setImpl("impl"))
          .setTaskToken(utf8("taskToken"))
          .build();
  public static final ScanWorkflowExecutionsRequest SCAN_WORKFLOW_EXECUTIONS_REQUEST =
      ScanWorkflowExecutionsRequest.newBuilder()
          .setDomain("domain")
          .setPageSize(1)
          .setNextPageToken(utf8("nextPageToken"))
          .setQuery("query")
          .build();
  public static final DescribeWorkflowExecutionRequest DESCRIBE_WORKFLOW_EXECUTION_REQUEST =
      DescribeWorkflowExecutionRequest.newBuilder()
          .setDomain("domain")
          .setWorkflowExecution(WORKFLOW_EXECUTION)
          .build();

  public static final GetWorkflowExecutionHistoryRequest GET_WORKFLOW_EXECUTION_HISTORY_REQUEST =
      GetWorkflowExecutionHistoryRequest.newBuilder()
          .setDomain("domain")
          .setWorkflowExecution(WORKFLOW_EXECUTION)
          .setPageSize(1)
          .setWaitForNewEvent(true)
          .setHistoryEventFilterType(EventFilterType.EVENT_FILTER_TYPE_CLOSE_EVENT)
          .setSkipArchival(true)
          .setNextPageToken(utf8("nextPageToken"))
          .build();

  public static final StartWorkflowExecutionRequest START_WORKFLOW_EXECUTION =
      StartWorkflowExecutionRequest.newBuilder()
          .setDomain("domain")
          .setWorkflowId("workflowId")
          .setWorkflowType(WorkflowType.newBuilder().setName("workflowType"))
          .setTaskList(TASK_LIST)
          .setInput(payload("input"))
          .setExecutionStartToCloseTimeout(seconds(1))
          .setTaskStartToCloseTimeout(seconds(2))
          .setIdentity("identity")
          .setRequestId("requestId")
          .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE)
          .setRetryPolicy(RETRY_POLICY)
          .setCronSchedule("cronSchedule")
          .setMemo(MEMO)
          .setSearchAttributes(SEARCH_ATTRIBUTES)
          .setHeader(HEADER)
          .setDelayStart(seconds(3))
          .build();

  public static final SignalWithStartWorkflowExecutionRequest SIGNAL_WITH_START_WORKFLOW_EXECUTION =
      com.uber.cadence.api.v1.SignalWithStartWorkflowExecutionRequest.newBuilder()
          .setStartRequest(START_WORKFLOW_EXECUTION)
          .setSignalInput(payload("signalInput"))
          .setSignalName("signalName")
          .setControl(utf8("control"))
          .build();

  public static final StartWorkflowExecutionAsyncRequest START_WORKFLOW_EXECUTION_ASYNC_REQUEST =
      StartWorkflowExecutionAsyncRequest.newBuilder().setRequest(START_WORKFLOW_EXECUTION).build();

  public static final SignalWithStartWorkflowExecutionAsyncRequest
      SIGNAL_WITH_START_WORKFLOW_EXECUTION_ASYNC_REQUEST =
          SignalWithStartWorkflowExecutionAsyncRequest.newBuilder()
              .setRequest(SIGNAL_WITH_START_WORKFLOW_EXECUTION)
              .build();

  public static final SignalWorkflowExecutionRequest SIGNAL_WORKFLOW_EXECUTION_REQUEST =
      SignalWorkflowExecutionRequest.newBuilder()
          .setDomain("domain")
          .setWorkflowExecution(WORKFLOW_EXECUTION)
          .setSignalName("signalName")
          .setSignalInput(payload("input"))
          .setRequestId("requestId")
          .setControl(utf8("control"))
          .setIdentity("identity")
          .build();

  public static final TerminateWorkflowExecutionRequest TERMINATE_WORKFLOW_EXECUTION_REQUEST =
      TerminateWorkflowExecutionRequest.newBuilder()
          .setDomain("domain")
          .setWorkflowExecution(WORKFLOW_EXECUTION)
          .setReason("reason")
          .setDetails(payload("details"))
          .setIdentity("identity")
          .build();

  public static final DeprecateDomainRequest DEPRECATE_DOMAIN_REQUEST =
      DeprecateDomainRequest.newBuilder()
          .setName("domain")
          .setSecurityToken("securityToken")
          .build();

  public static final ListWorkflowExecutionsRequest LIST_WORKFLOW_EXECUTIONS_REQUEST =
      ListWorkflowExecutionsRequest.newBuilder()
          .setDomain("domain")
          .setQuery("query")
          .setPageSize(1)
          .setNextPageToken(utf8("nextPageToken"))
          .build();

  public static final DescribeDomainRequest DESCRIBE_DOMAIN_BY_ID_REQUEST =
      DescribeDomainRequest.newBuilder().setId("uuid").build();

  public static final DescribeDomainRequest DESCRIBE_DOMAIN_BY_NAME_REQUEST =
      DescribeDomainRequest.newBuilder().setName("name").build();

  public static final ListDomainsRequest LIST_DOMAINS_REQUEST =
      ListDomainsRequest.newBuilder()
          .setPageSize(1)
          .setNextPageToken(utf8("nextPageToken"))
          .build();

  public static final ListTaskListPartitionsRequest LIST_TASK_LIST_PARTITIONS_REQUEST =
      ListTaskListPartitionsRequest.newBuilder().setDomain("domain").setTaskList(TASK_LIST).build();

  public static final PollForActivityTaskRequest POLL_FOR_ACTIVITY_TASK_REQUEST =
      PollForActivityTaskRequest.newBuilder()
          .setDomain("domain")
          .setTaskList(TASK_LIST)
          .setTaskListMetadata(TASK_LIST_METADATA)
          .setIdentity("identity")
          .build();
  public static final PollForDecisionTaskRequest POLL_FOR_DECISION_TASK_REQUEST =
      PollForDecisionTaskRequest.newBuilder()
          .setDomain("domain")
          .setTaskList(TASK_LIST)
          .setBinaryChecksum("binaryChecksum")
          .setIdentity("identity")
          .build();
  public static final QueryWorkflowRequest QUERY_WORKFLOW_REQUEST =
      QueryWorkflowRequest.newBuilder()
          .setDomain("domain")
          .setWorkflowExecution(WORKFLOW_EXECUTION)
          .setQuery(WORKFLOW_QUERY)
          .setQueryRejectCondition(
              QueryRejectCondition.QUERY_REJECT_CONDITION_NOT_COMPLETED_CLEANLY)
          .setQueryConsistencyLevel(QueryConsistencyLevel.QUERY_CONSISTENCY_LEVEL_STRONG)
          .build();

  public static final RecordActivityTaskHeartbeatByIDRequest
      RECORD_ACTIVITY_TASK_HEARTBEAT_BY_ID_REQUEST =
          RecordActivityTaskHeartbeatByIDRequest.newBuilder()
              .setDomain("domain")
              .setWorkflowExecution(WORKFLOW_EXECUTION)
              .setActivityId("activityId")
              .setDetails(payload("details"))
              .setIdentity("identity")
              .build();

  public static final RecordActivityTaskHeartbeatRequest RECORD_ACTIVITY_TASK_HEARTBEAT_REQUEST =
      RecordActivityTaskHeartbeatRequest.newBuilder()
          .setDetails(payload("details"))
          .setTaskToken(utf8("taskToken"))
          .setIdentity("identity")
          .build();

  public static final RegisterDomainRequest REGISTER_DOMAIN_REQUEST =
      RegisterDomainRequest.newBuilder()
          .setName("domain")
          .setDescription("description")
          .setOwnerEmail("ownerEmail")
          .setWorkflowExecutionRetentionPeriod(days(1))
          .addAllClusters(ImmutableList.of(CLUSTER_REPLICATION_CONFIGURATION))
          .setActiveClusterName("activeCluster")
          .putAllData(DATA)
          .setSecurityToken("securityToken")
          .setIsGlobalDomain(true)
          .setHistoryArchivalStatus(ArchivalStatus.ARCHIVAL_STATUS_ENABLED)
          .setHistoryArchivalUri("historyArchivalUri")
          .setVisibilityArchivalStatus(ArchivalStatus.ARCHIVAL_STATUS_DISABLED)
          .setVisibilityArchivalUri("visibilityArchivalUri")
          .build();

  public static final UpdateDomainRequest UPDATE_DOMAIN_REQUEST =
      UpdateDomainRequest.newBuilder()
          .setName("domain")
          .setSecurityToken("securityToken")
          .setDescription("description")
          .setOwnerEmail("ownerEmail")
          .setWorkflowExecutionRetentionPeriod(days(2))
          .setBadBinaries(
              BadBinaries.newBuilder()
                  .putBinaries(
                      "badBinaryKey",
                      BadBinaryInfo.newBuilder()
                          .setReason("reason")
                          .setOperator("operator")
                          .setCreatedTime(timestampNanos(3))
                          .build()))
          .setHistoryArchivalStatus(ArchivalStatus.ARCHIVAL_STATUS_ENABLED)
          .setHistoryArchivalUri("historyArchivalUri")
          .setVisibilityArchivalStatus(ArchivalStatus.ARCHIVAL_STATUS_DISABLED)
          .setVisibilityArchivalUri("visibilityArchivalUri")
          .setDeleteBadBinary("deleteBadBinary")
          .setFailoverTimeout(seconds(1))
          .setUpdateMask(
              FieldMask.newBuilder()
                  .addPaths("description")
                  .addPaths("owner_email")
                  .addPaths("data")
                  .addPaths("workflow_execution_retention_period")
                  .addPaths("bad_binaries")
                  .addPaths("history_archival_status")
                  .addPaths("history_archival_uri")
                  .addPaths("visibility_archival_status")
                  .addPaths("visibility_archival_uri")
                  .addPaths("delete_bad_binary")
                  .addPaths("failover_timeout")
                  .build())
          .build();

  public static final ListClosedWorkflowExecutionsRequest LIST_CLOSED_WORKFLOW_EXECUTIONS_REQUEST =
      ListClosedWorkflowExecutionsRequest.newBuilder()
          .setDomain("domain")
          .setPageSize(1)
          .setExecutionFilter(
              WorkflowExecutionFilter.newBuilder()
                  .setWorkflowId(WORKFLOW_EXECUTION.getWorkflowId())
                  .setRunId(WORKFLOW_EXECUTION.getRunId()))
          .setTypeFilter(WorkflowTypeFilter.newBuilder().setName(WORKFLOW_TYPE.getName()))
          .setStatusFilter(
              StatusFilter.newBuilder()
                  .setStatus(
                      WorkflowExecutionCloseStatus.WORKFLOW_EXECUTION_CLOSE_STATUS_COMPLETED))
          .setNextPageToken(utf8("nextPageToken"))
          .setStartTimeFilter(
              StartTimeFilter.newBuilder()
                  .setEarliestTime(timestampNanos(2))
                  .setLatestTime(timestampNanos(3)))
          .build();

  public static final ListOpenWorkflowExecutionsRequest LIST_OPEN_WORKFLOW_EXECUTIONS_REQUEST =
      ListOpenWorkflowExecutionsRequest.newBuilder()
          .setDomain("domain")
          .setPageSize(1)
          .setExecutionFilter(
              WorkflowExecutionFilter.newBuilder()
                  .setWorkflowId(WORKFLOW_EXECUTION.getWorkflowId())
                  .setRunId(WORKFLOW_EXECUTION.getRunId()))
          .setTypeFilter(WorkflowTypeFilter.newBuilder().setName(WORKFLOW_TYPE.getName()))
          .setNextPageToken(utf8("nextPageToken"))
          .setStartTimeFilter(
              StartTimeFilter.newBuilder()
                  .setEarliestTime(timestampNanos(2))
                  .setLatestTime(timestampNanos(3)))
          .build();

  private ProtoObjects() {}

  private static Payload payload(String value) {
    return Payload.newBuilder().setData(utf8(value)).build();
  }

  private static Duration seconds(int value) {
    return Duration.newBuilder().setSeconds(value).build();
  }

  private static Duration days(int value) {
    return Duration.newBuilder().setSeconds(((long) value) * 24 * 60 * 60).build();
  }

  private static Timestamp timestampNanos(int value) {
    return Timestamp.newBuilder().setNanos(value).build();
  }

  private static ByteString utf8(String value) {
    return ByteString.copyFromUtf8(value);
  }
}
