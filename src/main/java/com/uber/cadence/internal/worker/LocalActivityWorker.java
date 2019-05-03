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

package com.uber.cadence.internal.worker;

import com.uber.cadence.*;
import com.uber.cadence.converter.JsonDataConverter;
import com.uber.cadence.internal.metrics.MetricsType;
import com.uber.cadence.internal.replay.ClockDecisionContext;
import com.uber.cadence.internal.replay.ExecuteActivityParameters;
import com.uber.cadence.internal.replay.ReplayDecider;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class LocalActivityWorker implements SuspendableWorker {

  private static final String POLL_THREAD_NAME_PREFIX = "Local Activity Poller taskList=";

  private SuspendableWorker poller = new NoopSuspendableWorker();
  private final ActivityTaskHandler handler;
  private final String domain;
  private final String taskList;
  private final SingleWorkerOptions options;
  private final LocalActivityPollTask laPollTask;

  public LocalActivityWorker(
      String domain,
      String taskList,
      SingleWorkerOptions options,
      ActivityTaskHandler handler,
      LocalActivityPollTask laPollTask) {
    this.domain = Objects.requireNonNull(domain);
    this.taskList = Objects.requireNonNull(taskList);
    this.options = options;
    this.handler = handler;
    this.laPollTask = Objects.requireNonNull(laPollTask);
  }

  @Override
  public void start() {
    if (handler.isAnyTypeSupported()) {
      PollerOptions pollerOptions = options.getPollerOptions();
      if (pollerOptions.getPollThreadNamePrefix() == null) {
        pollerOptions =
            new PollerOptions.Builder(pollerOptions)
                .setPollThreadNamePrefix(
                    POLL_THREAD_NAME_PREFIX
                        + "\""
                        + taskList
                        + "\", domain=\""
                        + domain
                        + "\", type=\"activity\"")
                .build();
      }
      poller =
          new Poller<>(
              options.getIdentity(),
              laPollTask,
              new PollTaskExecutor<>(domain, taskList, options, new TaskHandlerImpl(handler)),
              pollerOptions,
              options.getMetricsScope());
      poller.start();
      options.getMetricsScope().counter(MetricsType.WORKER_START_COUNTER).inc(1);
    }
  }

  @Override
  public boolean isStarted() {
    return poller.isStarted();
  }

  @Override
  public boolean isShutdown() {
    return poller.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return poller.isTerminated();
  }

  @Override
  public void shutdown() {
    poller.shutdown();
  }

  @Override
  public void shutdownNow() {
    poller.shutdownNow();
  }

  @Override
  public void awaitTermination(long timeout, TimeUnit unit) {
    poller.awaitTermination(timeout, unit);
  }

  @Override
  public void suspendPolling() {
    poller.suspendPolling();
  }

  @Override
  public void resumePolling() {
    poller.resumePolling();
  }

  @Override
  public boolean isSuspended() {
    return poller.isSuspended();
  }

  public static class Task {
    ExecuteActivityParameters params;
    ReplayDecider replayDecider;

    public Task(ExecuteActivityParameters params, ReplayDecider replayDecider) {
      this.params = params;
      this.replayDecider = replayDecider;
    }
  }

  private class TaskHandlerImpl implements PollTaskExecutor.TaskHandler<Task> {

    final ActivityTaskHandler handler;

    private TaskHandlerImpl(ActivityTaskHandler handler) {
      this.handler = handler;
    }

    @Override
    public void handle(Task task) throws Exception {
      PollForActivityTaskResponse pollTask = new PollForActivityTaskResponse();
      pollTask.setActivityType(task.params.getActivityType());
      pollTask.setInput(task.params.getInput());

      ActivityTaskHandler.Result result =
          handler.handle(null, "", pollTask, options.getMetricsScope());

      ClockDecisionContext.LocalActivityMarkerData marker;
      if (result.getTaskCompleted() != null) {
        marker =
            new ClockDecisionContext.LocalActivityMarkerData(
                task.params.getActivityId(),
                task.params.getActivityType().toString(),
                result.getTaskCompleted());
      } else if (result.getTaskFailed() != null) {
        marker =
            new ClockDecisionContext.LocalActivityMarkerData(
                task.params.getActivityId(),
                task.params.getActivityType().toString(),
                result.getTaskFailed());
      } else {
        marker =
            new ClockDecisionContext.LocalActivityMarkerData(
                task.params.getActivityId(),
                task.params.getActivityType().toString(),
                result.getTaskCancelled());
      }

      // TODO: cancellation

      byte[] markerData = JsonDataConverter.getInstance().toData(marker);

      HistoryEvent event = new HistoryEvent();
      event.setEventType(EventType.MarkerRecorded);
      MarkerRecordedEventAttributes attributes = new MarkerRecordedEventAttributes();
      attributes.setMarkerName(ClockDecisionContext.LOCAL_ACTIVITY_MARKER_NAME);
      attributes.setDetails(markerData);
      event.setMarkerRecordedEventAttributes(attributes);
      try {
        task.replayDecider.processEvent(event);
      } catch (Throwable throwable) {
        throw new Exception("fix me");
      }
    }

    @Override
    public Throwable wrapFailure(Task task, Throwable failure) {
      return new RuntimeException("Failure processing local activity task.", failure);
    }
  }
}
