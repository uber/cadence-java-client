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

import com.google.common.base.Preconditions;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.activity.Activity;
import com.uber.cadence.activity.ActivityMethod;
import com.uber.cadence.activity.ActivityOptions;
import com.uber.cadence.activity.ActivityTask;
import com.uber.cadence.client.ActivityCompletionClient;
import com.uber.cadence.testUtils.CadenceTestRule;
import java.time.Duration;
import java.util.concurrent.CancellationException;
import org.junit.Rule;
import org.junit.Test;

public class ManualActivityCompletionWorkflowTest {
  @Rule
  public CadenceTestRule testRule =
      CadenceTestRule.builder()
          .withActivities(new ManualCompletionActivitiesImpl())
          .withWorkflowTypes(ManualCompletionWorkflowImpl.class)
          .startWorkersAutomatically()
          .build();

  @Test
  public void testManualActivityCompletion() {
    testRule.newWorkflowStub(ManualCompletionWorkflow.class).run();
  }

  public interface ManualCompletionActivities {
    @ActivityMethod
    String asyncActivity();

    @ActivityMethod
    void completeAsyncActivity(String result);

    @ActivityMethod
    void completeAsyncActivityById(String result);

    @ActivityMethod
    void failAsyncActivity(String details);

    @ActivityMethod
    void failAsyncActivityById(String details);

    @ActivityMethod
    void cancelAsyncActivity(String details);

    @ActivityMethod
    void cancelAsyncActivityById(String details);
  }

  private class ManualCompletionActivitiesImpl implements ManualCompletionActivities {
    private ActivityTask openTask;

    @Override
    public synchronized String asyncActivity() {
      openTask = Activity.getTask();

      Activity.doNotCompleteOnReturn();
      return null;
    }

    @Override
    public synchronized void completeAsyncActivity(String details) {
      Preconditions.checkState(openTask != null);
      getClient().complete(openTask.getTaskToken(), details);
    }

    @Override
    public synchronized void completeAsyncActivityById(String details) {
      Preconditions.checkState(openTask != null);
      getClient().complete(getCurrentWorkflow(), openTask.getActivityId(), details);
    }

    @Override
    public synchronized void failAsyncActivity(String details) {
      Preconditions.checkState(openTask != null);
      getClient()
          .completeExceptionally(openTask.getTaskToken(), new ExceptionWithDetaills(details));
    }

    @Override
    public synchronized void failAsyncActivityById(String details) {
      Preconditions.checkState(openTask != null);
      getClient()
          .completeExceptionally(
              getCurrentWorkflow(), openTask.getActivityId(), new ExceptionWithDetaills(details));
    }

    @Override
    public synchronized void cancelAsyncActivity(String details) {
      Preconditions.checkState(openTask != null);
      getClient().reportCancellation(openTask.getTaskToken(), details);
    }

    @Override
    public synchronized void cancelAsyncActivityById(String details) {
      Preconditions.checkState(openTask != null);
      getClient().reportCancellation(getCurrentWorkflow(), openTask.getActivityId(), details);
    }

    private WorkflowExecution getCurrentWorkflow() {
      return Activity.getWorkflowExecution();
    }

    private ActivityCompletionClient getClient() {
      return testRule.getWorkflowClient().newActivityCompletionClient();
    }
  }

  public interface ManualCompletionWorkflow {
    @WorkflowMethod
    void run();
  }

  public static class ManualCompletionWorkflowImpl implements ManualCompletionWorkflow {
    private final ManualCompletionActivities activities =
        Workflow.newActivityStub(
            ManualCompletionActivities.class,
            new ActivityOptions.Builder()
                .setScheduleToCloseTimeout(Duration.ofSeconds(10))
                .build());

    @Override
    public void run() {
      Promise<String> result = Async.function(activities::asyncActivity);
      activities.completeAsyncActivity("1");
      expectSuccess("1", result);
      expectFailure(() -> activities.completeAsyncActivity("again"));

      result = Async.function(activities::asyncActivity);
      activities.completeAsyncActivityById("2");
      expectSuccess("2", result);
      expectFailure(() -> activities.completeAsyncActivityById("again"));

      result = Async.function(activities::asyncActivity);
      activities.failAsyncActivity("3");
      expectFailureWithDetails(result, "3");
      expectFailure(() -> activities.failAsyncActivity("again"));

      result = Async.function(activities::asyncActivity);
      activities.failAsyncActivityById("4");
      expectFailureWithDetails(result, "4");
      expectFailure(() -> activities.failAsyncActivityById("again"));

      // Need to request  cancellation, then the activity can respond with the cancel
      CompletablePromise<String> completablePromise = Workflow.newPromise();
      CancellationScope scope =
          Workflow.newCancellationScope(
              () -> {
                completablePromise.completeFrom(Async.function(activities::asyncActivity));
              });
      result = completablePromise;
      scope.run();
      // Need to force a separate decision, otherwise the activity gets skipped since it's started
      // and cancelled
      // in the same decision
      Workflow.sleep(1);
      scope.cancel();
      activities.cancelAsyncActivity("5");
      expectCancelled(result);

      // Need to request  cancellation, then the activity can respond with the cancel
      CompletablePromise<String> completablePromise2 = Workflow.newPromise();
      scope =
          Workflow.newCancellationScope(
              () -> {
                completablePromise2.completeFrom(Async.function(activities::asyncActivity));
              });
      scope.run();
      // Need to force a separate decision, otherwise the activity gets skipped since it's started
      // and cancelled in the same decision
      Workflow.sleep(1);
      scope.cancel();
      result = completablePromise2;
      activities.cancelAsyncActivityById("6");
      expectCancelled(result);
    }

    private void expectCancelled(Promise<String> promise) {
      try {
        promise.get();
        throw new IllegalStateException("expected activity failure");
      } catch (CancellationException e) {
        // good
      }
    }

    private void expectFailureWithDetails(Promise<String> promise, String expectedDetails) {
      try {
        promise.get();
        throw new IllegalStateException("expected activity failure");
      } catch (ActivityFailureException e) {
        if (!(e.getCause() instanceof ExceptionWithDetaills)) {
          throw new IllegalStateException(
              "Didn't receive correct cause, instead found this:", e.getCause());
        }
        String details = ((ExceptionWithDetaills) e.getCause()).details;
        if (!expectedDetails.equals(details)) {
          throw new IllegalStateException(
              "Expected: '" + expectedDetails + "', got: '" + details + "'");
        }
      }
    }

    private void expectFailure(Runnable runnable) {
      try {
        runnable.run();
        throw new IllegalStateException("expected activity failure");
      } catch (ActivityFailureException e) {
        // good
      }
    }

    private void expectSuccess(String expected, Promise<String> actual) {
      if (!expected.equals(actual.get())) {
        throw new IllegalStateException("Expected: '" + expected + "', got: '" + actual + "'");
      }
    }
  }

  public static class ExceptionWithDetaills extends RuntimeException {
    public String details;

    public ExceptionWithDetaills() {}

    public ExceptionWithDetaills(String details) {
      this.details = details;
    }
  }
}
