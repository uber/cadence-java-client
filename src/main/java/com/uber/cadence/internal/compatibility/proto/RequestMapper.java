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
package com.uber.cadence.internal.compatibility.proto;

import static com.uber.cadence.internal.compatibility.proto.DecisionMapper.decisionArray;
import static com.uber.cadence.internal.compatibility.proto.EnumMapper.archivalStatus;
import static com.uber.cadence.internal.compatibility.proto.EnumMapper.decisionTaskFailedCause;
import static com.uber.cadence.internal.compatibility.proto.EnumMapper.eventFilterType;
import static com.uber.cadence.internal.compatibility.proto.EnumMapper.queryConsistencyLevel;
import static com.uber.cadence.internal.compatibility.proto.EnumMapper.queryRejectCondition;
import static com.uber.cadence.internal.compatibility.proto.EnumMapper.queryTaskCompletedType;
import static com.uber.cadence.internal.compatibility.proto.EnumMapper.taskListType;
import static com.uber.cadence.internal.compatibility.proto.EnumMapper.workflowIdReusePolicy;
import static com.uber.cadence.internal.compatibility.proto.Helpers.daysToDuration;
import static com.uber.cadence.internal.compatibility.proto.Helpers.newFieldMask;
import static com.uber.cadence.internal.compatibility.proto.Helpers.secondsToDuration;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.badBinaries;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.clusterReplicationConfigurationArray;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.failure;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.header;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.memo;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.payload;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.retryPolicy;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.searchAttributes;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.startTimeFilter;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.statusFilter;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.stickyExecutionAttributes;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.taskList;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.taskListMetadata;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.workerVersionInfo;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.workflowExecution;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.workflowExecutionFilter;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.workflowQuery;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.workflowQueryResultMap;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.workflowType;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.workflowTypeFilter;

import com.google.protobuf.ByteString;
import com.uber.cadence.DomainConfiguration;
import com.uber.cadence.DomainReplicationConfiguration;
import com.uber.cadence.UpdateDomainInfo;
import com.uber.cadence.api.v1.CountWorkflowExecutionsRequest;
import com.uber.cadence.api.v1.DeprecateDomainRequest;
import com.uber.cadence.api.v1.DescribeDomainRequest;
import com.uber.cadence.api.v1.DescribeTaskListRequest;
import com.uber.cadence.api.v1.DescribeWorkflowExecutionRequest;
import com.uber.cadence.api.v1.GetWorkflowExecutionHistoryRequest;
import com.uber.cadence.api.v1.ListArchivedWorkflowExecutionsRequest;
import com.uber.cadence.api.v1.ListClosedWorkflowExecutionsRequest;
import com.uber.cadence.api.v1.ListDomainsRequest;
import com.uber.cadence.api.v1.ListOpenWorkflowExecutionsRequest;
import com.uber.cadence.api.v1.ListTaskListPartitionsRequest;
import com.uber.cadence.api.v1.ListWorkflowExecutionsRequest;
import com.uber.cadence.api.v1.PollForActivityTaskRequest;
import com.uber.cadence.api.v1.PollForDecisionTaskRequest;
import com.uber.cadence.api.v1.QueryWorkflowRequest;
import com.uber.cadence.api.v1.RecordActivityTaskHeartbeatByIDRequest;
import com.uber.cadence.api.v1.RecordActivityTaskHeartbeatRequest;
import com.uber.cadence.api.v1.RefreshWorkflowTasksRequest;
import com.uber.cadence.api.v1.RegisterDomainRequest;
import com.uber.cadence.api.v1.RequestCancelWorkflowExecutionRequest;
import com.uber.cadence.api.v1.ResetStickyTaskListRequest;
import com.uber.cadence.api.v1.ResetWorkflowExecutionRequest;
import com.uber.cadence.api.v1.RespondActivityTaskCanceledByIDRequest;
import com.uber.cadence.api.v1.RespondActivityTaskCanceledRequest;
import com.uber.cadence.api.v1.RespondActivityTaskCompletedByIDRequest;
import com.uber.cadence.api.v1.RespondActivityTaskCompletedRequest;
import com.uber.cadence.api.v1.RespondActivityTaskFailedByIDRequest;
import com.uber.cadence.api.v1.RespondActivityTaskFailedRequest;
import com.uber.cadence.api.v1.RespondDecisionTaskCompletedRequest;
import com.uber.cadence.api.v1.RespondDecisionTaskFailedRequest;
import com.uber.cadence.api.v1.RespondQueryTaskCompletedRequest;
import com.uber.cadence.api.v1.ScanWorkflowExecutionsRequest;
import com.uber.cadence.api.v1.SignalWithStartWorkflowExecutionRequest;
import com.uber.cadence.api.v1.SignalWorkflowExecutionRequest;
import com.uber.cadence.api.v1.StartWorkflowExecutionRequest;
import com.uber.cadence.api.v1.TerminateWorkflowExecutionRequest;
import com.uber.cadence.api.v1.UpdateDomainRequest;
import com.uber.cadence.api.v1.UpdateDomainRequest.Builder;
import com.uber.cadence.api.v1.WorkflowQueryResult;
import java.util.ArrayList;
import java.util.List;

