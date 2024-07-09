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

package com.uber.cadence.internal.common;

import java.lang.reflect.InvocationTargetException;

/**
 * Do not reference directly by the application level code. Use {@link
 * com.uber.cadence.workflow.Workflow#wrap(Exception)} inside a workflow code and {@link
 * com.uber.cadence.activity.Activity#wrap(Exception)} inside an activity code instead.
 */
public final class CheckedExceptionWrapper extends RuntimeException {

  /**
   * Returns CheckedExceptionWrapper if e is checked exception. If there is a need to return a
   * checked exception from an activity or workflow implementation throw a wrapped exception it
   * using this method. The library code will unwrap it automatically when propagating exception to
   * the caller.
   *
   * <pre>
   * try {
   *     return someCall();
   * } catch (Exception e) {
   *     throw CheckedExceptionWrapper.wrap(e);
   * }
   * </pre>
   */
  public static RuntimeException wrap(Throwable e) {
    // Errors are expected to propagate without any handling.
    if (e instanceof Error) {
      throw (Error) e;
    }
    if (e instanceof InvocationTargetException) {
      return wrap(e.getCause());
    }
    if (e instanceof RuntimeException) {
      return (RuntimeException) e;
    }
    return new CheckedExceptionWrapper((Exception) e);
  }

  /**
   * Returns the underlying cause of {@code e} if it is a CheckedExceptionWrapper. Otherwise returns {@code e).
   */
  public static Exception unwrap(Exception e) {
    // the constructor accepts Exception so this is always safe
    return e instanceof CheckedExceptionWrapper ? (Exception) e.getCause() : e;
  }

  /**
   * Returns the underlying cause of {@code e} if it is a CheckedExceptionWrapper. Otherwise returns {@code e).
   */
  public static Throwable unwrap(Throwable t) {
    return t instanceof CheckedExceptionWrapper ? t.getCause() : t;
  }

  private CheckedExceptionWrapper(Exception e) {
    super(e);
  }
}
