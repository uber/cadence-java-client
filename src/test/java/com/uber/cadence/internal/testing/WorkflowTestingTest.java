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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowException;
import com.uber.cadence.testing.TestEnvironment;
import com.uber.cadence.testing.TestWorkflowEnvironment;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.workflow.Workflow;
import com.uber.cadence.workflow.WorkflowMethod;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class WorkflowTestingTest {

  @Rule
  public TestWatcher watchman= new TestWatcher() {
    @Override
    protected void failed(Throwable e, Description description) {
      System.err.println(testEnvironment.getDiagnostics());
    }
  };

  private static final String TASK_LIST = "test-workflow";

  private static TestEnvironment testEnvironment;

  @BeforeClass
  public static void setUp() {
    testEnvironment = TestEnvironment.newInstance();
  }

  public interface TestWorkflow {

    @WorkflowMethod(executionStartToCloseTimeoutSeconds = 10, taskList = TASK_LIST)
    String workflow1(String input);
  }

  public static class WorkflowImpl implements TestWorkflow {

    @Override
    public String workflow1(String input) {
      return Workflow.getWorkflowInfo().getWorkflowType() + "-" + input;
    }
  }

  @Test
  public void testSuccess() {
    TestWorkflowEnvironment env = testEnvironment.workflowEnvironment();
    Worker worker = env.newWorker(TASK_LIST);
    worker.registerWorkflowImplementationTypes(WorkflowImpl.class);
    worker.start();
    WorkflowClient client = env.newWorkflowClient();
    TestWorkflow activity = client.newWorkflowStub(TestWorkflow.class);
    String result = activity.workflow1("input1");
    assertEquals("TestWorkflow::workflow1-input1", result);
  }

  public static class FailingWorkflowImpl implements TestWorkflow {

    @Override
    public String workflow1(String input) {
      throw new IllegalThreadStateException(Workflow.getWorkflowInfo().getWorkflowType() + "-"
          + input);
    }
  }

  @Test
  public void testFailure() {
    TestWorkflowEnvironment env = testEnvironment.workflowEnvironment();
    Worker worker = env.newWorker(TASK_LIST);
    worker.registerWorkflowImplementationTypes(FailingWorkflowImpl.class);
    worker.start();
    WorkflowClient client = env.newWorkflowClient();
    TestWorkflow activity = client.newWorkflowStub(TestWorkflow.class);

    String result = null;
    try {
      result = activity.workflow1("input1");
      fail("unreacheable");
    } catch (WorkflowException e) {
      assertEquals("TestWorkflow::workflow1-input1", e.getCause().getMessage());
    }
  }

  //  private static class AngryWorkflowImpl implements TestWorkflow {
  //
  //    @Override
  //    public String workflow1(String input) {
  //      throw Activity.wrap(new IOException("simulated"));
  //    }
  //  }
  //
  //  @Test
  //  public void testFailure() {
  //    TestActivityEnvironment env = testEnvironment.activityEnvironment();
  //    env.registerActivitiesImplementations(new AngryWorkflowImpl());
  //    TestWorkflow activity = env.newActivityStub(TestWorkflow.class);
  //    try {
  //      activity.workflow1("input1");
  //      fail("unreachable");
  //    } catch (ActivityFailureException e) {
  //      assertTrue(e.getMessage().contains("TestWorkflow::workflow1"));
  //      assertTrue(e.getCause() instanceof IOException);
  //      assertEquals("simulated", e.getCause().getMessage());
  //    }
  //  }
  //
  //  private static class HeartbeatWorkflowImpl implements TestWorkflow {
  //
  //    @Override
  //    public String workflow1(String input) {
  //      Activity.heartbeat("details1");
  //      return input;
  //    }
  //  }
  //
  //  @Test
  //  public void testHeartbeat() {
  //    TestActivityEnvironment env = testEnvironment.activityEnvironment();
  //    env.registerActivitiesImplementations(new HeartbeatWorkflowImpl());
  //    AtomicReference<String> details = new AtomicReference<>();
  //    env.setActivityHeartbeatListener(
  //        String.class,
  //        (d) -> {
  //          details.set(d);
  //        });
  //    TestWorkflow activity = env.newActivityStub(TestWorkflow.class);
  //    String result = activity.workflow1("input1");
  //    assertEquals("input1", result);
  //    assertEquals("details1", details.get());
  //  }
}
