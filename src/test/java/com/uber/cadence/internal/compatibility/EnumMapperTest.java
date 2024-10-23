package com.uber.cadence.internal.compatibility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.uber.cadence.*;
import com.uber.cadence.api.v1.EventFilterType;
import com.uber.cadence.internal.compatibility.proto.EnumMapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.apache.thrift.TEnum;
import org.junit.Test;

public class EnumMapperTest {

  @Test
  public void testTaskListKind() {
    assertAllValuesRoundTrip(
        TaskListKind.class,
        com.uber.cadence.api.v1.TaskListKind.TASK_LIST_KIND_INVALID,
        EnumMapper::taskListKind,
        com.uber.cadence.internal.compatibility.thrift.EnumMapper::taskListKind);
  }

  @Test
  public void testQueryRejectionCondition() {
    assertAllValuesRoundTrip(
        QueryRejectCondition.class,
        com.uber.cadence.api.v1.QueryRejectCondition.QUERY_REJECT_CONDITION_INVALID,
        EnumMapper::queryRejectCondition,
        com.uber.cadence.internal.compatibility.thrift.EnumMapper::queryRejectCondition);
  }

  @Test
  public void testContinueAsNewInitiator() {
    assertAllValuesRoundTrip(
        ContinueAsNewInitiator.class,
        com.uber.cadence.api.v1.ContinueAsNewInitiator.CONTINUE_AS_NEW_INITIATOR_INVALID,
        EnumMapper::continueAsNewInitiator,
        com.uber.cadence.internal.compatibility.thrift.EnumMapper::continueAsNewInitiator);
  }

  @Test
  public void testWorkflowIdReusePolicy() {
    assertAllValuesRoundTrip(
        WorkflowIdReusePolicy.class,
        com.uber.cadence.api.v1.WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_INVALID,
        EnumMapper::workflowIdReusePolicy,
        com.uber.cadence.internal.compatibility.thrift.EnumMapper::workflowIdReusePolicy);
  }

  @Test
  public void testArchivalStatus() {
    assertAllValuesRoundTrip(
        ArchivalStatus.class,
        com.uber.cadence.api.v1.ArchivalStatus.ARCHIVAL_STATUS_INVALID,
        EnumMapper::archivalStatus,
        com.uber.cadence.internal.compatibility.thrift.EnumMapper::archivalStatus);
  }

  @Test
  public void testParentClosePolicy() {
    assertAllValuesRoundTrip(
        ParentClosePolicy.class,
        com.uber.cadence.api.v1.ParentClosePolicy.PARENT_CLOSE_POLICY_INVALID,
        EnumMapper::parentClosePolicy,
        com.uber.cadence.internal.compatibility.thrift.EnumMapper::parentClosePolicy);
  }

  @Test
  public void testDecisionTaskFailedCause() {
    assertAllValuesRoundTrip(
        DecisionTaskFailedCause.class,
        com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_INVALID,
        EnumMapper::decisionTaskFailedCause,
        com.uber.cadence.internal.compatibility.thrift.EnumMapper::decisionTaskFailedCause);
  }

  @Test
  public void testWorkflowExecutionCloseStatus() {
    assertAllValuesRoundTrip(
        WorkflowExecutionCloseStatus.class,
        com.uber.cadence.api.v1.WorkflowExecutionCloseStatus
            .WORKFLOW_EXECUTION_CLOSE_STATUS_INVALID,
        EnumMapper::workflowExecutionCloseStatus,
        com.uber.cadence.internal.compatibility.thrift.EnumMapper::workflowExecutionCloseStatus);
  }

  @Test
  public void testTaskListType() {
    assertMapping(
        thriftToProtoIdentical(
            TaskListType.class,
            com.uber.cadence.api.v1.TaskListType.class,
            com.uber.cadence.api.v1.TaskListType.TASK_LIST_TYPE_INVALID),
        EnumMapper::taskListType);
  }

  @Test
  public void testEventFilterType() {
    assertMapping(
        thriftToProtoIdentical(
            HistoryEventFilterType.class,
            EventFilterType.class,
            EventFilterType.EVENT_FILTER_TYPE_INVALID),
        EnumMapper::eventFilterType);
  }

  @Test
  public void testQueryConsistencyLevel() {
    assertMapping(
        thriftToProtoIdentical(
            QueryConsistencyLevel.class,
            com.uber.cadence.api.v1.QueryConsistencyLevel.class,
            com.uber.cadence.api.v1.QueryConsistencyLevel.QUERY_CONSISTENCY_LEVEL_INVALID),
        EnumMapper::queryConsistencyLevel);
  }

  @Test
  public void testQueryResultType() {
    assertMapping(
        thriftToProtoIdentical(
            QueryResultType.class,
            com.uber.cadence.api.v1.QueryResultType.class,
            com.uber.cadence.api.v1.QueryResultType.QUERY_RESULT_TYPE_INVALID),
        EnumMapper::queryResultType);
  }

