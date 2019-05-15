package com.uber.cadence.internal.sync;

import com.uber.cadence.activity.LocalActivityOptions;
import com.uber.cadence.workflow.ActivityStub;
import com.uber.cadence.workflow.Promise;
import com.uber.cadence.workflow.WorkflowInterceptor;
import java.lang.reflect.Type;

public class LocalActivityStubImpl extends ActivityStubBase {
  protected final LocalActivityOptions options;
  private final WorkflowInterceptor activityExecutor;

  static ActivityStub newInstance(
      LocalActivityOptions options, WorkflowInterceptor activityExecutor) {
    LocalActivityOptions validatedOptions =
        new LocalActivityOptions.Builder(options).validateAndBuildWithDefaults();
    return new LocalActivityStubImpl(validatedOptions, activityExecutor);
  }

  LocalActivityStubImpl(LocalActivityOptions options, WorkflowInterceptor activityExecutor) {
    this.options = options;
    this.activityExecutor = activityExecutor;
  }

  @Override
  public <R> Promise<R> executeAsync(
      String activityName, Class<R> resultClass, Type resultType, Object... args) {
    return activityExecutor.executeLocalActivity(
        activityName, resultClass, resultType, args, options);
  }
}
