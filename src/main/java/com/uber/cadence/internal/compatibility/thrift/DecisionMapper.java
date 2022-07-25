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
package com.uber.cadence.internal.compatibility.thrift;

import static com.uber.cadence.internal.compatibility.thrift.EnumMapper.continueAsNewInitiator;
import static com.uber.cadence.internal.compatibility.thrift.EnumMapper.parentClosePolicy;
import static com.uber.cadence.internal.compatibility.thrift.EnumMapper.workflowIdReusePolicy;
import static com.uber.cadence.internal.compatibility.thrift.Helpers.byteStringToArray;
import static com.uber.cadence.internal.compatibility.thrift.Helpers.durationToSeconds;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.activityType;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.failureDetails;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.failureReason;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.header;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.memo;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.payload;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.retryPolicy;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.runId;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.searchAttributes;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.taskList;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.workflowExecution;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.workflowId;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.workflowType;

import com.uber.cadence.CancelTimerDecisionAttributes;
import com.uber.cadence.CancelWorkflowExecutionDecisionAttributes;
import com.uber.cadence.CompleteWorkflowExecutionDecisionAttributes;
import com.uber.cadence.ContinueAsNewWorkflowExecutionDecisionAttributes;
import com.uber.cadence.Decision;
import com.uber.cadence.DecisionType;
import com.uber.cadence.FailWorkflowExecutionDecisionAttributes;
import com.uber.cadence.RecordMarkerDecisionAttributes;
import com.uber.cadence.RequestCancelActivityTaskDecisionAttributes;
import com.uber.cadence.RequestCancelExternalWorkflowExecutionDecisionAttributes;
import com.uber.cadence.ScheduleActivityTaskDecisionAttributes;
import com.uber.cadence.SignalExternalWorkflowExecutionDecisionAttributes;
import com.uber.cadence.StartChildWorkflowExecutionDecisionAttributes;
import com.uber.cadence.StartTimerDecisionAttributes;
import com.uber.cadence.UpsertWorkflowSearchAttributesDecisionAttributes;
import java.util.ArrayList;
import java.util.List;

class DecisionMapper {

  static List<Decision> decisionArray(List<com.uber.cadence.api.v1.Decision> t) {
    if (t == null || t.size() == 0) {
      return null;
    }
    List<Decision> v = new ArrayList<>();
    for (int i = 0; i < t.size(); i++) {
      v.add(decision(t.get(i)));
    }
    return v;
  }