public class RequestMapper {

  private static final String DomainUpdateDescriptionField = "description";
  private static final String DomainUpdateOwnerEmailField = "owner_email";
  private static final String DomainUpdateDataField = "data";
  private static final String DomainUpdateRetentionPeriodField =
      "workflow_execution_retention_period";

  private static final String DomainUpdateBadBinariesField = "bad_binaries";
  private static final String DomainUpdateHistoryArchivalStatusField = "history_archival_status";
  private static final String DomainUpdateHistoryArchivalURIField = "history_archival_uri";
  private static final String DomainUpdateVisibilityArchivalStatusField =
      "visibility_archival_status";
  private static final String DomainUpdateVisibilityArchivalURIField = "visibility_archival_uri";
  private static final String DomainUpdateActiveClusterNameField = "active_cluster_name";
  private static final String DomainUpdateClustersField = "clusters";
  private static final String DomainUpdateDeleteBadBinaryField = "delete_bad_binary";
  private static final String DomainUpdateFailoverTimeoutField = "failover_timeout";

  public static CountWorkflowExecutionsRequest countWorkflowExecutionsRequest(
      com.uber.cadence.CountWorkflowExecutionsRequest t) {
    if (t == null) {
      return null;
    }
    return CountWorkflowExecutionsRequest.newBuilder()
        .setDomain(t.getDomain())
        .setQuery(t.getQuery())
        .build();
  }

  public static DescribeTaskListRequest describeTaskListRequest(
      com.uber.cadence.DescribeTaskListRequest t) {
    if (t == null) {
      return null;
    }
    return DescribeTaskListRequest.newBuilder()
        .setDomain(t.getDomain())
        .setTaskList(taskList(t.getTaskList()))
        .setTaskListType(taskListType(t.getTaskListType()))
        .setIncludeTaskListStatus(t.isIncludeTaskListStatus())
        .build();
  }

  public static ListArchivedWorkflowExecutionsRequest listArchivedWorkflowExecutionsRequest(
      com.uber.cadence.ListArchivedWorkflowExecutionsRequest t) {
    if (t == null) {
      return null;
    }
    return ListArchivedWorkflowExecutionsRequest.newBuilder()
        .setDomain(t.getDomain())
        .setPageSize(t.getPageSize())
        .setNextPageToken(ByteString.copyFrom(t.getNextPageToken()))
        .setQuery(t.getQuery())
        .build();
  }

  public static RequestCancelWorkflowExecutionRequest requestCancelWorkflowExecutionRequest(
      com.uber.cadence.RequestCancelWorkflowExecutionRequest t) {
    if (t == null) {
      return null;
    }
    return RequestCancelWorkflowExecutionRequest.newBuilder()
        .setDomain(t.getDomain())
        .setWorkflowExecution(workflowExecution(t.getWorkflowExecution()))
        .setIdentity(t.getIdentity())
        .setRequestId(t.getRequestId())
        .build();
  }

