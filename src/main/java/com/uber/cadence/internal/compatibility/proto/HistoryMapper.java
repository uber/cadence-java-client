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

import static com.uber.cadence.internal.compatibility.proto.EnumMapper.cancelExternalWorkflowExecutionFailedCause;
import static com.uber.cadence.internal.compatibility.proto.EnumMapper.childWorkflowExecutionFailedCause;
import static com.uber.cadence.internal.compatibility.proto.EnumMapper.continueAsNewInitiator;
import static com.uber.cadence.internal.compatibility.proto.EnumMapper.decisionTaskFailedCause;
import static com.uber.cadence.internal.compatibility.proto.EnumMapper.decisionTaskTimedOutCause;
import static com.uber.cadence.internal.compatibility.proto.EnumMapper.parentClosePolicy;
import static com.uber.cadence.internal.compatibility.proto.EnumMapper.signalExternalWorkflowExecutionFailedCause;
import static com.uber.cadence.internal.compatibility.proto.EnumMapper.timeoutType;
import static com.uber.cadence.internal.compatibility.proto.EnumMapper.workflowIdReusePolicy;
import static com.uber.cadence.internal.compatibility.proto.Helpers.arrayToByteString;
import static com.uber.cadence.internal.compatibility.proto.Helpers.longToInt;
import static com.uber.cadence.internal.compatibility.proto.Helpers.secondsToDuration;
import static com.uber.cadence.internal.compatibility.proto.Helpers.unixNanoToTime;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.activityType;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.externalExecutionInfo;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.failure;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.header;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.memo;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.payload;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.resetPoints;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.retryPolicy;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.searchAttributes;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.taskList;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.workflowExecution;
import static com.uber.cadence.internal.compatibility.proto.TypeMapper.workflowType;

import com.google.protobuf.ByteString;
import com.uber.cadence.api.v1.ActivityTaskCancelRequestedEventAttributes;
import com.uber.cadence.api.v1.ActivityTaskCanceledEventAttributes;
import com.uber.cadence.api.v1.ActivityTaskCompletedEventAttributes;
import com.uber.cadence.api.v1.ActivityTaskFailedEventAttributes;
import com.uber.cadence.api.v1.ActivityTaskScheduledEventAttributes;
import com.uber.cadence.api.v1.ActivityTaskStartedEventAttributes;
import com.uber.cadence.api.v1.ActivityTaskTimedOutEventAttributes;
import com.uber.cadence.api.v1.CancelTimerFailedEventAttributes;
import com.uber.cadence.api.v1.ChildWorkflowExecutionCanceledEventAttributes;
import com.uber.cadence.api.v1.ChildWorkflowExecutionCompletedEventAttributes;
import com.uber.cadence.api.v1.ChildWorkflowExecutionFailedEventAttributes;
import com.uber.cadence.api.v1.ChildWorkflowExecutionStartedEventAttributes;
import com.uber.cadence.api.v1.ChildWorkflowExecutionTerminatedEventAttributes;
import com.uber.cadence.api.v1.ChildWorkflowExecutionTimedOutEventAttributes;
import com.uber.cadence.api.v1.DecisionTaskCompletedEventAttributes;
import com.uber.cadence.api.v1.DecisionTaskFailedEventAttributes;
import com.uber.cadence.api.v1.DecisionTaskScheduledEventAttributes;
import com.uber.cadence.api.v1.DecisionTaskStartedEventAttributes;
import com.uber.cadence.api.v1.DecisionTaskTimedOutEventAttributes;
import com.uber.cadence.api.v1.ExternalWorkflowExecutionCancelRequestedEventAttributes;
import com.uber.cadence.api.v1.ExternalWorkflowExecutionSignaledEventAttributes;
import com.uber.cadence.api.v1.History;
import com.uber.cadence.api.v1.HistoryEvent;
import com.uber.cadence.api.v1.HistoryEvent.Builder;
import com.uber.cadence.api.v1.MarkerRecordedEventAttributes;
import com.uber.cadence.api.v1.RequestCancelActivityTaskFailedEventAttributes;
import com.uber.cadence.api.v1.RequestCancelExternalWorkflowExecutionFailedEventAttributes;
import com.uber.cadence.api.v1.RequestCancelExternalWorkflowExecutionInitiatedEventAttributes;
import com.uber.cadence.api.v1.SignalExternalWorkflowExecutionFailedEventAttributes;
import com.uber.cadence.api.v1.SignalExternalWorkflowExecutionInitiatedEventAttributes;
import com.uber.cadence.api.v1.StartChildWorkflowExecutionFailedEventAttributes;
import com.uber.cadence.api.v1.StartChildWorkflowExecutionInitiatedEventAttributes;
import com.uber.cadence.api.v1.TimerCanceledEventAttributes;
import com.uber.cadence.api.v1.TimerFiredEventAttributes;
import com.uber.cadence.api.v1.TimerStartedEventAttributes;
import com.uber.cadence.api.v1.UpsertWorkflowSearchAttributesEventAttributes;
import com.uber.cadence.api.v1.WorkflowExecutionCancelRequestedEventAttributes;
import com.uber.cadence.api.v1.WorkflowExecutionCanceledEventAttributes;
import com.uber.cadence.api.v1.WorkflowExecutionCompletedEventAttributes;
import com.uber.cadence.api.v1.WorkflowExecutionContinuedAsNewEventAttributes;
import com.uber.cadence.api.v1.WorkflowExecutionFailedEventAttributes;
import com.uber.cadence.api.v1.WorkflowExecutionSignaledEventAttributes;
import com.uber.cadence.api.v1.WorkflowExecutionStartedEventAttributes;
import com.uber.cadence.api.v1.WorkflowExecutionTerminatedEventAttributes;
import com.uber.cadence.api.v1.WorkflowExecutionTimedOutEventAttributes;
import java.util.ArrayList;
import java.util.List;

