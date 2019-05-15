package com.uber.cadence.internal.sync;

import com.uber.cadence.activity.ActivityOptions;
import com.uber.cadence.workflow.ActivityStub;
import com.uber.cadence.workflow.Promise;
import com.uber.cadence.workflow.WorkflowInterceptor;
import java.lang.reflect.Type;

public class ActivityStubImpl extends ActivityStubBase {
  protected final ActivityOptions options;
  private final WorkflowInterceptor activityExecutor;

  static ActivityStub newInstance(ActivityOptions options, WorkflowInterceptor activityExecutor) {
    ActivityOptions validatedOptions =
        new ActivityOptions.Builder(options).validateAndBuildWithDefaults();
    return new ActivityStubImpl(validatedOptions, activityExecutor);
  }

  ActivityStubImpl(ActivityOptions options, WorkflowInterceptor activityExecutor) {
    this.options = options;
    this.activityExecutor = activityExecutor;
  }

  @Override
  public <R> Promise<R> executeAsync(
      String activityName, Class<R> resultClass, Type resultType, Object... args) {
    return activityExecutor.executeActivity(activityName, resultClass, resultType, args, options);
  }
}
