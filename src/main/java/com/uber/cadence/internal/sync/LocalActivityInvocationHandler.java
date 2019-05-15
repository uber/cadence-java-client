package com.uber.cadence.internal.sync;

import com.uber.cadence.activity.ActivityMethod;
import com.uber.cadence.activity.LocalActivityOptions;
import com.uber.cadence.common.MethodRetry;
import com.uber.cadence.workflow.ActivityStub;
import com.uber.cadence.workflow.WorkflowInterceptor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.function.Function;

public class LocalActivityInvocationHandler extends ActivityInvocationHandlerBase {
  private final LocalActivityOptions options;
  private final WorkflowInterceptor activityExecutor;

  static InvocationHandler newInstance(
      LocalActivityOptions options, WorkflowInterceptor activityExecutor) {
    return new LocalActivityInvocationHandler(options, activityExecutor);
  }

  private LocalActivityInvocationHandler(
      LocalActivityOptions options, WorkflowInterceptor activityExecutor) {
    this.options = options;
    this.activityExecutor = activityExecutor;
  }

  @Override
  Function<Object[], Object> getActivityFunc(
      Method method, MethodRetry methodRetry, ActivityMethod activityMethod, String activityName) {
    Function<Object[], Object> function;
    LocalActivityOptions mergedOptions =
        LocalActivityOptions.merge(activityMethod, methodRetry, options);
    ActivityStub stub = LocalActivityStubImpl.newInstance(mergedOptions, activityExecutor);
    function =
        (a) -> stub.execute(activityName, method.getReturnType(), method.getGenericReturnType(), a);
    return function;
  }
}
