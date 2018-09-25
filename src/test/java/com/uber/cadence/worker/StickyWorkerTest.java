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

package com.uber.cadence.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.uber.cadence.activity.ActivityMethod;
import com.uber.cadence.activity.ActivityOptions;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.internal.metrics.MetricsTag;
import com.uber.cadence.internal.metrics.MetricsType;
import com.uber.cadence.internal.replay.DeciderCache;
import com.uber.cadence.testing.TestEnvironmentOptions;
import com.uber.cadence.testing.TestWorkflowEnvironment;
import com.uber.cadence.workflow.*;
import com.uber.m3.tally.RootScopeBuilder;
import com.uber.m3.tally.Scope;
import com.uber.m3.tally.StatsReporter;
import com.uber.m3.util.ImmutableMap;
import java.time.Duration;
import java.util.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Parameterized.class)
public class StickyWorkerTest {
  public static final String DOMAIN = "UnitTest";

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

  @Test
  public void whenStickyIsEnabledThenTheWorkflowIsCached() throws Exception {
    // Arrange
    String taskListName = "cachedStickyTest";

    TestEnvironmentWrapper wrapper =
        new TestEnvironmentWrapper(
            new Worker.FactoryOptions.Builder().setEnableStickyExecution(true).Build());
    Worker.Factory factory = wrapper.getWorkerFactory();
    Worker worker =
        factory.newWorker(taskListName, new WorkerOptions.Builder().build());
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
    factory.start();

    WorkflowOptions workflowOptions =
        new WorkflowOptions.Builder()
            .setTaskList(taskListName)
            .setExecutionStartToCloseTimeout(Duration.ofDays(30))
            .setTaskStartToCloseTimeout(Duration.ofSeconds(30))
            .build();
    GreetingWorkflow workflow =
        wrapper.getWorkflowClient().newWorkflowStub(GreetingWorkflow.class, workflowOptions);

    // Act
    WorkflowClient.start(workflow::getGreeting);
    workflow.waitForName("World");
    String greeting = workflow.getGreeting();
    assertEquals("Hello World!", greeting);

    // Assert
    DeciderCache cache = factory.getCache();
    assertNotNull(cache);
    assertEquals(1, cache.size());

    wrapper.close();
  }

  @Test
  public void whenStickyIsEnabledThenTheWorkflowIsCached_Activities() throws Exception {
    // Arrange
    String taskListName = "cachedStickyTest_Activities" + UUID.randomUUID();

    Map<String, String> tags =
            new ImmutableMap.Builder<String, String>(2)
                    .put(MetricsTag.DOMAIN, "domain")
                    .put(MetricsTag.TASK_LIST, taskListName)
                    .build();
    StatsReporter reporter = mock(StatsReporter.class);
    Scope scope =
            new RootScopeBuilder()
                    .reporter(reporter)
                    .reportEvery(com.uber.m3.util.Duration.ofMillis(300))
                    .tagged(tags);


    TestEnvironmentWrapper wrapper =
        new TestEnvironmentWrapper(
            new Worker.FactoryOptions.Builder().setEnableStickyExecution(true).Build());
    Worker.Factory factory = wrapper.getWorkerFactory();
    Worker worker = factory.newWorker(taskListName, new WorkerOptions.Builder().setMetricsScope(scope).build());
    worker.registerWorkflowImplementationTypes(WorkflowImpl.class);
    worker.registerActivitiesImplementations(new ActivitiesImpl());
    factory.start();

    WorkflowOptions workflowOptions =
        new WorkflowOptions.Builder()
            .setTaskList(taskListName)
            .setExecutionStartToCloseTimeout(Duration.ofDays(30))
            .setTaskStartToCloseTimeout(Duration.ofSeconds(30))
            .build();
    StressWorkflow workflow =
        wrapper.getWorkflowClient().newWorkflowStub(StressWorkflow.class, workflowOptions);

    // Act
    WorkflowParams w = new WorkflowParams();
    w.CadenceSleep = Duration.ofMillis(100);
    w.ChainSequence = 4;
    w.ConcurrentCount = 1;
    w.PayloadSizeBytes = 10;
    w.TaskListName = taskListName;
    workflow.execute(w);

    // Wait for reporter
    Thread.sleep(600);
    // Verify the workflow succeeded without having to recover from a failure
    verify(reporter, times(0)).reportCounter(MetricsType.STICKY_CACHE_MISS, tags, 0);
    // Finish Workflow

    wrapper.close();
  }