  @Test
  public void testQueryTaskCompletedType() {
    Map<QueryTaskCompletedType, com.uber.cadence.api.v1.QueryResultType> mapping =
        ImmutableMap.of(
            QueryTaskCompletedType.COMPLETED,
            com.uber.cadence.api.v1.QueryResultType.QUERY_RESULT_TYPE_ANSWERED,
            QueryTaskCompletedType.FAILED,
            com.uber.cadence.api.v1.QueryResultType.QUERY_RESULT_TYPE_FAILED);
    assertAllValuesPresent(QueryTaskCompletedType.class, mapping);
    assertMapping(mapping, EnumMapper::queryTaskCompletedType);
    // ImmutableMap doesn't accept null
    assertEquals(
        com.uber.cadence.api.v1.QueryResultType.QUERY_RESULT_TYPE_INVALID,
        EnumMapper.queryTaskCompletedType(null));
  }

  @Test
  public void testDomainStatus() {
    assertMapping(
        protoToThriftIdentical(
            com.uber.cadence.api.v1.DomainStatus.class,
            DomainStatus.class,
            com.uber.cadence.api.v1.DomainStatus.DOMAIN_STATUS_INVALID),
        com.uber.cadence.internal.compatibility.thrift.EnumMapper::domainStatus);
  }

  @Test
  public void testPendingActivityState() {
    assertMapping(
        protoToThriftIdentical(
            com.uber.cadence.api.v1.PendingActivityState.class,
            PendingActivityState.class,
            com.uber.cadence.api.v1.PendingActivityState.PENDING_ACTIVITY_STATE_INVALID),
        com.uber.cadence.internal.compatibility.thrift.EnumMapper::pendingActivityState);
  }

  @Test
  public void testPendingDecisionState() {
    assertMapping(
        protoToThriftIdentical(
            com.uber.cadence.api.v1.PendingDecisionState.class,
            PendingDecisionState.class,
            com.uber.cadence.api.v1.PendingDecisionState.PENDING_DECISION_STATE_INVALID),
        com.uber.cadence.internal.compatibility.thrift.EnumMapper::pendingDecisionState);
  }

