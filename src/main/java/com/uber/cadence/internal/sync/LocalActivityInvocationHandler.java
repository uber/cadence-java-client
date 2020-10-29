/*
 *  Modifications Copyright (c) 2017-2020 Uber Technologies Inc.
 *  Portions of the Software are attributed to Copyright (c) 2020 Temporal Technologies Inc.
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.uber.cadence.internal.sync;

import com.uber.cadence.activity.ActivityMethod;
import com.uber.cadence.activity.LocalActivityOptions;
import com.uber.cadence.common.MethodRetry;
import com.uber.cadence.workflow.ActivityStub;
import com.uber.cadence.workflow.WorkflowInterceptor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.function.Function;

class LocalActivityInvocationHandler extends ActivityInvocationHandlerBase {
  private final LocalActivityOptions options;
  private final WorkflowInterceptor activityExecutor;

  static InvocationHandler newInstance(
      Class<?> activityInterface,
      LocalActivityOptions options,
      WorkflowInterceptor activityExecutor) {
    return new LocalActivityInvocationHandler(activityInterface, activityExecutor, options);
  }

  private LocalActivityInvocationHandler(
      Class<?> activityInterface,
      WorkflowInterceptor activityExecutor,
      LocalActivityOptions options) {
    this.options = options;
    this.activityExecutor = activityExecutor;
    init(activityInterface);
  }

  @SuppressWarnings("deprecation")
  @Override
  protected Function<Object[], Object> getActivityFunc(
      Method method, MethodRetry methodRetry, ActivityMethod activityMethod, String activityName) {
    Function<Object[], Object> function;
    LocalActivityOptions.Builder optionsBuilder =
        LocalActivityOptions.newBuilder(options).setMethodRetry(methodRetry);
    if (activityMethod != null) {
      // options always take precedence over activity method annotation.
      if (options.getScheduleToCloseTimeout() == null) {
        optionsBuilder.setScheduleToCloseTimeout(
            Duration.ofSeconds(activityMethod.scheduleToCloseTimeoutSeconds()));
      }
    }
    LocalActivityOptions mergedOptions = optionsBuilder.validateAndBuildWithDefaults();
    ActivityStub stub = LocalActivityStubImpl.newInstance(mergedOptions, activityExecutor);
    function =
        (a) -> stub.execute(activityName, method.getReturnType(), method.getGenericReturnType(), a);
    return function;
  }
}
