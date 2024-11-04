/**
 * Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * <p>Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. A copy of the License is located at
 *
 * <p>http://aws.amazon.com/apache2.0
 *
 * <p>or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.uber.cadence.workflow;

import com.uber.cadence.testing.WorkflowReplayer;
import org.junit.Ignore;
import org.junit.Test;

public class WorkflowReplayTest {

  // Server doesn't guarantee that the timer fire timestamp is larger or equal of the
  // expected fire time. This test ensures that client still fires timer in this case.
  @Test
  public void testTimerFiringTimestampEarlierThanExpected() throws Exception {
    WorkflowReplayer.replayWorkflowExecutionFromResource(
        "timerfiring.json", WorkflowTest.TimerFiringWorkflowImpl.class);
  }

  @Test
  public void testWorkflowReset() throws Exception {
    // Leave the following code to generate history.
    //    startWorkerFor(TestWorkflowResetReplayWorkflow.class, TestMultiargsWorkflowsImpl.class);
    //    TestWorkflow1 workflowStub =
    //        workflowClient.newWorkflowStub(
    //            TestWorkflow1.class, newWorkflowOptionsBuilder(taskList).build());
    //    workflowStub.execute(taskList);
    //
    //    try {
    //      Thread.sleep(60000000);
    //    } catch (InterruptedException e) {
    //      e.printStackTrace();
    //    }

    WorkflowReplayer.replayWorkflowExecutionFromResource(
        "resetWorkflowHistory.json", WorkflowTest.TestWorkflowResetReplayWorkflow.class);
  }

  @Test
  public void testGetVersionWithRetryReplay() throws Exception {

    WorkflowReplayer.replayWorkflowExecutionFromResource(
        "testGetVersionWithRetryHistory.json", WorkflowTest.TestGetVersionWorkflowRetryImpl.class);
  }

  @Test
  public void testGetVersionRemoveAndAdd() throws Exception {
    WorkflowReplayer.replayWorkflowExecutionFromResource(
        "testGetVersionHistory.json", WorkflowTest.TestGetVersionRemoveAndAddImpl.class);
  }

  /**
   * Tests that history that was created before server side retry was supported is backwards
   * compatible with the client that supports the server side retry.
   */
  @Test
  public void testAsyncActivityRetryReplay() throws Exception {
    WorkflowReplayer.replayWorkflowExecutionFromResource(
        "testAsyncActivityRetryHistory.json", WorkflowTest.TestAsyncActivityRetry.class);
  }

  /**
   * Tests that history created before marker header change is backwards compatible with old markers
   * generated without headers.
   */
  @Test
  // This test previously had a check for the incorrect test name and never ran. The json doesn't
  // parse.
  // Keeping it around in case we decide to fix it.
  @Ignore
  public void testMutableSideEffectReplay() throws Exception {
    WorkflowReplayer.replayWorkflowExecutionFromResource(
        "testMutableSideEffectBackwardCompatibility.json",
        WorkflowTest.TestMutableSideEffectWorkflowImpl.class);
  }

  @Test
  public void testGetVersionRemoved() throws Exception {
    WorkflowReplayer.replayWorkflowExecutionFromResource(
        "testGetVersionHistory.json", WorkflowTest.TestGetVersionRemovedImpl.class);
  }

  @Test
  public void testGetVersionAdded() throws Exception {
    WorkflowReplayer.replayWorkflowExecutionFromResource(
        "testGetVersionHistory.json", WorkflowTest.TestGetVersionAddedImpl.class);
  }

  @Test
  public void testGetVersionAddedWithCadenceChangeVersion() throws Exception {
    WorkflowReplayer.replayWorkflowExecutionFromResource(
        "testGetVersionHistoryWithCadenceChangeVersion.json",
        WorkflowTest.TestGetVersionAddedImpl.class);
  }

  /**
   * Tests that history that was created before server side retry was supported is backwards
   * compatible with the client that supports the server side retry.
   */
  @Test
  public void testChildWorkflowRetryReplay() throws Exception {
    WorkflowReplayer.replayWorkflowExecutionFromResource(
        "testChildWorkflowRetryHistory.json", WorkflowTest.TestChildWorkflowRetryWorkflow.class);
  }
}
