package com.uber.cadence.internal.compatibility;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.uber.cadence.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class ThriftObjects {
  public static final WorkflowType WORKFLOW_TYPE =
      new com.uber.cadence.WorkflowType().setName("workflowType");
  public static final ActivityType ACTIVITY_TYPE = new ActivityType().setName("activityName");
  public static final TaskList TASK_LIST =
      new com.uber.cadence.TaskList()
          .setName("taskList")
          .setKind(com.uber.cadence.TaskListKind.NORMAL);
  public static final TaskListMetadata TASK_LIST_METADATA =
      new TaskListMetadata().setMaxTasksPerSecond(10);
  public static final RetryPolicy RETRY_POLICY =
      new com.uber.cadence.RetryPolicy()
          .setInitialIntervalInSeconds(11)
          .setBackoffCoefficient(0.5)
          .setMaximumIntervalInSeconds(12)
          .setMaximumAttempts(13)
          .setNonRetriableErrorReasons(ImmutableList.of("error"))
          .setExpirationIntervalInSeconds(14);
  public static final String WORKFLOW_ID = "workflowId";
  public static final String RUN_ID = "runId";
  public static final WorkflowExecution WORKFLOW_EXECUTION =
      new WorkflowExecution().setWorkflowId(WORKFLOW_ID).setRunId(RUN_ID);
  public static final StickyExecutionAttributes STICKY_EXECUTION_ATTRIBUTES =
      new StickyExecutionAttributes()
          .setWorkerTaskList(TASK_LIST)
          .setScheduleToStartTimeoutSeconds(1);
  public static final WorkflowQuery WORKFLOW_QUERY =
      new WorkflowQuery().setQueryType("queryType").setQueryArgs(utf8("queryArgs"));
  public static final WorkflowQueryResult WORKFLOW_QUERY_RESULT =
      new WorkflowQueryResult()
          .setResultType(QueryResultType.ANSWERED)
          .setAnswer(utf8("answer"))
          .setErrorMessage("error");
  public static final Header HEADER = new Header().setFields(ImmutableMap.of("key", utf8("value")));
  public static final Memo MEMO = new Memo().setFields(ImmutableMap.of("memo", utf8("memoValue")));
  public static final SearchAttributes SEARCH_ATTRIBUTES =
      new SearchAttributes().setIndexedFields(ImmutableMap.of("search", utf8("attributes")));
  public static final Map<String, String> DATA = ImmutableMap.of("dataKey", "dataValue");

  public static final ClusterReplicationConfiguration CLUSTER_REPLICATION_CONFIGURATION =
      new ClusterReplicationConfiguration().setClusterName("cluster");

  public static Decision DECISION_SCHEDULE_ACTIVITY_TASK =
      new Decision()
          .setDecisionType(DecisionType.ScheduleActivityTask)
          .setScheduleActivityTaskDecisionAttributes(
              new ScheduleActivityTaskDecisionAttributes()
                  .setActivityId("activityId")
                  .setActivityType(ACTIVITY_TYPE)
                  .setTaskList(TASK_LIST)
                  .setInput(utf8("input"))
                  .setScheduleToCloseTimeoutSeconds(1)
                  .setScheduleToStartTimeoutSeconds(2)
                  .setStartToCloseTimeoutSeconds(3)
                  .setHeartbeatTimeoutSeconds(4)
                  .setHeader(HEADER)
                  .setRequestLocalDispatch(true)
                  .setRetryPolicy(RETRY_POLICY)
                  .setDomain("domain"));
  public static Decision DECISION_REQUEST_CANCEL_ACTIVITY_TASK =
      new Decision()
          .setDecisionType(DecisionType.RequestCancelActivityTask)
          .setRequestCancelActivityTaskDecisionAttributes(
              new RequestCancelActivityTaskDecisionAttributes().setActivityId("activityId"));
  public static Decision DECISION_START_TIMER =
      new Decision()
          .setDecisionType(DecisionType.StartTimer)
          .setStartTimerDecisionAttributes(
              new StartTimerDecisionAttributes()
                  .setTimerId("timerId")
                  .setStartToFireTimeoutSeconds(2));
  public static Decision DECISION_COMPLETE_WORKFLOW_EXECUTION =
      new Decision()
          .setDecisionType(DecisionType.CompleteWorkflowExecution)
          .setCompleteWorkflowExecutionDecisionAttributes(
              new CompleteWorkflowExecutionDecisionAttributes().setResult(utf8("result")));
  public static Decision DECISION_FAIL_WORKFLOW_EXECUTION =
      new Decision()
          .setDecisionType(DecisionType.FailWorkflowExecution)
          .setFailWorkflowExecutionDecisionAttributes(
              new FailWorkflowExecutionDecisionAttributes()
                  .setReason("reason")
                  .setDetails(utf8("details")));
  public static Decision DECISION_CANCEL_TIMER =
      new Decision()
          .setDecisionType(DecisionType.CancelTimer)
          .setCancelTimerDecisionAttributes(
              new CancelTimerDecisionAttributes().setTimerId("timerId"));
  public static Decision DECISION_CANCEL_WORKFLOW =
      new Decision()
          .setDecisionType(DecisionType.CancelWorkflowExecution)
          .setCancelWorkflowExecutionDecisionAttributes(
              new CancelWorkflowExecutionDecisionAttributes().setDetails(utf8("details")));
  public static Decision DECISION_REQUEST_CANCEL_EXTERNAL_WORKFLOW_EXECUTION =
      new Decision()
          .setDecisionType(DecisionType.RequestCancelExternalWorkflowExecution)
          .setRequestCancelExternalWorkflowExecutionDecisionAttributes(
              new RequestCancelExternalWorkflowExecutionDecisionAttributes()
                  .setDomain("domain")
                  .setWorkflowId(WORKFLOW_ID)
                  .setRunId(RUN_ID)
                  .setChildWorkflowOnly(true)
                  .setControl(utf8("control")));
  public static Decision DECISION_CONTINUE_AS_NEW_WORKFLOW_EXECUTION =
      new Decision()
          .setDecisionType(DecisionType.ContinueAsNewWorkflowExecution)
          .setContinueAsNewWorkflowExecutionDecisionAttributes(
              new ContinueAsNewWorkflowExecutionDecisionAttributes()
                  .setWorkflowType(WORKFLOW_TYPE)
                  .setTaskList(TASK_LIST)
                  .setInput(utf8("input"))
                  .setExecutionStartToCloseTimeoutSeconds(1)
                  .setTaskStartToCloseTimeoutSeconds(2)
                  .setBackoffStartIntervalInSeconds(3)
                  .setInitiator(ContinueAsNewInitiator.Decider)
                  .setFailureDetails(utf8("details"))
                  .setFailureReason("reason")
                  .setLastCompletionResult(utf8("lastCompletionResult"))
                  .setHeader(HEADER)
                  .setMemo(MEMO)
                  .setSearchAttributes(SEARCH_ATTRIBUTES)
                  .setRetryPolicy(RETRY_POLICY)
                  .setCronSchedule("cron"));
  public static Decision DECISION_START_CHILD_WORKFLOW_EXECUTION =
      new Decision()
          .setDecisionType(DecisionType.StartChildWorkflowExecution)
          .setStartChildWorkflowExecutionDecisionAttributes(
              new StartChildWorkflowExecutionDecisionAttributes()
                  .setDomain("domain")
                  .setWorkflowId(WORKFLOW_ID)
                  .setWorkflowType(WORKFLOW_TYPE)
                  .setTaskList(TASK_LIST)
                  .setInput(utf8("input"))
                  .setExecutionStartToCloseTimeoutSeconds(1)
                  .setTaskStartToCloseTimeoutSeconds(2)
                  .setHeader(HEADER)
                  .setMemo(MEMO)
                  .setSearchAttributes(SEARCH_ATTRIBUTES)
                  .setRetryPolicy(RETRY_POLICY)
                  .setCronSchedule("cron")
                  .setControl(utf8("control"))
                  .setParentClosePolicy(ParentClosePolicy.ABANDON)
                  .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.AllowDuplicate));
  public static Decision DECISION_SIGNAL_EXTERNAL_WORKFLOW_EXECUTION =
      new Decision()
          .setDecisionType(DecisionType.SignalExternalWorkflowExecution)
          .setSignalExternalWorkflowExecutionDecisionAttributes(
              new SignalExternalWorkflowExecutionDecisionAttributes()
                  .setDomain("domain")
                  .setExecution(WORKFLOW_EXECUTION)
                  .setSignalName("signalName")
                  .setInput(utf8("input"))
                  .setChildWorkflowOnly(true)
                  .setControl(utf8("control")));
  public static Decision DECISION_UPSERT_WORKFLOW_SEARCH_ATTRIBUTES =
      new Decision()
          .setDecisionType(DecisionType.UpsertWorkflowSearchAttributes)
          .setUpsertWorkflowSearchAttributesDecisionAttributes(
              new UpsertWorkflowSearchAttributesDecisionAttributes()
                  .setSearchAttributes(SEARCH_ATTRIBUTES));
  public static Decision DECISION_RECORD_MARKER =
      new Decision()
          .setDecisionType(DecisionType.RecordMarker)
          .setRecordMarkerDecisionAttributes(
              new RecordMarkerDecisionAttributes()
                  .setMarkerName("markerName")
                  .setDetails(utf8("details"))
                  .setHeader(HEADER));
  public static final CountWorkflowExecutionsRequest COUNT_WORKFLOW_EXECUTIONS_REQUEST =
      new CountWorkflowExecutionsRequest().setDomain("domain").setQuery("query");
  public static final DescribeTaskListRequest DESCRIBE_TASK_LIST_REQUEST =
      new DescribeTaskListRequest()
          .setDomain("domain")
          .setTaskList(TASK_LIST)
          .setTaskListType(TaskListType.Activity)
          .setIncludeTaskListStatus(true);
  public static final ListArchivedWorkflowExecutionsRequest
      LIST_ARCHIVED_WORKFLOW_EXECUTIONS_REQUEST =
          new ListArchivedWorkflowExecutionsRequest()
              .setDomain("domain")
              .setPageSize(1)
              .setNextPageToken(utf8Bytes("pageToken"))
              .setQuery("query");
  public static final RequestCancelWorkflowExecutionRequest
      REQUEST_CANCEL_WORKFLOW_EXECUTION_REQUEST =
          new RequestCancelWorkflowExecutionRequest()
              .setDomain("domain")
              .setWorkflowExecution(WORKFLOW_EXECUTION)
              .setRequestId("requestId")
              .setIdentity("identity");
  public static final ResetStickyTaskListRequest RESET_STICKY_TASK_LIST_REQUEST =
      new ResetStickyTaskListRequest().setDomain("domain").setExecution(WORKFLOW_EXECUTION);
  public static final ResetWorkflowExecutionRequest RESET_WORKFLOW_EXECUTION_REQUEST =
      new ResetWorkflowExecutionRequest()
          .setDomain("domain")
          .setWorkflowExecution(WORKFLOW_EXECUTION)
          .setReason("reason")
          .setDecisionFinishEventId(1)
          .setRequestId("requestId")
          .setSkipSignalReapply(true);
  public static final RespondActivityTaskCanceledByIDRequest
      RESPOND_ACTIVITY_TASK_CANCELED_BY_ID_REQUEST =
          new RespondActivityTaskCanceledByIDRequest()
              .setDomain("domain")
              .setWorkflowID(WORKFLOW_ID)
              .setRunID(RUN_ID)
              .setActivityID("activityId")
              .setDetails(utf8("details"))
              .setIdentity("identity");
  public static final RespondActivityTaskCanceledRequest RESPOND_ACTIVITY_TASK_CANCELED_REQUEST =
      new com.uber.cadence.RespondActivityTaskCanceledRequest()
          .setTaskToken(utf8("taskToken"))
          .setDetails(utf8("details"))
          .setIdentity("identity");
  public static final RespondActivityTaskCompletedByIDRequest
      RESPOND_ACTIVITY_TASK_COMPLETED_BY_ID_REQUEST =
          new RespondActivityTaskCompletedByIDRequest()
              .setDomain("domain")
              .setWorkflowID(WORKFLOW_ID)
              .setRunID(RUN_ID)
              .setActivityID("activityId")
              .setResult(utf8("result"))
              .setIdentity("identity");
  public static final RespondActivityTaskCompletedRequest RESPOND_ACTIVITY_TASK_COMPLETED_REQUEST =
      new RespondActivityTaskCompletedRequest()
          .setTaskToken(utf8("taskToken"))
          .setIdentity("identity")
          .setResult(utf8("result"));
  public static final RespondActivityTaskFailedByIDRequest
      RESPOND_ACTIVITY_TASK_FAILED_BY_ID_REQUEST =
          new RespondActivityTaskFailedByIDRequest()
              .setDomain("domain")
              .setWorkflowID(WORKFLOW_ID)
              .setRunID(RUN_ID)
              .setActivityID("activityId")
              .setReason("reason")
              .setDetails(utf8("details"))
              .setIdentity("identity");
  public static final RespondActivityTaskFailedRequest RESPOND_ACTIVITY_TASK_FAILED_REQUEST =
      new RespondActivityTaskFailedRequest()
          .setTaskToken(utf8("taskToken"))
          .setDetails(utf8("details"))
          .setReason("reason")
          .setIdentity("identity");
  public static final RespondDecisionTaskCompletedRequest RESPOND_DECISION_TASK_COMPLETED_REQUEST =
      new RespondDecisionTaskCompletedRequest()
          .setDecisions(ImmutableList.of(DECISION_COMPLETE_WORKFLOW_EXECUTION))
          .setStickyAttributes(STICKY_EXECUTION_ATTRIBUTES)
          .setReturnNewDecisionTask(true)
          .setForceCreateNewDecisionTask(false)
          .setQueryResults(ImmutableMap.of("query", WORKFLOW_QUERY_RESULT))
          .setExecutionContext(utf8("executionContext"))
          .setBinaryChecksum("binaryChecksum")
          .setTaskToken(utf8("taskToken"))
          .setIdentity("identity");
  public static final RespondDecisionTaskFailedRequest RESPOND_DECISION_TASK_FAILED_REQUEST =
      new RespondDecisionTaskFailedRequest()
          .setCause(DecisionTaskFailedCause.BAD_BINARY)
          .setDetails(utf8("details"))
          .setBinaryChecksum("binaryChecksum")
          .setTaskToken(utf8("taskToken"))
          .setIdentity("identity");
  public static final RespondQueryTaskCompletedRequest RESPOND_QUERY_TASK_COMPLETED_REQUEST =
      new RespondQueryTaskCompletedRequest()
          .setCompletedType(QueryTaskCompletedType.COMPLETED)
          .setQueryResult(utf8("queryResult"))
          .setErrorMessage("errorMessage")
          .setWorkerVersionInfo(
              new WorkerVersionInfo().setFeatureVersion("featureVersion").setImpl("impl"))
          .setTaskToken(utf8("taskToken"));

  public static final ListWorkflowExecutionsRequest LIST_WORKFLOW_EXECUTIONS_REQUEST =
      new ListWorkflowExecutionsRequest()
          .setDomain("domain")
          .setPageSize(1)
          .setNextPageToken(utf8("nextPageToken"))
          .setQuery("query");

  public static final DescribeWorkflowExecutionRequest DESCRIBE_WORKFLOW_EXECUTION_REQUEST =
      new DescribeWorkflowExecutionRequest().setDomain("domain").setExecution(WORKFLOW_EXECUTION);

  public static final GetWorkflowExecutionHistoryRequest GET_WORKFLOW_EXECUTION_HISTORY_REQUEST =
      new GetWorkflowExecutionHistoryRequest()
          .setDomain("domain")
          .setExecution(WORKFLOW_EXECUTION)
          .setMaximumPageSize(1)
          .setWaitForNewEvent(true)
          .setHistoryEventFilterType(HistoryEventFilterType.CLOSE_EVENT)
          .setSkipArchival(true)
          .setNextPageToken(utf8("nextPageToken"));

  public static final com.uber.cadence.StartWorkflowExecutionRequest START_WORKFLOW_EXECUTION =
      new com.uber.cadence.StartWorkflowExecutionRequest()
          .setDomain("domain")
          .setWorkflowId(WORKFLOW_ID)
          .setWorkflowType(WORKFLOW_TYPE)
          .setTaskList(TASK_LIST)
          .setInput("input".getBytes(StandardCharsets.UTF_8))
          .setExecutionStartToCloseTimeoutSeconds(1)
          .setTaskStartToCloseTimeoutSeconds(2)
          .setIdentity("identity")
          .setRequestId("requestId")
          .setWorkflowIdReusePolicy(com.uber.cadence.WorkflowIdReusePolicy.AllowDuplicate)
          .setRetryPolicy(RETRY_POLICY)
          .setCronSchedule("cronSchedule")
          .setMemo(MEMO)
          .setSearchAttributes(SEARCH_ATTRIBUTES)
          .setHeader(HEADER)
          .setDelayStartSeconds(3);
  public static final com.uber.cadence.SignalWithStartWorkflowExecutionRequest
      SIGNAL_WITH_START_WORKFLOW_EXECUTION =
          new SignalWithStartWorkflowExecutionRequest()
              .setDomain("domain")
              .setWorkflowId(WORKFLOW_ID)
              .setWorkflowType(WORKFLOW_TYPE)
              .setTaskList(TASK_LIST)
              .setInput("input".getBytes(StandardCharsets.UTF_8))
              .setExecutionStartToCloseTimeoutSeconds(1)
              .setTaskStartToCloseTimeoutSeconds(2)
              .setIdentity("identity")
              .setRequestId("requestId")
              .setWorkflowIdReusePolicy(com.uber.cadence.WorkflowIdReusePolicy.AllowDuplicate)
              .setSignalName("signalName")
              .setSignalInput("signalInput".getBytes(StandardCharsets.UTF_8))
              .setControl("control".getBytes(StandardCharsets.UTF_8))
              .setRetryPolicy(RETRY_POLICY)
              .setCronSchedule("cronSchedule")
              .setMemo(MEMO)
              .setSearchAttributes(SEARCH_ATTRIBUTES)
              .setHeader(HEADER)
              .setDelayStartSeconds(3);

  public static final StartWorkflowExecutionAsyncRequest START_WORKFLOW_EXECUTION_ASYNC_REQUEST =
      new StartWorkflowExecutionAsyncRequest().setRequest(START_WORKFLOW_EXECUTION);

  public static final SignalWithStartWorkflowExecutionAsyncRequest
      SIGNAL_WITH_START_WORKFLOW_EXECUTION_ASYNC_REQUEST =
          new SignalWithStartWorkflowExecutionAsyncRequest()
              .setRequest(SIGNAL_WITH_START_WORKFLOW_EXECUTION);

  public static final SignalWorkflowExecutionRequest SIGNAL_WORKFLOW_EXECUTION_REQUEST =
      new SignalWorkflowExecutionRequest()
          .setDomain("domain")
          .setWorkflowExecution(WORKFLOW_EXECUTION)
          .setSignalName("signalName")
          .setInput(utf8("input"))
          .setRequestId("requestId")
          .setControl(utf8("control"))
          .setIdentity("identity");

  public static final TerminateWorkflowExecutionRequest TERMINATE_WORKFLOW_EXECUTION_REQUEST =
      new TerminateWorkflowExecutionRequest()
          .setDomain("domain")
          .setWorkflowExecution(WORKFLOW_EXECUTION)
          .setReason("reason")
          .setDetails(utf8("details"))
          .setIdentity("identity");

  public static final DeprecateDomainRequest DEPRECATE_DOMAIN_REQUEST =
      new DeprecateDomainRequest().setName("domain").setSecurityToken("securityToken");

  public static final DescribeDomainRequest DESCRIBE_DOMAIN_BY_ID_REQUEST =
      new DescribeDomainRequest().setUuid("uuid");

  public static final DescribeDomainRequest DESCRIBE_DOMAIN_BY_NAME_REQUEST =
      new DescribeDomainRequest().setName("name");

  public static final ListDomainsRequest LIST_DOMAINS_REQUEST =
      new ListDomainsRequest().setPageSize(1).setNextPageToken(utf8("nextPageToken"));

  public static final ListTaskListPartitionsRequest LIST_TASK_LIST_PARTITIONS_REQUEST =
      new ListTaskListPartitionsRequest().setDomain("domain").setTaskList(TASK_LIST);

  public static final PollForActivityTaskRequest POLL_FOR_ACTIVITY_TASK_REQUEST =
      new PollForActivityTaskRequest()
          .setDomain("domain")
          .setTaskList(TASK_LIST)
          .setTaskListMetadata(TASK_LIST_METADATA)
          .setIdentity("identity");
  public static final PollForDecisionTaskRequest POLL_FOR_DECISION_TASK_REQUEST =
      new PollForDecisionTaskRequest()
          .setDomain("domain")
          .setTaskList(TASK_LIST)
          .setBinaryChecksum("binaryChecksum")
          .setIdentity("identity");
  public static final QueryWorkflowRequest QUERY_WORKFLOW_REQUEST =
      new QueryWorkflowRequest()
          .setDomain("domain")
          .setExecution(WORKFLOW_EXECUTION)
          .setQuery(WORKFLOW_QUERY)
          .setQueryRejectCondition(QueryRejectCondition.NOT_COMPLETED_CLEANLY)
          .setQueryConsistencyLevel(QueryConsistencyLevel.STRONG);

  public static final RecordActivityTaskHeartbeatByIDRequest
      RECORD_ACTIVITY_TASK_HEARTBEAT_BY_ID_REQUEST =
          new RecordActivityTaskHeartbeatByIDRequest()
              .setDomain("domain")
              .setWorkflowID(WORKFLOW_ID)
              .setRunID(RUN_ID)
              .setActivityID("activityId")
              .setDetails(utf8("details"))
              .setIdentity("identity");

  public static final RecordActivityTaskHeartbeatRequest RECORD_ACTIVITY_TASK_HEARTBEAT_REQUEST =
      new RecordActivityTaskHeartbeatRequest()
          .setDetails(utf8("details"))
          .setTaskToken(utf8("taskToken"))
          .setIdentity("identity");

  public static final RegisterDomainRequest REGISTER_DOMAIN_REQUEST =
      new RegisterDomainRequest()
          .setName("domain")
          .setDescription("description")
          .setOwnerEmail("ownerEmail")
          .setWorkflowExecutionRetentionPeriodInDays(1)
          .setClusters(ImmutableList.of(CLUSTER_REPLICATION_CONFIGURATION))
          .setActiveClusterName("activeCluster")
          .setData(DATA)
          .setSecurityToken("securityToken")
          .setIsGlobalDomain(true)
          .setHistoryArchivalStatus(ArchivalStatus.ENABLED)
          .setHistoryArchivalURI("historyArchivalUri")
          .setVisibilityArchivalStatus(ArchivalStatus.DISABLED)
          .setVisibilityArchivalURI("visibilityArchivalUri");

  public static final UpdateDomainRequest UPDATE_DOMAIN_REQUEST =
      new UpdateDomainRequest()
          .setName("domain")
          .setSecurityToken("securityToken")
          .setUpdatedInfo(
              new UpdateDomainInfo()
                  .setData(DATA)
                  .setDescription("description")
                  .setOwnerEmail("ownerEmail"))
          .setReplicationConfiguration(
              new DomainReplicationConfiguration()
                  .setActiveClusterName("activeCluster")
                  .setClusters(ImmutableList.of(CLUSTER_REPLICATION_CONFIGURATION)))
          .setConfiguration(
              new DomainConfiguration()
                  .setWorkflowExecutionRetentionPeriodInDays(2)
                  .setBadBinaries(
                      new BadBinaries()
                          .setBinaries(
                              ImmutableMap.of(
                                  "badBinaryKey",
                                  new BadBinaryInfo()
                                      .setReason("reason")
                                      .setOperator("operator")
                                      .setCreatedTimeNano(3))))
                  .setHistoryArchivalStatus(ArchivalStatus.ENABLED)
                  .setHistoryArchivalURI("historyArchivalUri")
                  .setVisibilityArchivalStatus(ArchivalStatus.DISABLED)
                  .setVisibilityArchivalURI("visibilityArchivalUri"))
          .setDeleteBadBinary("deleteBadBinary")
          .setFailoverTimeoutInSeconds(1);

  public static final ListClosedWorkflowExecutionsRequest LIST_CLOSED_WORKFLOW_EXECUTIONS_REQUEST =
      new ListClosedWorkflowExecutionsRequest()
          .setDomain("domain")
          .setMaximumPageSize(1)
          .setExecutionFilter(
              new WorkflowExecutionFilter().setWorkflowId(WORKFLOW_ID).setRunId(RUN_ID))
          .setTypeFilter(new WorkflowTypeFilter().setName(WORKFLOW_TYPE.getName()))
          .setStatusFilter(WorkflowExecutionCloseStatus.COMPLETED)
          .setNextPageToken(utf8("nextPageToken"))
          .setStartTimeFilter(new StartTimeFilter().setEarliestTime(2).setLatestTime(3));

  public static final ListOpenWorkflowExecutionsRequest LIST_OPEN_WORKFLOW_EXECUTIONS_REQUEST =
      new ListOpenWorkflowExecutionsRequest()
          .setDomain("domain")
          .setMaximumPageSize(1)
          .setExecutionFilter(
              new WorkflowExecutionFilter().setWorkflowId(WORKFLOW_ID).setRunId(RUN_ID))
          .setTypeFilter(new WorkflowTypeFilter().setName(WORKFLOW_TYPE.getName()))
          .setNextPageToken(utf8("nextPageToken"))
          .setStartTimeFilter(new StartTimeFilter().setEarliestTime(2).setLatestTime(3));

  private ThriftObjects() {}

  public static ByteBuffer utf8(String value) {
    return ByteBuffer.wrap(utf8Bytes(value));
  }

  public static byte[] utf8Bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }
}
