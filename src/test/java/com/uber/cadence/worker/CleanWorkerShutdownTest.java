package com.uber.cadence.worker;

import com.uber.cadence.activity.Activity;
import com.uber.cadence.activity.ActivityMethod;
import com.uber.cadence.client.ActivityCompletionClient;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowClientOptions;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.converter.JsonDataConverter;
import com.uber.cadence.internal.worker.PollerOptions;
import com.uber.cadence.serviceclient.WorkflowServiceTChannel;
import com.uber.cadence.testing.TestEnvironmentOptions;
import com.uber.cadence.testing.TestWorkflowEnvironment;
import com.uber.cadence.workflow.Workflow;
import com.uber.cadence.workflow.WorkflowMethod;
import com.uber.cadence.workflow.WorkflowTest;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runners.Parameterized;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static com.uber.cadence.workflow.WorkflowTest.DOMAIN;
import static org.junit.Assert.assertEquals;

public class CleanWorkerShutdownTest {

  private static final boolean skipDockerService =
      Boolean.parseBoolean(System.getenv("SKIP_DOCKER_SERVICE"));

  @Parameterized.Parameter public boolean useExternalService;

  @Parameterized.Parameters(name = "{1}")
  public static Object[] data() {
    if (skipDockerService) {
      return new Object[][] {{false, "TestService"}};
    } else {
      return new Object[][] {{true, "Docker"}, {false, "TestService"}};
    }
  }

  @Parameterized.Parameter(1)
  public String testType;

  @Rule public TestName testName = new TestName();

  private static WorkflowServiceTChannel service;

  @BeforeClass
  public static void setUp() {
    if (!skipDockerService) {
      service = new WorkflowServiceTChannel();
    }
  }

  @AfterClass
  public static void tearDown() {
    if (service != null) {
      service.close();
    }
  }

  public interface TestWorkflow {
    @WorkflowMethod(executionStartToCloseTimeoutSeconds = 100)
    String execute();
  }

  public static class TestWorkflowImpl implements TestWorkflow {

    private final Activities activities = Workflow.newActivityStub(Activities.class);

    @Override
    public String execute() {
      return activities.execute();
    }
  }

  public interface Activities {
      @ActivityMethod(scheduleToCloseTimeoutSeconds = 100)
    String execute();
  }

  public static class ActivitiesImpl implements Activities {
    @Override
    public String execute() {
      try {
        Thread.sleep(1000);
        throw new RuntimeException("should be interrupted");
      } catch (InterruptedException e) {
        return "interrupted";
      }
    }
  }

  @Test
  public void testCleanShutdown() {
    String taskList =
        "CleanWorkerShutdownTest-" + testName.getMethodName() + "-" + UUID.randomUUID().toString();
    WorkflowClient workflowClient;
      Worker.Factory workerFactory = null;
      TestWorkflowEnvironment testEnvironment = null;
    if (useExternalService) {
        workerFactory = new Worker.Factory(service, DOMAIN);
    Worker worker= workerFactory.newWorker(taskList);
      workflowClient = WorkflowClient.newInstance(service, DOMAIN);
      worker.registerWorkflowImplementationTypes(TestWorkflowImpl.class);
      worker.registerActivitiesImplementations(new ActivitiesImpl());
      workerFactory.start();
    } else {
      TestEnvironmentOptions testOptions =
          new TestEnvironmentOptions.Builder().setDomain(DOMAIN).build();
        testEnvironment = TestWorkflowEnvironment.newInstance(testOptions);
        Worker worker = testEnvironment.newWorker(taskList);
      workflowClient = testEnvironment.newWorkflowClient();
      worker.registerWorkflowImplementationTypes(TestWorkflowImpl.class);
      worker.registerActivitiesImplementations(new ActivitiesImpl());
      testEnvironment.start();
    }
      WorkflowOptions options = new WorkflowOptions.Builder().setTaskList(taskList).build();
      TestWorkflow workflow = workflowClient.newWorkflowStub(TestWorkflow.class, options);
      WorkflowClient.start(workflow::execute);
    if (useExternalService) {
      workerFactory.shutdown(Duration.ofSeconds(1));
    } else {
        testEnvironment.close();
    }
          assertEquals("interrupted", workflow.execute());
  }
}
