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
import com.uber.cadence.internal.worker.ActivityTaskHandler.Result;
import com.uber.cadence.workflow.ActivityFailureException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

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
    return ActivityInvocationHandler.newInstance(activityInterface, options,
        new TestActivityExecutor(activityTaskHandler));
  }

  private class TestActivityExecutor implements ActivityExecutor {

    private final POJOActivityTaskHandler taskHandler;

    public TestActivityExecutor(POJOActivityTaskHandler activityTaskHandler) {
      this.taskHandler = activityTaskHandler;
    }

    public <T> void executeActivity(String activityType, ActivityOptions options, Object[] args,
        Class<T> returnType, BiConsumer<T, RuntimeException> resultCallback) {
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
      getReply(task, taskResult, returnType, resultCallback);
    }

    private <T> void getReply(PollForActivityTaskResponse task, Result
        response, Class<T> returnType,
        BiConsumer<T, RuntimeException> resultCallback) {
      RetryOptions ro = response.getRequestRetryOptions();
      RespondActivityTaskCompletedRequest taskCompleted = response.getTaskCompleted();
      if (taskCompleted != null) {

        resultCallback.accept(testEnvironmentOptions.getDataConverter()
            .fromData(taskCompleted.getResult(), returnType), null);
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
          resultCallback.accept(null,
              new ActivityFailureException(0, task.getActivityType(), task.getActivityId(), cause));

        } else {
          RespondActivityTaskCanceledRequest taskCancelled = response.getTaskCancelled();
          if (taskCancelled != null) {
            resultCallback.accept(null, new CancellationException(
                new String(taskCancelled.getDetails(), StandardCharsets.UTF_8)));
          }
        }
      }
      resultCallback.accept(Defaults.defaultValue(returnType), null);
    }
  }
}
