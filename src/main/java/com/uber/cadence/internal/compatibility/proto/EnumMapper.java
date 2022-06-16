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

import static com.uber.cadence.api.v1.CancelExternalWorkflowExecutionFailedCause.CANCEL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_CAUSE_INVALID;
import static com.uber.cadence.api.v1.CancelExternalWorkflowExecutionFailedCause.CANCEL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_CAUSE_UNKNOWN_EXTERNAL_WORKFLOW_EXECUTION;
import static com.uber.cadence.api.v1.ChildWorkflowExecutionFailedCause.CHILD_WORKFLOW_EXECUTION_FAILED_CAUSE_INVALID;
import static com.uber.cadence.api.v1.ChildWorkflowExecutionFailedCause.CHILD_WORKFLOW_EXECUTION_FAILED_CAUSE_WORKFLOW_ALREADY_RUNNING;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_BINARY;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_CANCEL_TIMER_ATTRIBUTES;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_CANCEL_WORKFLOW_EXECUTION_ATTRIBUTES;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_COMPLETE_WORKFLOW_EXECUTION_ATTRIBUTES;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_CONTINUE_AS_NEW_ATTRIBUTES;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_FAIL_WORKFLOW_EXECUTION_ATTRIBUTES;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_RECORD_MARKER_ATTRIBUTES;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_REQUEST_CANCEL_ACTIVITY_ATTRIBUTES;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_REQUEST_CANCEL_EXTERNAL_WORKFLOW_EXECUTION_ATTRIBUTES;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_SCHEDULE_ACTIVITY_ATTRIBUTES;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_SEARCH_ATTRIBUTES;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_SIGNAL_INPUT_SIZE;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_SIGNAL_WORKFLOW_EXECUTION_ATTRIBUTES;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_START_CHILD_EXECUTION_ATTRIBUTES;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_START_TIMER_ATTRIBUTES;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_FAILOVER_CLOSE_DECISION;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_FORCE_CLOSE_DECISION;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_INVALID;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_RESET_STICKY_TASK_LIST;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_RESET_WORKFLOW;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_SCHEDULE_ACTIVITY_DUPLICATE_ID;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_START_TIMER_DUPLICATE_ID;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_UNHANDLED_DECISION;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_WORKFLOW_WORKER_UNHANDLED_FAILURE;
import static com.uber.cadence.api.v1.DecisionTaskTimedOutCause.DECISION_TASK_TIMED_OUT_CAUSE_INVALID;
import static com.uber.cadence.api.v1.DecisionTaskTimedOutCause.DECISION_TASK_TIMED_OUT_CAUSE_RESET;
import static com.uber.cadence.api.v1.DecisionTaskTimedOutCause.DECISION_TASK_TIMED_OUT_CAUSE_TIMEOUT;
import static com.uber.cadence.api.v1.DomainStatus.DOMAIN_STATUS_DELETED;
import static com.uber.cadence.api.v1.DomainStatus.DOMAIN_STATUS_DEPRECATED;
import static com.uber.cadence.api.v1.DomainStatus.DOMAIN_STATUS_INVALID;
import static com.uber.cadence.api.v1.DomainStatus.DOMAIN_STATUS_REGISTERED;
import static com.uber.cadence.api.v1.EncodingType.ENCODING_TYPE_INVALID;
import static com.uber.cadence.api.v1.EncodingType.ENCODING_TYPE_JSON;
import static com.uber.cadence.api.v1.EncodingType.ENCODING_TYPE_THRIFTRW;
import static com.uber.cadence.api.v1.IndexedValueType.INDEXED_VALUE_TYPE_BOOL;
import static com.uber.cadence.api.v1.IndexedValueType.INDEXED_VALUE_TYPE_DATETIME;
import static com.uber.cadence.api.v1.IndexedValueType.INDEXED_VALUE_TYPE_DOUBLE;
import static com.uber.cadence.api.v1.IndexedValueType.INDEXED_VALUE_TYPE_INT;
import static com.uber.cadence.api.v1.IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD;
import static com.uber.cadence.api.v1.IndexedValueType.INDEXED_VALUE_TYPE_STRING;
import static com.uber.cadence.api.v1.PendingActivityState.PENDING_ACTIVITY_STATE_CANCEL_REQUESTED;
import static com.uber.cadence.api.v1.PendingActivityState.PENDING_ACTIVITY_STATE_INVALID;
import static com.uber.cadence.api.v1.PendingActivityState.PENDING_ACTIVITY_STATE_SCHEDULED;
import static com.uber.cadence.api.v1.PendingActivityState.PENDING_ACTIVITY_STATE_STARTED;
import static com.uber.cadence.api.v1.PendingDecisionState.PENDING_DECISION_STATE_INVALID;
import static com.uber.cadence.api.v1.PendingDecisionState.PENDING_DECISION_STATE_SCHEDULED;
import static com.uber.cadence.api.v1.PendingDecisionState.PENDING_DECISION_STATE_STARTED;
import static com.uber.cadence.api.v1.QueryResultType.QUERY_RESULT_TYPE_ANSWERED;
import static com.uber.cadence.api.v1.QueryResultType.QUERY_RESULT_TYPE_FAILED;
import static com.uber.cadence.api.v1.QueryResultType.QUERY_RESULT_TYPE_INVALID;
import static com.uber.cadence.api.v1.SignalExternalWorkflowExecutionFailedCause.SIGNAL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_CAUSE_INVALID;
import static com.uber.cadence.api.v1.SignalExternalWorkflowExecutionFailedCause.SIGNAL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_CAUSE_UNKNOWN_EXTERNAL_WORKFLOW_EXECUTION;
import static com.uber.cadence.api.v1.TimeoutType.TIMEOUT_TYPE_HEARTBEAT;
import static com.uber.cadence.api.v1.TimeoutType.TIMEOUT_TYPE_SCHEDULE_TO_CLOSE;
import static com.uber.cadence.api.v1.TimeoutType.TIMEOUT_TYPE_SCHEDULE_TO_START;
import static com.uber.cadence.api.v1.TimeoutType.TIMEOUT_TYPE_START_TO_CLOSE;

