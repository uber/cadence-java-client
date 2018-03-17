package com.uber.cadence.internal.sync;

import com.google.common.base.Defaults;
import com.uber.cadence.ActivityType;
import com.uber.cadence.PollForActivityTaskResponse;
import com.uber.cadence.RespondActivityTaskCanceledRequest;
import com.uber.cadence.RespondActivityTaskCompletedRequest;
import com.uber.cadence.RespondActivityTaskFailedRequest;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.activity.ActivityOptions;
import com.uber.cadence.common.RetryOptions;
import com.uber.cadence.internal.replay.ReplayDecisionTaskHandler;
import com.uber.cadence.internal.testing.TestEnvironment;
import com.uber.cadence.internal.testing.TestEnvironmentOptions;
import com.uber.cadence.internal.worker.ActivityTaskHandler;
import com.uber.cadence.internal.worker.ActivityTaskHandler.Result;
import com.uber.cadence.workflow.ActivityFailureException;
import com.uber.cadence.workflow.Promise;
import com.uber.cadence.workflow.Workflow;
import java.lang.reflect.InvocationHandler;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TestEnvironmentInternal implements TestEnvironment {

  private static final int WORKFLOW_THREAD_POOL_SIZE = 1000;
  private final POJOActivityTaskHandler activityTaskHandler;

  private final ReplayDecisionTaskHandler decisionTaskHandler;
  private final POJOWorkflowImplementationFactory workflowImplementationFactory;
  private final TestEnvironmentOptions testEnvironmentOptions;
  private final AtomicInteger idSequencer = new AtomicInteger();

  public TestEnvironmentInternal(TestEnvironmentOptions options) {
    if (options == null) {
      this.testEnvironmentOptions = new TestEnvironmentOptions.Builder().build();
    } else {
      this.testEnvironmentOptions = options;
    }
    ThreadPoolExecutor workflowThreadPool =
        new ThreadPoolExecutor(
            WORKFLOW_THREAD_POOL_SIZE,
            WORKFLOW_THREAD_POOL_SIZE,
            10,
            TimeUnit.SECONDS,
            new SynchronousQueue<>());

    workflowImplementationFactory = new POJOWorkflowImplementationFactory(
        testEnvironmentOptions.getDataConverter(), workflowThreadPool);
    decisionTaskHandler = new ReplayDecisionTaskHandler(testEnvironmentOptions.getDomain(),
        workflowImplementationFactory);
    activityTaskHandler = new POJOActivityTaskHandler(testEnvironmentOptions.getDataConverter());
  }

  @Override
  public void registerWorkflowImplementationTypes(Class<?>... workflowImplementationClasses) {
    workflowImplementationFactory.setWorkflowImplementationTypes(workflowImplementationClasses);
  }

  /**
   * Register activity implementation objects with a worker. Overwrites previously registered
   * objects. As activities are reentrant and stateless only one instance per activity type is
   * registered.
   *
   * <p>Implementations that share a worker must implement different interfaces as an activity type
   * is identified by the activity interface, not by the implementation.
   *
   * <p>
   */
  @Override
  public void registerActivitiesImplementations(Object... activityImplementations) {
    activityTaskHandler.setActivitiesImplementation(activityImplementations);
  }

  /**
   * Creates client stub to activities that implement given interface.
   *
   * @param activityInterface interface type implemented by activities
   */
  @Override
  public <T> T newActivityStub(Class<T> activityInterface) {
    ActivityOptions options = new ActivityOptions.Builder()
        .setScheduleToCloseTimeout(Duration.ofDays(1))
        .build();
    InvocationHandler invocationHandler = ActivityInvocationHandler
        .newInstance(options, new TestActivityExecutor(activityTaskHandler));
    invocationHandler = new DeterministicRunnerWrapper(invocationHandler);
    return ActivityInvocationHandler.newProxy(activityInterface, invocationHandler);

  }

  private class TestActivityExecutor implements ActivityExecutor {

    private final POJOActivityTaskHandler taskHandler;

    public TestActivityExecutor(POJOActivityTaskHandler activityTaskHandler) {
      this.taskHandler = activityTaskHandler;
    }

    public <T> Promise<T> executeActivity(String activityType, ActivityOptions options,
        Object[] args,
        Class<T> returnType) {
      PollForActivityTaskResponse task = new PollForActivityTaskResponse();
      task.setScheduleToCloseTimeoutSeconds((int) options.getScheduleToCloseTimeout().getSeconds());
      task.setHeartbeatTimeoutSeconds((int) options.getHeartbeatTimeout().getSeconds());
      task.setStartToCloseTimeoutSeconds((int) options.getStartToCloseTimeout().getSeconds());
      task.setScheduledTimestamp(Duration.ofMillis(System.currentTimeMillis()).toNanos());
      task.setStartedTimestamp(Duration.ofMillis(System.currentTimeMillis()).toNanos());
      task.setInput(testEnvironmentOptions.getDataConverter().toData(args));
      task.setTaskToken("test-task-token".getBytes());
      task.setActivityId(String.valueOf(idSequencer.incrementAndGet()));
      task.setWorkflowExecution(new WorkflowExecution().setWorkflowId("test-workflow-id")
          .setRunId(UUID.randomUUID().toString()));
      task.setActivityType(new ActivityType().setName(activityType));
      Result taskResult = activityTaskHandler
          .handle(null, testEnvironmentOptions.getDomain(), task);
      return Workflow.newPromise(getReply(task, taskResult, returnType));
    }

    private <T> T getReply(PollForActivityTaskResponse task, ActivityTaskHandler.Result
        response, Class<T> returnType) {
      RetryOptions ro = response.getRequestRetryOptions();
      RespondActivityTaskCompletedRequest taskCompleted = response.getTaskCompleted();
      if (taskCompleted != null) {

        return testEnvironmentOptions.getDataConverter().fromData(taskCompleted.getResult(),
            returnType);
      } else {
        RespondActivityTaskFailedRequest taskFailed = response.getTaskFailed();
        if (taskFailed != null) {
          String causeClassName = taskFailed.getReason();
          Class<? extends Exception> causeClass;
          Exception cause;
          try {
            @SuppressWarnings("unchecked") // cc is just to have a place to put this annotation
                Class<? extends Exception> cc = (Class<? extends Exception>) Class
                .forName(causeClassName);
            causeClass = cc;
            cause = testEnvironmentOptions.getDataConverter().fromData(taskFailed.getDetails(),
                causeClass);
          } catch (Exception e) {
            cause = e;
          }
          throw new ActivityFailureException(
              0, task.getActivityType(), task.getActivityId(), cause);

        } else {
          RespondActivityTaskCanceledRequest taskCancelled = response.getTaskCancelled();
          if (taskCancelled != null) {
            throw new CancellationException(new String(taskCancelled.getDetails(),
                StandardCharsets.UTF_8));
          }
        }
      }
      return Defaults.defaultValue(returnType);
    }
  }
}
