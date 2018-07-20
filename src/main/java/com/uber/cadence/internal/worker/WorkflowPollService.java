package com.uber.cadence.internal.worker;

import com.google.common.base.Preconditions;
import com.uber.cadence.*;
import com.uber.cadence.internal.metrics.MetricsType;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.m3.tally.Stopwatch;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WorkflowPollService implements Poller2.Poll<PollForDecisionTaskResponse>{

  private SingleWorkerOptions options;
  private IWorkflowService service;
  private String domain;
  private String taskList;
  private static final Logger log = LoggerFactory.getLogger(WorkflowWorker.class);

  WorkflowPollService(IWorkflowService service, String domain, String taskList, SingleWorkerOptions options){
    Preconditions.checkNotNull(service, "service should not be null");
    Preconditions.checkNotNull(domain, "domain should not be null");
    Preconditions.checkNotNull(taskList, "taskList should not be null");
    Preconditions.checkNotNull(options, "options should not be null");


    this.service = service;
    this.domain = domain;
    this.taskList = taskList;
    this.options = options;
  }

  @Override
  public PollForDecisionTaskResponse poll() throws TException {
    options.getMetricsScope().counter(MetricsType.DECISION_POLL_COUNTER).inc(1);
    Stopwatch sw = options.getMetricsScope().timer(MetricsType.DECISION_POLL_LATENCY).start();

    PollForDecisionTaskRequest pollRequest = new PollForDecisionTaskRequest();
    pollRequest.setDomain(domain);
    pollRequest.setIdentity(options.getIdentity());

    TaskList tl = new TaskList();
    tl.setName(taskList);
    pollRequest.setTaskList(tl);

    if (log.isDebugEnabled()) {
      log.debug("poll request begin: " + pollRequest);
    }
    PollForDecisionTaskResponse result;
    try {
      result = service.PollForDecisionTask(pollRequest);
    } catch (InternalServiceError | ServiceBusyError e) {
      options
              .getMetricsScope()
              .counter(MetricsType.DECISION_POLL_TRANSIENT_FAILED_COUNTER)
              .inc(1);
      throw e;
    } catch (TException e) {
      options.getMetricsScope().counter(MetricsType.DECISION_POLL_FAILED_COUNTER).inc(1);
      throw e;
    }

    if (log.isDebugEnabled()) {
      log.debug(
              "poll request returned decision task: workflowType="
                      + result.getWorkflowType()
                      + ", workflowExecution="
                      + result.getWorkflowExecution()
                      + ", startedEventId="
                      + result.getStartedEventId()
                      + ", previousStartedEventId="
                      + result.getPreviousStartedEventId()
                      + (result.getQuery() != null
                      ? ", queryType=" + result.getQuery().getQueryType()
                      : ""));
    }

    if (result == null || result.getTaskToken() == null) {
      options.getMetricsScope().counter(MetricsType.DECISION_POLL_NO_TASK_COUNTER).inc(1);
      return null;
    }

    options.getMetricsScope().counter(MetricsType.DECISION_POLL_SUCCEED_COUNTER).inc(1);
    sw.stop();
    return result;
  }
}