import com.uber.cadence.api.v1.ArchivalStatus;
import com.uber.cadence.api.v1.CancelExternalWorkflowExecutionFailedCause;
import com.uber.cadence.api.v1.ChildWorkflowExecutionFailedCause;
import com.uber.cadence.api.v1.ContinueAsNewInitiator;
import com.uber.cadence.api.v1.DecisionTaskFailedCause;
import com.uber.cadence.api.v1.DecisionTaskTimedOutCause;
import com.uber.cadence.api.v1.DomainStatus;
import com.uber.cadence.api.v1.EncodingType;
import com.uber.cadence.api.v1.EventFilterType;
import com.uber.cadence.api.v1.IndexedValueType;
import com.uber.cadence.api.v1.ParentClosePolicy;
import com.uber.cadence.api.v1.PendingActivityState;
import com.uber.cadence.api.v1.PendingDecisionState;
import com.uber.cadence.api.v1.QueryConsistencyLevel;
import com.uber.cadence.api.v1.QueryRejectCondition;
import com.uber.cadence.api.v1.QueryResultType;
import com.uber.cadence.api.v1.SignalExternalWorkflowExecutionFailedCause;
import com.uber.cadence.api.v1.TaskListKind;
import com.uber.cadence.api.v1.TaskListType;
import com.uber.cadence.api.v1.TimeoutType;
import com.uber.cadence.api.v1.WorkflowExecutionCloseStatus;
import com.uber.cadence.api.v1.WorkflowIdReusePolicy;

class EnumMapper {