class HistoryMapper {

  static History History(com.uber.cadence.History t) {
    if (t == null) {
      return null;
    }
    return History.newBuilder().addAllEvents(historyEventArray(t.getEvents())).build();
  }

  static List<HistoryEvent> historyEventArray(List<com.uber.cadence.HistoryEvent> t) {
    if (t == null) {
      return null;
    }
    List<HistoryEvent> v = new ArrayList<>();
    for (int i = 0; i < t.size(); i++) {
      v.add(historyEvent(t.get(i)));
    }

    return v;
  }

  static HistoryEvent historyEvent(com.uber.cadence.HistoryEvent e) {
    if (e == null) {
      return null;
    }
    Builder event =
        HistoryEvent.newBuilder()
            .setEventId(e.getEventId())
            .setEventTime(unixNanoToTime(e.getTimestamp()))
            .setVersion(e.getVersion())
            .setTaskId(e.getTaskId());

    switch (e.getEventType()) {
      case WorkflowExecutionStarted:
        event.setWorkflowExecutionStartedEventAttributes(
            workflowExecutionStartedEventAttributes(
                e.getWorkflowExecutionStartedEventAttributes()));
        break;
      case WorkflowExecutionCompleted:
        event.setWorkflowExecutionCompletedEventAttributes(
            workflowExecutionCompletedEventAttributes(
                e.getWorkflowExecutionCompletedEventAttributes()));
        break;
      case WorkflowExecutionFailed:
        event.setWorkflowExecutionFailedEventAttributes(
            workflowExecutionFailedEventAttributes(e.getWorkflowExecutionFailedEventAttributes()));
        break;
      case WorkflowExecutionTimedOut:
        event.setWorkflowExecutionTimedOutEventAttributes(
            workflowExecutionTimedOutEventAttributes(
                e.getWorkflowExecutionTimedOutEventAttributes()));
        break;
      case DecisionTaskScheduled:
        event.setDecisionTaskScheduledEventAttributes(
            decisionTaskScheduledEventAttributes(e.getDecisionTaskScheduledEventAttributes()));
        break;
      case DecisionTaskStarted:
        event.setDecisionTaskStartedEventAttributes(
            decisionTaskStartedEventAttributes(e.getDecisionTaskStartedEventAttributes()));
        break;
      case DecisionTaskCompleted:
        event.setDecisionTaskCompletedEventAttributes(
            decisionTaskCompletedEventAttributes(e.getDecisionTaskCompletedEventAttributes()));
        break;
      case DecisionTaskTimedOut:
        event.setDecisionTaskTimedOutEventAttributes(
            decisionTaskTimedOutEventAttributes(e.getDecisionTaskTimedOutEventAttributes()));
        break;
      case DecisionTaskFailed:
        event.setDecisionTaskFailedEventAttributes(
            decisionTaskFailedEventAttributes(e.getDecisionTaskFailedEventAttributes()));
        break;
      case ActivityTaskScheduled:
        event.setActivityTaskScheduledEventAttributes(
            activityTaskScheduledEventAttributes(e.getActivityTaskScheduledEventAttributes()));
        break;
      case ActivityTaskStarted:
        event.setActivityTaskStartedEventAttributes(
            activityTaskStartedEventAttributes(e.getActivityTaskStartedEventAttributes()));
        break;
      case ActivityTaskCompleted:
        event.setActivityTaskCompletedEventAttributes(
            activityTaskCompletedEventAttributes(e.getActivityTaskCompletedEventAttributes()));
        break;
      case ActivityTaskFailed:
        event.setActivityTaskFailedEventAttributes(
            activityTaskFailedEventAttributes(e.getActivityTaskFailedEventAttributes()));
        break;
      case ActivityTaskTimedOut:
        event.setActivityTaskTimedOutEventAttributes(
            activityTaskTimedOutEventAttributes(e.getActivityTaskTimedOutEventAttributes()));
        break;
      case TimerStarted:
        event.setTimerStartedEventAttributes(
            timerStartedEventAttributes(e.getTimerStartedEventAttributes()));
        break;
      case TimerFired:
        event.setTimerFiredEventAttributes(
            timerFiredEventAttributes(e.getTimerFiredEventAttributes()));
        break;
      case ActivityTaskCancelRequested:
        event.setActivityTaskCancelRequestedEventAttributes(
            activityTaskCancelRequestedEventAttributes(
                e.getActivityTaskCancelRequestedEventAttributes()));
        break;
      case RequestCancelActivityTaskFailed:
        event.setRequestCancelActivityTaskFailedEventAttributes(
            requestCancelActivityTaskFailedEventAttributes(
                e.getRequestCancelActivityTaskFailedEventAttributes()));
        break;
      case ActivityTaskCanceled:
        event.setActivityTaskCanceledEventAttributes(
            activityTaskCanceledEventAttributes(e.getActivityTaskCanceledEventAttributes()));
        break;
      case TimerCanceled:
        event.setTimerCanceledEventAttributes(
            timerCanceledEventAttributes(e.getTimerCanceledEventAttributes()));
        break;
      case CancelTimerFailed:
        event.setCancelTimerFailedEventAttributes(
            cancelTimerFailedEventAttributes(e.getCancelTimerFailedEventAttributes()));
        break;
      case MarkerRecorded:
        event.setMarkerRecordedEventAttributes(
            markerRecordedEventAttributes(e.getMarkerRecordedEventAttributes()));
        break;
      case WorkflowExecutionSignaled:
        event.setWorkflowExecutionSignaledEventAttributes(
            workflowExecutionSignaledEventAttributes(
                e.getWorkflowExecutionSignaledEventAttributes()));
        break;
      case WorkflowExecutionTerminated:
        event.setWorkflowExecutionTerminatedEventAttributes(
            workflowExecutionTerminatedEventAttributes(
                e.getWorkflowExecutionTerminatedEventAttributes()));
        break;
      case WorkflowExecutionCancelRequested:
        event.setWorkflowExecutionCancelRequestedEventAttributes(
            workflowExecutionCancelRequestedEventAttributes(
                e.getWorkflowExecutionCancelRequestedEventAttributes()));
        break;
      case WorkflowExecutionCanceled:
        event.setWorkflowExecutionCanceledEventAttributes(
            workflowExecutionCanceledEventAttributes(
                e.getWorkflowExecutionCanceledEventAttributes()));
        break;
      case RequestCancelExternalWorkflowExecutionInitiated:
        event.setRequestCancelExternalWorkflowExecutionInitiatedEventAttributes(
            requestCancelExternalWorkflowExecutionInitiatedEventAttributes(
                e.getRequestCancelExternalWorkflowExecutionInitiatedEventAttributes()));
        break;
      case RequestCancelExternalWorkflowExecutionFailed:
        event.setRequestCancelExternalWorkflowExecutionFailedEventAttributes(
            requestCancelExternalWorkflowExecutionFailedEventAttributes(
                e.getRequestCancelExternalWorkflowExecutionFailedEventAttributes()));
        break;
      case ExternalWorkflowExecutionCancelRequested:
        event.setExternalWorkflowExecutionCancelRequestedEventAttributes(
            externalWorkflowExecutionCancelRequestedEventAttributes(
                e.getExternalWorkflowExecutionCancelRequestedEventAttributes()));
        break;
      case WorkflowExecutionContinuedAsNew:
        event.setWorkflowExecutionContinuedAsNewEventAttributes(
            workflowExecutionContinuedAsNewEventAttributes(
                e.getWorkflowExecutionContinuedAsNewEventAttributes()));
        break;
      case StartChildWorkflowExecutionInitiated:
        event.setStartChildWorkflowExecutionInitiatedEventAttributes(
            startChildWorkflowExecutionInitiatedEventAttributes(
                e.getStartChildWorkflowExecutionInitiatedEventAttributes()));
        break;
      case StartChildWorkflowExecutionFailed:
        event.setStartChildWorkflowExecutionFailedEventAttributes(
            startChildWorkflowExecutionFailedEventAttributes(
                e.getStartChildWorkflowExecutionFailedEventAttributes()));
        break;
      case ChildWorkflowExecutionStarted:
        event.setChildWorkflowExecutionStartedEventAttributes(
            childWorkflowExecutionStartedEventAttributes(
                e.getChildWorkflowExecutionStartedEventAttributes()));
        break;
      case ChildWorkflowExecutionCompleted:
        event.setChildWorkflowExecutionCompletedEventAttributes(
            childWorkflowExecutionCompletedEventAttributes(
                e.getChildWorkflowExecutionCompletedEventAttributes()));
        break;
      case ChildWorkflowExecutionFailed:
        event.setChildWorkflowExecutionFailedEventAttributes(
            childWorkflowExecutionFailedEventAttributes(
                e.getChildWorkflowExecutionFailedEventAttributes()));
        break;
      case ChildWorkflowExecutionCanceled:
        event.setChildWorkflowExecutionCanceledEventAttributes(
            childWorkflowExecutionCanceledEventAttributes(
                e.getChildWorkflowExecutionCanceledEventAttributes()));
        break;
      case ChildWorkflowExecutionTimedOut:
        event.setChildWorkflowExecutionTimedOutEventAttributes(
            childWorkflowExecutionTimedOutEventAttributes(
                e.getChildWorkflowExecutionTimedOutEventAttributes()));
        break;
      case ChildWorkflowExecutionTerminated:
        event.setChildWorkflowExecutionTerminatedEventAttributes(
            childWorkflowExecutionTerminatedEventAttributes(
                e.getChildWorkflowExecutionTerminatedEventAttributes()));
        break;
      case SignalExternalWorkflowExecutionInitiated:
        event.setSignalExternalWorkflowExecutionInitiatedEventAttributes(
            signalExternalWorkflowExecutionInitiatedEventAttributes(
                e.getSignalExternalWorkflowExecutionInitiatedEventAttributes()));
        break;
      case SignalExternalWorkflowExecutionFailed:
        event.setSignalExternalWorkflowExecutionFailedEventAttributes(
            signalExternalWorkflowExecutionFailedEventAttributes(
                e.getSignalExternalWorkflowExecutionFailedEventAttributes()));
        break;
      case ExternalWorkflowExecutionSignaled:
        event.setExternalWorkflowExecutionSignaledEventAttributes(
            externalWorkflowExecutionSignaledEventAttributes(
                e.getExternalWorkflowExecutionSignaledEventAttributes()));
        break;
      case UpsertWorkflowSearchAttributes:
        event.setUpsertWorkflowSearchAttributesEventAttributes(
            upsertWorkflowSearchAttributesEventAttributes(
                e.getUpsertWorkflowSearchAttributesEventAttributes()));
        break;
      default:
        throw new IllegalArgumentException("unknown event type");
    }
    return event.build();
  }