  public static ResetStickyTaskListRequest resetStickyTaskListRequest(
      com.uber.cadence.ResetStickyTaskListRequest t) {
    if (t == null) {
      return null;
    }
    return ResetStickyTaskListRequest.newBuilder()
        .setDomain(t.getDomain())
        .setWorkflowExecution(workflowExecution(t.getExecution()))
        .build();
  }

  public static ResetWorkflowExecutionRequest resetWorkflowExecutionRequest(
      com.uber.cadence.ResetWorkflowExecutionRequest t) {
    if (t == null) {
      return null;
    }
    return ResetWorkflowExecutionRequest.newBuilder()
        .setDomain(t.getDomain())
        .setWorkflowExecution(workflowExecution(t.getWorkflowExecution()))
        .setReason(t.getReason())
        .setDecisionFinishEventId(t.getDecisionFinishEventId())
        .setRequestId(t.getRequestId())
        .setSkipSignalReapply(t.isSkipSignalReapply())
        .build();
  }

  public static RespondActivityTaskCanceledByIDRequest respondActivityTaskCanceledByIdRequest(
      com.uber.cadence.RespondActivityTaskCanceledByIDRequest t) {
    if (t == null) {
      return null;
    }
    return RespondActivityTaskCanceledByIDRequest.newBuilder()
        .setDomain(t.getDomain())
        .setWorkflowExecution(TypeMapper.workflowRunPair(t.getWorkflowID(), t.getRunID()))
        .setActivityId(t.getActivityID())
        .setDetails(payload(t.getDetails()))
        .setIdentity(t.getIdentity())
        .build();
  }

  public static RespondActivityTaskCanceledRequest respondActivityTaskCanceledRequest(
      com.uber.cadence.RespondActivityTaskCanceledRequest t) {
    if (t == null) {
      return null;
    }
    return RespondActivityTaskCanceledRequest.newBuilder()
        .setTaskToken(ByteString.copyFrom(t.getTaskToken()))
        .setDetails(payload(t.getDetails()))
        .setIdentity(t.getIdentity())
        .build();
  }

  public static RespondActivityTaskCompletedByIDRequest respondActivityTaskCompletedByIdRequest(
      com.uber.cadence.RespondActivityTaskCompletedByIDRequest t) {
    if (t == null) {
      return null;
    }
    return RespondActivityTaskCompletedByIDRequest.newBuilder()
        .setDomain(t.getDomain())
        .setWorkflowExecution(TypeMapper.workflowRunPair(t.getWorkflowID(), t.getRunID()))
        .setActivityId(t.getActivityID())
        .setResult(payload(t.getResult()))
        .setIdentity(t.getIdentity())
        .build();
  }

  public static RespondActivityTaskCompletedRequest respondActivityTaskCompletedRequest(
      com.uber.cadence.RespondActivityTaskCompletedRequest t) {
    if (t == null) {
      return null;
    }
    return RespondActivityTaskCompletedRequest.newBuilder()
        .setTaskToken(ByteString.copyFrom(t.getTaskToken()))
        .setResult(payload(t.getResult()))
        .setIdentity(t.getIdentity())
        .build();
  }

  public static RespondActivityTaskFailedByIDRequest respondActivityTaskFailedByIdRequest(
      com.uber.cadence.RespondActivityTaskFailedByIDRequest t) {
    if (t == null) {
      return null;
    }
    return RespondActivityTaskFailedByIDRequest.newBuilder()
        .setDomain(t.getDomain())
        .setWorkflowExecution(TypeMapper.workflowRunPair(t.getWorkflowID(), t.getRunID()))
        .setActivityId(t.getActivityID())
        .setFailure(failure(t.getReason(), t.getDetails()))
        .setIdentity(t.getIdentity())
        .build();
  }

  public static RespondActivityTaskFailedRequest respondActivityTaskFailedRequest(
      com.uber.cadence.RespondActivityTaskFailedRequest t) {
    if (t == null) {
      return null;
    }
    return RespondActivityTaskFailedRequest.newBuilder()
        .setTaskToken(ByteString.copyFrom(t.getTaskToken()))
        .setFailure(failure(t.getReason(), t.getDetails()))
        .setIdentity(t.getIdentity())
        .build();
  }

