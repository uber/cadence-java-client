/**
 * Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * <p>Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. A copy of the License is located at
 *
 * <p>http://aws.amazon.com/apache2.0
 *
 * <p>or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.uber.cadence.client;

import static junit.framework.TestCase.*;
import static org.mockito.Mockito.*;

import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.activity.ActivityTask;
import org.junit.Before;
import org.junit.Test;

public class ActivityCompletionExceptionTest {

  private ActivityTask mockTask;
  private WorkflowExecution mockExecution;

  @Before
  public void setUp() {
    // Set up mock objects for testing
    mockTask = mock(ActivityTask.class);
    mockExecution = mock(WorkflowExecution.class);

    when(mockTask.getWorkflowExecution()).thenReturn(mockExecution);
    when(mockTask.getActivityType()).thenReturn("TestActivityType");
    when(mockTask.getActivityId()).thenReturn("TestActivityId");
  }

  @Test
  public void testConstructorWithActivityTask() {
    ActivityCompletionException exception = new ActivityCompletionException(mockTask);

    assertEquals(mockExecution, exception.getExecution());
    assertEquals("TestActivityType", exception.getActivityType());
    assertEquals("TestActivityId", exception.getActivityId());
    assertNull(exception.getCause());
  }

  @Test
  public void testConstructorWithActivityTaskAndCause() {
    Throwable cause = new Throwable("TestCause");
    ActivityCompletionException exception = new ActivityCompletionException(mockTask, cause);

    assertEquals(mockExecution, exception.getExecution());
    assertEquals("TestActivityType", exception.getActivityType());
    assertEquals("TestActivityId", exception.getActivityId());
    assertEquals(cause, exception.getCause());
  }

  @Test
  public void testConstructorWithActivityIdAndCause() {
    Throwable cause = new Throwable("TestCause");
    ActivityCompletionException exception =
        new ActivityCompletionException("TestActivityId", cause);

    assertNull(exception.getExecution());
    assertNull(exception.getActivityType());
    assertEquals("TestActivityId", exception.getActivityId());
    assertEquals(cause, exception.getCause());
  }

  @Test
  public void testConstructorWithCauseOnly() {
    Throwable cause = new Throwable("TestCause");
    ActivityCompletionException exception = new ActivityCompletionException(cause);

    assertNull(exception.getExecution());
    assertNull(exception.getActivityType());
    assertNull(exception.getActivityId());
    assertEquals(cause, exception.getCause());
  }

  @Test
  public void testDefaultConstructor() {
    ActivityCompletionException exception = new ActivityCompletionException();

    assertNull(exception.getExecution());
    assertNull(exception.getActivityType());
    assertNull(exception.getActivityId());
    assertNull(exception.getCause());
  }

  @Test
  public void testExceptionMessageWithActivityTaskAndCause() {
    Throwable cause = new Throwable("TestCause");
    ActivityCompletionException exception = new ActivityCompletionException(mockTask, cause);

    String expectedMessage =
        "Execution=" + mockExecution + ", ActivityType=TestActivityType, ActivityID=TestActivityId";
    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  public void testExceptionMessageWithActivityIdAndCause() {
    Throwable cause = new Throwable("TestCause");
    ActivityCompletionException exception =
        new ActivityCompletionException("TestActivityId", cause);

    String expectedMessage = "ActivityIdTestActivityId";
    assertEquals(expectedMessage, exception.getMessage());
  }
}