  static DecisionTaskTimedOutCause decisionTaskTimedOutCause(
      com.uber.cadence.DecisionTaskTimedOutCause t) {
    if (t == null) {
      return DECISION_TASK_TIMED_OUT_CAUSE_INVALID;
    }
    switch (t) {
      case TIMEOUT:
        return DECISION_TASK_TIMED_OUT_CAUSE_TIMEOUT;
      case RESET:
        return DECISION_TASK_TIMED_OUT_CAUSE_RESET;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  static TaskListKind taskListKind(com.uber.cadence.TaskListKind t) {
    if (t == null) {
      return TaskListKind.TASK_LIST_KIND_INVALID;
    }
    switch (t) {
      case NORMAL:
        return TaskListKind.TASK_LIST_KIND_NORMAL;
      case STICKY:
        return TaskListKind.TASK_LIST_KIND_STICKY;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  static TaskListType taskListType(com.uber.cadence.TaskListType t) {
    if (t == null) {
      return TaskListType.TASK_LIST_TYPE_INVALID;
    }
    switch (t) {
      case Decision:
        return TaskListType.TASK_LIST_TYPE_DECISION;
      case Activity:
        return TaskListType.TASK_LIST_TYPE_ACTIVITY;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  static EventFilterType eventFilterType(com.uber.cadence.HistoryEventFilterType t) {
    if (t == null) {
      return EventFilterType.EVENT_FILTER_TYPE_INVALID;
    }
    switch (t) {
      case ALL_EVENT:
        return EventFilterType.EVENT_FILTER_TYPE_ALL_EVENT;
      case CLOSE_EVENT:
        return EventFilterType.EVENT_FILTER_TYPE_CLOSE_EVENT;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  static QueryRejectCondition queryRejectCondition(com.uber.cadence.QueryRejectCondition t) {
    if (t == null) {
      return QueryRejectCondition.QUERY_REJECT_CONDITION_INVALID;
    }
    switch (t) {
      case NOT_OPEN:
        return QueryRejectCondition.QUERY_REJECT_CONDITION_NOT_OPEN;
      case NOT_COMPLETED_CLEANLY:
        return QueryRejectCondition.QUERY_REJECT_CONDITION_NOT_COMPLETED_CLEANLY;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  static QueryConsistencyLevel queryConsistencyLevel(com.uber.cadence.QueryConsistencyLevel t) {
    if (t == null) {
      return QueryConsistencyLevel.QUERY_CONSISTENCY_LEVEL_INVALID;
    }
    switch (t) {
      case EVENTUAL:
        return QueryConsistencyLevel.QUERY_CONSISTENCY_LEVEL_EVENTUAL;
      case STRONG:
        return QueryConsistencyLevel.QUERY_CONSISTENCY_LEVEL_STRONG;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  static ContinueAsNewInitiator continueAsNewInitiator(com.uber.cadence.ContinueAsNewInitiator t) {
    if (t == null) {
      return ContinueAsNewInitiator.CONTINUE_AS_NEW_INITIATOR_INVALID;
    }
    switch (t) {
      case Decider:
        return ContinueAsNewInitiator.CONTINUE_AS_NEW_INITIATOR_DECIDER;
      case RetryPolicy:
        return ContinueAsNewInitiator.CONTINUE_AS_NEW_INITIATOR_RETRY_POLICY;
      case CronSchedule:
        return ContinueAsNewInitiator.CONTINUE_AS_NEW_INITIATOR_CRON_SCHEDULE;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  static WorkflowIdReusePolicy workflowIdReusePolicy(com.uber.cadence.WorkflowIdReusePolicy t) {
    if (t == null) {
      return WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_INVALID;
    }
    switch (t) {
      case AllowDuplicateFailedOnly:
        return WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE_FAILED_ONLY;
      case AllowDuplicate:
        return WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE;
      case RejectDuplicate:
        return WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE;
      case TerminateIfRunning:
        return WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_TERMINATE_IF_RUNNING;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  static QueryResultType queryResultType(com.uber.cadence.QueryResultType t) {
    if (t == null) {
      return QUERY_RESULT_TYPE_INVALID;
    }
    switch (t) {
      case ANSWERED:
        return QUERY_RESULT_TYPE_ANSWERED;
      case FAILED:
        return QUERY_RESULT_TYPE_FAILED;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  static ArchivalStatus archivalStatus(com.uber.cadence.ArchivalStatus t) {
    if (t == null) {
      return ArchivalStatus.ARCHIVAL_STATUS_INVALID;
    }
    switch (t) {
      case DISABLED:
        return ArchivalStatus.ARCHIVAL_STATUS_DISABLED;
      case ENABLED:
        return ArchivalStatus.ARCHIVAL_STATUS_ENABLED;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  static ParentClosePolicy parentClosePolicy(com.uber.cadence.ParentClosePolicy t) {
    if (t == null) {
      return ParentClosePolicy.PARENT_CLOSE_POLICY_INVALID;
    }
    switch (t) {
      case ABANDON:
        return ParentClosePolicy.PARENT_CLOSE_POLICY_ABANDON;
      case REQUEST_CANCEL:
        return ParentClosePolicy.PARENT_CLOSE_POLICY_REQUEST_CANCEL;
      case TERMINATE:
        return ParentClosePolicy.PARENT_CLOSE_POLICY_TERMINATE;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  static DecisionTaskFailedCause decisionTaskFailedCause(
      com.uber.cadence.DecisionTaskFailedCause t) {
    if (t == null) {
      return DECISION_TASK_FAILED_CAUSE_INVALID;
    }
    switch (t) {
      case UNHANDLED_DECISION:
        return DECISION_TASK_FAILED_CAUSE_UNHANDLED_DECISION;
      case BAD_SCHEDULE_ACTIVITY_ATTRIBUTES:
        return DECISION_TASK_FAILED_CAUSE_BAD_SCHEDULE_ACTIVITY_ATTRIBUTES;
      case BAD_REQUEST_CANCEL_ACTIVITY_ATTRIBUTES:
        return DECISION_TASK_FAILED_CAUSE_BAD_REQUEST_CANCEL_ACTIVITY_ATTRIBUTES;
      case BAD_START_TIMER_ATTRIBUTES:
        return DECISION_TASK_FAILED_CAUSE_BAD_START_TIMER_ATTRIBUTES;
      case BAD_CANCEL_TIMER_ATTRIBUTES:
        return DECISION_TASK_FAILED_CAUSE_BAD_CANCEL_TIMER_ATTRIBUTES;
      case BAD_RECORD_MARKER_ATTRIBUTES:
        return DECISION_TASK_FAILED_CAUSE_BAD_RECORD_MARKER_ATTRIBUTES;
      case BAD_COMPLETE_WORKFLOW_EXECUTION_ATTRIBUTES:
        return DECISION_TASK_FAILED_CAUSE_BAD_COMPLETE_WORKFLOW_EXECUTION_ATTRIBUTES;
      case BAD_FAIL_WORKFLOW_EXECUTION_ATTRIBUTES:
        return DECISION_TASK_FAILED_CAUSE_BAD_FAIL_WORKFLOW_EXECUTION_ATTRIBUTES;
      case BAD_CANCEL_WORKFLOW_EXECUTION_ATTRIBUTES:
        return DECISION_TASK_FAILED_CAUSE_BAD_CANCEL_WORKFLOW_EXECUTION_ATTRIBUTES;
      case BAD_REQUEST_CANCEL_EXTERNAL_WORKFLOW_EXECUTION_ATTRIBUTES:
        return DECISION_TASK_FAILED_CAUSE_BAD_REQUEST_CANCEL_EXTERNAL_WORKFLOW_EXECUTION_ATTRIBUTES;
      case BAD_CONTINUE_AS_NEW_ATTRIBUTES:
        return DECISION_TASK_FAILED_CAUSE_BAD_CONTINUE_AS_NEW_ATTRIBUTES;
      case START_TIMER_DUPLICATE_ID:
        return DECISION_TASK_FAILED_CAUSE_START_TIMER_DUPLICATE_ID;
      case RESET_STICKY_TASKLIST:
        return DECISION_TASK_FAILED_CAUSE_RESET_STICKY_TASK_LIST;
      case WORKFLOW_WORKER_UNHANDLED_FAILURE:
        return DECISION_TASK_FAILED_CAUSE_WORKFLOW_WORKER_UNHANDLED_FAILURE;
      case BAD_SIGNAL_WORKFLOW_EXECUTION_ATTRIBUTES:
        return DECISION_TASK_FAILED_CAUSE_BAD_SIGNAL_WORKFLOW_EXECUTION_ATTRIBUTES;
      case BAD_START_CHILD_EXECUTION_ATTRIBUTES:
        return DECISION_TASK_FAILED_CAUSE_BAD_START_CHILD_EXECUTION_ATTRIBUTES;
      case FORCE_CLOSE_DECISION:
        return DECISION_TASK_FAILED_CAUSE_FORCE_CLOSE_DECISION;
      case FAILOVER_CLOSE_DECISION:
        return DECISION_TASK_FAILED_CAUSE_FAILOVER_CLOSE_DECISION;
      case BAD_SIGNAL_INPUT_SIZE:
        return DECISION_TASK_FAILED_CAUSE_BAD_SIGNAL_INPUT_SIZE;
      case RESET_WORKFLOW:
        return DECISION_TASK_FAILED_CAUSE_RESET_WORKFLOW;
      case BAD_BINARY:
        return DECISION_TASK_FAILED_CAUSE_BAD_BINARY;
      case SCHEDULE_ACTIVITY_DUPLICATE_ID:
        return DECISION_TASK_FAILED_CAUSE_SCHEDULE_ACTIVITY_DUPLICATE_ID;
      case BAD_SEARCH_ATTRIBUTES:
        return DECISION_TASK_FAILED_CAUSE_BAD_SEARCH_ATTRIBUTES;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  static WorkflowExecutionCloseStatus workflowExecutionCloseStatus(
      com.uber.cadence.WorkflowExecutionCloseStatus t) {
    if (t == null) {
      return WorkflowExecutionCloseStatus.WORKFLOW_EXECUTION_CLOSE_STATUS_INVALID;
    }
    switch (t) {
      case COMPLETED:
        return WorkflowExecutionCloseStatus.WORKFLOW_EXECUTION_CLOSE_STATUS_COMPLETED;
      case FAILED:
        return WorkflowExecutionCloseStatus.WORKFLOW_EXECUTION_CLOSE_STATUS_FAILED;
      case CANCELED:
        return WorkflowExecutionCloseStatus.WORKFLOW_EXECUTION_CLOSE_STATUS_CANCELED;
      case TERMINATED:
        return WorkflowExecutionCloseStatus.WORKFLOW_EXECUTION_CLOSE_STATUS_TERMINATED;
      case CONTINUED_AS_NEW:
        return WorkflowExecutionCloseStatus.WORKFLOW_EXECUTION_CLOSE_STATUS_CONTINUED_AS_NEW;
      case TIMED_OUT:
        return WorkflowExecutionCloseStatus.WORKFLOW_EXECUTION_CLOSE_STATUS_TIMED_OUT;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  static QueryResultType queryTaskCompletedType(com.uber.cadence.QueryTaskCompletedType t) {
    if (t == null) {
      return QUERY_RESULT_TYPE_INVALID;
    }
    switch (t) {
      case COMPLETED:
        return QUERY_RESULT_TYPE_ANSWERED;
      case FAILED:
        return QUERY_RESULT_TYPE_FAILED;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  static DomainStatus domainStatus(com.uber.cadence.DomainStatus t) {
    if (t == null) {
      return DOMAIN_STATUS_INVALID;
    }
    switch (t) {
      case REGISTERED:
        return DOMAIN_STATUS_REGISTERED;
      case DEPRECATED:
        return DOMAIN_STATUS_DEPRECATED;
      case DELETED:
        return DOMAIN_STATUS_DELETED;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  static PendingActivityState pendingActivityState(com.uber.cadence.PendingActivityState t) {
    if (t == null) {
      return PENDING_ACTIVITY_STATE_INVALID;
    }
    switch (t) {
      case SCHEDULED:
        return PENDING_ACTIVITY_STATE_SCHEDULED;
      case STARTED:
        return PENDING_ACTIVITY_STATE_STARTED;
      case CANCEL_REQUESTED:
        return PENDING_ACTIVITY_STATE_CANCEL_REQUESTED;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  static PendingDecisionState pendingDecisionState(com.uber.cadence.PendingDecisionState t) {
    if (t == null) {
      return PENDING_DECISION_STATE_INVALID;
    }
    switch (t) {
      case SCHEDULED:
        return PENDING_DECISION_STATE_SCHEDULED;
      case STARTED:
        return PENDING_DECISION_STATE_STARTED;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  static IndexedValueType indexedValueType(com.uber.cadence.IndexedValueType t) {
    switch (t) {
      case STRING:
        return INDEXED_VALUE_TYPE_STRING;
      case KEYWORD:
        return INDEXED_VALUE_TYPE_KEYWORD;
      case INT:
        return INDEXED_VALUE_TYPE_INT;
      case DOUBLE:
        return INDEXED_VALUE_TYPE_DOUBLE;
      case BOOL:
        return INDEXED_VALUE_TYPE_BOOL;
      case DATETIME:
        return INDEXED_VALUE_TYPE_DATETIME;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  static EncodingType encodingType(com.uber.cadence.EncodingType t) {
    if (t == null) {
      return ENCODING_TYPE_INVALID;
    }
    switch (t) {
      case ThriftRW:
        return ENCODING_TYPE_THRIFTRW;
      case JSON:
        return ENCODING_TYPE_JSON;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  static TimeoutType timeoutType(com.uber.cadence.TimeoutType t) {
    if (t == null) {
      return TimeoutType.TIMEOUT_TYPE_INVALID;
    }
    switch (t) {
      case START_TO_CLOSE:
        return TIMEOUT_TYPE_START_TO_CLOSE;
      case SCHEDULE_TO_START:
        return TIMEOUT_TYPE_SCHEDULE_TO_START;
      case SCHEDULE_TO_CLOSE:
        return TIMEOUT_TYPE_SCHEDULE_TO_CLOSE;
      case HEARTBEAT:
        return TIMEOUT_TYPE_HEARTBEAT;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  static CancelExternalWorkflowExecutionFailedCause cancelExternalWorkflowExecutionFailedCause(
      com.uber.cadence.CancelExternalWorkflowExecutionFailedCause t) {
    if (t == null) {
      return CANCEL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_CAUSE_INVALID;
    }
    switch (t) {
      case UNKNOWN_EXTERNAL_WORKFLOW_EXECUTION:
        return CANCEL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_CAUSE_UNKNOWN_EXTERNAL_WORKFLOW_EXECUTION;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  static SignalExternalWorkflowExecutionFailedCause signalExternalWorkflowExecutionFailedCause(
      com.uber.cadence.SignalExternalWorkflowExecutionFailedCause t) {
    if (t == null) {
      return SIGNAL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_CAUSE_INVALID;
    }
    switch (t) {
      case UNKNOWN_EXTERNAL_WORKFLOW_EXECUTION:
        return SIGNAL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_CAUSE_UNKNOWN_EXTERNAL_WORKFLOW_EXECUTION;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  static ChildWorkflowExecutionFailedCause childWorkflowExecutionFailedCause(
      com.uber.cadence.ChildWorkflowExecutionFailedCause t) {
    if (t == null) {
      return CHILD_WORKFLOW_EXECUTION_FAILED_CAUSE_INVALID;
    }
    switch (t) {
      case WORKFLOW_ALREADY_RUNNING:
        return CHILD_WORKFLOW_EXECUTION_FAILED_CAUSE_WORKFLOW_ALREADY_RUNNING;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }
}
