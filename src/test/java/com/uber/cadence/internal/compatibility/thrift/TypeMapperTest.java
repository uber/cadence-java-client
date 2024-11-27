/*
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
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

import static com.uber.cadence.internal.compatibility.thrift.Helpers.byteStringToArray;
import static org.junit.Assert.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.uber.cadence.internal.compatibility.ProtoObjects;
import com.uber.cadence.internal.compatibility.ThriftObjects;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TypeMapperTest<T, P> {

  @Parameterized.Parameter(0)
  public String testName;

  @Parameterized.Parameter(1)
  public T from;

  @Parameterized.Parameter(2)
  public P to;

  @Parameterized.Parameter(3)
  public Function<T, P> via;

  @Test
  public void testMapper() {
    P actual = via.apply(from);
    if (actual instanceof byte[] && to instanceof byte[]) {
      // Handle the byte[] comparison
      assertArrayEquals((byte[]) to, (byte[]) actual);
    } else {
      // Handle all other types
      assertEquals(to, actual);
    }
  }

  @Test
  public void testHandlesNull() {
    P actual = via.apply(null);

    if (actual instanceof List<?>) {
      assertTrue(
          "Mapper functions returning a list should return an empty list",
          ((List<?>) actual).isEmpty());
    } else if (actual instanceof Map<?, ?>) {
      assertTrue(
          "Mapper functions returning a map should return an empty map",
          ((Map<?, ?>) actual).isEmpty());
    } else if (actual instanceof Long) {
      assertEquals("For long we expect -1", -1L, actual);
    } else {
      assertNull("Mapper functions should accept null, returning null", actual);
    }
  }

  @Parameterized.Parameters(name = "{0}")
  public static Iterable<Object[]> cases() {
    return Arrays.asList(
        testCase(
            ProtoObjects.BAD_BINARY_INFO, ThriftObjects.BAD_BINARY_INFO, TypeMapper::badBinaryInfo),
        testCase(ProtoObjects.FAILURE, "reason", TypeMapper::failureReason),
        testCase(ProtoObjects.DATA_BLOB, ThriftObjects.DATA_BLOB, TypeMapper::dataBlob),
        testCase(
            ProtoObjects.EXTERNAL_WORKFLOW_EXECUTION_INFO,
            ThriftObjects.EXTERNAL_WORKFLOW_EXECUTION,
            TypeMapper::externalWorkflowExecution),
        testCase(
            ProtoObjects.FAILURE,
            byteStringToArray(ProtoObjects.FAILURE.getDetails()),
            TypeMapper::failureDetails),
        testCase(ProtoObjects.ACTIVITY_TYPE, ThriftObjects.ACTIVITY_TYPE, TypeMapper::activityType),
        testCase(ProtoObjects.WORKFLOW_TYPE, ThriftObjects.WORKFLOW_TYPE, TypeMapper::workflowType),
        testCase(ProtoObjects.RESET_POINTS, ThriftObjects.RESET_POINTS, TypeMapper::resetPoints),
        testCase(
            ProtoObjects.RESET_POINT_INFO,
            ThriftObjects.RESET_POINT_INFO,
            TypeMapper::resetPointInfo),
        testCase(ProtoObjects.POLLER_INFO, ThriftObjects.POLLER_INFO, TypeMapper::pollerInfo),
        testCase(
            Collections.singletonList(ProtoObjects.POLLER_INFO),
            Collections.singletonList(ThriftObjects.POLLER_INFO),
            TypeMapper::pollerInfoArray),
        testCase(
            ProtoObjects.SUPPORTED_CLIENT_VERSIONS,
            ThriftObjects.SUPPORTED_CLIENT_VERSIONS,
            TypeMapper::supportedClientVersions),
        testCase(
            ProtoObjects.TASK_LIST_STATUS,
            ThriftObjects.TASK_LIST_STATUS,
            TypeMapper::taskListStatus),
        testCase(
            ProtoObjects.WORKFLOW_EXECUTION,
            ThriftObjects.WORKFLOW_EXECUTION,
            TypeMapper::workflowExecution),
        testCase(ProtoObjects.WORKFLOW_EXECUTION, "workflowId", TypeMapper::workflowId),
        testCase(ProtoObjects.WORKFLOW_EXECUTION, "runId", TypeMapper::runId),
        testCase(
            ProtoObjects.WORKFLOW_EXECUTION_INFO,
            ThriftObjects.WORKFLOW_EXECUTION_INFO,
            TypeMapper::workflowExecutionInfo),
        testCase(
            Collections.singletonList(ProtoObjects.WORKFLOW_EXECUTION_INFO),
            Collections.singletonList(ThriftObjects.WORKFLOW_EXECUTION_INFO),
            TypeMapper::workflowExecutionInfoArray),
        testCase(
            ProtoObjects.INDEXED_VALUES,
            ThriftObjects.INDEXED_VALUES,
            TypeMapper::indexedValueTypeMap),
        testCase(
            ProtoObjects.WORKFLOW_EXECUTION_INFO.getParentExecutionInfo(),
            "parentDomainId",
            TypeMapper::parentDomainId),
        testCase(
            ProtoObjects.WORKFLOW_EXECUTION_INFO.getParentExecutionInfo(),
            "parentDomainName",
            TypeMapper::parentDomainName),
        testCase(
            ProtoObjects.WORKFLOW_EXECUTION_INFO.getParentExecutionInfo(),
            1L,
            TypeMapper::parentInitiatedId),
        testCase(
            ProtoObjects.WORKFLOW_EXECUTION_INFO.getParentExecutionInfo(),
            ThriftObjects.PARENT_WORKFLOW_EXECUTION,
            TypeMapper::parentWorkflowExecution),
        testCase(
            Collections.singletonList(ProtoObjects.PENDING_CHILD_EXECUTION_INFO),
            Collections.singletonList(ThriftObjects.PENDING_CHILD_EXECUTION_INFO),
            TypeMapper::pendingChildExecutionInfoArray),
        testCase(
            Collections.singletonList(ProtoObjects.PENDING_ACTIVITY_INFO),
            Collections.singletonList(ThriftObjects.PENDING_ACTIVITY_INFO),
            TypeMapper::pendingActivityInfoArray),
        testCase(
            Collections.singletonList(ProtoObjects.RESET_POINT_INFO),
            Collections.singletonList(ThriftObjects.RESET_POINT_INFO),
            TypeMapper::resetPointInfoArray),
        testCase(ProtoObjects.TASK_LIST, ThriftObjects.TASK_LIST, TypeMapper::taskList),
        testCase(
            ProtoObjects.TASK_LIST_METADATA,
            ThriftObjects.TASK_LIST_METADATA,
            TypeMapper::taskListMetadata),
        testCase(ProtoObjects.RETRY_POLICY, ThriftObjects.RETRY_POLICY, TypeMapper::retryPolicy),
        testCase(ProtoObjects.HEADER, ThriftObjects.HEADER, TypeMapper::header),
        testCase(ProtoObjects.MEMO, ThriftObjects.MEMO, TypeMapper::memo),
        testCase(
            ProtoObjects.SEARCH_ATTRIBUTES,
            ThriftObjects.SEARCH_ATTRIBUTES,
            TypeMapper::searchAttributes),
        testCase(ProtoObjects.BAD_BINARIES, ThriftObjects.BAD_BINARIES, TypeMapper::badBinaries),
        testCase(
            ProtoObjects.CLUSTER_REPLICATION_CONFIGURATION,
            ThriftObjects.CLUSTER_REPLICATION_CONFIGURATION,
            TypeMapper::clusterReplicationConfiguration),
        testCase(
            ProtoObjects.WORKFLOW_QUERY, ThriftObjects.WORKFLOW_QUERY, TypeMapper::workflowQuery),
        testCase(
            ImmutableMap.of("key", ProtoObjects.BAD_BINARY_INFO),
            ImmutableMap.of("key", ThriftObjects.BAD_BINARY_INFO),
            TypeMapper::badBinaryInfoMap),
        testCase(
            ImmutableList.of(ProtoObjects.CLUSTER_REPLICATION_CONFIGURATION),
            ImmutableList.of(ThriftObjects.CLUSTER_REPLICATION_CONFIGURATION),
            TypeMapper::clusterReplicationConfigurationArray),
        testCase(
            ImmutableMap.of("key", ProtoObjects.WORKFLOW_QUERY),
            ImmutableMap.of("key", ThriftObjects.WORKFLOW_QUERY),
            TypeMapper::workflowQueryMap),
        testCase(
            ImmutableMap.of("key", ProtoObjects.ACTIVITY_LOCAL_DISPATCH_INFO),
            ImmutableMap.of("key", ThriftObjects.ACTIVITY_LOCAL_DISPATCH_INFO),
            TypeMapper::activityLocalDispatchInfoMap),
        testCase(
            Collections.singletonList(ProtoObjects.DATA_BLOB),
            Collections.singletonList(ThriftObjects.DATA_BLOB),
            TypeMapper::dataBlobArray),
        testCase(
            ProtoObjects.DOMAIN,
            ThriftObjects.DESCRIBE_DOMAIN_RESPONSE,
            TypeMapper::describeDomainResponseDomain),
        testCase(
            Collections.singletonList(ProtoObjects.DOMAIN),
            Collections.singletonList(ThriftObjects.DESCRIBE_DOMAIN_RESPONSE),
            TypeMapper::describeDomainResponseArray),
        testCase(
            Collections.singletonList(ProtoObjects.TASK_LIST_PARTITION_METADATA),
            Collections.singletonList(ThriftObjects.TASK_LIST_PARTITION_METADATA),
            TypeMapper::taskListPartitionMetadataArray));
  }

  private static <T, P> Object[] testCase(T from, P to, Function<T, P> via) {
    return new Object[] {from.getClass().getSimpleName(), from, to, via};
  }
}