  static Decision decision(com.uber.cadence.api.v1.Decision d) {
    if (d == null || d == com.uber.cadence.api.v1.Decision.getDefaultInstance()) {
      return null;
    }
    Decision decision = new Decision();

    if (d.getScheduleActivityTaskDecisionAttributes()
        != com.uber.cadence.api.v1.ScheduleActivityTaskDecisionAttributes.getDefaultInstance()) {
      decision.setDecisionType(DecisionType.ScheduleActivityTask);
      com.uber.cadence.api.v1.ScheduleActivityTaskDecisionAttributes a =
          d.getScheduleActivityTaskDecisionAttributes();
      ScheduleActivityTaskDecisionAttributes attrs = new ScheduleActivityTaskDecisionAttributes();
      decision.setScheduleActivityTaskDecisionAttributes(attrs);
      attrs.setActivityId(a.getActivityId());
      attrs.setActivityType(activityType(a.getActivityType()));
      attrs.setDomain(a.getDomain());
      attrs.setTaskList(taskList(a.getTaskList()));
      attrs.setInput(payload(a.getInput()));
      attrs.setScheduleToCloseTimeoutSeconds(durationToSeconds(a.getScheduleToCloseTimeout()));
      attrs.setScheduleToStartTimeoutSeconds(durationToSeconds(a.getScheduleToStartTimeout()));
      attrs.setStartToCloseTimeoutSeconds(durationToSeconds(a.getStartToCloseTimeout()));
      attrs.setHeartbeatTimeoutSeconds(durationToSeconds(a.getHeartbeatTimeout()));
      attrs.setRetryPolicy(retryPolicy(a.getRetryPolicy()));
      attrs.setHeader(header(a.getHeader()));
      attrs.setRequestLocalDispatch(a.getRequestLocalDispatch());
    } else if (d.getStartTimerDecisionAttributes()
        != com.uber.cadence.api.v1.StartTimerDecisionAttributes.getDefaultInstance()) {
      decision.setDecisionType(DecisionType.StartTimer);
      com.uber.cadence.api.v1.StartTimerDecisionAttributes a = d.getStartTimerDecisionAttributes();
      StartTimerDecisionAttributes attrs = new StartTimerDecisionAttributes();
      decision.setStartTimerDecisionAttributes(attrs);
      attrs.setTimerId(a.getTimerId());
      attrs.setStartToFireTimeoutSeconds(durationToSeconds(a.getStartToFireTimeout()));
    } else if (d.getCompleteWorkflowExecutionDecisionAttributes()
        != com.uber.cadence.api.v1.CompleteWorkflowExecutionDecisionAttributes
            .getDefaultInstance()) {
      decision.setDecisionType(DecisionType.CompleteWorkflowExecution);
      com.uber.cadence.api.v1.CompleteWorkflowExecutionDecisionAttributes a =
          d.getCompleteWorkflowExecutionDecisionAttributes();
      CompleteWorkflowExecutionDecisionAttributes attrs =
          new CompleteWorkflowExecutionDecisionAttributes();
      decision.setCompleteWorkflowExecutionDecisionAttributes(attrs);
      attrs.setResult(payload(a.getResult()));
    } else if (d.getFailWorkflowExecutionDecisionAttributes()
        != com.uber.cadence.api.v1.FailWorkflowExecutionDecisionAttributes.getDefaultInstance()) {
      decision.setDecisionType(DecisionType.FailWorkflowExecution);
      com.uber.cadence.api.v1.FailWorkflowExecutionDecisionAttributes a =
          d.getFailWorkflowExecutionDecisionAttributes();
      FailWorkflowExecutionDecisionAttributes attrs = new FailWorkflowExecutionDecisionAttributes();
      decision.setFailWorkflowExecutionDecisionAttributes(attrs);
      attrs.setReason(failureReason(a.getFailure()));
      attrs.setDetails(failureDetails(a.getFailure()));
    } else if (d.getRequestCancelActivityTaskDecisionAttributes()
        != com.uber.cadence.api.v1.RequestCancelActivityTaskDecisionAttributes
            .getDefaultInstance()) {
      decision.setDecisionType(DecisionType.RequestCancelActivityTask);
      com.uber.cadence.api.v1.RequestCancelActivityTaskDecisionAttributes a =
          d.getRequestCancelActivityTaskDecisionAttributes();
      RequestCancelActivityTaskDecisionAttributes attrs =
          new RequestCancelActivityTaskDecisionAttributes();
      decision.setRequestCancelActivityTaskDecisionAttributes(attrs);
      attrs.setActivityId(a.getActivityId());
    } else if (d.getCancelTimerDecisionAttributes()
        != com.uber.cadence.api.v1.CancelTimerDecisionAttributes.getDefaultInstance()) {
      decision.setDecisionType(DecisionType.CancelTimer);
      com.uber.cadence.api.v1.CancelTimerDecisionAttributes a =
          d.getCancelTimerDecisionAttributes();
      CancelTimerDecisionAttributes attrs = new CancelTimerDecisionAttributes();
      decision.setCancelTimerDecisionAttributes(attrs);
      attrs.setTimerId(a.getTimerId());
    } else if (d.getCancelWorkflowExecutionDecisionAttributes()
        != com.uber.cadence.api.v1.CancelWorkflowExecutionDecisionAttributes.getDefaultInstance()) {
      decision.setDecisionType(DecisionType.CancelWorkflowExecution);
      com.uber.cadence.api.v1.CancelWorkflowExecutionDecisionAttributes a =
          d.getCancelWorkflowExecutionDecisionAttributes();
      CancelWorkflowExecutionDecisionAttributes attrs =
          new CancelWorkflowExecutionDecisionAttributes();
      decision.setCancelWorkflowExecutionDecisionAttributes(attrs);
      attrs.setDetails(payload(a.getDetails()));
    } else if (d.getRequestCancelExternalWorkflowExecutionDecisionAttributes()
        != com.uber.cadence.api.v1.RequestCancelExternalWorkflowExecutionDecisionAttributes
            .getDefaultInstance()) {
      decision.setDecisionType(DecisionType.RequestCancelExternalWorkflowExecution);
      com.uber.cadence.api.v1.RequestCancelExternalWorkflowExecutionDecisionAttributes a =
          d.getRequestCancelExternalWorkflowExecutionDecisionAttributes();
      RequestCancelExternalWorkflowExecutionDecisionAttributes attrs =
          new RequestCancelExternalWorkflowExecutionDecisionAttributes();
      decision.setRequestCancelExternalWorkflowExecutionDecisionAttributes(attrs);
      attrs.setDomain(a.getDomain());
      attrs.setWorkflowId(workflowId(a.getWorkflowExecution()));
      attrs.setRunId(runId(a.getWorkflowExecution()));
      attrs.setControl(byteStringToArray(a.getControl()));
      attrs.setChildWorkflowOnly(a.getChildWorkflowOnly());
    } else if (d.getRecordMarkerDecisionAttributes()
        != com.uber.cadence.api.v1.RecordMarkerDecisionAttributes.getDefaultInstance()) {
      decision.setDecisionType(DecisionType.RecordMarker);
      com.uber.cadence.api.v1.RecordMarkerDecisionAttributes a =
          d.getRecordMarkerDecisionAttributes();
      RecordMarkerDecisionAttributes attrs = new RecordMarkerDecisionAttributes();
      decision.setRecordMarkerDecisionAttributes(attrs);
      attrs.setMarkerName(a.getMarkerName());
      attrs.setDetails(payload(a.getDetails()));
      attrs.setHeader(header(a.getHeader()));
    } else if (d.getContinueAsNewWorkflowExecutionDecisionAttributes()
        != com.uber.cadence.api.v1.ContinueAsNewWorkflowExecutionDecisionAttributes
            .getDefaultInstance()) {
      decision.setDecisionType(DecisionType.ContinueAsNewWorkflowExecution);
      com.uber.cadence.api.v1.ContinueAsNewWorkflowExecutionDecisionAttributes a =
          d.getContinueAsNewWorkflowExecutionDecisionAttributes();
      ContinueAsNewWorkflowExecutionDecisionAttributes attrs =
          new ContinueAsNewWorkflowExecutionDecisionAttributes();
      decision.setContinueAsNewWorkflowExecutionDecisionAttributes(attrs);
      attrs.setWorkflowType(workflowType(a.getWorkflowType()));
      attrs.setTaskList(taskList(a.getTaskList()));
      attrs.setInput(payload(a.getInput()));
      attrs.setExecutionStartToCloseTimeoutSeconds(
          durationToSeconds(a.getExecutionStartToCloseTimeout()));
      attrs.setTaskStartToCloseTimeoutSeconds(durationToSeconds(a.getTaskStartToCloseTimeout()));
      attrs.setBackoffStartIntervalInSeconds(durationToSeconds(a.getBackoffStartInterval()));
      attrs.setRetryPolicy(retryPolicy(a.getRetryPolicy()));
      attrs.setInitiator(continueAsNewInitiator(a.getInitiator()));
      attrs.setFailureReason(failureReason(a.getFailure()));
      attrs.setFailureDetails(failureDetails(a.getFailure()));
      attrs.setLastCompletionResult(payload(a.getLastCompletionResult()));
      attrs.setCronSchedule(a.getCronSchedule());
      attrs.setHeader(header(a.getHeader()));
      attrs.setMemo(memo(a.getMemo()));
      attrs.setSearchAttributes(searchAttributes(a.getSearchAttributes()));
    } else if (d.getStartChildWorkflowExecutionDecisionAttributes()
        != com.uber.cadence.api.v1.StartChildWorkflowExecutionDecisionAttributes
            .getDefaultInstance()) {
      decision.setDecisionType(DecisionType.StartChildWorkflowExecution);
      com.uber.cadence.api.v1.StartChildWorkflowExecutionDecisionAttributes a =
          d.getStartChildWorkflowExecutionDecisionAttributes();
      StartChildWorkflowExecutionDecisionAttributes attrs =
          new StartChildWorkflowExecutionDecisionAttributes();
      decision.setStartChildWorkflowExecutionDecisionAttributes(attrs);

      attrs.setDomain(a.getDomain());
      attrs.setWorkflowId(a.getWorkflowId());
      attrs.setWorkflowType(workflowType(a.getWorkflowType()));
      attrs.setTaskList(taskList(a.getTaskList()));
      attrs.setInput(payload(a.getInput()));
      attrs.setExecutionStartToCloseTimeoutSeconds(
          durationToSeconds(a.getExecutionStartToCloseTimeout()));
      attrs.setTaskStartToCloseTimeoutSeconds(durationToSeconds(a.getTaskStartToCloseTimeout()));
      attrs.setParentClosePolicy(parentClosePolicy(a.getParentClosePolicy()));
      attrs.setControl(byteStringToArray(a.getControl()));
      attrs.setWorkflowIdReusePolicy(workflowIdReusePolicy(a.getWorkflowIdReusePolicy()));
      attrs.setRetryPolicy(retryPolicy(a.getRetryPolicy()));
      attrs.setCronSchedule(a.getCronSchedule());
      attrs.setHeader(header(a.getHeader()));
      attrs.setMemo(memo(a.getMemo()));
      attrs.setSearchAttributes(searchAttributes(a.getSearchAttributes()));
    } else if (d.getSignalExternalWorkflowExecutionDecisionAttributes()
        != com.uber.cadence.api.v1.SignalExternalWorkflowExecutionDecisionAttributes
            .getDefaultInstance()) {
      decision.setDecisionType(DecisionType.SignalExternalWorkflowExecution);
      com.uber.cadence.api.v1.SignalExternalWorkflowExecutionDecisionAttributes a =
          d.getSignalExternalWorkflowExecutionDecisionAttributes();
      SignalExternalWorkflowExecutionDecisionAttributes attrs =
          new SignalExternalWorkflowExecutionDecisionAttributes();
      decision.setSignalExternalWorkflowExecutionDecisionAttributes(attrs);

      attrs.setDomain(a.getDomain());
      attrs.setExecution(workflowExecution(a.getWorkflowExecution()));
      attrs.setSignalName(a.getSignalName());
      attrs.setInput(payload(a.getInput()));
      attrs.setControl(byteStringToArray(a.getControl()));
      attrs.setChildWorkflowOnly(a.getChildWorkflowOnly());
    } else if (d.getUpsertWorkflowSearchAttributesDecisionAttributes()
        != com.uber.cadence.api.v1.UpsertWorkflowSearchAttributesDecisionAttributes
            .getDefaultInstance()) {
      decision.setDecisionType(DecisionType.UpsertWorkflowSearchAttributes);
      com.uber.cadence.api.v1.UpsertWorkflowSearchAttributesDecisionAttributes a =
          d.getUpsertWorkflowSearchAttributesDecisionAttributes();
      UpsertWorkflowSearchAttributesDecisionAttributes attrs =
          new UpsertWorkflowSearchAttributesDecisionAttributes();
      decision.setUpsertWorkflowSearchAttributesDecisionAttributes(attrs);
      attrs.setSearchAttributes(searchAttributes(a.getSearchAttributes()));
    } else {
      throw new IllegalArgumentException("unknown decision type");
    }
    return decision;
  }
}