  @Test
  public void whenStickyIsNotEnabledThenTheWorkflowIsNotCached() {
    // Arrange
    String taskListName = "notCachedStickyTest";
    TestEnvironmentWrapper wrapper =
        new TestEnvironmentWrapper(
            new Worker.FactoryOptions.Builder().setEnableStickyExecution(false).Build());
    Worker.Factory factory = wrapper.getWorkerFactory();
    Worker worker = factory.newWorker(taskListName);
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
    factory.start();

    WorkflowOptions workflowOptions =
        new WorkflowOptions.Builder()
            .setTaskList(taskListName)
            .setExecutionStartToCloseTimeout(Duration.ofDays(30))
            .setTaskStartToCloseTimeout(Duration.ofSeconds(30))
            .build();
    GreetingWorkflow workflow =
        wrapper.getWorkflowClient().newWorkflowStub(GreetingWorkflow.class, workflowOptions);

    // Act
    WorkflowClient.start(workflow::getGreeting);
    workflow.waitForName("World");
    String greeting = workflow.getGreeting();
    assertEquals("Hello World!", greeting);

    // Assert
    DeciderCache cache = factory.getCache();
    assertNull(cache);
    wrapper.close();
  }

  @Test
  public void whenCacheIsEvictedTheWorkerCanRecover() throws Exception {
    // Arrange
    String taskListName = "evictedStickyTest";
    TestEnvironmentWrapper wrapper =
        new TestEnvironmentWrapper(
            new Worker.FactoryOptions.Builder().setEnableStickyExecution(true).Build());
    Worker.Factory factory = wrapper.getWorkerFactory();
    Worker worker = factory.newWorker(taskListName);
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
    factory.start();

    WorkflowOptions workflowOptions =
        new WorkflowOptions.Builder()
            .setTaskList(taskListName)
            .setExecutionStartToCloseTimeout(Duration.ofDays(30))
            .setTaskStartToCloseTimeout(Duration.ofSeconds(30))
            .build();
    GreetingWorkflow workflow =
        wrapper.getWorkflowClient().newWorkflowStub(GreetingWorkflow.class, workflowOptions);

    // Act
    WorkflowClient.start(workflow::getGreeting);

    Thread.sleep(200); // Wait for workflow to start

    DeciderCache cache = factory.getCache();
    assertNotNull(cache);
    assertEquals(1, cache.size());
    cache.invalidateAll();
    assertEquals(0, cache.size());

    workflow.waitForName("World");
    String greeting = workflow.getGreeting();

    // Assert
    assertEquals("Hello World!", greeting);
    wrapper.close();
  }

  @Test
  public void workflowsCanBeQueried() throws Exception {
    // Arrange
    String taskListName = "queryStickyTest";
    TestEnvironmentWrapper wrapper =
        new TestEnvironmentWrapper(
            new Worker.FactoryOptions.Builder().setEnableStickyExecution(true).Build());
    Worker.Factory factory = wrapper.getWorkerFactory();
    Worker worker = factory.newWorker(taskListName);
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
    factory.start();

    WorkflowOptions workflowOptions =
        new WorkflowOptions.Builder()
            .setTaskList(taskListName)
            .setExecutionStartToCloseTimeout(Duration.ofDays(30))
            .setTaskStartToCloseTimeout(Duration.ofSeconds(30))
            .build();
    GreetingWorkflow workflow =
        wrapper.getWorkflowClient().newWorkflowStub(GreetingWorkflow.class, workflowOptions);

    // Act
    WorkflowClient.start(workflow::getGreeting);

    Thread.sleep(200); // Wait for workflow to start

    DeciderCache cache = factory.getCache();
    assertNotNull(cache);
    assertEquals(1, cache.size());

    // Assert
    assertEquals(workflow.getProgress(), GreetingWorkflow.Status.WAITING_FOR_NAME);

    workflow.waitForName("World");
    String greeting = workflow.getGreeting();

    assertEquals("Hello World!", greeting);
    assertEquals(workflow.getProgress(), GreetingWorkflow.Status.GREETING_GENERATED);
    wrapper.close();
  }