  public static RespondDecisionTaskCompletedRequest respondDecisionTaskCompletedRequest(
      com.uber.cadence.RespondDecisionTaskCompletedRequest t) {
    if (t == null) {
      return null;
    }
    return RespondDecisionTaskCompletedRequest.newBuilder()
        .setTaskToken(ByteString.copyFrom(t.getTaskToken()))
        .addAllDecisions(decisionArray(t.getDecisions()))
        .setExecutionContext(ByteString.copyFrom(t.getExecutionContext()))
        .setIdentity(t.getIdentity())
        .setStickyAttributes(stickyExecutionAttributes(t.getStickyAttributes()))
        .setReturnNewDecisionTask(t.isReturnNewDecisionTask())
        .setForceCreateNewDecisionTask(t.isForceCreateNewDecisionTask())
        .setBinaryChecksum(t.getBinaryChecksum())
        .putAllQueryResults(workflowQueryResultMap(t.getQueryResults()))
        .build();
  }

  public static RespondDecisionTaskFailedRequest respondDecisionTaskFailedRequest(
      com.uber.cadence.RespondDecisionTaskFailedRequest t) {
    if (t == null) {
      return null;
    }
    return RespondDecisionTaskFailedRequest.newBuilder()
        .setTaskToken(ByteString.copyFrom(t.getTaskToken()))
        .setCause(decisionTaskFailedCause(t.getCause()))
        .setDetails(payload(t.getDetails()))
        .setIdentity(t.getIdentity())
        .setBinaryChecksum(t.getBinaryChecksum())
        .build();
  }

  public static RespondQueryTaskCompletedRequest respondQueryTaskCompletedRequest(
      com.uber.cadence.RespondQueryTaskCompletedRequest t) {
    if (t == null) {
      return null;
    }
    return RespondQueryTaskCompletedRequest.newBuilder()
        .setTaskToken(ByteString.copyFrom(t.getTaskToken()))
        .setResult(
            WorkflowQueryResult.newBuilder()
                .setResultType(queryTaskCompletedType(t.getCompletedType()))
                .setAnswer(payload(t.getQueryResult()))
                .setErrorMessage(t.getErrorMessage())
                .build())
        .setWorkerVersionInfo(workerVersionInfo(t.getWorkerVersionInfo()))
        .build();
  }

  public static ScanWorkflowExecutionsRequest scanWorkflowExecutionsRequest(
      com.uber.cadence.ListWorkflowExecutionsRequest t) {
    if (t == null) {
      return null;
    }
    return ScanWorkflowExecutionsRequest.newBuilder()
        .setDomain(t.getDomain())
        .setPageSize(t.getPageSize())
        .setNextPageToken(ByteString.copyFrom(t.getNextPageToken()))
        .setQuery(t.getQuery())
        .build();
  }

  public static DescribeWorkflowExecutionRequest describeWorkflowExecutionRequest(
      com.uber.cadence.DescribeWorkflowExecutionRequest t) {
    if (t == null) {
      return null;
    }
    return DescribeWorkflowExecutionRequest.newBuilder()
        .setDomain(t.getDomain())
        .setWorkflowExecution(workflowExecution(t.getExecution()))
        .build();
  }

  public static GetWorkflowExecutionHistoryRequest getWorkflowExecutionHistoryRequest(
      com.uber.cadence.GetWorkflowExecutionHistoryRequest t) {
    if (t == null) {
      return null;
    }
    return GetWorkflowExecutionHistoryRequest.newBuilder()
        .setDomain(t.getDomain())
        .setWorkflowExecution(workflowExecution(t.getExecution()))
        .setPageSize(t.getMaximumPageSize())
        .setNextPageToken(ByteString.copyFrom(t.getNextPageToken()))
        .setWaitForNewEvent(t.isWaitForNewEvent())
        .setHistoryEventFilterType(eventFilterType(t.HistoryEventFilterType))
        .setSkipArchival(t.isSkipArchival())
        .build();
  }

