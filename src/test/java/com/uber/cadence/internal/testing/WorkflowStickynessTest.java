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

package com.uber.cadence.internal.testing;

import com.uber.cadence.EventType;
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.PollForDecisionTaskResponse;
import com.uber.cadence.internal.testservice.TestWorkflowService;
import com.uber.cadence.testUtils.TestServiceUtils;
import org.junit.Test;

import java.time.Duration;
import java.util.List;

import static com.uber.cadence.internal.common.InternalUtils.createNormalTaskList;
import static com.uber.cadence.internal.common.InternalUtils.createStickyTaskList;
import static org.junit.Assert.assertEquals;

public class WorkflowStickynessTest {

  @Test
  public void taskCompletionWithStickyExecutionAttributesWillScheduleDecisionsOnStickyTaskList()
      throws Exception {

    TestWorkflowService service = new TestWorkflowService();
    service.lockTimeSkipping("HistoryUtils");

    TestServiceUtils.startWorkflowExecution("domain", "tasklist", "wfType", service);
    PollForDecisionTaskResponse response =
        TestServiceUtils.pollForDecisionTask("domain", createNormalTaskList("tasklist"), service);

    TestServiceUtils.respondDecisionTaskCompletedWithSticky(
        response.taskToken, "tasklist-sticky", service);
    TestServiceUtils.signalWorkflow(response.workflowExecution, "domain", service);
    response =
        TestServiceUtils.pollForDecisionTask(
            "domain", createStickyTaskList("tasklist-sticky"), service);

    assertEquals(4, response.history.getEventsSize());
    List<HistoryEvent> events = response.history.getEvents();
    assertEquals(EventType.DecisionTaskCompleted, events.get(0).eventType);
    assertEquals(EventType.WorkflowExecutionSignaled, events.get(1).eventType);
    assertEquals(EventType.DecisionTaskScheduled, events.get(2).eventType);
    assertEquals(EventType.DecisionTaskStarted, events.get(3).eventType);
  }

  @Test
  public void taskFailureWillRescheduleTheTaskOnTheGlobalList() throws Exception {

    TestWorkflowService service = new TestWorkflowService();
    service.lockTimeSkipping("HistoryUtils");

    TestServiceUtils.startWorkflowExecution("domain", "tasklist", "wfType", service);
    PollForDecisionTaskResponse response =
        TestServiceUtils.pollForDecisionTask("domain", createNormalTaskList("tasklist"), service);

    TestServiceUtils.respondDecisionTaskCompletedWithSticky(
        response.taskToken, "tasklist-sticky", service);
    TestServiceUtils.signalWorkflow(response.workflowExecution, "domain", service);
    response =
        TestServiceUtils.pollForDecisionTask(
            "domain", createStickyTaskList("tasklist-sticky"), service);
    TestServiceUtils.respondDecisionTaskFailedWithSticky(response.taskToken, service);
    response =
        TestServiceUtils.pollForDecisionTask("domain", createStickyTaskList("tasklist"), service);

    assertEquals(10, response.history.getEventsSize());
  }

  @Test
  public void taskTimeoutWillRescheduleTheTaskOnTheGlobalList() throws Exception {

    TestWorkflowService service = new TestWorkflowService();
    service.lockTimeSkipping("HistoryUtils");
    TestServiceUtils.startWorkflowExecution("domain", "tasklist", "wfType", 10, 2, service);
    PollForDecisionTaskResponse response =
        TestServiceUtils.pollForDecisionTask("domain", createNormalTaskList("tasklist"), service);

    TestServiceUtils.respondDecisionTaskCompletedWithSticky(
        response.taskToken, "tasklist-sticky", 1, service);
    TestServiceUtils.signalWorkflow(response.workflowExecution, "domain", service);
    TestServiceUtils.pollForDecisionTask(
        "domain", createStickyTaskList("tasklist-sticky"), service);
    service.unlockTimeSkipping("HistoryUtils");
    service.sleep(Duration.ofMillis(1100));

    response =
        TestServiceUtils.pollForDecisionTask("domain", createStickyTaskList("tasklist"), service);

    assertEquals(10, response.history.getEventsSize());
  }
}
