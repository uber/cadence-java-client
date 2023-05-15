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

package com.uber.cadence.migration;

import static org.junit.Assert.*;

import com.uber.cadence.*;
import com.uber.cadence.client.*;
import com.uber.cadence.internal.sync.SyncWorkflowDefinition;
import com.uber.cadence.testing.TestWorkflowEnvironment;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.workflow.TestEnvironmentWorkflowTest;
import com.uber.cadence.workflow.WorkflowInterceptor;
import java.time.Duration;
import org.apache.thrift.TException;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class MigrationInterceptorTest {

  static final String TASK_LIST = "HelloMigration";
  static final String CRON_WORKFLOW_ID = "cron_workflow";
  private final SyncWorkflowDefinition syncWorkflowDefinition = null;
  private final WorkflowInterceptor.WorkflowExecuteInput input = null;

  @Rule
  public TestWatcher watchman =
      new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
          if (testEnv != null) {
            System.err.println(testEnv.getDiagnostics());
            testEnv.close();
          }
        }
      };

  private TestWorkflowEnvironment testEnv;
  private TestWorkflowEnvironment testEnvInNew;
  private Worker worker;
  private WorkflowClient workflowClient;
  private WorkflowClient workflowClientInNew;

  @Before
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    testEnvInNew = TestWorkflowEnvironment.newInstance();
    workflowClient = testEnv.newWorkflowClient();
    workflowClientInNew = testEnvInNew.newWorkflowClient();

    worker =
        testEnv.newWorker(
            TASK_LIST,
            builder ->
                builder.setInterceptorFactory(
                    next -> new MigrationInterceptor(next, workflowClientInNew)));
    worker.registerWorkflowImplementationTypes(
        SampleWorkflow.GreetingWorkflowImpl.class, SampleWorkflow.GreetingChildImpl.class);
    testEnv.start();
    testEnvInNew.start();
  }

  @After
  public void tearDown() {
    testEnv.close();
    testEnvInNew.close();
  }

  @Test
  public void testWorkflowWithoutCron() {

    WorkflowOptions workflowOptions =
        new WorkflowOptions.Builder()
            .setExecutionStartToCloseTimeout(Duration.ofSeconds(10))
            .setTaskList(TASK_LIST)
            .build();

    SampleWorkflow.GreetingWorkflow workflow =
        workflowClient.newWorkflowStub(SampleWorkflow.GreetingWorkflow.class, workflowOptions);
    String res = workflow.getGreeting("Migration");
    assertEquals("Hello Migration!", res);
  }

  @Test
  public void testWorkflowWithCron() throws TException {
    // Get a workflow stub using the same task list the worker uses.
    TestEnvironmentWorkflowTest.CronW workflow =
        workflowClient.newWorkflowStub(TestEnvironmentWorkflowTest.CronW.class);
    WorkflowExecution execution = WorkflowClient.start(workflow::cron);
    assertEquals(CRON_WORKFLOW_ID, execution.getWorkflowId());

    testEnv.sleep(Duration.ofMinutes(2));
    ListClosedWorkflowExecutionsRequest request =
        new ListClosedWorkflowExecutionsRequest()
            .setDomain(testEnv.getDomain())
            .setExecutionFilter(new WorkflowExecutionFilter().setWorkflowId(CRON_WORKFLOW_ID));
    try {
      ListClosedWorkflowExecutionsResponse listResponse =
          testEnv.getWorkflowService().ListClosedWorkflowExecutions(request);
      Assert.assertEquals(1, listResponse.getExecutions().size());
      for (WorkflowExecutionInfo e : listResponse.getExecutions()) {
        assertTrue(e.isIsCron());
        assertEquals(WorkflowExecutionCloseStatus.TIMED_OUT, e.getCloseStatus());
      }
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }
}
