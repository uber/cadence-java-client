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

package com.uber.cadence.context;

import static org.junit.Assert.*;

import com.uber.cadence.activity.ActivityMethod;
import com.uber.cadence.activity.ActivityOptions;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowClientOptions;
import com.uber.cadence.client.WorkflowException;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.internal.testing.WorkflowTestingTest.ChildWorkflow;
import com.uber.cadence.testing.TestEnvironmentOptions;
import com.uber.cadence.testing.TestWorkflowEnvironment;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.workflow.Async;
import com.uber.cadence.workflow.ChildWorkflowOptions;
import com.uber.cadence.workflow.Promise;
import com.uber.cadence.workflow.SignalMethod;
import com.uber.cadence.workflow.Workflow;
import com.uber.cadence.workflow.WorkflowMethod;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class ContextTests {
  private static final Logger log = LoggerFactory.getLogger(ContextTests.class);

  @Rule public Timeout globalTimeout = Timeout.seconds(5000);

  @Rule
  public TestWatcher watchman =
      new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
          System.err.println(testEnvironment.getDiagnostics());
        }
      };

  private static final String TASK_LIST = "test-workflow";

  private TestWorkflowEnvironment testEnvironment;

  @Before
  public void setUp() {
    WorkflowClientOptions clientOptions =
        WorkflowClientOptions.newBuilder()
            .setContextPropagators(
                Arrays.asList(new TestContextPropagator(), new OpenTelemetryContextPropagator()))
            .build();
    TestEnvironmentOptions options =
        new TestEnvironmentOptions.Builder().setWorkflowClientOptions(clientOptions).build();
    testEnvironment = TestWorkflowEnvironment.newInstance(options);
  }

  @After
  public void tearDown() {
    testEnvironment.close();
  }

  public interface TestWorkflow {

    @WorkflowMethod(executionStartToCloseTimeoutSeconds = 3600 * 24, taskList = TASK_LIST)
    String workflow1(String input);
  }

  public interface ParentWorkflow {

    @WorkflowMethod(executionStartToCloseTimeoutSeconds = 3600 * 24, taskList = TASK_LIST)
    String workflow(String input);

    @SignalMethod
    void signal(String value);
  }

  public interface TestActivity {

    @ActivityMethod(scheduleToCloseTimeoutSeconds = 3600)
    String activity1(String input);
  }

  public static class TestContextPropagator implements ContextPropagator {

    @Override
    public String getName() {
      return "TestContextPropagator::withSomeColons";
    }

    @Override
    public Map<String, byte[]> serializeContext(Object context) {
      String testKey = (String) context;
      if (testKey != null) {
        return Collections.singletonMap("test", testKey.getBytes(StandardCharsets.UTF_8));
      } else {
        return Collections.emptyMap();
      }
    }

    @Override
    public Object deserializeContext(Map<String, byte[]> context) {
      if (context.containsKey("test")) {
        return new String(context.get("test"), StandardCharsets.UTF_8);
      } else {
        return null;
      }
    }

    @Override
    public Object getCurrentContext() {
      return MDC.get("test");
    }

    @Override
    public void setCurrentContext(Object context) {
      MDC.put("test", String.valueOf(context));
    }
  }

  public static class ContextPropagationWorkflowImpl implements TestWorkflow {

    @Override
    public String workflow1(String input) {
      // The test value should be in the MDC
      return MDC.get("test");
    }
  }

  @Test()
  public void testWorkflowContextPropagation() {
    Worker worker = testEnvironment.newWorker(TASK_LIST);
    worker.registerWorkflowImplementationTypes(ContextPropagationWorkflowImpl.class);
    testEnvironment.start();
    MDC.put("test", "testing123");
    WorkflowClient client = testEnvironment.newWorkflowClient();
    WorkflowOptions options =
        new WorkflowOptions.Builder()
            .setContextPropagators(Collections.singletonList(new TestContextPropagator()))
            .build();
    TestWorkflow workflow = client.newWorkflowStub(TestWorkflow.class, options);
    String result = workflow.workflow1("input1");
    assertEquals("testing123", result);
  }

  public static class ContextPropagationParentWorkflowImpl implements ParentWorkflow {

    @Override
    public String workflow(String input) {
      // Get the MDC value
      String mdcValue = MDC.get("test");

      // Fire up a child workflow
      ChildWorkflowOptions options =
          new ChildWorkflowOptions.Builder()
              .setContextPropagators(Collections.singletonList(new TestContextPropagator()))
              .build();
      ChildWorkflow child = Workflow.newChildWorkflowStub(ChildWorkflow.class, options);

      String result = child.workflow(mdcValue, Workflow.getWorkflowInfo().getWorkflowId());
      return result;
    }

    @Override
    public void signal(String value) {}
  }

  public static class ContextPropagationChildWorkflowImpl implements ChildWorkflow {

    @Override
    public String workflow(String input, String parentId) {
      String mdcValue = MDC.get("test");
      return input + mdcValue;
    }
  }

  @Test
  public void testChildWorkflowContextPropagation() {
    Worker worker = testEnvironment.newWorker(TASK_LIST);
    worker.registerWorkflowImplementationTypes(
        ContextPropagationParentWorkflowImpl.class, ContextPropagationChildWorkflowImpl.class);
    testEnvironment.start();
    MDC.put("test", "testing123");
    WorkflowClient client = testEnvironment.newWorkflowClient();
    WorkflowOptions options =
        new WorkflowOptions.Builder()
            .setContextPropagators(Collections.singletonList(new TestContextPropagator()))
            .build();
    ParentWorkflow workflow = client.newWorkflowStub(ParentWorkflow.class, options);
    String result = workflow.workflow("input1");
    assertEquals("testing123testing123", result);
  }

  public static class ContextPropagationThreadWorkflowImpl implements TestWorkflow {

    @Override
    public String workflow1(String input) {
      Promise<String> asyncPromise = Async.function(this::async);
      return asyncPromise.get();
    }

    private String async() {
      return "async" + MDC.get("test");
    }
  }

  @Test
  public void testThreadContextPropagation() {
    Worker worker = testEnvironment.newWorker(TASK_LIST);
    worker.registerWorkflowImplementationTypes(ContextPropagationThreadWorkflowImpl.class);
    testEnvironment.start();
    MDC.put("test", "testing123");
    WorkflowClient client = testEnvironment.newWorkflowClient();
    WorkflowOptions options =
        new WorkflowOptions.Builder()
            .setContextPropagators(Collections.singletonList(new TestContextPropagator()))
            .build();
    TestWorkflow workflow = client.newWorkflowStub(TestWorkflow.class, options);
    String result = workflow.workflow1("input1");
    assertEquals("asynctesting123", result);
  }

  public static class ContextActivityImpl implements TestActivity {
    @Override
    public String activity1(String input) {
      return "activity" + MDC.get("test");
    }
  }

  public static class ContextPropagationActivityWorkflowImpl implements TestWorkflow {
    @Override
    public String workflow1(String input) {
      ActivityOptions options =
          new ActivityOptions.Builder()
              .setScheduleToCloseTimeout(Duration.ofSeconds(5))
              .setContextPropagators(Collections.singletonList(new TestContextPropagator()))
              .build();
      TestActivity activity = Workflow.newActivityStub(TestActivity.class, options);
      return activity.activity1("foo");
    }
  }

  @Test
  public void testActivityContextPropagation() {
    Worker worker = testEnvironment.newWorker(TASK_LIST);
    worker.registerWorkflowImplementationTypes(ContextPropagationActivityWorkflowImpl.class);
    worker.registerActivitiesImplementations(new ContextActivityImpl());
    testEnvironment.start();
    MDC.put("test", "testing123");
    WorkflowClient client = testEnvironment.newWorkflowClient();
    WorkflowOptions options =
        new WorkflowOptions.Builder()
            .setContextPropagators(Collections.singletonList(new TestContextPropagator()))
            .build();
    TestWorkflow workflow = client.newWorkflowStub(TestWorkflow.class, options);
    String result = workflow.workflow1("input1");
    assertEquals("activitytesting123", result);
  }

  public static class DefaultContextPropagationActivityWorkflowImpl implements TestWorkflow {
    @Override
    public String workflow1(String input) {
      ActivityOptions options =
          new ActivityOptions.Builder().setScheduleToCloseTimeout(Duration.ofSeconds(5)).build();
      TestActivity activity = Workflow.newActivityStub(TestActivity.class, options);
      return activity.activity1("foo");
    }
  }

  @Test
  public void testDefaultActivityContextPropagation() {
    Worker worker = testEnvironment.newWorker(TASK_LIST);
    worker.registerWorkflowImplementationTypes(DefaultContextPropagationActivityWorkflowImpl.class);
    worker.registerActivitiesImplementations(new ContextActivityImpl());
    testEnvironment.start();
    MDC.put("test", "testing123");
    WorkflowClient client = testEnvironment.newWorkflowClient();
    WorkflowOptions options =
        new WorkflowOptions.Builder()
            .setContextPropagators(Collections.singletonList(new TestContextPropagator()))
            .build();
    TestWorkflow workflow = client.newWorkflowStub(TestWorkflow.class, options);
    String result = workflow.workflow1("input1");
    assertEquals("activitytesting123", result);
  }

  public static class DefaultContextPropagationParentWorkflowImpl implements ParentWorkflow {

    @Override
    public String workflow(String input) {
      // Get the MDC value
      String mdcValue = MDC.get("test");

      // Fire up a child workflow
      ChildWorkflowOptions options = new ChildWorkflowOptions.Builder().build();
      ChildWorkflow child = Workflow.newChildWorkflowStub(ChildWorkflow.class, options);

      String result = child.workflow(mdcValue, Workflow.getWorkflowInfo().getWorkflowId());
      return result;
    }

    @Override
    public void signal(String value) {}
  }

  @Test
  public void testDefaultChildWorkflowContextPropagation() {
    Worker worker = testEnvironment.newWorker(TASK_LIST);
    worker.registerWorkflowImplementationTypes(
        DefaultContextPropagationParentWorkflowImpl.class,
        ContextPropagationChildWorkflowImpl.class);
    testEnvironment.start();
    MDC.put("test", "testing123");
    WorkflowClient client = testEnvironment.newWorkflowClient();
    WorkflowOptions options =
        new WorkflowOptions.Builder()
            .setContextPropagators(Collections.singletonList(new TestContextPropagator()))
            .build();
    ParentWorkflow workflow = client.newWorkflowStub(ParentWorkflow.class, options);
    String result = workflow.workflow("input1");
    assertEquals("testing123testing123", result);
  }

  public static class OpenTelemetryContextPropagationWorkflowImpl implements TestWorkflow {
    @Override
    public String workflow1(String input) {
      if ("fail".equals(input)) {
        throw new IllegalArgumentException();
      } else if ("baggage".equals(input)) {
        return Baggage.current().toString();
      } else {
        SpanContext ctx = Span.current().getSpanContext();
        return ctx.getTraceId() + ":" + ctx.getSpanId() + ":" + ctx.getTraceState().toString();
      }
    }
  }

  @Test
  public void testOpenTelemetryContextPropagation() {
    TraceState TRACE_STATE = TraceState.builder().put("foo", "bar").build();
    String TRACE_ID_BASE16 = "ff000000000000000000000000000041";
    String SPAN_ID_BASE16 = "ff00000000000041";

    Context ctx =
        Context.current()
            .with(
                Span.wrap(
                    SpanContext.create(
                        TRACE_ID_BASE16, SPAN_ID_BASE16, TraceFlags.getDefault(), TRACE_STATE)));

    Span span =
        GlobalOpenTelemetry.getTracer("test-tracer")
            .spanBuilder("test-span")
            .setParent(ctx)
            .setSpanKind(SpanKind.CLIENT)
            .startSpan();

    Worker worker = testEnvironment.newWorker(TASK_LIST);
    worker.registerWorkflowImplementationTypes(OpenTelemetryContextPropagationWorkflowImpl.class);
    testEnvironment.start();
    WorkflowClient client = testEnvironment.newWorkflowClient();
    WorkflowOptions options =
        new WorkflowOptions.Builder()
            .setContextPropagators(
                Arrays.asList(new TestContextPropagator(), new OpenTelemetryContextPropagator()))
            .build();

    try (Scope scope = span.makeCurrent()) {
      TestWorkflow workflow = client.newWorkflowStub(TestWorkflow.class, options);
      assertEquals(
          TRACE_ID_BASE16 + ":" + SPAN_ID_BASE16 + ":" + TRACE_STATE.toString(),
          workflow.workflow1("success"));
    }

    try (Scope scope = span.makeCurrent()) {
      TestWorkflow workflow = client.newWorkflowStub(TestWorkflow.class, options);
      workflow.workflow1("fail");
    } catch (WorkflowException e) {
      assertEquals(IllegalArgumentException.class, e.getCause().getClass());
    } finally {
      span.end();
    }
  }

  @Test
  public void testDefaultOpenTelemetryContextPropagation() {
    TraceState TRACE_STATE = TraceState.builder().put("foo", "bar").build();
    String TRACE_ID_BASE16 = "ff000000000000000000000000000041";
    String SPAN_ID_BASE16 = "ff00000000000041";

    Context ctx =
        Context.current()
            .with(
                Span.wrap(
                    SpanContext.create(
                        TRACE_ID_BASE16, SPAN_ID_BASE16, TraceFlags.getDefault(), TRACE_STATE)));

    Span span =
        GlobalOpenTelemetry.getTracer("test-tracer")
            .spanBuilder("test-span")
            .setParent(ctx)
            .setSpanKind(SpanKind.CLIENT)
            .startSpan();

    Worker worker = testEnvironment.newWorker(TASK_LIST);
    worker.registerWorkflowImplementationTypes(OpenTelemetryContextPropagationWorkflowImpl.class);
    testEnvironment.start();
    WorkflowClient client = testEnvironment.newWorkflowClient();
    WorkflowOptions options = new WorkflowOptions.Builder().build();

    try (Scope scope = span.makeCurrent()) {
      TestWorkflow workflow = client.newWorkflowStub(TestWorkflow.class, options);
      assertEquals(
          TRACE_ID_BASE16 + ":" + SPAN_ID_BASE16 + ":" + TRACE_STATE.toString(),
          workflow.workflow1("success"));
    }

    try (Scope scope = span.makeCurrent()) {
      TestWorkflow workflow = client.newWorkflowStub(TestWorkflow.class, options);
      workflow.workflow1("fail");
    } catch (WorkflowException e) {
      assertEquals(IllegalArgumentException.class, e.getCause().getClass());
    } finally {
      span.end();
    }
  }

  @Test
  public void testNoDefaultOpenTelemetryContextPropagation() {
    TraceState TRACE_STATE = TraceState.builder().put("foo", "bar").build();
    String TRACE_ID_BASE16 = "ff000000000000000000000000000041";
    String SPAN_ID_BASE16 = "ff00000000000041";

    Context ctx =
        Context.current()
            .with(
                Span.wrap(
                    SpanContext.create(
                        TRACE_ID_BASE16, SPAN_ID_BASE16, TraceFlags.getDefault(), TRACE_STATE)));

    Span span =
        GlobalOpenTelemetry.getTracer("test-tracer")
            .spanBuilder("test-span")
            .setParent(ctx)
            .setSpanKind(SpanKind.CLIENT)
            .startSpan();

    Worker worker = testEnvironment.newWorker(TASK_LIST);
    worker.registerWorkflowImplementationTypes(OpenTelemetryContextPropagationWorkflowImpl.class);
    testEnvironment.start();
    WorkflowClient client = testEnvironment.newWorkflowClient();
    WorkflowOptions options =
        new WorkflowOptions.Builder().build();

    try (Scope scope = span.makeCurrent()) {
      TestWorkflow workflow = client.newWorkflowStub(TestWorkflow.class, options);
      assertNotEquals(
          TRACE_ID_BASE16 + ":" + SPAN_ID_BASE16 + ":" + TRACE_STATE.toString(),
          workflow.workflow1("success"));
    }
  }

  @Test
  public void testBaggagePropagation() {
    Baggage baggage =
        Baggage.builder().put("keyFoo1", "valueFoo1").put("keyFoo2", "valueFoo2").build();

    Worker worker = testEnvironment.newWorker(TASK_LIST);
    worker.registerWorkflowImplementationTypes(OpenTelemetryContextPropagationWorkflowImpl.class);
    testEnvironment.start();
    WorkflowClient client = testEnvironment.newWorkflowClient();
    WorkflowOptions options = new WorkflowOptions.Builder().build();

    try (Scope scope = Context.current().with(baggage).makeCurrent()) {
      TestWorkflow workflow = client.newWorkflowStub(TestWorkflow.class, options);
      assertEquals(baggage.toString(), workflow.workflow1("baggage"));
    }
  }
}
