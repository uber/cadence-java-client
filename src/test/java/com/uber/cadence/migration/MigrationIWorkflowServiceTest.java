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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;

import com.uber.cadence.*;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.testing.TestWorkflowEnvironment;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.workflow.WorkflowMethod;
import java.util.stream.Stream;
import org.apache.thrift.TException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MigrationIWorkflowServiceTest {

  static final String TASK_LIST = "tasklist";
  static final String WORKFLOW_NAME = "SimpleWF";
  private TestWorkflowEnvironment testEnvNew;
  private Worker workerNew;
  private WorkflowClient clientNew;

  private TestWorkflowEnvironment testEnvOld;
  private Worker workerOld;
  private WorkflowClient clientOld;

  private MigrationIWorkflowService migrationService;

  public interface SimpleW {
    @WorkflowMethod(
      name = WORKFLOW_NAME,
      executionStartToCloseTimeoutSeconds = 10,
      taskList = TASK_LIST
    )
    void run();
  }

  public static class SimpleWImpl implements SimpleW {
    @Override
    public void run() {}
  }

  @Before
  public void setUp() {
    testEnvNew = TestWorkflowEnvironment.newInstance();
    clientNew = testEnvNew.newWorkflowClient();
    workerNew = testEnvNew.newWorker(TASK_LIST);
    workerNew.registerWorkflowImplementationTypes(SimpleWImpl.class);

    testEnvOld = TestWorkflowEnvironment.newInstance();
    clientOld = testEnvOld.newWorkflowClient();
    workerOld = testEnvOld.newWorker(TASK_LIST);
    workerOld.registerWorkflowImplementationTypes(SimpleWImpl.class);

    testEnvNew.start();
    testEnvOld.start();

    migrationService =
        new MigrationIWorkflowService(
            testEnvOld.getWorkflowService(),
            testEnvOld.getDomain(),
            testEnvNew.getWorkflowService(),
            testEnvNew.getDomain());

    // start and run some workflows in both domains
    try {
      runSyncWF(clientNew, "wf-completed-only-in-new");
      runSyncWF(clientOld, "wf-completed-only-in-old");
      runSyncWF(clientNew, "wf-completed-in-both");
      runSyncWF(clientOld, "wf-completed-in-both");
    } catch (Exception e) {
      fail("no exception excepted in starting workflows:" + e.getMessage());
    }
  }

  @After
  public void tearDown() {
    testEnvNew.close();
    testEnvOld.close();
  }

  @Test
  public void testStartWorkflowExecutionWithNewWorkflow() {
    try {
      StartWorkflowExecutionResponse response =
          migrationService.StartWorkflowExecution(
              new StartWorkflowExecutionRequest()
                  .setRequestId("requestID")
                  .setDomain(testEnvOld.getDomain())
                  .setTaskList(new TaskList().setName(TASK_LIST))
                  .setWorkflowType(new WorkflowType().setName(WORKFLOW_NAME))
                  .setWorkflowId("wf-new")
                  .setExecutionStartToCloseTimeoutSeconds(1)
                  .setTaskStartToCloseTimeoutSeconds(10));
      assertNumOfExecutions(testEnvNew, "wf-new", 1);
      assertNumOfExecutions(testEnvOld, "wf-new", 0);
    } catch (TException e) {
      fail("should not throw error on start workflow" + e.getMessage());
    }
  }

  private void runSyncWF(WorkflowClient wc, String workflowID) {
    wc.newWorkflowStub(
            SimpleW.class,
            new WorkflowOptions.Builder().setTaskList(TASK_LIST).setWorkflowId(workflowID).build())
        .run();
  }

  private void assertNumOfExecutions(
      TestWorkflowEnvironment env, String workflowID, int expectRuns) {
    try {
      ListClosedWorkflowExecutionsResponse responseClose =
          env.getWorkflowService()
              .ListClosedWorkflowExecutions(
                  new ListClosedWorkflowExecutionsRequest()
                      .setDomain(env.getDomain())
                      .setExecutionFilter(new WorkflowExecutionFilter().setWorkflowId(workflowID)));
      ListOpenWorkflowExecutionsResponse responseOpen =
          env.getWorkflowService()
              .ListOpenWorkflowExecutions(
                  new ListOpenWorkflowExecutionsRequest()
                      .setDomain(env.getDomain())
                      .setExecutionFilter(new WorkflowExecutionFilter().setWorkflowId(workflowID)));
      long totalSize =
          Stream.concat(
                  responseClose.getExecutions().stream(), responseOpen.getExecutions().stream())
              .distinct()
              .count();
      assertEquals(expectRuns, totalSize);
    } catch (Exception e) {
      fail("should not throw error:" + e.getMessage());
    }
  }
}
