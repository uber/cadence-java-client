package com.uber.cadence.internal.worker;

import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.serviceclient.WorkflowServiceTChannel;
import java.util.Objects;

public final class PollDecisionTaskDispatcherFactory implements DispatcherFactory {
  private IWorkflowService service = new WorkflowServiceTChannel();
  private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

  public PollDecisionTaskDispatcherFactory(
      IWorkflowService service, Thread.UncaughtExceptionHandler handler) {
    this.service = Objects.requireNonNull(service);
    uncaughtExceptionHandler = handler;
  }

  public PollDecisionTaskDispatcherFactory() {}

  @Override
  public Dispatcher create() {
    return new PollDecisionTaskDispatcher(service, uncaughtExceptionHandler);
  }
}
