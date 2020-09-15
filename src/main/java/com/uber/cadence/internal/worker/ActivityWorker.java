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

import com.uber.cadence.BadRequestError;
import com.uber.cadence.DomainNotActiveError;
import com.uber.cadence.EntityNotExistsError;
import com.uber.cadence.Header;
import com.uber.cadence.PollForActivityTaskResponse;
import com.uber.cadence.RespondActivityTaskCanceledRequest;
import com.uber.cadence.RespondActivityTaskCompletedRequest;
import com.uber.cadence.RespondActivityTaskFailedRequest;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.common.RetryOptions;
import com.uber.cadence.context.ContextPropagator;
import com.uber.cadence.context.OpenTracingContextPropagator;
import com.uber.cadence.internal.common.Retryer;
import com.uber.cadence.internal.logging.LoggerTag;
import com.uber.cadence.internal.metrics.MetricsTag;
import com.uber.cadence.internal.metrics.MetricsType;
import com.uber.cadence.internal.worker.ActivityTaskHandler.Result;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.m3.tally.Scope;
import com.uber.m3.tally.Stopwatch;
import com.uber.m3.util.Duration;
import com.uber.m3.util.ImmutableMap;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.thrift.TException;
import org.slf4j.MDC;

public final class ActivityWorker implements SuspendableWorker {

  private static final String POLL_THREAD_NAME_PREFIX = "Activity Poller taskList=";

  private SuspendableWorker poller = new NoopSuspendableWorker();
  private final ActivityTaskHandler handler;
  private final IWorkflowService service;
  private final String domain;
  private final String taskList;
  private final SingleWorkerOptions options;

  public ActivityWorker(
      IWorkflowService service,
      String domain,
      String taskList,
      SingleWorkerOptions options,
      ActivityTaskHandler handler) {
    this.service = Objects.requireNonNull(service);
    this.domain = Objects.requireNonNull(domain);
    this.taskList = Objects.requireNonNull(taskList);
    this.handler = handler;

    PollerOptions pollerOptions = options.getPollerOptions();
    if (pollerOptions.getPollThreadNamePrefix() == null) {
      pollerOptions =
          new PollerOptions.Builder(pollerOptions)
              .setPollThreadNamePrefix(
                  POLL_THREAD_NAME_PREFIX + "\"" + taskList + "\", domain=\"" + domain + "\"")
              .build();
    }
    this.options = new SingleWorkerOptions.Builder(options).setPollerOptions(pollerOptions).build();
  }

