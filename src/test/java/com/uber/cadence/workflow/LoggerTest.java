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

package com.uber.cadence.workflow;

import static org.junit.Assert.assertEquals;

import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.testing.TestEnvironmentOptions;
import com.uber.cadence.testing.TestEnvironmentOptions.Builder;
import com.uber.cadence.testing.TestWorkflowEnvironment;
import com.uber.cadence.worker.Worker;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;

public class LoggerTest {
  private static final String taskList = "logger-test";
  private static final Logger workflowLogger = Workflow.getLogger(TestLoggingInWorkflow.class);
  private static final Logger childWorkflowLogger =
      Workflow.getLogger(TestLoggerInChildWorkflow.class);

  public interface TestWorkflow {
    @WorkflowMethod
    void execute();
  }

  public static class TestLoggingInWorkflow implements LoggerTest.TestWorkflow {
    @Override
    public void execute() {
      workflowLogger.info("Start executing workflow.");
      ChildWorkflowOptions options =
          new ChildWorkflowOptions.Builder().setTaskList(taskList).build();
      LoggerTest.TestChildWorkflow workflow =
          Workflow.newChildWorkflowStub(LoggerTest.TestChildWorkflow.class, options);
      workflow.executeChild();
      workflowLogger.info("Done executing workflow.");
    }
  }

  public interface TestChildWorkflow {
    @WorkflowMethod
    void executeChild();
  }

  public static class TestLoggerInChildWorkflow implements LoggerTest.TestChildWorkflow {

    @Override
    public void executeChild() {
      childWorkflowLogger.info("Executing child workflow.");
    }
  }

  @Test
  public void testWorkflowMetrics() throws IOException {
    TestEnvironmentOptions testOptions = new Builder().setDomain(WorkflowTest.DOMAIN).build();
    TestWorkflowEnvironment env = TestWorkflowEnvironment.newInstance(testOptions);
    Worker worker = env.newWorker(taskList);
    worker.registerWorkflowImplementationTypes(
        TestLoggingInWorkflow.class, TestLoggerInChildWorkflow.class);
    worker.start();

    WorkflowClient workflowClient = env.newWorkflowClient();
    WorkflowOptions options =
        new WorkflowOptions.Builder()
            .setExecutionStartToCloseTimeout(Duration.ofSeconds(1000))
            .setTaskList(taskList)
            .build();
    LoggerTest.TestWorkflow workflow =
        workflowClient.newWorkflowStub(LoggerTest.TestWorkflow.class, options);
    workflow.execute();

    String tempDir = System.getProperty("java.io.tmpdir");
    File logFile = new File(tempDir, "log-cadence-java-client.log");
    List<String> logs = Files.readAllLines(logFile.toPath());
    assertEquals(1, matchingLines(logs, "Start executing workflow."));
    assertEquals(1, matchingLines(logs, "Executing child workflow."));
    assertEquals(1, matchingLines(logs, "Done executing workflow."));
  }

  private int matchingLines(List<String> logs, String message) {
    int i = 0;
    for (String log : logs) {
      if (log.contains(message)) {
        i++;
      }
    }
    return i;
  }
}
