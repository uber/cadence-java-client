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

package com.uber.cadence.internal.compatibility.proto;

import static com.uber.cadence.internal.compatibility.MapperTestUtil.assertMissingFields;
import static com.uber.cadence.internal.compatibility.MapperTestUtil.assertNoMissingFields;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;
import com.uber.cadence.SignalWithStartWorkflowExecutionRequest;
import com.uber.cadence.api.v1.Header;
import com.uber.cadence.api.v1.Memo;
import com.uber.cadence.api.v1.Payload;
import com.uber.cadence.api.v1.RetryPolicy;
import com.uber.cadence.api.v1.SearchAttributes;
import com.uber.cadence.api.v1.SignalWithStartWorkflowExecutionAsyncRequest;
import com.uber.cadence.api.v1.StartWorkflowExecutionAsyncRequest;
import com.uber.cadence.api.v1.StartWorkflowExecutionRequest;
import com.uber.cadence.api.v1.TaskList;
import com.uber.cadence.api.v1.TaskListKind;
import com.uber.cadence.api.v1.WorkflowIdReusePolicy;
import com.uber.cadence.api.v1.WorkflowType;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class RequestMapperTest {

  // These are shared between testStartWorkflowExecutionRequest and
  // testStartWorkflowExecutionAsyncRequest
  // because the mapper throws an exception if necessary fields are missing
  private static final com.uber.cadence.StartWorkflowExecutionRequest
      THRIFT_START_WORKFLOW_EXECUTION =
          new com.uber.cadence.StartWorkflowExecutionRequest()
              .setDomain("domain")
              .setWorkflowId("workflowId")
              .setWorkflowType(new com.uber.cadence.WorkflowType().setName("workflowType"))
              .setTaskList(
                  new com.uber.cadence.TaskList()
                      .setName("taskList")
                      .setKind(com.uber.cadence.TaskListKind.NORMAL))
              .setInput("input".getBytes(StandardCharsets.UTF_8))
              .setExecutionStartToCloseTimeoutSeconds(1)
              .setTaskStartToCloseTimeoutSeconds(2)
              .setIdentity("identity")
              .setRequestId("requestId")
              .setWorkflowIdReusePolicy(com.uber.cadence.WorkflowIdReusePolicy.AllowDuplicate)
              .setRetryPolicy(
                  new com.uber.cadence.RetryPolicy()
                      .setInitialIntervalInSeconds(11)
                      .setBackoffCoefficient(0.5)
                      .setMaximumIntervalInSeconds(12)
                      .setMaximumAttempts(13)
                      .setNonRetriableErrorReasons(ImmutableList.of("error"))
                      .setExpirationIntervalInSeconds(14))
              .setCronSchedule("cronSchedule")
              .setMemo(
                  new com.uber.cadence.Memo().setFields(ImmutableMap.of("memo", utf8("memoValue"))))
              .setSearchAttributes(
                  new com.uber.cadence.SearchAttributes()
                      .setIndexedFields(ImmutableMap.of("search", utf8("searchValue"))))
              .setHeader(
                  new com.uber.cadence.Header().setFields(ImmutableMap.of("key", utf8("value"))))
              .setDelayStartSeconds(3);
  private static final com.uber.cadence.api.v1.StartWorkflowExecutionRequest
      PROTO_START_WORKFLOW_EXECUTION =
          StartWorkflowExecutionRequest.newBuilder()
              .setDomain("domain")
              .setWorkflowId("workflowId")
              .setWorkflowType(WorkflowType.newBuilder().setName("workflowType"))
              .setTaskList(
                  TaskList.newBuilder()
                      .setName("taskList")
                      .setKind(TaskListKind.TASK_LIST_KIND_NORMAL))
              .setInput(protoPayload("input"))
              .setExecutionStartToCloseTimeout(seconds(1))
              .setTaskStartToCloseTimeout(seconds(2))
              .setIdentity("identity")
              .setRequestId("requestId")
              .setWorkflowIdReusePolicy(
                  WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE)
              .setRetryPolicy(
                  RetryPolicy.newBuilder()
                      .setInitialInterval(seconds(11))
                      .setBackoffCoefficient(0.5)
                      .setMaximumInterval(seconds(12))
                      .setMaximumAttempts(13)
                      .addNonRetryableErrorReasons("error")
                      .setExpirationInterval(seconds(14)))
              .setCronSchedule("cronSchedule")
              .setMemo(Memo.newBuilder().putFields("memo", protoPayload("memoValue")).build())
              .setSearchAttributes(
                  SearchAttributes.newBuilder()
                      .putIndexedFields("search", protoPayload("searchValue"))
                      .build())
              .setHeader(Header.newBuilder().putFields("key", protoPayload("value")).build())
              .setDelayStart(seconds(3))
              .build();
  private static final com.uber.cadence.SignalWithStartWorkflowExecutionRequest
      THRIFT_SIGNAL_WITH_START_WORKFLOW_EXECUTION =
          new SignalWithStartWorkflowExecutionRequest()
              .setDomain("domain")
              .setWorkflowId("workflowId")
              .setWorkflowType(new com.uber.cadence.WorkflowType().setName("workflowType"))
              .setTaskList(
                  new com.uber.cadence.TaskList()
                      .setName("taskList")
                      .setKind(com.uber.cadence.TaskListKind.NORMAL))
              .setInput("input".getBytes(StandardCharsets.UTF_8))
              .setExecutionStartToCloseTimeoutSeconds(1)
              .setTaskStartToCloseTimeoutSeconds(2)
              .setIdentity("identity")
              .setRequestId("requestId")
              .setWorkflowIdReusePolicy(com.uber.cadence.WorkflowIdReusePolicy.AllowDuplicate)
              .setSignalName("signalName")
              .setSignalInput("signalInput".getBytes(StandardCharsets.UTF_8))
              .setControl("control".getBytes(StandardCharsets.UTF_8))
              .setRetryPolicy(
                  new com.uber.cadence.RetryPolicy()
                      .setInitialIntervalInSeconds(11)
                      .setBackoffCoefficient(0.5)
                      .setMaximumIntervalInSeconds(12)
                      .setMaximumAttempts(13)
                      .setNonRetriableErrorReasons(ImmutableList.of("error"))
                      .setExpirationIntervalInSeconds(14))
              .setCronSchedule("cronSchedule")
              .setMemo(
                  new com.uber.cadence.Memo().setFields(ImmutableMap.of("memo", utf8("memoValue"))))
              .setSearchAttributes(
                  new com.uber.cadence.SearchAttributes()
                      .setIndexedFields(ImmutableMap.of("search", utf8("searchValue"))))
              .setHeader(
                  new com.uber.cadence.Header().setFields(ImmutableMap.of("key", utf8("value"))))
              .setDelayStartSeconds(3);
  private static final com.uber.cadence.api.v1.SignalWithStartWorkflowExecutionRequest
      PROTO_SIGNAL_WITH_START_WORKFLOW_EXECUTION =
          com.uber.cadence.api.v1.SignalWithStartWorkflowExecutionRequest.newBuilder()
              .setStartRequest(PROTO_START_WORKFLOW_EXECUTION)
              .setSignalInput(protoPayload("signalInput"))
              .setSignalName("signalName")
              .setControl(ByteString.copyFromUtf8("control"))
              .build();

  @Test
  public void testStartWorkflowExecutionRequest() {
    // Pulling in new IDL will intentionally cause this to fail. Either update the mapper or account
    // for it here
    assertMissingFields(
        THRIFT_START_WORKFLOW_EXECUTION,
        com.uber.cadence.StartWorkflowExecutionRequest._Fields.class,
        "jitterStartSeconds");

    assertEquals(
        PROTO_START_WORKFLOW_EXECUTION,
        RequestMapper.startWorkflowExecutionRequest(THRIFT_START_WORKFLOW_EXECUTION));
  }

  @Test
  public void testStartWorkflowExecutionAsyncRequest() {
    com.uber.cadence.StartWorkflowExecutionAsyncRequest thrift =
        new com.uber.cadence.StartWorkflowExecutionAsyncRequest()
            .setRequest(THRIFT_START_WORKFLOW_EXECUTION);

    com.uber.cadence.api.v1.StartWorkflowExecutionAsyncRequest expected =
        StartWorkflowExecutionAsyncRequest.newBuilder()
            .setRequest(PROTO_START_WORKFLOW_EXECUTION)
            .build();

    assertNoMissingFields(
        thrift, com.uber.cadence.StartWorkflowExecutionAsyncRequest._Fields.class);

    assertEquals(expected, RequestMapper.startWorkflowExecutionAsyncRequest(thrift));
  }

  @Test
  public void testSignalWithStartWorkflowExecutionRequest() {
    assertMissingFields(
        THRIFT_SIGNAL_WITH_START_WORKFLOW_EXECUTION,
        com.uber.cadence.SignalWithStartWorkflowExecutionRequest._Fields.class,
        "jitterStartSeconds");

    assertEquals(
        PROTO_SIGNAL_WITH_START_WORKFLOW_EXECUTION,
        RequestMapper.signalWithStartWorkflowExecutionRequest(
            THRIFT_SIGNAL_WITH_START_WORKFLOW_EXECUTION));
  }

  @Test
  public void testSignalWithStartWorkflowExecutionAsyncRequest() {
    com.uber.cadence.SignalWithStartWorkflowExecutionAsyncRequest thrift =
        new com.uber.cadence.SignalWithStartWorkflowExecutionAsyncRequest()
            .setRequest(THRIFT_SIGNAL_WITH_START_WORKFLOW_EXECUTION);

    com.uber.cadence.api.v1.SignalWithStartWorkflowExecutionAsyncRequest expected =
        SignalWithStartWorkflowExecutionAsyncRequest.newBuilder()
            .setRequest(PROTO_SIGNAL_WITH_START_WORKFLOW_EXECUTION)
            .build();

    assertNoMissingFields(
        thrift, com.uber.cadence.SignalWithStartWorkflowExecutionAsyncRequest._Fields.class);

    assertEquals(expected, RequestMapper.signalWithStartWorkflowExecutionAsyncRequest(thrift));
  }

  private static Duration seconds(int value) {
    return Duration.newBuilder().setSeconds(value).build();
  }

  private static Payload protoPayload(String value) {
    return Payload.newBuilder().setData(ByteString.copyFromUtf8(value)).build();
  }

  private static ByteBuffer utf8(String value) {
    return ByteBuffer.wrap(utf8Bytes(value));
  }

  private static byte[] utf8Bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }
}