  public static SignalWithStartWorkflowExecutionRequest signalWithStartWorkflowExecutionRequest(
      com.uber.cadence.SignalWithStartWorkflowExecutionRequest t) {
    if (t == null) {
      return null;
    }
    return SignalWithStartWorkflowExecutionRequest.newBuilder()
        .setStartRequest(
            StartWorkflowExecutionRequest.newBuilder()
                .setDomain(t.getDomain())
                .setWorkflowId(t.getWorkflowId())
                .setWorkflowType(workflowType(t.getWorkflowType()))
                .setTaskList(taskList(t.getTaskList()))
                .setInput(payload(t.getInput()))
                .setExecutionStartToCloseTimeout(
                    secondsToDuration(t.getExecutionStartToCloseTimeoutSeconds()))
                .setTaskStartToCloseTimeout(
                    secondsToDuration(t.getTaskStartToCloseTimeoutSeconds()))
                .setIdentity(t.getIdentity())
                .setRequestId(t.getRequestId())
                .setWorkflowIdReusePolicy(workflowIdReusePolicy(t.getWorkflowIdReusePolicy()))
                .setRetryPolicy(retryPolicy(t.getRetryPolicy()))
                .setCronSchedule(t.getCronSchedule())
                .setMemo(memo(t.getMemo()))
                .setSearchAttributes(searchAttributes(t.getSearchAttributes()))
                .setHeader(header(t.getHeader()))
                .setDelayStart(secondsToDuration(t.getDelayStartSeconds()))
                .build())
        .setSignalName(t.getSignalName())
        .setSignalInput(payload(t.getSignalInput()))
        .setControl(ByteString.copyFrom(t.getControl()))
        .build();
  }

  public static SignalWorkflowExecutionRequest signalWorkflowExecutionRequest(
      com.uber.cadence.SignalWorkflowExecutionRequest t) {
    if (t == null) {
      return null;
    }
    return SignalWorkflowExecutionRequest.newBuilder()
        .setDomain(t.getDomain())
        .setWorkflowExecution(workflowExecution(t.getWorkflowExecution()))
        .setSignalName(t.getSignalName())
        .setSignalInput(payload(t.getInput()))
        .setIdentity(t.getIdentity())
        .setRequestId(t.getRequestId())
        .setControl(ByteString.copyFrom(t.getControl()))
        .build();
  }

  public static StartWorkflowExecutionRequest startWorkflowExecutionRequest(
      com.uber.cadence.StartWorkflowExecutionRequest t) {
    if (t == null) {
      return null;
    }
    return StartWorkflowExecutionRequest.newBuilder()
        .setDomain(t.getDomain())
        .setWorkflowId(t.getWorkflowId())
        .setWorkflowType(workflowType(t.getWorkflowType()))
        .setTaskList(taskList(t.getTaskList()))
        .setInput(payload(t.getInput()))
        .setExecutionStartToCloseTimeout(
            secondsToDuration(t.getExecutionStartToCloseTimeoutSeconds()))
        .setTaskStartToCloseTimeout(secondsToDuration(t.getTaskStartToCloseTimeoutSeconds()))
        .setIdentity(t.getIdentity())
        .setRequestId(t.getRequestId())
        .setWorkflowIdReusePolicy(workflowIdReusePolicy(t.getWorkflowIdReusePolicy()))
        .setRetryPolicy(retryPolicy(t.getRetryPolicy()))
        .setCronSchedule(t.getCronSchedule())
        .setMemo(memo(t.getMemo()))
        .setSearchAttributes(searchAttributes(t.getSearchAttributes()))
        .setHeader(header(t.getHeader()))
        .setDelayStart(secondsToDuration(t.getDelayStartSeconds()))
        .build();
  }

