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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOError;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import org.junit.Test;

public class CheckedExceptionWrapperTest {

  @Test
  public void testWrap() {
    IOException exception = new IOException("oh no");

    RuntimeException wrapped = CheckedExceptionWrapper.wrap(exception);

    assertEquals(wrapped.getCause(), exception);
    assertTrue(wrapped instanceof CheckedExceptionWrapper);
  }

  @Test
  public void testWrap_rethrowsError() {
    assertThrows(
        IOError.class,
        () -> {
          throw CheckedExceptionWrapper.wrap(new IOError(null));
        });
  }

  @Test
  public void testWrap_getsInvocationTargetCause() {
    IOException exception = new IOException("oh no");
    InvocationTargetException invocationTargetException = new InvocationTargetException(exception);

    RuntimeException wrapped = CheckedExceptionWrapper.wrap(invocationTargetException);

    assertEquals(wrapped.getCause(), exception);
    assertTrue(wrapped instanceof CheckedExceptionWrapper);
  }

  @Test
  public void testWrap_doesntWrapRuntimeException() {
    RuntimeException exception = new RuntimeException("oh no");

    RuntimeException wrapped = CheckedExceptionWrapper.wrap(exception);

    assertEquals(exception, wrapped);
  }

  @Test
  public void testWrap_doesntWrapNestedRuntimeException() {
    RuntimeException exception = new RuntimeException("oh no");
    InvocationTargetException invocationTargetException = new InvocationTargetException(exception);

    RuntimeException wrapped = CheckedExceptionWrapper.wrap(invocationTargetException);

    assertEquals(exception, wrapped);
  }

  @Test
  public void testUnwrap() {
    IOException exception = new IOException("oh no");
    RuntimeException wrapped = CheckedExceptionWrapper.wrap(exception);

    Exception unwrapped = CheckedExceptionWrapper.unwrap(wrapped);

    assertEquals(exception, unwrapped);
  }

  @Test
  public void testUnwrap_errorUnchanged() {
    Error error = new Error("oh no");

    Throwable unwrapped = CheckedExceptionWrapper.unwrap(error);

    assertEquals(error, unwrapped);
  }

  @Test
  public void testUnwrap_topLevelOnly() {
    IOException exception = new IOException("oh no");
    RuntimeException wrapped = CheckedExceptionWrapper.wrap(exception);
    IOException anotherLayer = new IOException("it gets worse", wrapped);
    RuntimeException wrappedAgain = CheckedExceptionWrapper.wrap(anotherLayer);

    Exception unwrapped = CheckedExceptionWrapper.unwrap(wrappedAgain);

    assertEquals(anotherLayer, unwrapped);
  }
}
