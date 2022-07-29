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

import static com.uber.cadence.internal.compatibility.proto.EnumMapper.archivalStatus;
import static com.uber.cadence.internal.compatibility.proto.EnumMapper.domainStatus;
import static com.uber.cadence.internal.compatibility.proto.EnumMapper.encodingType;
import static com.uber.cadence.internal.compatibility.proto.EnumMapper.parentClosePolicy;
import static com.uber.cadence.internal.compatibility.proto.EnumMapper.queryResultType;
import static com.uber.cadence.internal.compatibility.proto.EnumMapper.taskListKind;
import static com.uber.cadence.internal.compatibility.proto.EnumMapper.workflowExecutionCloseStatus;
import static com.uber.cadence.internal.compatibility.proto.Helpers.arrayToByteString;
import static com.uber.cadence.internal.compatibility.proto.Helpers.daysToDuration;
import static com.uber.cadence.internal.compatibility.proto.Helpers.fromDoubleValue;
import static com.uber.cadence.internal.compatibility.proto.Helpers.secondsToDuration;
import static com.uber.cadence.internal.compatibility.proto.Helpers.unixNanoToTime;

import com.google.common.base.Strings;
import com.uber.cadence.DomainConfiguration;
import com.uber.cadence.DomainInfo;
import com.uber.cadence.DomainReplicationConfiguration;
import com.uber.cadence.api.v1.ActivityLocalDispatchInfo;
import com.uber.cadence.api.v1.ActivityType;
import com.uber.cadence.api.v1.BadBinaries;
import com.uber.cadence.api.v1.BadBinaryInfo;
import com.uber.cadence.api.v1.ClusterReplicationConfiguration;
import com.uber.cadence.api.v1.DataBlob;
import com.uber.cadence.api.v1.Domain;
import com.uber.cadence.api.v1.Domain.Builder;
import com.uber.cadence.api.v1.ExternalExecutionInfo;
import com.uber.cadence.api.v1.Failure;
import com.uber.cadence.api.v1.Header;
import com.uber.cadence.api.v1.IndexedValueType;
import com.uber.cadence.api.v1.Memo;
import com.uber.cadence.api.v1.ParentExecutionInfo;
import com.uber.cadence.api.v1.Payload;
import com.uber.cadence.api.v1.PendingActivityInfo;
import com.uber.cadence.api.v1.PendingChildExecutionInfo;
import com.uber.cadence.api.v1.PollerInfo;
import com.uber.cadence.api.v1.ResetPointInfo;
import com.uber.cadence.api.v1.ResetPoints;
import com.uber.cadence.api.v1.RetryPolicy;
import com.uber.cadence.api.v1.SearchAttributes;
import com.uber.cadence.api.v1.StartTimeFilter;
import com.uber.cadence.api.v1.StatusFilter;
import com.uber.cadence.api.v1.StickyExecutionAttributes;
import com.uber.cadence.api.v1.SupportedClientVersions;
import com.uber.cadence.api.v1.TaskIDBlock;
import com.uber.cadence.api.v1.TaskList;
import com.uber.cadence.api.v1.TaskListMetadata;
import com.uber.cadence.api.v1.TaskListPartitionMetadata;
import com.uber.cadence.api.v1.TaskListStatus;
import com.uber.cadence.api.v1.WorkerVersionInfo;
import com.uber.cadence.api.v1.WorkflowExecution;
import com.uber.cadence.api.v1.WorkflowExecutionConfiguration;
import com.uber.cadence.api.v1.WorkflowExecutionFilter;
import com.uber.cadence.api.v1.WorkflowExecutionInfo;
import com.uber.cadence.api.v1.WorkflowQuery;
import com.uber.cadence.api.v1.WorkflowQueryResult;
import com.uber.cadence.api.v1.WorkflowType;
import com.uber.cadence.api.v1.WorkflowTypeFilter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TypeMapper {

  static BadBinaryInfo badBinaryInfo(com.uber.cadence.BadBinaryInfo t) {
    if (t == null) {
      return null;
    }
    return BadBinaryInfo.newBuilder()
        .setReason(t.getReason())
        .setOperator(t.getOperator())
        .setCreatedTime(unixNanoToTime(t.getCreatedTimeNano()))
        .build();
  }

  static Payload payload(byte[] data) {
    if (data == null) {
      return Payload.newBuilder().build();
    }
    return Payload.newBuilder().setData(arrayToByteString(data)).build();
  }

  static Failure failure(String reason, byte[] details) {
    if (reason == null) {
      return Failure.newBuilder().build();
    }
    return Failure.newBuilder().setReason(reason).setDetails(arrayToByteString(details)).build();
  }

  static WorkflowExecution workflowExecution(com.uber.cadence.WorkflowExecution t) {
    if (t == null) {
      return WorkflowExecution.newBuilder().build();
    }
    if (t.getWorkflowId() == null && t.getRunId() == null) {
      return WorkflowExecution.newBuilder().build();
    }
    WorkflowExecution.Builder builder =
        WorkflowExecution.newBuilder().setWorkflowId(t.getWorkflowId());
    if (t.getRunId() != null) {
      builder.setRunId(t.getRunId());
    }
    return builder.build();
  }

  static WorkflowExecution workflowRunPair(String workflowId, String runId) {
    if (Strings.isNullOrEmpty(workflowId) && Strings.isNullOrEmpty(runId)) {
      return WorkflowExecution.newBuilder().build();
    }
    return WorkflowExecution.newBuilder().setWorkflowId(workflowId).setRunId(runId).build();
  }

  static ActivityType activityType(com.uber.cadence.ActivityType t) {
    if (t == null) {
      return ActivityType.newBuilder().build();
    }
    return ActivityType.newBuilder().setName(t.getName()).build();
  }

  static WorkflowType workflowType(com.uber.cadence.WorkflowType t) {
    if (t == null) {
      return WorkflowType.newBuilder().build();
    }
    return WorkflowType.newBuilder().setName(t.getName()).build();
  }

  static TaskList taskList(com.uber.cadence.TaskList t) {
    if (t == null) {
      return TaskList.newBuilder().build();
    }
    return TaskList.newBuilder().setName(t.getName()).setKind(taskListKind(t.getKind())).build();
  }

  static TaskListMetadata taskListMetadata(com.uber.cadence.TaskListMetadata t) {
    if (t == null) {
      return TaskListMetadata.newBuilder().build();
    }
    return TaskListMetadata.newBuilder()
        .setMaxTasksPerSecond(fromDoubleValue(t.getMaxTasksPerSecond()))
        .build();
  }

  static RetryPolicy retryPolicy(com.uber.cadence.RetryPolicy t) {
    if (t == null) {
      return null;
    }
    RetryPolicy.Builder builder =
        RetryPolicy.newBuilder()
            .setInitialInterval(secondsToDuration(t.getInitialIntervalInSeconds()))
            .setBackoffCoefficient(t.getBackoffCoefficient())
            .setMaximumInterval(secondsToDuration(t.getMaximumIntervalInSeconds()))
            .setMaximumAttempts(t.getMaximumAttempts())
            .setExpirationInterval(secondsToDuration(t.getExpirationIntervalInSeconds()));
    if (t.getNonRetriableErrorReasons() != null) {
      builder.addAllNonRetryableErrorReasons(t.getNonRetriableErrorReasons());
    }
    return builder.build();
  }

  static Header header(com.uber.cadence.Header t) {
    if (t == null) {
      return Header.newBuilder().build();
    }
    return Header.newBuilder().putAllFields(payloadByteBufferMap(t.getFields())).build();
  }

  static Memo memo(com.uber.cadence.Memo t) {
    if (t == null) {
      return Memo.newBuilder().build();
    }
    return Memo.newBuilder().putAllFields(payloadByteBufferMap(t.getFields())).build();
  }

  static SearchAttributes searchAttributes(com.uber.cadence.SearchAttributes t) {
    if (t == null) {
      return SearchAttributes.newBuilder().build();
    }
    return SearchAttributes.newBuilder()
        .putAllIndexedFields(payloadByteBufferMap(t.getIndexedFields()))
        .build();
  }

  static BadBinaries badBinaries(com.uber.cadence.BadBinaries t) {
    if (t == null) {
      return BadBinaries.newBuilder().build();
    }
    return BadBinaries.newBuilder().putAllBinaries(badBinaryInfoMap(t.getBinaries())).build();
  }

  static ClusterReplicationConfiguration clusterReplicationConfiguration(
      com.uber.cadence.ClusterReplicationConfiguration t) {
    if (t == null) {
      return ClusterReplicationConfiguration.newBuilder().build();
    }
    return ClusterReplicationConfiguration.newBuilder().setClusterName(t.getClusterName()).build();
  }

  static WorkflowQuery workflowQuery(com.uber.cadence.WorkflowQuery t) {
    if (t == null) {
      return WorkflowQuery.newBuilder().build();
    }
    return WorkflowQuery.newBuilder()
        .setQueryType(t.getQueryType())
        .setQueryArgs(payload(t.getQueryArgs()))
        .build();
  }

  static WorkflowQueryResult workflowQueryResult(com.uber.cadence.WorkflowQueryResult t) {
    if (t == null) {
      return WorkflowQueryResult.newBuilder().build();
    }
    return WorkflowQueryResult.newBuilder()
        .setResultType(queryResultType(t.getResultType()))
        .setAnswer(payload(t.getAnswer()))
        .setErrorMessage(t.getErrorMessage())
        .build();
  }

  static StickyExecutionAttributes stickyExecutionAttributes(
      com.uber.cadence.StickyExecutionAttributes t) {
    if (t == null) {
      return StickyExecutionAttributes.newBuilder().build();
    }
    return StickyExecutionAttributes.newBuilder()
        .setWorkerTaskList(taskList(t.getWorkerTaskList()))
        .setScheduleToStartTimeout(secondsToDuration(t.getScheduleToStartTimeoutSeconds()))
        .build();
  }

  static WorkerVersionInfo workerVersionInfo(com.uber.cadence.WorkerVersionInfo t) {
    if (t == null) {
      return WorkerVersionInfo.newBuilder().build();
    }
    return WorkerVersionInfo.newBuilder()
        .setImpl(t.getImpl())
        .setFeatureVersion(t.getFeatureVersion())
        .build();
  }

  static StartTimeFilter startTimeFilter(com.uber.cadence.StartTimeFilter t) {
    if (t == null) {
      return StartTimeFilter.newBuilder().build();
    }
    return StartTimeFilter.newBuilder()
        .setEarliestTime(unixNanoToTime(t.getEarliestTime()))
        .setLatestTime(unixNanoToTime(t.getLatestTime()))
        .build();
  }

  static WorkflowExecutionFilter workflowExecutionFilter(
      com.uber.cadence.WorkflowExecutionFilter t) {
    if (t == null) {
      return WorkflowExecutionFilter.newBuilder().build();
    }
    return WorkflowExecutionFilter.newBuilder()
        .setWorkflowId(t.getWorkflowId())
        .setRunId(t.getRunId())
        .build();
  }

  static WorkflowTypeFilter workflowTypeFilter(com.uber.cadence.WorkflowTypeFilter t) {
    if (t == null) {
      return WorkflowTypeFilter.newBuilder().build();
    }
    return WorkflowTypeFilter.newBuilder().setName(t.getName()).build();
  }

  static StatusFilter statusFilter(com.uber.cadence.WorkflowExecutionCloseStatus t) {
    if (t == null) {
      return StatusFilter.newBuilder().build();
    }
    return StatusFilter.newBuilder().setStatus(workflowExecutionCloseStatus(t)).build();
  }

  static Map<String, Payload> payloadMap(Map<String, byte[]> t) {
    if (t == null) {
      return Collections.emptyMap();
    }
    Map<String, Payload> v = new HashMap<>();
    for (String key : t.keySet()) {
      v.put(key, payload(t.get(key)));
    }
    return v;
  }

  static Map<String, Payload> payloadByteBufferMap(Map<String, ByteBuffer> t) {
    if (t == null) {
      return Collections.emptyMap();
    }
    Map<String, Payload> v = new HashMap<>();
    for (String key : t.keySet()) {
      v.put(key, payload(t.get(key).array()));
    }
    return v;
  }

  static Map<String, BadBinaryInfo> badBinaryInfoMap(
      Map<String, com.uber.cadence.BadBinaryInfo> t) {
    if (t == null) {
      return Collections.emptyMap();
    }
    Map<String, BadBinaryInfo> v = new HashMap<>();
    for (String key : t.keySet()) {
      v.put(key, badBinaryInfo(t.get(key)));
    }
    return v;
  }

  static List<ClusterReplicationConfiguration> clusterReplicationConfigurationArray(
      List<com.uber.cadence.ClusterReplicationConfiguration> t) {
    if (t == null) {
      return Collections.emptyList();
    }
    List<ClusterReplicationConfiguration> v = new ArrayList<>();
    for (int i = 0; i < t.size(); i++) {
      v.add(clusterReplicationConfiguration(t.get(i)));
    }
    return v;
  }

  static Map<String, WorkflowQueryResult> workflowQueryResultMap(
      Map<String, com.uber.cadence.WorkflowQueryResult> t) {
    if (t == null) {
      return Collections.emptyMap();
    }
    Map<String, WorkflowQueryResult> v = new HashMap<>();
    for (String key : t.keySet()) {
      v.put(key, workflowQueryResult(t.get(key)));
    }
    return v;
  }

  static DataBlob dataBlob(com.uber.cadence.DataBlob t) {
    if (t == null) {
      return DataBlob.newBuilder().build();
    }
    return DataBlob.newBuilder()
        .setEncodingType(encodingType(t.getEncodingType()))
        .setData(arrayToByteString(t.getData()))
        .build();
  }

  static ExternalExecutionInfo externalExecutionInfo(
      com.uber.cadence.WorkflowExecution we, long initiatedID) {
    if (we == null && initiatedID == 0) {
      return ExternalExecutionInfo.newBuilder().build();
    }
    if (we == null || initiatedID == 0) {
      throw new IllegalArgumentException(
          "either all or none external execution info fields must be set");
    }
    return ExternalExecutionInfo.newBuilder()
        .setWorkflowExecution(workflowExecution(we))
        .setInitiatedId(initiatedID)
        .build();
  }

  static ResetPoints resetPoints(com.uber.cadence.ResetPoints t) {
    if (t == null) {
      return ResetPoints.newBuilder().build();
    }
    return ResetPoints.newBuilder().addAllPoints(resetPointInfoArray(t.getPoints())).build();
  }

  static ResetPointInfo resetPointInfo(com.uber.cadence.ResetPointInfo t) {
    if (t == null) {
      return null;
    }
    return ResetPointInfo.newBuilder()
        .setBinaryChecksum(t.getBinaryChecksum())
        .setRunId(t.getRunId())
        .setFirstDecisionCompletedId(t.getFirstDecisionCompletedId())
        .setCreatedTime(unixNanoToTime(t.getCreatedTimeNano()))
        .setExpiringTime(unixNanoToTime(t.getExpiringTimeNano()))
        .setResettable(t.isResettable())
        .build();
  }

  static PollerInfo pollerInfo(com.uber.cadence.PollerInfo t) {
    if (t == null) {
      return null;
    }
    return PollerInfo.newBuilder()
        .setLastAccessTime(unixNanoToTime(t.getLastAccessTime()))
        .setIdentity(t.getIdentity())
        .setRatePerSecond(t.getRatePerSecond())
        .build();
  }

  static TaskListStatus taskListStatus(com.uber.cadence.TaskListStatus t) {
    if (t == null) {
      return null;
    }
    return TaskListStatus.newBuilder()
        .setBacklogCountHint(t.getBacklogCountHint())
        .setReadLevel(t.getReadLevel())
        .setAckLevel(t.getAckLevel())
        .setRatePerSecond(t.getRatePerSecond())
        .setTaskIdBlock(taskIdBlock(t.getTaskIDBlock()))
        .build();
  }

  static TaskIDBlock taskIdBlock(com.uber.cadence.TaskIDBlock t) {
    if (t == null) {
      return null;
    }
    return TaskIDBlock.newBuilder().setStartId(t.getStartID()).setEndId(t.getEndID()).build();
  }

  static WorkflowExecutionConfiguration workflowExecutionConfiguration(
      com.uber.cadence.WorkflowExecutionConfiguration t) {
    if (t == null) {
      return null;
    }
    return WorkflowExecutionConfiguration.newBuilder()
        .setTaskList(taskList(t.getTaskList()))
        .setExecutionStartToCloseTimeout(
            secondsToDuration(t.getExecutionStartToCloseTimeoutSeconds()))
        .setTaskStartToCloseTimeout(secondsToDuration(t.getTaskStartToCloseTimeoutSeconds()))
        .build();
  }

  static ParentExecutionInfo parentExecutionInfo2(
      String domainID, com.uber.cadence.WorkflowExecution we) {
    if (domainID == null && we == null) {
      return null;
    }

    return ParentExecutionInfo.newBuilder()
        .setDomainId(domainID)
        .setWorkflowExecution(workflowExecution(we))
        .build();
  }

  static WorkflowExecutionInfo workflowExecutionInfo(com.uber.cadence.WorkflowExecutionInfo t) {
    if (t == null) {
      return null;
    }
    WorkflowExecutionInfo.Builder builder =
        WorkflowExecutionInfo.newBuilder()
            .setWorkflowExecution(workflowExecution(t.getExecution()))
            .setType(workflowType(t.getType()))
            .setStartTime(unixNanoToTime(t.getStartTime()))
            .setCloseTime(unixNanoToTime(t.getCloseTime()))
            .setCloseStatus(workflowExecutionCloseStatus(t.getCloseStatus()))
            .setHistoryLength(t.getHistoryLength())
            .setExecutionTime(unixNanoToTime(t.getExecutionTime()))
            .setMemo(memo(t.getMemo()))
            .setSearchAttributes(searchAttributes(t.getSearchAttributes()))
            .setAutoResetPoints(resetPoints(t.getAutoResetPoints()))
            .setTaskList(t.getTaskList())
            .setIsCron(t.isIsCron());
    ParentExecutionInfo parentExecutionInfo =
        parentExecutionInfo2(t.getParentDomainId(), t.getParentExecution());
    if (parentExecutionInfo != null) {
      builder.setParentExecutionInfo(parentExecutionInfo);
    }
    return builder.build();
  }

  static ParentExecutionInfo parentExecutionInfo(
      String domainID, String domainName, com.uber.cadence.WorkflowExecution we, long initiatedID) {
    if (domainID == null && domainName == null && we == null && initiatedID == 0) {
      return null;
    }
    // TODO test initiatedID == 0
    if (domainName == null || we == null || initiatedID == 0) {
      throw new IllegalArgumentException("either all or none parent execution info must be set");
    }

    // Domain ID was added to unify parent execution info in several places.
    // However it may not be present:
    // - on older histories
    // - if conversion involves thrift data types
    // Fallback to empty string in those cases
    String parentDomainID = "";
    if (domainID != null) {
      parentDomainID = domainID;
    }

    return ParentExecutionInfo.newBuilder()
        .setDomainId(parentDomainID)
        .setDomainName(domainName)
        .setWorkflowExecution(workflowExecution(we))
        .setInitiatedId(initiatedID)
        .build();
  }

  static PendingActivityInfo pendingActivityInfo(com.uber.cadence.PendingActivityInfo t) {
    if (t == null) {
      return null;
    }
    return PendingActivityInfo.newBuilder()
        .setActivityId(t.getActivityID())
        .setActivityType(activityType(t.getActivityType()))
        .setState(EnumMapper.pendingActivityState(t.getState()))
        .setHeartbeatDetails(payload(t.getHeartbeatDetails()))
        .setLastHeartbeatTime(unixNanoToTime(t.getLastHeartbeatTimestamp()))
        .setLastStartedTime(unixNanoToTime(t.getLastStartedTimestamp()))
        .setAttempt(t.getAttempt())
        .setMaximumAttempts(t.getMaximumAttempts())
        .setScheduledTime(unixNanoToTime(t.getScheduledTimestamp()))
        .setExpirationTime(unixNanoToTime(t.getExpirationTimestamp()))
        .setLastFailure(failure(t.getLastFailureReason(), t.getLastFailureDetails()))
        .setLastWorkerIdentity(t.getLastWorkerIdentity())
        .build();
  }

  static PendingChildExecutionInfo pendingChildExecutionInfo(
      com.uber.cadence.PendingChildExecutionInfo t) {
    if (t == null) {
      return null;
    }
    return PendingChildExecutionInfo.newBuilder()
        .setWorkflowExecution(workflowRunPair(t.getWorkflowID(), t.getRunID()))
        .setWorkflowTypeName(t.getWorkflowTypName())
        .setInitiatedId(t.getInitiatedID())
        .setParentClosePolicy(parentClosePolicy(t.getParentClosePolicy()))
        .build();
  }

  static ActivityLocalDispatchInfo activityLocalDispatchInfo(
      com.uber.cadence.ActivityLocalDispatchInfo t) {
    if (t == null) {
      return null;
    }
    return ActivityLocalDispatchInfo.newBuilder()
        .setActivityId(t.getActivityId())
        .setScheduledTime(unixNanoToTime(t.getScheduledTimestamp()))
        .setStartedTime(unixNanoToTime(t.getStartedTimestamp()))
        .setScheduledTimeOfThisAttempt(unixNanoToTime(t.getScheduledTimestampOfThisAttempt()))
        .setTaskToken(arrayToByteString(t.getTaskToken()))
        .build();
  }

  static SupportedClientVersions supportedClientVersions(
      com.uber.cadence.SupportedClientVersions t) {
    if (t == null) {
      return SupportedClientVersions.newBuilder().build();
    }
    return SupportedClientVersions.newBuilder()
        .setGoSdk(t.getGoSdk())
        .setJavaSdk(t.getJavaSdk())
        .build();
  }

  static Domain describeDomainResponseDomain(com.uber.cadence.DescribeDomainResponse t) {
    if (t == null) {
      return null;
    }
    Builder domain =
        Domain.newBuilder()
            .setFailoverVersion(t.getFailoverVersion())
            .setIsGlobalDomain(t.isIsGlobalDomain());

    DomainInfo info = t.getDomainInfo();
    if (info != null) {
      domain.setId(info.getUuid());
      domain.setName(info.getName());
      domain.setStatus(domainStatus(info.getStatus()));
      domain.setDescription(info.getDescription());
      domain.setOwnerEmail(info.getOwnerEmail());
      domain.putAllData(info.getData());
    }
    DomainConfiguration config = t.getConfiguration();
    if (config != null) {
      domain.setWorkflowExecutionRetentionPeriod(
          daysToDuration(config.getWorkflowExecutionRetentionPeriodInDays()));
      domain.setBadBinaries(badBinaries(config.getBadBinaries()));
      domain.setHistoryArchivalStatus(archivalStatus(config.getHistoryArchivalStatus()));
      domain.setHistoryArchivalUri(config.getHistoryArchivalURI());
      domain.setVisibilityArchivalStatus(archivalStatus(config.getVisibilityArchivalStatus()));
      domain.setVisibilityArchivalUri(config.getVisibilityArchivalURI());
    }
    DomainReplicationConfiguration repl = t.getReplicationConfiguration();
    if (repl != null) {
      domain.setActiveClusterName(repl.getActiveClusterName());
      domain.addAllClusters(clusterReplicationConfigurationArray(repl.getClusters()));
    }
    return domain.build();
  }

  static TaskListPartitionMetadata taskListPartitionMetadata(
      com.uber.cadence.TaskListPartitionMetadata t) {
    if (t == null) {
      return null;
    }
    return TaskListPartitionMetadata.newBuilder()
        .setKey(t.getKey())
        .setOwnerHostName(t.getOwnerHostName())
        .build();
  }

  static List<PollerInfo> pollerInfoArray(List<com.uber.cadence.PollerInfo> t) {
    if (t == null) {
      return Collections.emptyList();
    }
    List<PollerInfo> v = new ArrayList<>();
    for (int i = 0; i < t.size(); i++) {
      v.add(pollerInfo(t.get(i)));
    }
    return v;
  }

  static List<ResetPointInfo> resetPointInfoArray(List<com.uber.cadence.ResetPointInfo> t) {
    if (t == null) {
      return Collections.emptyList();
    }
    List<ResetPointInfo> v = new ArrayList<>();
    for (int i = 0; i < t.size(); i++) {
      v.add(resetPointInfo(t.get(i)));
    }
    return v;
  }

  static List<PendingChildExecutionInfo> pendingChildExecutionInfoArray(
      List<com.uber.cadence.PendingChildExecutionInfo> t) {
    if (t == null) {
      return Collections.emptyList();
    }
    List<PendingChildExecutionInfo> v = new ArrayList<>();
    for (int i = 0; i < t.size(); i++) {
      v.add(pendingChildExecutionInfo(t.get(i)));
    }
    return v;
  }

  static Map<String, IndexedValueType> indexedValueTypeMap(
      Map<String, com.uber.cadence.IndexedValueType> t) {
    if (t == null) {
      return Collections.emptyMap();
    }
    Map<String, IndexedValueType> v = new HashMap<>();
    for (String key : t.keySet()) {
      v.put(key, EnumMapper.indexedValueType(t.get(key)));
    }
    return v;
  }

  static List<WorkflowExecutionInfo> workflowExecutionInfoArray(
      List<com.uber.cadence.WorkflowExecutionInfo> t) {
    if (t == null) {
      return Collections.emptyList();
    }
    List<WorkflowExecutionInfo> v = new ArrayList<>();
    for (int i = 0; i < t.size(); i++) {
      v.add(workflowExecutionInfo(t.get(i)));
    }
    return v;
  }

  static List<Domain> describeDomainResponseArray(List<com.uber.cadence.DescribeDomainResponse> t) {
    if (t == null) {
      return Collections.emptyList();
    }
    List<Domain> v = new ArrayList<>();
    for (int i = 0; i < t.size(); i++) {
      v.add(describeDomainResponseDomain(t.get(i)));
    }
    return v;
  }
}
