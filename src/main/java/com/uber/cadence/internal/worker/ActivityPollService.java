package com.uber.cadence.internal.worker;

import com.uber.cadence.*;
import com.uber.cadence.internal.metrics.MetricsType;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.m3.tally.Stopwatch;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ActivityPollService implements Poller2.Poll<ActivityWorker.MeasurableActivityTask>{

  private IWorkflowService service;
  private String domain;
  private String taskList;
  private SingleWorkerOptions options;
  private static final Logger log = LoggerFactory.getLogger(ActivityPollService.class);

  public ActivityPollService(IWorkflowService service, String domain, String taskList, SingleWorkerOptions options){

    this.service = service;
    this.domain = domain;
    this.taskList = taskList;
    this.options = options;
  }
  @Override
  public ActivityWorker.MeasurableActivityTask poll() throws TException {
    options.getMetricsScope().counter(MetricsType.ACTIVITY_POLL_COUNTER).inc(1);
    Stopwatch sw = options.getMetricsScope().timer(MetricsType.ACTIVITY_POLL_LATENCY).start();
    Stopwatch e2eSW = options.getMetricsScope().timer(MetricsType.ACTIVITY_E2E_LATENCY).start();

    PollForActivityTaskRequest pollRequest = new PollForActivityTaskRequest();
    pollRequest.setDomain(domain);
    pollRequest.setIdentity(options.getIdentity());
    pollRequest.setTaskList(new TaskList().setName(taskList));
    if (log.isDebugEnabled()) {
      log.debug("poll request begin: " + pollRequest);
    }
    PollForActivityTaskResponse result;
    try {
      result = service.PollForActivityTask(pollRequest);
    } catch (InternalServiceError | ServiceBusyError e) {
      options
              .getMetricsScope()
              .counter(MetricsType.ACTIVITY_POLL_TRANSIENT_FAILED_COUNTER)
              .inc(1);
      throw e;
    } catch (TException e) {
      options.getMetricsScope().counter(MetricsType.ACTIVITY_POLL_FAILED_COUNTER).inc(1);
      throw e;
    }

    if (result == null || result.getTaskToken() == null) {
      if (log.isDebugEnabled()) {
        log.debug("poll request returned no task");
      }
      options.getMetricsScope().counter(MetricsType.ACTIVITY_POLL_NO_TASK_COUNTER).inc(1);
      return null;
    }

    if (log.isTraceEnabled()) {
      log.trace("poll request returned " + result);
    }

    options.getMetricsScope().counter(MetricsType.ACTIVITY_POLL_SUCCEED_COUNTER).inc(1);
    sw.stop();
    return new ActivityWorker.MeasurableActivityTask(result, e2eSW);
  }
}
