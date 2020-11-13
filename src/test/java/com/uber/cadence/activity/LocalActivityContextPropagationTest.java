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

package com.uber.cadence.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowClientOptions;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.context.ContextPropagator;
import com.uber.cadence.testing.TestEnvironmentOptions;
import com.uber.cadence.testing.TestWorkflowEnvironment;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.workflow.Workflow;
import com.uber.cadence.workflow.WorkflowMethod;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LocalActivityContextPropagationTest {

  private static final String LOCAL_ACTIVITY_CP_TASK_LIST = "LOCAL_ACTIVITY_CP_TASK_LIST";
  private static final String EXPECTED_CONTEXT_NAME = "EXPECTED_CONTEXT_NAME";
  private static final Gson gson = new GsonBuilder().create();

  private final List<ContextPropagator> PROPAGATORS =
      Collections.singletonList(new ContextPropagationTest());

  private final LocalActivityContextPropagation localActivityContextPropagation =
      new LocalActivityContextPropagationImpl();

  private final WrapperContext wrapperContext = new WrapperContext(EXPECTED_CONTEXT_NAME);

  // let's add safe TestWorkflowEnvironment closing and make configurable propagation
  // enabling/disabling
  private class TestEnvAutoCloseable implements AutoCloseable {

    private TestWorkflowEnvironment testEnv;

    public WorkflowClient runTestCadenceEnv(boolean propagationEnabled) {
      testEnv =
          TestWorkflowEnvironment.newInstance(
              new TestEnvironmentOptions.Builder()
                  .setWorkflowClientOptions(
                      WorkflowClientOptions.newBuilder()
                          .setContextPropagators(
                              propagationEnabled ? PROPAGATORS : Collections.emptyList())
                          .build())
                  .build());
      Worker worker = testEnv.newWorker(LOCAL_ACTIVITY_CP_TASK_LIST);

      worker.addWorkflowImplementationFactory(
          LocalActivityContextPropagationWorkflowImpl.class,
          LocalActivityContextPropagationWorkflowImpl::new);

      worker.registerActivitiesImplementations(localActivityContextPropagation);
      WorkflowClient workflowClient =
          testEnv.newWorkflowClient(WorkflowClientOptions.defaultInstance());
      testEnv.start();

      return workflowClient;
    }

    @Override
    public void close() {
      testEnv.close();
    }
  }

  @Test
  public void testLocalActivityContextPropagation_propagationEnabled() {
    try (TestEnvAutoCloseable testEnvAutoCloseable = new TestEnvAutoCloseable()) {
      WorkflowClient workflowClient = testEnvAutoCloseable.runTestCadenceEnv(true);

      LocalActivityContextPropagationWorkflow workflowStub =
          workflowClient.newWorkflowStub(
              LocalActivityContextPropagationWorkflow.class,
              new WorkflowOptions.Builder()
                  .setTaskList(LOCAL_ACTIVITY_CP_TASK_LIST)
                  .setContextPropagators(PROPAGATORS)
                  .setExecutionStartToCloseTimeout(Duration.ofSeconds(10))
                  .build());

      String actual = workflowStub.foo();

      assertEquals(EXPECTED_CONTEXT_NAME, actual);
    }
  }

  @Test
  public void testLocalActivityContextPropagation_propagationDisabled() {
    try (TestEnvAutoCloseable testEnvAutoCloseable = new TestEnvAutoCloseable()) {
      WorkflowClient workflowClient = testEnvAutoCloseable.runTestCadenceEnv(false);

      LocalActivityContextPropagationWorkflow workflowStub =
          workflowClient.newWorkflowStub(
              LocalActivityContextPropagationWorkflow.class,
              new WorkflowOptions.Builder()
                  .setTaskList(LOCAL_ACTIVITY_CP_TASK_LIST)
                  .setContextPropagators(PROPAGATORS)
                  .setExecutionStartToCloseTimeout(Duration.ofSeconds(10))
                  .build());

      String actual = workflowStub.foo();

      assertNull(actual);
    }
  }

  public interface LocalActivityContextPropagationWorkflow {

    @WorkflowMethod
    String foo();
  }

  public class LocalActivityContextPropagationWorkflowImpl
      implements LocalActivityContextPropagationWorkflow {

    @Override
    public String foo() {
      LocalActivityOptions localActivityOptions =
          new LocalActivityOptions.Builder().setContextPropagators(PROPAGATORS).build();

      LocalActivityContextPropagation localActivityStub =
          Workflow.newLocalActivityStub(
              LocalActivityContextPropagation.class, localActivityOptions);

      return localActivityStub.execute();
    }
  }

  public interface LocalActivityContextPropagation {

    @ActivityMethod
    String execute();
  }

  public class LocalActivityContextPropagationImpl implements LocalActivityContextPropagation {

    @Override
    public String execute() {
      return Optional.ofNullable(wrapperContext.getLocalContext())
          .map(Context::getContextName)
          .orElse(null);
    }
  }

  private class ContextPropagationTest implements ContextPropagator {

    private static final String CONTEXT = "context";

    @Override
    public String getName() {
      return CONTEXT;
    }

    @Override
    public Map<String, byte[]> serializeContext(Object context) {
      return Optional.ofNullable(context)
          .map(gson::toJson)
          .map(this::serialize)
          .orElseGet(HashMap::new);
    }

    private Map<String, byte[]> serialize(String contextJson) {
      Map<String, byte[]> serializedContext = new HashMap<>();
      serializedContext.put(CONTEXT, contextJson.getBytes(StandardCharsets.UTF_8));

      return serializedContext;
    }

    @Override
    public Object deserializeContext(Map<String, byte[]> context) {

      return Optional.ofNullable(context.get(CONTEXT))
          .map(contextBytes -> new String(contextBytes, StandardCharsets.UTF_8))
          .map(contextJson -> gson.fromJson(contextJson, Context.class))
          .orElse(null);
    }

    @Override
    public Object getCurrentContext() {
      return wrapperContext.getLocalContext();
    }

    @Override
    public void setCurrentContext(Object context) {
      if (context == null) {
        return;
      }

      String propagatedContextName = ((Context) context).getContextName();
      wrapperContext.newContext(propagatedContextName);
    }
  }

  private class WrapperContext {

    private ThreadLocal<Context> threadLocalContext;

    public WrapperContext(String testData) {
      newContext(testData);
    }

    private void newContext(String testData) {
      Context context = new Context();
      context.setContextName(testData);

      threadLocalContext = new ThreadLocal<>();
      threadLocalContext.set(context);
    }

    public Context getLocalContext() {
      return threadLocalContext.get();
    }
  }

  private class Context {

    private String contextName;

    public String getContextName() {
      return contextName;
    }

    public void setContextName(String testData) {
      this.contextName = testData;
    }
  }
}
