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

package com.uber.cadence.testUtils;

import static com.uber.cadence.internal.common.InternalUtils.createNormalTaskList;
import static com.uber.cadence.internal.common.InternalUtils.createStickyTaskList;

import com.uber.cadence.*;
import com.uber.cadence.internal.testservice.TestWorkflowService;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

public class HistoryUtils {
  private HistoryUtils() {}

  public static PollForDecisionTaskResponse generateDecisionTaskWithInitialHistory()
      throws Exception {
    return generateDecisionTaskWithInitialHistory(
        "domain", "taskList", "workflowType", new TestWorkflowService());
  }

  public static PollForDecisionTaskResponse generateDecisionTaskWithInitialHistory(
      String domain, String tasklistName, String workflowType, TestWorkflowService service)
      throws Exception {
    service.lockTimeSkipping("test");
    startWorkflowExecution(domain, tasklistName, workflowType, service);
    return pollForDecisionTask(domain, createNormalTaskList(tasklistName), service);
  }

  public static PollForDecisionTaskResponse generateDecisionTaskWithPartialHistory()
      throws Exception {
    return generateDecisionTaskWithPartialHistory("domain", "taskList", "workflowType");
  }

  public static PollForDecisionTaskResponse generateDecisionTaskWithPartialHistory(
      String domain, String tasklistName, String workflowType) throws Exception {

    TestWorkflowService service = new TestWorkflowService();
    service.lockTimeSkipping("HistoryUtils");

    PollForDecisionTaskResponse response =
        generateDecisionTaskWithInitialHistory(domain, tasklistName, workflowType, service);
    return generateDecisionTaskWithPartialHistoryFromExistingTask(response, domain, service);
  }

  public static PollForDecisionTaskResponse generateDecisionTaskWithPartialHistoryFromExistingTask(
      PollForDecisionTaskResponse response, TestWorkflowService service) throws Exception {

    return generateDecisionTaskWithPartialHistoryFromExistingTask(response, "domain", service);
  }

  public static PollForDecisionTaskResponse generateDecisionTaskWithPartialHistoryFromExistingTask(
      PollForDecisionTaskResponse response, String domain, TestWorkflowService service)
      throws Exception {
    service.lockTimeSkipping("HistoryUtils");
    respondDecisionTaskCompletedWithSticky(
        response.taskToken, response.getWorkflowExecutionTaskList().name, service);
    signalWorkflow(response.workflowExecution, domain, service);
    return pollForDecisionTask(
        domain,
        createStickyTaskList(ToStickyName(response.getWorkflowExecutionTaskList().name)),
        service);
  }

  private static void startWorkflowExecution(
      String domain, String tasklistName, String workflowType, TestWorkflowService service)
      throws Exception {
    StartWorkflowExecutionRequest request = new StartWorkflowExecutionRequest();
    request.domain = domain;
    request.workflowId = UUID.randomUUID().toString();
    request.taskList = createNormalTaskList(tasklistName);
    request.setExecutionStartToCloseTimeoutSeconds(10000);
    request.setTaskStartToCloseTimeoutSeconds(10000);
    WorkflowType type = new WorkflowType();
    type.name = workflowType;
    request.workflowType = type;
    service.StartWorkflowExecution(request);
  }

  private static void respondDecisionTaskCompletedWithSticky(
      ByteBuffer taskToken, String tasklistName, TestWorkflowService service) throws Exception {
    RespondDecisionTaskCompletedRequest request = new RespondDecisionTaskCompletedRequest();
    StickyExecutionAttributes attributes = new StickyExecutionAttributes();
    attributes.setWorkerTaskList(createStickyTaskList(ToStickyName(tasklistName)));
    attributes.setScheduleToStartTimeoutSeconds(10000);
    request.setStickyAttributes(attributes);
    request.setTaskToken(taskToken);
    request.setDecisions(new ArrayList<>());
    service.RespondDecisionTaskCompleted(request);
  }

  private static String ToStickyName(String tasklistName) {
    return tasklistName + "Sticky";
  }

  private static void signalWorkflow(
      WorkflowExecution workflowExecution, String domain, TestWorkflowService service)
      throws Exception {
    SignalWorkflowExecutionRequest signalRequest = new SignalWorkflowExecutionRequest();
    signalRequest.setDomain(domain);
    signalRequest.setSignalName("my-signal");
    signalRequest.setWorkflowExecution(workflowExecution);
    service.SignalWorkflowExecution(signalRequest);
  }

  private static PollForDecisionTaskResponse pollForDecisionTask(
      String domain, TaskList tasklist, TestWorkflowService service) throws Exception {
    PollForDecisionTaskRequest request = new PollForDecisionTaskRequest();
    request.setDomain(domain);
    request.setTaskList(tasklist);
    return service.PollForDecisionTask(request);
  }
}