  public static TerminateWorkflowExecutionRequest terminateWorkflowExecutionRequest(
      com.uber.cadence.TerminateWorkflowExecutionRequest t) {
    if (t == null) {
      return null;
    }
    return TerminateWorkflowExecutionRequest.newBuilder()
        .setDomain(t.getDomain())
        .setWorkflowExecution(workflowExecution(t.getWorkflowExecution()))
        .setReason(t.getReason())
        .setDetails(payload(t.getDetails()))
        .setIdentity(t.getIdentity())
        .build();
  }

  public static DeprecateDomainRequest deprecateDomainRequest(
      com.uber.cadence.DeprecateDomainRequest t) {
    if (t == null) {
      return null;
    }
    return DeprecateDomainRequest.newBuilder()
        .setName(t.getName())
        .setSecurityToken(t.getSecurityToken())
        .build();
  }

  public static DescribeDomainRequest describeDomainRequest(
      com.uber.cadence.DescribeDomainRequest t) {
    if (t == null) {
      return null;
    }
    if (t.uuid != null) {
      return DescribeDomainRequest.newBuilder().setId(t.uuid).build();
    }
    if (t.name != null) {
      return DescribeDomainRequest.newBuilder().setName(t.name).build();
    }
    throw new IllegalArgumentException("neither one of field is set for DescribeDomainRequest");
  }

  public static ListDomainsRequest listDomainsRequest(com.uber.cadence.ListDomainsRequest t) {
    if (t == null) {
      return null;
    }
    return ListDomainsRequest.newBuilder()
        .setPageSize(t.pageSize)
        .setNextPageToken(ByteString.copyFrom(t.getNextPageToken()))
        .build();
  }

  public static ListTaskListPartitionsRequest listTaskListPartitionsRequest(
      com.uber.cadence.ListTaskListPartitionsRequest t) {
    if (t == null) {
      return null;
    }
    return ListTaskListPartitionsRequest.newBuilder()
        .setDomain(t.getDomain())
        .setTaskList(taskList(t.getTaskList()))
        .build();
  }

  public static ListWorkflowExecutionsRequest listWorkflowExecutionsRequest(
      com.uber.cadence.ListWorkflowExecutionsRequest t) {
    if (t == null) {
      return null;
    }
    return ListWorkflowExecutionsRequest.newBuilder()
        .setDomain(t.getDomain())
        .setPageSize(t.getPageSize())
        .setNextPageToken(ByteString.copyFrom(t.getNextPageToken()))
        .setQuery(t.getQuery())
        .build();
  }

  public static PollForActivityTaskRequest pollForActivityTaskRequest(
      com.uber.cadence.PollForActivityTaskRequest t) {
    if (t == null) {
      return null;
    }
    return PollForActivityTaskRequest.newBuilder()
        .setDomain(t.getDomain())
        .setTaskList(taskList(t.getTaskList()))
        .setIdentity(t.getIdentity())
        .setTaskListMetadata(taskListMetadata(t.getTaskListMetadata()))
        .build();
  }

  public static PollForDecisionTaskRequest pollForDecisionTaskRequest(
      com.uber.cadence.PollForDecisionTaskRequest t) {
    if (t == null) {
      return null;
    }
    return PollForDecisionTaskRequest.newBuilder()
        .setDomain(t.getDomain())
        .setTaskList(taskList(t.getTaskList()))
        .setIdentity(t.getIdentity())
        .setBinaryChecksum(t.getBinaryChecksum())
        .build();
  }

  public static QueryWorkflowRequest queryWorkflowRequest(com.uber.cadence.QueryWorkflowRequest t) {
    if (t == null) {
      return null;
    }
    return QueryWorkflowRequest.newBuilder()
        .setDomain(t.getDomain())
        .setWorkflowExecution(workflowExecution(t.getExecution()))
        .setQuery(workflowQuery(t.getQuery()))
        .setQueryRejectCondition(queryRejectCondition(t.getQueryRejectCondition()))
        .setQueryConsistencyLevel(queryConsistencyLevel(t.getQueryConsistencyLevel()))
        .build();
  }

