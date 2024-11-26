/*
 Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.

 Modifications copyright (C) 2017 Uber Technologies, Inc.

 Licensed under the Apache License, Version 2.0 (the "License"). You may not
 use this file except in compliance with the License. A copy of the License is
 located at

 http://aws.amazon.com/apache2.0

 or in the "license" file accompanying this file. This file is distributed on
 an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 express or implied. See the License for the specific language governing
 permissions and limitations under the License.
*/

package com.uber.cadence.internal.sync;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.workflow.Functions;
import com.uber.cadence.workflow.WorkflowInterceptorBase;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TestActivityEnvironmentInternalTest {
  @Mock private IWorkflowService mockWorkflowService;

  @Mock private WorkflowInterceptorBase mockNext;

  private Object testActivityExecutor;

  // Helper method to find the inner class
  private Class<?> findTestActivityExecutorClass() {
    for (Class<?> declaredClass : TestActivityEnvironmentInternal.class.getDeclaredClasses()) {
      if (declaredClass.getSimpleName().equals("TestActivityExecutor")) {
        return declaredClass;
      }
    }
    throw new RuntimeException("Could not find TestActivityExecutor inner class");
  }

  // Helper method to print all methods
  private void printMethods(Class<?> clazz) {
    System.out.println("Methods for " + clazz.getName() + ":");
    for (Method method : clazz.getDeclaredMethods()) {
      System.out.println("  " + method);
    }
  }

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    try {
      // Find the inner class first
      Class<?> innerClass = findTestActivityExecutorClass();

      // Get the constructor with the specific parameter types
      Constructor<?> constructor =
          innerClass.getDeclaredConstructor(
              TestActivityEnvironmentInternal.class,
              IWorkflowService.class,
              WorkflowInterceptorBase.class);
      constructor.setAccessible(true);

      // Create an instance of the outer class
      TestActivityEnvironmentInternal outerInstance = mock(TestActivityEnvironmentInternal.class);

      // Create the instance
      testActivityExecutor = constructor.newInstance(outerInstance, mockWorkflowService, mockNext);

      // Debug print the class and methods
      System.out.println("TestActivityExecutor class: " + innerClass);
      printMethods(innerClass);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Failed to set up test: " + e.getMessage(), e);
    }
  }

  @Test
  public void testAllMethodsThrowUnsupportedOperationException() throws Exception {
    // Define test cases for different methods
    MethodTestCase[] methodCases = {
      // Signature: newRandom()
      new MethodTestCase("newRandom", new Class<?>[0], new Object[0]),

      // Signature: signalExternalWorkflow(String, WorkflowExecution, String, Object[])
      new MethodTestCase(
          "signalExternalWorkflow",
          new Class<?>[] {String.class, WorkflowExecution.class, String.class, Object[].class},
          new Object[] {
            "testSignal", mock(WorkflowExecution.class), "signalName", new Object[] {}
          }),

      // Signature: signalExternalWorkflow(WorkflowExecution, String, Object[])
      new MethodTestCase(
          "signalExternalWorkflow",
          new Class<?>[] {WorkflowExecution.class, String.class, Object[].class},
          new Object[] {mock(WorkflowExecution.class), "signalName", new Object[] {}}),

      // Signature: cancelWorkflow(WorkflowExecution)
      new MethodTestCase(
          "cancelWorkflow",
          new Class<?>[] {WorkflowExecution.class},
          new Object[] {mock(WorkflowExecution.class)}),

      // Signature: sleep(Duration)
      new MethodTestCase(
          "sleep", new Class<?>[] {Duration.class}, new Object[] {Duration.ofSeconds(1)}),

      // Signature: await(Duration, String, Supplier)
      new MethodTestCase(
          "await",
          new Class<?>[] {Duration.class, String.class, Supplier.class},
          new Object[] {Duration.ofSeconds(1), "testReason", (Supplier<?>) () -> true}),

      // Signature: await(String, Supplier)
      new MethodTestCase(
          "await",
          new Class<?>[] {String.class, Supplier.class},
          new Object[] {"testReason", (Supplier<?>) () -> true}),

      // Signature: newTimer(Duration)
      new MethodTestCase(
          "newTimer", new Class<?>[] {Duration.class}, new Object[] {Duration.ofSeconds(1)}),

      // Signature: sideEffect(Class, Type, Functions.Func)
      new MethodTestCase(
          "sideEffect",
          new Class<?>[] {Class.class, Type.class, Functions.Func.class},
          new Object[] {String.class, String.class, (Functions.Func<String>) () -> "test"}),

      // Signature: mutableSideEffect(String, Class, Type, BiPredicate, Functions.Func)
      new MethodTestCase(
          "mutableSideEffect",
          new Class<?>[] {
            String.class, Class.class, Type.class, BiPredicate.class, Functions.Func.class
          },
          new Object[] {
            "testId",
            String.class,
            String.class,
            (BiPredicate<String, String>) (a, b) -> false,
            (Functions.Func<String>) () -> "test"
          }),

      // Signature: getVersion(String, int, int)
      new MethodTestCase(
          "getVersion",
          new Class<?>[] {String.class, int.class, int.class},
          new Object[] {"changeId", 0, 1}),

      // Signature: continueAsNew(Optional, Optional, Object[])
      new MethodTestCase(
          "continueAsNew",
          new Class<?>[] {Optional.class, Optional.class, Object[].class},
          new Object[] {Optional.empty(), Optional.empty(), new Object[] {}}),

      // Signature: registerQuery(String, Type[], Func1)
      new MethodTestCase(
          "registerQuery",
          new Class<?>[] {String.class, Type[].class, Functions.Func1.class},
          new Object[] {
            "queryType",
            new Type[] {String.class},
            (Functions.Func1<Object[], Object>) args -> "result"
          }),

      // Signature: randomUUID()
      new MethodTestCase("randomUUID", new Class<?>[0], new Object[0]),

      // Signature: upsertSearchAttributes(Map)
      new MethodTestCase(
          "upsertSearchAttributes",
          new Class<?>[] {Map.class},
          new Object[] {java.util.Collections.emptyMap()})
    };

    // Test each method
    for (MethodTestCase testCase : methodCases) {
      try {
        // Find the method
        Method method =
            testActivityExecutor
                .getClass()
                .getDeclaredMethod(testCase.methodName, testCase.parameterTypes);
        method.setAccessible(true);

        // Invoke the method
        Object result = method.invoke(testActivityExecutor, testCase.arguments);

        // If we get here, the method did not throw UnsupportedOperationException
        fail("Expected UnsupportedOperationException for method " + testCase.methodName);

      } catch (Exception e) {
        // Check if the cause is UnsupportedOperationException
        if (!(e.getCause() instanceof UnsupportedOperationException)) {
          // If it's not the expected exception, rethrow
          throw new RuntimeException("Unexpected exception for method " + testCase.methodName, e);
        }
        // Expected behavior - UnsupportedOperationException was thrown
        // Continue to next method
      }
    }
  }

  // Helper class to encapsulate method test cases
  private static class MethodTestCase {
    String methodName;
    Class<?>[] parameterTypes;
    Object[] arguments;

    MethodTestCase(String methodName, Class<?>[] parameterTypes, Object[] arguments) {
      this.methodName = methodName;
      this.parameterTypes = parameterTypes;
      this.arguments = arguments;
    }
  }
}