  @Test
  public void workflowsCanBeQueriedAfterEviction() throws Exception {
    // Arrange
    String taskListName = "queryEvictionStickyTest";
    TestEnvironmentWrapper wrapper =
        new TestEnvironmentWrapper(
            new Worker.FactoryOptions.Builder().setEnableStickyExecution(true).Build());
    Worker.Factory factory = wrapper.getWorkerFactory();
    Worker worker = factory.newWorker(taskListName);
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
    factory.start();

    WorkflowOptions workflowOptions =
        new WorkflowOptions.Builder()
            .setTaskList(taskListName)
            .setExecutionStartToCloseTimeout(Duration.ofDays(30))
            .setTaskStartToCloseTimeout(Duration.ofSeconds(30))
            .build();
    GreetingWorkflow workflow =
        wrapper.getWorkflowClient().newWorkflowStub(GreetingWorkflow.class, workflowOptions);

    // Act
    WorkflowClient.start(workflow::getGreeting);

    Thread.sleep(200); // Wait for workflow to start

    DeciderCache cache = factory.getCache();
    assertNotNull(cache);
    assertEquals(1, cache.size());
    cache.invalidateAll();
    assertEquals(0, cache.size());

    // Assert
    assertEquals(workflow.getProgress(), GreetingWorkflow.Status.WAITING_FOR_NAME);

    workflow.waitForName("World");
    String greeting = workflow.getGreeting();

    assertEquals("Hello World!", greeting);
    assertEquals(workflow.getProgress(), GreetingWorkflow.Status.GREETING_GENERATED);
    wrapper.close();
  }

  // Todo: refactor TestEnvironment to toggle between real and test service.
  private class TestEnvironmentWrapper {

    private TestWorkflowEnvironment testEnv;
    private Worker.Factory factory;

    public TestEnvironmentWrapper(Worker.FactoryOptions options) {
      if (options == null) {
        options = new Worker.FactoryOptions.Builder().Build();
      }
      factory = new Worker.Factory(DOMAIN, options);
      TestEnvironmentOptions testOptions =
          new TestEnvironmentOptions.Builder().setDomain(DOMAIN).setFactoryOptions(options).build();
      testEnv = TestWorkflowEnvironment.newInstance(testOptions);
    }

    private Worker.Factory getWorkerFactory() {
      return useExternalService ? factory : testEnv.getWorkerFactory();
    }

    private WorkflowClient getWorkflowClient() {
      return useExternalService ? WorkflowClient.newInstance(DOMAIN) : testEnv.newWorkflowClient();
    }

    private void close() {
      factory.shutdown(Duration.ofSeconds(1));
      testEnv.close();
    }
  }

  public static class WorkflowParams {

    public int ChainSequence;
    public int ConcurrentCount;
    public String TaskListName;
    public int PayloadSizeBytes;
    public Duration CadenceSleep; // nano
  }

  public interface GreetingWorkflow {
    /** @return greeting string */
    @QueryMethod
    Status getProgress();

    /** @return greeting string */
    @WorkflowMethod
    String getGreeting();

    /** Receives name through an external signal. */
    @SignalMethod
    void waitForName(String name);

    enum Status {
      WAITING_FOR_NAME,
      GREETING_GENERATED
    }
  }

  /** GreetingWorkflow implementation that returns a greeting. */
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    private final CompletablePromise<String> name = Workflow.newPromise();
    private Status status = Status.WAITING_FOR_NAME;

    @Override
    public Status getProgress() {
      return status;
    }

    @Override
    public String getGreeting() {
      String greeting = "Hello " + name.get() + "!";
      status = Status.GREETING_GENERATED;
      return greeting;
    }

    @Override
    public void waitForName(String name) {
      this.name.complete(name);
    }
  }

  public interface StressWorkflow {

    @WorkflowMethod()
    void execute(WorkflowParams params);
  }

  public static class WorkflowImpl implements StressWorkflow {

    @Override
    public void execute(WorkflowParams params) {
      SleepActivity activity =
          Workflow.newActivityStub(
              SleepActivity.class,
              new ActivityOptions.Builder()
                  .setTaskList(params.TaskListName)
                  .setScheduleToStartTimeout(Duration.ofMinutes(1))
                  .setStartToCloseTimeout(Duration.ofMinutes(1))
                  .setHeartbeatTimeout(Duration.ofSeconds(20))
                  .build());

      for (int i = 0; i < params.ChainSequence; i++) {
        List<Promise<Void>> promises = new ArrayList<>();
        for (int j = 0; j < params.ConcurrentCount; j++) {
          byte[] bytes = new byte[params.PayloadSizeBytes];
          new Random().nextBytes(bytes);
          Promise<Void> promise = Async.procedure(activity::sleep, i, j, bytes);
          promises.add(promise);
        }

        for (Promise<Void> promise : promises) {
          promise.get();
        }

        Workflow.sleep(params.CadenceSleep);
      }
    }
  }

  public interface SleepActivity {

    @ActivityMethod()
    void sleep(int chain, int concurrency, byte[] bytes);
  }

  public static class ActivitiesImpl implements SleepActivity {
    private static final Logger log = LoggerFactory.getLogger("sleep-activity");

    @Override
    public void sleep(int chain, int concurrency, byte[] bytes) {}
  }
}