  public static RecordActivityTaskHeartbeatByIDRequest recordActivityTaskHeartbeatByIdRequest(
      com.uber.cadence.RecordActivityTaskHeartbeatByIDRequest t) {
    if (t == null) {
      return null;
    }
    return RecordActivityTaskHeartbeatByIDRequest.newBuilder()
        .setDomain(t.getDomain())
        .setWorkflowExecution(TypeMapper.workflowRunPair(t.getWorkflowID(), t.getRunID()))
        .setActivityId(t.getActivityID())
        .setDetails(payload(t.getDetails()))
        .setIdentity(t.getIdentity())
        .build();
  }

  public static RecordActivityTaskHeartbeatRequest recordActivityTaskHeartbeatRequest(
      com.uber.cadence.RecordActivityTaskHeartbeatRequest t) {
    if (t == null) {
      return null;
    }
    return RecordActivityTaskHeartbeatRequest.newBuilder()
        .setTaskToken(ByteString.copyFrom(t.getTaskToken()))
        .setDetails(payload(t.getDetails()))
        .setIdentity(t.getIdentity())
        .build();
  }

  public static RegisterDomainRequest registerDomainRequest(
      com.uber.cadence.RegisterDomainRequest t) {
    if (t == null) {
      return null;
    }
    RegisterDomainRequest request =
        RegisterDomainRequest.newBuilder()
            .setDescription(t.getDescription())
            .setOwnerEmail(t.getOwnerEmail())
            .setWorkflowExecutionRetentionPeriod(
                daysToDuration(t.getWorkflowExecutionRetentionPeriodInDays()))
            .addAllClusters(clusterReplicationConfigurationArray(t.getClusters()))
            .setActiveClusterName(t.getActiveClusterName())
            .putAllData(t.getData())
            .setSecurityToken(t.getSecurityToken())
            .setIsGlobalDomain(t.isIsGlobalDomain())
            .setHistoryArchivalStatus(archivalStatus(t.getHistoryArchivalStatus()))
            .setHistoryArchivalUri(t.getHistoryArchivalURI())
            .setVisibilityArchivalStatus(archivalStatus(t.getVisibilityArchivalStatus()))
            .setVisibilityArchivalUri(t.getVisibilityArchivalURI())
            .build();
    return request;
  }

  public static UpdateDomainRequest updateDomainRequest(com.uber.cadence.UpdateDomainRequest t) {
    if (t == null) {
      return null;
    }
    Builder request =
        UpdateDomainRequest.newBuilder()
            .setName(t.getName())
            .setSecurityToken(t.getSecurityToken());

    List<String> fields = new ArrayList<>();
    UpdateDomainInfo updatedInfo = t.getUpdatedInfo();
    if (updatedInfo != null) {
      if (updatedInfo.getDescription() != null) {
        request.setDescription(updatedInfo.getDescription());
        fields.add(DomainUpdateDescriptionField);
      }
      if (updatedInfo.getOwnerEmail() != null) {
        request.setOwnerEmail(updatedInfo.getOwnerEmail());
        fields.add(DomainUpdateOwnerEmailField);
      }
      if (updatedInfo.getData() != null) {
        updatedInfo.setData(updatedInfo.getData());
        fields.add(DomainUpdateDataField);
      }
    }
    DomainConfiguration configuration = t.getConfiguration();
    if (configuration != null) {
      if (configuration.getWorkflowExecutionRetentionPeriodInDays() > 0) {
        request.setWorkflowExecutionRetentionPeriod(
            daysToDuration(configuration.getWorkflowExecutionRetentionPeriodInDays()));
        fields.add(DomainUpdateRetentionPeriodField);
      }
      // if t.EmitMetric != null {} - DEPRECATED
      if (configuration.getBadBinaries() != null) {
        request.setBadBinaries(badBinaries(configuration.getBadBinaries()));
        fields.add(DomainUpdateBadBinariesField);
      }
      if (configuration.getHistoryArchivalStatus() != null) {
        request.setHistoryArchivalStatus(archivalStatus(configuration.getHistoryArchivalStatus()));
        fields.add(DomainUpdateHistoryArchivalStatusField);
      }
      if (configuration.getHistoryArchivalURI() != null) {
        request.setHistoryArchivalUri(configuration.getHistoryArchivalURI());
        fields.add(DomainUpdateHistoryArchivalURIField);
      }
      if (configuration.getVisibilityArchivalStatus() != null) {
        request.setVisibilityArchivalStatus(
            archivalStatus(configuration.getVisibilityArchivalStatus()));
        fields.add(DomainUpdateVisibilityArchivalStatusField);
      }
      if (configuration.getVisibilityArchivalURI() != null) {
        request.setVisibilityArchivalUri(configuration.getVisibilityArchivalURI());
        fields.add(DomainUpdateVisibilityArchivalURIField);
      }
    }
    DomainReplicationConfiguration replicationConfiguration = new DomainReplicationConfiguration();
    if (replicationConfiguration != null) {
      if (replicationConfiguration.getActiveClusterName() != null) {
        request.setActiveClusterName(replicationConfiguration.getActiveClusterName());
        fields.add(DomainUpdateActiveClusterNameField);
      }
      if (replicationConfiguration.getClusters() != null) {
        request.addAllClusters(
            clusterReplicationConfigurationArray(replicationConfiguration.getClusters()));
        fields.add(DomainUpdateClustersField);
      }
    }
    if (t.getDeleteBadBinary() != null) {
      request.setDeleteBadBinary(t.getDeleteBadBinary());
      fields.add(DomainUpdateDeleteBadBinaryField);
    }
    if (t.getFailoverTimeoutInSeconds() > 0) {
      request.setFailoverTimeout(secondsToDuration(t.getFailoverTimeoutInSeconds()));
      fields.add(DomainUpdateFailoverTimeoutField);
    }

    request.setUpdateMask(newFieldMask(fields));

    return request.build();
  }