  @Test
  public void testIndexedValueType() {
    Map<com.uber.cadence.api.v1.IndexedValueType, IndexedValueType> mapping =
        protoToThriftIdentical(
            com.uber.cadence.api.v1.IndexedValueType.class,
            IndexedValueType.class,
            com.uber.cadence.api.v1.IndexedValueType.INDEXED_VALUE_TYPE_INVALID);
    // This mapper uniquely throws when encountering this value
    mapping.remove(com.uber.cadence.api.v1.IndexedValueType.INDEXED_VALUE_TYPE_INVALID);
    assertMapping(
        mapping, com.uber.cadence.internal.compatibility.thrift.EnumMapper::indexedValueType);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            com.uber.cadence.internal.compatibility.thrift.EnumMapper.indexedValueType(
                com.uber.cadence.api.v1.IndexedValueType.INDEXED_VALUE_TYPE_INVALID));
  }

  @Test
  public void testEncodingType() {
    Map<com.uber.cadence.api.v1.EncodingType, EncodingType> mapping =
        ImmutableMap.of(
            com.uber.cadence.api.v1.EncodingType.ENCODING_TYPE_THRIFTRW,
            EncodingType.ThriftRW,
            com.uber.cadence.api.v1.EncodingType.ENCODING_TYPE_JSON,
            EncodingType.JSON);

    assertAllValuesPresent(
        com.uber.cadence.api.v1.EncodingType.class,
        mapping,
        com.uber.cadence.api.v1.EncodingType.ENCODING_TYPE_INVALID,
        com.uber.cadence.api.v1.EncodingType.ENCODING_TYPE_PROTO3,
        com.uber.cadence.api.v1.EncodingType.UNRECOGNIZED);

    assertMapping(mapping, com.uber.cadence.internal.compatibility.thrift.EnumMapper::encodingType);
    // ImmutableMap doesn't accept null
    assertNull(
        com.uber.cadence.internal.compatibility.thrift.EnumMapper.encodingType(
            com.uber.cadence.api.v1.EncodingType.ENCODING_TYPE_INVALID));
    // No thrift equivalent
    assertThrows(
        UnsupportedOperationException.class,
        () ->
            com.uber.cadence.internal.compatibility.thrift.EnumMapper.encodingType(
                com.uber.cadence.api.v1.EncodingType.ENCODING_TYPE_PROTO3));
  }

  @Test
  public void testTimeoutType() {
    assertMapping(
        protoToThriftIdentical(
            com.uber.cadence.api.v1.TimeoutType.class,
            TimeoutType.class,
            com.uber.cadence.api.v1.TimeoutType.TIMEOUT_TYPE_INVALID),
        com.uber.cadence.internal.compatibility.thrift.EnumMapper::timeoutType);
  }

  @Test
  public void testDecisionTaskTimedOutCause() {
    assertMapping(
        protoToThriftIdentical(
            com.uber.cadence.api.v1.DecisionTaskTimedOutCause.class,
            DecisionTaskTimedOutCause.class,
            com.uber.cadence.api.v1.DecisionTaskTimedOutCause
                .DECISION_TASK_TIMED_OUT_CAUSE_INVALID),
        com.uber.cadence.internal.compatibility.thrift.EnumMapper::decisionTaskTimedOutCause);
  }

  @Test
  public void testCancelExternalWorkflowExecutionFailedCause() {
    assertMapping(
        protoToThriftIdentical(
            com.uber.cadence.api.v1.CancelExternalWorkflowExecutionFailedCause.class,
            CancelExternalWorkflowExecutionFailedCause.class,
            com.uber.cadence.api.v1.CancelExternalWorkflowExecutionFailedCause
                .CANCEL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_CAUSE_INVALID),
        com.uber.cadence.internal.compatibility.thrift.EnumMapper
            ::cancelExternalWorkflowExecutionFailedCause);
  }

  @Test
  public void testSignalExternalWorkflowExecutionFailedCause() {

    assertMapping(
        protoToThriftIdentical(
            com.uber.cadence.api.v1.SignalExternalWorkflowExecutionFailedCause.class,
            SignalExternalWorkflowExecutionFailedCause.class,
            com.uber.cadence.api.v1.SignalExternalWorkflowExecutionFailedCause
                .SIGNAL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_CAUSE_INVALID),
        com.uber.cadence.internal.compatibility.thrift.EnumMapper
            ::signalExternalWorkflowExecutionFailedCause);
  }

  @Test
  public void testchildWorkflowExecutionFailedCause() {
    assertMapping(
        protoToThriftIdentical(
            com.uber.cadence.api.v1.ChildWorkflowExecutionFailedCause.class,
            ChildWorkflowExecutionFailedCause.class,
            com.uber.cadence.api.v1.ChildWorkflowExecutionFailedCause
                .CHILD_WORKFLOW_EXECUTION_FAILED_CAUSE_INVALID),
        com.uber.cadence.internal.compatibility.thrift.EnumMapper
            ::childWorkflowExecutionFailedCause);
  }

  private static <F extends Enum<F>, T extends Enum<T>> void assertAllValuesRoundTrip(
      Class<F> fromType, T invalidType, Function<F, T> to, Function<T, F> inverse) {
    F[] values = fromType.getEnumConstants();
    for (F fromValue : values) {
      F result = inverse.apply(to.apply(fromValue));
      assertEquals("Round tripping " + fromValue.toString(), fromValue, result);
    }
    assertEquals("null -> invalid", invalidType, to.apply(null));
    assertNull("invalid -> null", inverse.apply(invalidType));
  }

  private static <E extends Enum<E>> void assertAllValuesPresent(
      Class<E> type, Map<E, ?> mapping, E... except) {
    Set<E> exclusions = ImmutableSet.copyOf(except);
    for (E value : type.getEnumConstants()) {
      if (!exclusions.contains(value)) {
        assertTrue("Missing mapping for " + value, mapping.containsKey(value));
      }
    }
  }

  private static <F extends Enum<F>, T extends Enum<T>> void assertMapping(
      Map<F, T> mapping, Function<F, T> mapper) {
    for (Map.Entry<F, T> entry : mapping.entrySet()) {
      F from = entry.getKey();
      T actual = mapper.apply(from);
      T expected = entry.getValue();
      assertEquals("Mapping " + from, expected, actual);
    }
  }

  private static <F extends Enum<F>, T extends Enum<T> & TEnum> Map<F, T> protoToThriftIdentical(
      Class<F> from, Class<T> to, F invalid) {
    // There are more clever and succinct ways to do this but most Map types don't accept null as a
    // key
    Map<F, T> result = new HashMap<>();
    Map<T, F> inverse = thriftToProtoIdentical(to, from, invalid);
    for (Map.Entry<T, F> entry : inverse.entrySet()) {
      result.put(entry.getValue(), entry.getKey());
    }
    return result;
  }

  private static <F extends Enum<F> & TEnum, T extends Enum<T>> Map<F, T> thriftToProtoIdentical(
      Class<F> from, Class<T> to, T invalid) {
    Map<String, T> toByName =
        Arrays.stream(to.getEnumConstants())
            .collect(ImmutableMap.toImmutableMap(Enum::name, x -> x));
    Map<F, T> cases = new HashMap<>();
    for (F fromValue : from.getEnumConstants()) {
      String protoName = getProtoNameFor(to, fromValue);
      T expected = toByName.get(protoName);
      Preconditions.checkState(
          expected != null,
          "Failed to find an equivalent for %s in %s with name %s",
          fromValue,
          to,
          protoName);
      cases.put(fromValue, expected);
    }
    cases.put(null, invalid);
    return cases;
  }

  private static String getProtoNameFor(Class<?> protoType, Enum<?> value) {
    // TaskListType.Decision -> TASK_LIST_TYPE_DECISION
    // EventFilterType.ALL_EVENT -> EVENT_FILTER_TYPE_ALL_EVENT
    String typePart =
        CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, protoType.getSimpleName());
    String valuePart = value.name();
    // Some Thrift enums use UPPER_CAMEL, some use UPPER_UNDERSCORE
    if (!value.name().toUpperCase().equals(value.name())) {
      valuePart = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, valuePart);
    }
    return typePart + "_" + valuePart;
  }
}
