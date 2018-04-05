package com.uber.cadence.workflow;

import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.activity.ActivityOptions;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

public class WorkflowInterceptorBase implements WorkflowInterceptor {

  private final WorkflowInterceptor next;

  public WorkflowInterceptorBase(WorkflowInterceptor next) {
    this.next = next;
  }

  @Override
  public <R> Promise<R> executeActivity(String activityName, Class<R> returnType, Object[] args,
      ActivityOptions options) {
    return next.executeActivity(activityName, returnType, args, options);
  }

  @Override
  public <R> WorkflowResult<R> executeChildWorkflow(String workflowType, Class<R> returnType,
      Object[] args, ChildWorkflowOptions options) {
    return next.executeChildWorkflow(workflowType, returnType, args, options);
  }

  @Override
  public void signalWorkflow(WorkflowExecution execution, String signalName, Object[] args) {
    next.signalWorkflow(execution, signalName, args);
  }

  @Override
  public void cancelWorkflow(WorkflowExecution execution) {
    next.cancelWorkflow(execution);
  }

  @Override
  public void sleep(Duration duration) {
    next.sleep(duration);
  }

  @Override
  public boolean await(Duration timeout, Supplier<Boolean> unblockCondition) {
    return next.await(timeout, unblockCondition);
  }

  @Override
  public boolean await(Supplier<Boolean> unblockCondition) {
    return next.await(unblockCondition);

  }

  @Override
  public Promise<Void> createTimer(Duration duration) {
    return next.createTimer(duration);
  }

  @Override
  public void continueAsNew(Optional<String> workflowType, Optional<ContinueAsNewOptions> options,
      Object[] args) {
    next.continueAsNew(workflowType, options, args);
  }
}