  static ActivityTaskCancelRequestedEventAttributes activityTaskCancelRequestedEventAttributes(
      com.uber.cadence.ActivityTaskCancelRequestedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return ActivityTaskCancelRequestedEventAttributes.newBuilder()
        .setActivityId(t.getActivityId())
        .setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId())
        .build();
  }

  static ActivityTaskCanceledEventAttributes activityTaskCanceledEventAttributes(
      com.uber.cadence.ActivityTaskCanceledEventAttributes t) {
    if (t == null) {
      return null;
    }
    return ActivityTaskCanceledEventAttributes.newBuilder()
        .setDetails(TypeMapper.payload(t.getDetails()))
        .setLatestCancelRequestedEventId(t.getLatestCancelRequestedEventId())
        .setScheduledEventId(t.getScheduledEventId())
        .setStartedEventId(t.getStartedEventId())
        .setIdentity(t.getIdentity())
        .build();
  }

  static ActivityTaskCompletedEventAttributes activityTaskCompletedEventAttributes(
      com.uber.cadence.ActivityTaskCompletedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return ActivityTaskCompletedEventAttributes.newBuilder()
        .setResult(TypeMapper.payload(t.getResult()))
        .setScheduledEventId(t.getScheduledEventId())
        .setStartedEventId(t.getStartedEventId())
        .setIdentity(t.getIdentity())
        .build();
  }

  static ActivityTaskFailedEventAttributes activityTaskFailedEventAttributes(
      com.uber.cadence.ActivityTaskFailedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return ActivityTaskFailedEventAttributes.newBuilder()
        .setFailure(failure(t.getReason(), t.getDetails()))
        .setScheduledEventId(t.getScheduledEventId())
        .setStartedEventId(t.getStartedEventId())
        .setIdentity(t.getIdentity())
        .build();
  }

  static ActivityTaskScheduledEventAttributes activityTaskScheduledEventAttributes(
      com.uber.cadence.ActivityTaskScheduledEventAttributes t) {
    if (t == null) {
      return null;
    }
    return ActivityTaskScheduledEventAttributes.newBuilder()
        .setActivityId(t.getActivityId())
        .setActivityType(activityType(t.getActivityType()))
        .setDomain(t.getDomain())
        .setTaskList(taskList(t.getTaskList()))
        .setInput(payload(t.getInput()))
        .setScheduleToCloseTimeout(secondsToDuration(t.getScheduleToCloseTimeoutSeconds()))
        .setScheduleToStartTimeout(secondsToDuration(t.getScheduleToStartTimeoutSeconds()))
        .setStartToCloseTimeout(secondsToDuration(t.getStartToCloseTimeoutSeconds()))
        .setHeartbeatTimeout(secondsToDuration(t.getHeartbeatTimeoutSeconds()))
        .setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId())
        .setRetryPolicy(retryPolicy(t.getRetryPolicy()))
        .setHeader(header(t.getHeader()))
        .build();
  }

  static ActivityTaskStartedEventAttributes activityTaskStartedEventAttributes(
      com.uber.cadence.ActivityTaskStartedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return ActivityTaskStartedEventAttributes.newBuilder()
        .setScheduledEventId(t.getScheduledEventId())
        .setIdentity(t.getIdentity())
        .setRequestId(t.getRequestId())
        .setAttempt(t.getAttempt())
        .setLastFailure(failure(t.getLastFailureReason(), t.getLastFailureDetails()))
        .build();
  }

  static ActivityTaskTimedOutEventAttributes activityTaskTimedOutEventAttributes(
      com.uber.cadence.ActivityTaskTimedOutEventAttributes t) {
    if (t == null) {
      return null;
    }
    return ActivityTaskTimedOutEventAttributes.newBuilder()
        .setDetails(payload(t.getDetails()))
        .setScheduledEventId(t.getScheduledEventId())
        .setStartedEventId(t.getStartedEventId())
        .setTimeoutType(timeoutType(t.getTimeoutType()))
        .setLastFailure(failure(t.getLastFailureReason(), t.getLastFailureDetails()))
        .build();
  }

  static CancelTimerFailedEventAttributes cancelTimerFailedEventAttributes(
      com.uber.cadence.CancelTimerFailedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return CancelTimerFailedEventAttributes.newBuilder()
        .setTimerId(t.getTimerId())
        .setCause(t.getCause())
        .setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId())
        .setIdentity(t.getIdentity())
        .build();
  }

  static ChildWorkflowExecutionCanceledEventAttributes
  childWorkflowExecutionCanceledEventAttributes(
      com.uber.cadence.ChildWorkflowExecutionCanceledEventAttributes t) {
    if (t == null) {
      return null;
    }
    return ChildWorkflowExecutionCanceledEventAttributes.newBuilder()
        .setDomain(t.getDomain())
        .setWorkflowExecution(workflowExecution(t.getWorkflowExecution()))
        .setWorkflowType(workflowType(t.getWorkflowType()))
        .setInitiatedEventId(t.getInitiatedEventId())
        .setStartedEventId(t.getStartedEventId())
        .setDetails(payload(t.getDetails()))
        .build();
  }

  static ChildWorkflowExecutionCompletedEventAttributes
  childWorkflowExecutionCompletedEventAttributes(
      com.uber.cadence.ChildWorkflowExecutionCompletedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return ChildWorkflowExecutionCompletedEventAttributes.newBuilder()
        .setDomain(t.getDomain())
        .setWorkflowExecution(workflowExecution(t.getWorkflowExecution()))
        .setWorkflowType(workflowType(t.getWorkflowType()))
        .setInitiatedEventId(t.getInitiatedEventId())
        .setStartedEventId(t.getStartedEventId())
        .setResult(payload(t.getResult()))
        .build();
  }

  static ChildWorkflowExecutionFailedEventAttributes childWorkflowExecutionFailedEventAttributes(
      com.uber.cadence.ChildWorkflowExecutionFailedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return ChildWorkflowExecutionFailedEventAttributes.newBuilder()
        .setDomain(t.getDomain())
        .setWorkflowExecution(workflowExecution(t.getWorkflowExecution()))
        .setWorkflowType(workflowType(t.getWorkflowType()))
        .setInitiatedEventId(t.getInitiatedEventId())
        .setStartedEventId(t.getStartedEventId())
        .setFailure(failure(t.getReason(), t.getDetails()))
        .build();
  }

  static ChildWorkflowExecutionStartedEventAttributes childWorkflowExecutionStartedEventAttributes(
      com.uber.cadence.ChildWorkflowExecutionStartedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return ChildWorkflowExecutionStartedEventAttributes.newBuilder()
        .setDomain(t.getDomain())
        .setWorkflowExecution(workflowExecution(t.getWorkflowExecution()))
        .setWorkflowType(workflowType(t.getWorkflowType()))
        .setInitiatedEventId(t.getInitiatedEventId())
        .setHeader(header(t.getHeader()))
        .build();
  }

  static ChildWorkflowExecutionTerminatedEventAttributes
  childWorkflowExecutionTerminatedEventAttributes(
      com.uber.cadence.ChildWorkflowExecutionTerminatedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return ChildWorkflowExecutionTerminatedEventAttributes.newBuilder()
        .setDomain(t.getDomain())
        .setWorkflowExecution(workflowExecution(t.getWorkflowExecution()))
        .setWorkflowType(workflowType(t.getWorkflowType()))
        .setInitiatedEventId(t.getInitiatedEventId())
        .setStartedEventId(t.getStartedEventId())
        .build();
  }

  static ChildWorkflowExecutionTimedOutEventAttributes
  childWorkflowExecutionTimedOutEventAttributes(
      com.uber.cadence.ChildWorkflowExecutionTimedOutEventAttributes t) {
    if (t == null) {
      return null;
    }
    return ChildWorkflowExecutionTimedOutEventAttributes.newBuilder()
        .setDomain(t.getDomain())
        .setWorkflowExecution(workflowExecution(t.getWorkflowExecution()))
        .setWorkflowType(workflowType(t.getWorkflowType()))
        .setInitiatedEventId(t.getInitiatedEventId())
        .setStartedEventId(t.getStartedEventId())
        .setTimeoutType(timeoutType(t.getTimeoutType()))
        .build();
  }

  static DecisionTaskFailedEventAttributes decisionTaskFailedEventAttributes(
      com.uber.cadence.DecisionTaskFailedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return DecisionTaskFailedEventAttributes.newBuilder()
        .setScheduledEventId(t.getScheduledEventId())
        .setStartedEventId(t.getStartedEventId())
        .setCause(decisionTaskFailedCause(t.getCause()))
        .setFailure(failure(t.getReason(), t.getDetails()))
        .setIdentity(t.getIdentity())
        .setBaseRunId(t.getBaseRunId())
        .setNewRunId(t.getNewRunId())
        .setForkEventVersion(t.getForkEventVersion())
        .setBinaryChecksum(t.getBinaryChecksum())
        .build();
  }

  static DecisionTaskScheduledEventAttributes decisionTaskScheduledEventAttributes(
      com.uber.cadence.DecisionTaskScheduledEventAttributes t) {
    if (t == null) {
      return null;
    }
    return DecisionTaskScheduledEventAttributes.newBuilder()
        .setTaskList(taskList(t.getTaskList()))
        .setStartToCloseTimeout(secondsToDuration(t.getStartToCloseTimeoutSeconds()))
        .setAttempt(longToInt(t.getAttempt()))
        .build();
  }

  static DecisionTaskStartedEventAttributes decisionTaskStartedEventAttributes(
      com.uber.cadence.DecisionTaskStartedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return DecisionTaskStartedEventAttributes.newBuilder()
        .setScheduledEventId(t.getScheduledEventId())
        .setIdentity(t.getIdentity())
        .setRequestId(t.getRequestId())
        .build();
  }

  static DecisionTaskCompletedEventAttributes decisionTaskCompletedEventAttributes(
      com.uber.cadence.DecisionTaskCompletedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return DecisionTaskCompletedEventAttributes.newBuilder()
        .setScheduledEventId(t.getScheduledEventId())
        .setStartedEventId(t.getStartedEventId())
        .setIdentity(t.getIdentity())
        .setBinaryChecksum(t.getBinaryChecksum())
        .setExecutionContext(arrayToByteString(t.getExecutionContext()))
        .build();
  }

  static DecisionTaskTimedOutEventAttributes decisionTaskTimedOutEventAttributes(
      com.uber.cadence.DecisionTaskTimedOutEventAttributes t) {
    if (t == null) {
      return null;
    }
    return DecisionTaskTimedOutEventAttributes.newBuilder()
        .setScheduledEventId(t.getScheduledEventId())
        .setStartedEventId(t.getStartedEventId())
        .setTimeoutType(timeoutType(t.getTimeoutType()))
        .setBaseRunId(t.getBaseRunId())
        .setNewRunId(t.getNewRunId())
        .setForkEventVersion(t.getForkEventVersion())
        .setReason(t.getReason())
        .setCause(decisionTaskTimedOutCause(t.getCause()))
        .build();
  }

  static ExternalWorkflowExecutionCancelRequestedEventAttributes
  externalWorkflowExecutionCancelRequestedEventAttributes(
      com.uber.cadence.ExternalWorkflowExecutionCancelRequestedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return ExternalWorkflowExecutionCancelRequestedEventAttributes.newBuilder()
        .setInitiatedEventId(t.getInitiatedEventId())
        .setDomain(t.getDomain())
        .setWorkflowExecution(workflowExecution(t.getWorkflowExecution()))
        .build();
  }

  static ExternalWorkflowExecutionSignaledEventAttributes
  externalWorkflowExecutionSignaledEventAttributes(
      com.uber.cadence.ExternalWorkflowExecutionSignaledEventAttributes t) {
    if (t == null) {
      return null;
    }
    return ExternalWorkflowExecutionSignaledEventAttributes.newBuilder()
        .setInitiatedEventId(t.getInitiatedEventId())
        .setDomain(t.getDomain())
        .setWorkflowExecution(workflowExecution(t.getWorkflowExecution()))
        .setControl(arrayToByteString(t.getControl()))
        .build();
  }

  static MarkerRecordedEventAttributes markerRecordedEventAttributes(
      com.uber.cadence.MarkerRecordedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return MarkerRecordedEventAttributes.newBuilder()
        .setMarkerName(t.getMarkerName())
        .setDetails(payload(t.getDetails()))
        .setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId())
        .setHeader(header(t.getHeader()))
        .build();
  }

  static RequestCancelActivityTaskFailedEventAttributes
  requestCancelActivityTaskFailedEventAttributes(
      com.uber.cadence.RequestCancelActivityTaskFailedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return RequestCancelActivityTaskFailedEventAttributes.newBuilder()
        .setActivityId(t.getActivityId())
        .setCause(t.getCause())
        .setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId())
        .build();
  }

  static RequestCancelExternalWorkflowExecutionFailedEventAttributes
  requestCancelExternalWorkflowExecutionFailedEventAttributes(
      com.uber.cadence.RequestCancelExternalWorkflowExecutionFailedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return RequestCancelExternalWorkflowExecutionFailedEventAttributes.newBuilder()
        .setCause(cancelExternalWorkflowExecutionFailedCause(t.getCause()))
        .setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId())
        .setDomain(t.getDomain())
        .setWorkflowExecution(workflowExecution(t.getWorkflowExecution()))
        .setInitiatedEventId(t.getInitiatedEventId())
        .setControl(arrayToByteString(t.getControl()))
        .build();
  }

  static RequestCancelExternalWorkflowExecutionInitiatedEventAttributes
  requestCancelExternalWorkflowExecutionInitiatedEventAttributes(
      com.uber.cadence.RequestCancelExternalWorkflowExecutionInitiatedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return RequestCancelExternalWorkflowExecutionInitiatedEventAttributes.newBuilder()
        .setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId())
        .setDomain(t.getDomain())
        .setWorkflowExecution(workflowExecution(t.getWorkflowExecution()))
        .setControl(arrayToByteString(t.getControl()))
        .setChildWorkflowOnly(t.isChildWorkflowOnly())
        .build();
  }

  static SignalExternalWorkflowExecutionFailedEventAttributes
  signalExternalWorkflowExecutionFailedEventAttributes(
      com.uber.cadence.SignalExternalWorkflowExecutionFailedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return SignalExternalWorkflowExecutionFailedEventAttributes.newBuilder()
        .setCause(signalExternalWorkflowExecutionFailedCause(t.getCause()))
        .setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId())
        .setDomain(t.getDomain())
        .setWorkflowExecution(workflowExecution(t.getWorkflowExecution()))
        .setInitiatedEventId(t.getInitiatedEventId())
        .setControl(arrayToByteString(t.getControl()))
        .build();
  }

  static SignalExternalWorkflowExecutionInitiatedEventAttributes
  signalExternalWorkflowExecutionInitiatedEventAttributes(
      com.uber.cadence.SignalExternalWorkflowExecutionInitiatedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return SignalExternalWorkflowExecutionInitiatedEventAttributes.newBuilder()
        .setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId())
        .setDomain(t.getDomain())
        .setWorkflowExecution(workflowExecution(t.getWorkflowExecution()))
        .setSignalName(t.getSignalName())
        .setInput(payload(t.getInput()))
        .setControl(arrayToByteString(t.getControl()))
        .setChildWorkflowOnly(t.isChildWorkflowOnly())
        .build();
  }

  static StartChildWorkflowExecutionFailedEventAttributes
  startChildWorkflowExecutionFailedEventAttributes(
      com.uber.cadence.StartChildWorkflowExecutionFailedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return StartChildWorkflowExecutionFailedEventAttributes.newBuilder()
        .setDomain(t.getDomain())
        .setWorkflowId(t.getWorkflowId())
        .setWorkflowType(workflowType(t.getWorkflowType()))
        .setCause(childWorkflowExecutionFailedCause(t.getCause()))
        .setControl(arrayToByteString(t.getControl()))
        .setInitiatedEventId(t.getInitiatedEventId())
        .setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId())
        .build();
  }

  static StartChildWorkflowExecutionInitiatedEventAttributes
  startChildWorkflowExecutionInitiatedEventAttributes(
      com.uber.cadence.StartChildWorkflowExecutionInitiatedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return StartChildWorkflowExecutionInitiatedEventAttributes.newBuilder()
        .setDomain(t.getDomain())
        .setWorkflowId(t.getWorkflowId())
        .setWorkflowType(workflowType(t.getWorkflowType()))
        .setTaskList(taskList(t.getTaskList()))
        .setInput(payload(t.getInput()))
        .setExecutionStartToCloseTimeout(
            secondsToDuration(t.getExecutionStartToCloseTimeoutSeconds()))
        .setTaskStartToCloseTimeout(secondsToDuration(t.getTaskStartToCloseTimeoutSeconds()))
        .setParentClosePolicy(parentClosePolicy(t.getParentClosePolicy()))
        .setControl(arrayToByteString(t.getControl()))
        .setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId())
        .setWorkflowIdReusePolicy(workflowIdReusePolicy(t.getWorkflowIdReusePolicy()))
        .setRetryPolicy(retryPolicy(t.getRetryPolicy()))
        .setCronSchedule(t.getCronSchedule())
        .setHeader(header(t.getHeader()))
        .setMemo(memo(t.getMemo()))
        .setSearchAttributes(searchAttributes(t.getSearchAttributes()))
        .setDelayStart(secondsToDuration(t.getDelayStartSeconds()))
        .build();
  }

  static TimerCanceledEventAttributes timerCanceledEventAttributes(
      com.uber.cadence.TimerCanceledEventAttributes t) {
    if (t == null) {
      return null;
    }
    return TimerCanceledEventAttributes.newBuilder()
        .setTimerId(t.getTimerId())
        .setStartedEventId(t.getStartedEventId())
        .setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId())
        .setIdentity(t.getIdentity())
        .build();
  }

  static TimerFiredEventAttributes timerFiredEventAttributes(
      com.uber.cadence.TimerFiredEventAttributes t) {
    if (t == null) {
      return null;
    }
    return TimerFiredEventAttributes.newBuilder()
        .setTimerId(t.getTimerId())
        .setStartedEventId(t.getStartedEventId())
        .build();
  }

  static TimerStartedEventAttributes timerStartedEventAttributes(
      com.uber.cadence.TimerStartedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return TimerStartedEventAttributes.newBuilder()
        .setTimerId(t.getTimerId())
        .setStartToFireTimeout(secondsToDuration(longToInt(t.getStartToFireTimeoutSeconds())))
        .setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId())
        .build();
  }

  static UpsertWorkflowSearchAttributesEventAttributes
  upsertWorkflowSearchAttributesEventAttributes(
      com.uber.cadence.UpsertWorkflowSearchAttributesEventAttributes t) {
    if (t == null) {
      return null;
    }
    return UpsertWorkflowSearchAttributesEventAttributes.newBuilder()
        .setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId())
        .setSearchAttributes(searchAttributes(t.getSearchAttributes()))
        .build();
  }

  static WorkflowExecutionCancelRequestedEventAttributes
  workflowExecutionCancelRequestedEventAttributes(
      com.uber.cadence.WorkflowExecutionCancelRequestedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return WorkflowExecutionCancelRequestedEventAttributes.newBuilder()
        .setCause(t.getCause())
        .setExternalExecutionInfo(
            externalExecutionInfo(
                t.getExternalWorkflowExecution(), t.getExternalInitiatedEventId()))
        .setIdentity(t.getIdentity())
        .build();
  }

  static WorkflowExecutionCanceledEventAttributes workflowExecutionCanceledEventAttributes(
      com.uber.cadence.WorkflowExecutionCanceledEventAttributes t) {
    if (t == null) {
      return null;
    }
    return WorkflowExecutionCanceledEventAttributes.newBuilder()
        .setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId())
        .setDetails(payload(t.getDetails()))
        .build();
  }

  static WorkflowExecutionCompletedEventAttributes workflowExecutionCompletedEventAttributes(
      com.uber.cadence.WorkflowExecutionCompletedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return WorkflowExecutionCompletedEventAttributes.newBuilder()
        .setResult(payload(t.getResult()))
        .setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId())
        .build();
  }

  static WorkflowExecutionContinuedAsNewEventAttributes
  workflowExecutionContinuedAsNewEventAttributes(
      com.uber.cadence.WorkflowExecutionContinuedAsNewEventAttributes t) {
    if (t == null) {
      return null;
    }
    return WorkflowExecutionContinuedAsNewEventAttributes.newBuilder()
        .setNewExecutionRunId(t.getNewExecutionRunId())
        .setWorkflowType(workflowType(t.getWorkflowType()))
        .setTaskList(taskList(t.getTaskList()))
        .setInput(payload(t.getInput()))
        .setExecutionStartToCloseTimeout(
            secondsToDuration(t.getExecutionStartToCloseTimeoutSeconds()))
        .setTaskStartToCloseTimeout(secondsToDuration(t.getTaskStartToCloseTimeoutSeconds()))
        .setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId())
        .setBackoffStartInterval(secondsToDuration(t.getBackoffStartIntervalInSeconds()))
        .setInitiator(continueAsNewInitiator(t.getInitiator()))
        .setFailure(failure(t.getFailureReason(), t.getFailureDetails()))
        .setLastCompletionResult(payload(t.getLastCompletionResult()))
        .setHeader(header(t.getHeader()))
        .setMemo(memo(t.getMemo()))
        .setSearchAttributes(searchAttributes(t.getSearchAttributes()))
        .build();
  }

  static WorkflowExecutionFailedEventAttributes workflowExecutionFailedEventAttributes(
      com.uber.cadence.WorkflowExecutionFailedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return WorkflowExecutionFailedEventAttributes.newBuilder()
        .setFailure(failure(t.getReason(), t.getDetails()))
        .setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId())
        .build();
  }

  static WorkflowExecutionSignaledEventAttributes workflowExecutionSignaledEventAttributes(
      com.uber.cadence.WorkflowExecutionSignaledEventAttributes t) {
    if (t == null) {
      return null;
    }
    return WorkflowExecutionSignaledEventAttributes.newBuilder()
        .setSignalName(t.getSignalName())
        .setInput(payload(t.getInput()))
        .setIdentity(t.getIdentity())
        .build();
  }

  static WorkflowExecutionStartedEventAttributes workflowExecutionStartedEventAttributes(
      com.uber.cadence.WorkflowExecutionStartedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return WorkflowExecutionStartedEventAttributes.newBuilder()
        .setWorkflowType(workflowType(t.getWorkflowType()))
        .setParentExecutionInfo(
            TypeMapper.parentExecutionInfo(
                null,
                t.getParentWorkflowDomain(),
                t.getParentWorkflowExecution(),
                t.getParentInitiatedEventId()))
        .setTaskList(taskList(t.getTaskList()))
        .setInput(payload(t.getInput()))
        .setExecutionStartToCloseTimeout(
            secondsToDuration(t.getExecutionStartToCloseTimeoutSeconds()))
        .setTaskStartToCloseTimeout(secondsToDuration(t.getTaskStartToCloseTimeoutSeconds()))
        .setContinuedExecutionRunId(t.getContinuedExecutionRunId())
        .setInitiator(continueAsNewInitiator(t.getInitiator()))
        .setContinuedFailure(failure(t.getContinuedFailureReason(), t.getContinuedFailureDetails()))
        .setLastCompletionResult(payload(t.getLastCompletionResult()))
        .setOriginalExecutionRunId(t.getOriginalExecutionRunId())
        .setIdentity(t.getIdentity())
        .setFirstExecutionRunId(t.getFirstExecutionRunId())
        .setRetryPolicy(retryPolicy(t.getRetryPolicy()))
        .setAttempt(t.getAttempt())
        .setExpirationTime(unixNanoToTime(t.getExpirationTimestamp()))
        .setCronSchedule(t.getCronSchedule())
        .setFirstDecisionTaskBackoff(secondsToDuration(t.getFirstDecisionTaskBackoffSeconds()))
        .setMemo(memo(t.getMemo()))
        .setSearchAttributes(searchAttributes(t.getSearchAttributes()))
        .setPrevAutoResetPoints(resetPoints(t.getPrevAutoResetPoints()))
        .setHeader(header(t.getHeader()))
        .build();
  }

  static WorkflowExecutionTerminatedEventAttributes workflowExecutionTerminatedEventAttributes(
      com.uber.cadence.WorkflowExecutionTerminatedEventAttributes t) {
    if (t == null) {
      return null;
    }
    return WorkflowExecutionTerminatedEventAttributes.newBuilder()
        .setReason(t.getReason())
        .setDetails(payload(t.getDetails()))
        .setIdentity(t.getIdentity())
        .build();
  }

  static WorkflowExecutionTimedOutEventAttributes workflowExecutionTimedOutEventAttributes(
      com.uber.cadence.WorkflowExecutionTimedOutEventAttributes t) {
    if (t == null) {
      return null;
    }
    return WorkflowExecutionTimedOutEventAttributes.newBuilder()
        .setTimeoutType(timeoutType(t.getTimeoutType()))
        .build();
  }
}