  public static ListClosedWorkflowExecutionsRequest listClosedWorkflowExecutionsRequest(
      com.uber.cadence.ListClosedWorkflowExecutionsRequest r) {
    if (r == null) {
      return null;
    }
    ListClosedWorkflowExecutionsRequest.Builder request =
        ListClosedWorkflowExecutionsRequest.newBuilder()
            .setDomain(r.getDomain())
            .setPageSize(r.getMaximumPageSize())
            .setNextPageToken(ByteString.copyFrom(r.getNextPageToken()))
            .setStartTimeFilter(startTimeFilter(r.StartTimeFilter));

    if (r.getExecutionFilter() != null) {
      request.setExecutionFilter(workflowExecutionFilter(r.getExecutionFilter()));
    }
    if (r.getTypeFilter() != null) {
      request.setTypeFilter(workflowTypeFilter(r.getTypeFilter()));
    }
    if (r.getStatusFilter() != null) {
      request.setStatusFilter(statusFilter(r.getStatusFilter()));
    }

    return request.build();
  }

  public static ListOpenWorkflowExecutionsRequest listOpenWorkflowExecutionsRequest(
      com.uber.cadence.ListOpenWorkflowExecutionsRequest r) {
    if (r == null) {
      return null;
    }
    ListOpenWorkflowExecutionsRequest.Builder request =
        ListOpenWorkflowExecutionsRequest.newBuilder()
            .setDomain(r.getDomain())
            .setPageSize(r.getMaximumPageSize())
            .setNextPageToken(ByteString.copyFrom(r.getNextPageToken()))
            .setStartTimeFilter(startTimeFilter(r.StartTimeFilter));

    if (r.getExecutionFilter() != null) {
      request.setExecutionFilter(workflowExecutionFilter(r.getExecutionFilter()));
    }
    if (r.getTypeFilter() != null) {
      request.setTypeFilter(workflowTypeFilter(r.getTypeFilter()));
    }

    return request.build();
  }

  public static RefreshWorkflowTasksRequest refreshWorkflowTasksRequest(
      com.uber.cadence.RefreshWorkflowTasksRequest r) {
    if (r == null) {
      return null;
    }
    return RefreshWorkflowTasksRequest.newBuilder()
        .setDomain(r.getDomain())
        .setWorkflowExecution(workflowExecution(r.getExecution()))
        .build();
  }
}