  @Override
  public void start() {
    if (handler.isAnyTypeSupported()) {
      poller =
          new Poller<>(
              options.getIdentity(),
              new ActivityPollTask(service, domain, taskList, options),
              new PollTaskExecutor<>(domain, taskList, options, new TaskHandlerImpl(handler)),
              options.getPollerOptions(),
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

  private class TaskHandlerImpl
      implements PollTaskExecutor.TaskHandler<PollForActivityTaskResponse> {

    final ActivityTaskHandler handler;

    private TaskHandlerImpl(ActivityTaskHandler handler) {
      this.handler = handler;
    }

    @Override
    public void handle(PollForActivityTaskResponse task) throws Exception {
      Scope metricsScope =
          options
              .getMetricsScope()
              .tagged(
                  ImmutableMap.of(
                      MetricsTag.ACTIVITY_TYPE,
                      task.getActivityType().getName(),
                      MetricsTag.WORKFLOW_TYPE,
                      task.getWorkflowType().getName()));

      metricsScope
          .timer(MetricsType.ACTIVITY_SCHEDULED_TO_START_LATENCY)
          .record(
              Duration.ofNanos(
                  task.getStartedTimestamp() - task.getScheduledTimestampOfThisAttempt()));

      // The following tags are for logging.
      MDC.put(LoggerTag.ACTIVITY_ID, task.getActivityId());
      MDC.put(LoggerTag.ACTIVITY_TYPE, task.getActivityType().getName());
      MDC.put(LoggerTag.WORKFLOW_ID, task.getWorkflowExecution().getWorkflowId());
      MDC.put(LoggerTag.RUN_ID, task.getWorkflowExecution().getRunId());

      propagateContext(task);

      // Set up an opentracing span
      Tracer openTracingTracer = GlobalTracer.get();
      Tracer.SpanBuilder builder =
          openTracingTracer
              .buildSpan("cadence.activity")
              .withTag("resource.name", task.getActivityType().getName());
      if (OpenTracingContextPropagator.getCurrentOpenTracingSpanContext() != null) {
        builder.asChildOf(OpenTracingContextPropagator.getCurrentOpenTracingSpanContext());
      }
      Span span = builder.start();

      try (io.opentracing.Scope scope = openTracingTracer.activateSpan(span)) {
        Stopwatch sw = metricsScope.timer(MetricsType.ACTIVITY_EXEC_LATENCY).start();
        ActivityTaskHandler.Result response = handler.handle(task, metricsScope, false);
        sw.stop();

        sw = metricsScope.timer(MetricsType.ACTIVITY_RESP_LATENCY).start();
        sendReply(task, response, metricsScope);
        sw.stop();

        metricsScope
            .timer(MetricsType.ACTIVITY_E2E_LATENCY)
            .record(
                Duration.ofNanos(System.nanoTime() - task.getScheduledTimestampOfThisAttempt()));

      } catch (CancellationException e) {
        RespondActivityTaskCanceledRequest cancelledRequest =
            new RespondActivityTaskCanceledRequest();
        cancelledRequest.setDetails(
            String.valueOf(e.getMessage()).getBytes(StandardCharsets.UTF_8));
        Stopwatch sw = metricsScope.timer(MetricsType.ACTIVITY_RESP_LATENCY).start();
        sendReply(task, new Result(null, null, cancelledRequest, null), metricsScope);
        sw.stop();
      } catch (Exception e) {
        Tags.ERROR.set(span, true);
        Map<String, Object> errorData = new HashMap<>();
        errorData.put(Fields.EVENT, "error");
        if (e != null) {
          errorData.put(Fields.ERROR_OBJECT, e);
          errorData.put(Fields.MESSAGE, e.getMessage());
        }
        span.log(errorData);
        throw e;
      } finally {
        MDC.remove(LoggerTag.ACTIVITY_ID);
        MDC.remove(LoggerTag.ACTIVITY_TYPE);
        MDC.remove(LoggerTag.WORKFLOW_ID);
        MDC.remove(LoggerTag.RUN_ID);
        span.finish();
      }
    }

    void propagateContext(PollForActivityTaskResponse response) {
      if (options.getContextPropagators() == null || options.getContextPropagators().isEmpty()) {
        return;
      }

      Header headers = response.getHeader();
      if (headers == null) {
        return;
      }

      Map<String, byte[]> headerData = new HashMap<>();
      headers
          .getFields()
          .forEach(
              (k, v) -> {
                headerData.put(k, org.apache.thrift.TBaseHelper.byteBufferToByteArray(v));
              });

      for (ContextPropagator propagator : options.getContextPropagators()) {
        // Only send the context propagator the fields that belong to them
        // Change the map from MyPropagator:foo -> bar to foo -> bar
        Map<String, byte[]> filteredData =
            headerData
                .entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith(propagator.getName()))
                .collect(
                    Collectors.toMap(
                        e -> e.getKey().substring(propagator.getName().length() + 1),
                        Map.Entry::getValue));
        propagator.setCurrentContext(propagator.deserializeContext(filteredData));
      }
    }

    @Override
    public Throwable wrapFailure(PollForActivityTaskResponse task, Throwable failure) {
      WorkflowExecution execution = task.getWorkflowExecution();
      return new RuntimeException(
          "Failure processing activity task. WorkflowID="
              + execution.getWorkflowId()
              + ", RunID="
              + execution.getRunId()
              + ", ActivityType="
              + task.getActivityType().getName()
              + ", ActivityID="
              + task.getActivityId(),
          failure);
    }

    private void sendReply(
        PollForActivityTaskResponse task, ActivityTaskHandler.Result response, Scope metricsScope)
        throws TException {
      RetryOptions ro = response.getRequestRetryOptions();
      RespondActivityTaskCompletedRequest taskCompleted = response.getTaskCompleted();
      if (taskCompleted != null) {
        ro =
            options
                .getReportCompletionRetryOptions()
                .merge(ro)
                .addDoNotRetry(
                    BadRequestError.class, EntityNotExistsError.class, DomainNotActiveError.class);
        taskCompleted.setTaskToken(task.getTaskToken());
        taskCompleted.setIdentity(options.getIdentity());
        Retryer.retry(ro, () -> service.RespondActivityTaskCompleted(taskCompleted));
        metricsScope.counter(MetricsType.ACTIVITY_TASK_COMPLETED_COUNTER).inc(1);
      } else {
        if (response.getTaskFailedResult() != null) {
          RespondActivityTaskFailedRequest taskFailed =
              response.getTaskFailedResult().getTaskFailedRequest();
          ro =
              options
                  .getReportFailureRetryOptions()
                  .merge(ro)
                  .addDoNotRetry(
                      BadRequestError.class,
                      EntityNotExistsError.class,
                      DomainNotActiveError.class);
          taskFailed.setTaskToken(task.getTaskToken());
          taskFailed.setIdentity(options.getIdentity());
          Retryer.retry(ro, () -> service.RespondActivityTaskFailed(taskFailed));
          metricsScope.counter(MetricsType.ACTIVITY_TASK_FAILED_COUNTER).inc(1);
        } else {
          RespondActivityTaskCanceledRequest taskCancelled = response.getTaskCancelled();
          if (taskCancelled != null) {
            taskCancelled.setTaskToken(task.getTaskToken());
            taskCancelled.setIdentity(options.getIdentity());
            ro =
                options
                    .getReportFailureRetryOptions()
                    .merge(ro)
                    .addDoNotRetry(
                        BadRequestError.class,
                        EntityNotExistsError.class,
                        DomainNotActiveError.class);
            Retryer.retry(ro, () -> service.RespondActivityTaskCanceled(taskCancelled));
            metricsScope.counter(MetricsType.ACTIVITY_TASK_CANCELED_COUNTER).inc(1);
          }
        }
      }
      // Manual activity completion
    }
  }
}
