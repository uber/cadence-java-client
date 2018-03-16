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

package com.uber.cadence.internal.sync;

import com.uber.cadence.testing.TestActivityEnvironment;
import com.uber.cadence.workflow.Functions;
import com.uber.cadence.workflow.Promise;

public class TestActivityEnvironmentInternal implements TestActivityEnvironment {

  //    private Method findInterfaceMethod(Object function) {
  //        SerializedLambda lambda = toSerializedLambda(function);
  //        if (lambda == null || lambda.getImplMethodKind() == MethodHandleInfo.REF_invokeInterface) {
  //            throw new IllegalArgumentException("Expected activity implementation method reference: " + lambda.getImplMethodSignature())
  //        }
  //        Object target = getTarget(lambda);
  //        if (target == null) {
  //            throw new IllegalArgumentException("Expected activity implementation method reference: " + lambda.getImplMethodSignature())
  //        }
  //
  //    }

  @Override
  public <R> R executeActivity(Functions.Func<R> function) {
    return null;
  }

  @Override
  public <A1, R> R executeActivity(Functions.Func1<A1, R> function, A1 arg1) {
    return null;
  }

  @Override
  public <A1, A2, R> R executeActivity(Functions.Func2<A1, A2, R> function, A1 arg1, A2 arg2) {
    return null;
  }

  @Override
  public <A1, A2, A3, R> R executeActivity(
      Functions.Func3<A1, A2, A3, R> function, A1 arg1, A2 arg2, A3 arg3) {
    return null;
  }

  @Override
  public <A1, A2, A3, A4, R> R executeActivity(
      Functions.Func4<A1, A2, A3, A4, R> function, A1 arg1, A2 arg2, A3 arg3, A4 arg4) {
    return null;
  }

  @Override
  public <A1, A2, A3, A4, A5, R> R executeActivity(
      Functions.Func5<A1, A2, A3, A4, A5, R> function,
      A1 arg1,
      A2 arg2,
      A3 arg3,
      A4 arg4,
      A5 arg5) {
    return null;
  }

  @Override
  public <A1, A2, A3, A4, A5, A6, R> R executeActivity(
      Functions.Func6<A1, A2, A3, A4, A5, A6, R> function,
      A1 arg1,
      A2 arg2,
      A3 arg3,
      A4 arg4,
      A5 arg5,
      A6 arg6) {
    return null;
  }

  @Override
  public Promise<Void> executeActivity(Functions.Proc activity) {
    return null;
  }

  @Override
  public <A1> Promise<Void> executeActivity(Functions.Proc1<A1> activity, A1 arg1) {
    return null;
  }

  @Override
  public <A1, A2> Promise<Void> executeActivity(
      Functions.Proc2<A1, A2> activity, A1 arg1, A2 arg2) {
    return null;
  }

  @Override
  public <A1, A2, A3> Promise<Void> executeActivity(
      Functions.Proc3<A1, A2, A3> activity, A1 arg1, A2 arg2, A3 arg3) {
    return null;
  }

  @Override
  public <A1, A2, A3, A4> Promise<Void> executeActivity(
      Functions.Proc4<A1, A2, A3, A4> activity, A1 arg1, A2 arg2, A3 arg3, A4 arg4) {
    return null;
  }

  @Override
  public <A1, A2, A3, A4, A5> Promise<Void> executeActivity(
      Functions.Proc5<A1, A2, A3, A4, A5> activity, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5) {
    return null;
  }

  @Override
  public <A1, A2, A3, A4, A5, A6> Promise<Void> executeActivity(
      Functions.Proc6<A1, A2, A3, A4, A5, A6> activity,
      A1 arg1,
      A2 arg2,
      A3 arg3,
      A4 arg4,
      A5 arg5,
      A6 arg6) {
    return null;
  }
}
