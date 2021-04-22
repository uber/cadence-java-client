/*
 *  Modifications Copyright (c) 2017-2021 Uber Technologies Inc.
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
package com.uber.cadence.worker;

import com.google.common.base.MoreObjects;
import com.uber.cadence.StartWorkflowExecutionRequest;
import com.uber.cadence.TaskList;
import com.uber.cadence.WorkflowExecutionAlreadyStartedError;
import com.uber.cadence.WorkflowIdReusePolicy;
import com.uber.cadence.WorkflowType;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.internal.common.InternalUtils;
import com.uber.cadence.internal.common.RpcRetryer;
import com.uber.cadence.internal.metrics.MetricsTag;
import com.uber.cadence.internal.shadowing.ReplayWorkflowActivity;
import com.uber.cadence.internal.shadowing.ReplayWorkflowActivityImpl;
import com.uber.cadence.internal.shadowing.ScanWorkflowActivity;
import com.uber.cadence.internal.shadowing.ScanWorkflowActivityImpl;
import com.uber.cadence.internal.sync.SyncActivityWorker;
import com.uber.cadence.internal.worker.SingleWorkerOptions;
import com.uber.cadence.internal.worker.Suspendable;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.shadower.WorkflowParams;
import com.uber.cadence.shadower.shadowerConstants;
import com.uber.cadence.testing.TestEnvironmentOptions;
import com.uber.cadence.workflow.Functions;
import com.uber.m3.tally.Scope;
import com.uber.m3.util.ImmutableMap;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TSimpleJSONProtocol;

public final class ShadowingWorker implements Suspendable {

  private final IWorkflowService service;
  private final SyncActivityWorker activityWorker;
  private final ReplayWorkflowActivity replayActivity;
  private final String taskList;
  private final ShadowingOptions shadowingOptions;
  private final AtomicBoolean started = new AtomicBoolean();

  /**
   * ShadowingWorker starts a shadowing workflow to replay the target workflows.
   *
   * @param client is the target endpoint to fetch workflow history.
   * @param taskList is the task list used in the workflows.
   * @param options is worker option.
   * @param shadowingOptions is the shadowing options.
   */
  public ShadowingWorker(
      WorkflowClient client,
      String taskList,
      WorkerOptions options,
      ShadowingOptions shadowingOptions) {
    this(client, taskList, options, shadowingOptions, new TestEnvironmentOptions.Builder().build());
  }

  /**
   * ShadowingWorker starts a shadowing workflow to replay the target workflows.
   *
   * @param client is the target endpoint to fetch workflow history.
   * @param taskList is the task list used in the workflows.
   * @param options is worker option.
   * @param shadowingOptions is the shadowing options.
   * @param testOptions uses to set customized data converter, interceptor and context propagator.
   */
  public ShadowingWorker(
      WorkflowClient client,
      String taskList,
      WorkerOptions options,
      ShadowingOptions shadowingOptions,
      TestEnvironmentOptions testOptions) {
    options = MoreObjects.firstNonNull(options, WorkerOptions.defaultInstance());
    this.shadowingOptions = Objects.requireNonNull(shadowingOptions);
    this.taskList = shadowingOptions.getDomain() + "-" + taskList;
    this.service = client.getService();
    Scope metricsScope =
        client
            .getOptions()
            .getMetricsScope()
            .tagged(
                ImmutableMap.of(
                    MetricsTag.DOMAIN,
                    shadowingOptions.getDomain(),
                    MetricsTag.TASK_LIST,
                    this.taskList));
    ScanWorkflowActivity scanActivity = new ScanWorkflowActivityImpl(client.getService());
    replayActivity =
        new ReplayWorkflowActivityImpl(client.getService(), metricsScope, taskList, testOptions);

    SingleWorkerOptions activityOptions =
        SingleWorkerOptions.newBuilder()
            .setIdentity(client.getOptions().getIdentity())
            .setDataConverter(client.getOptions().getDataConverter())
            .setTaskExecutorThreadPoolSize(options.getMaxConcurrentActivityExecutionSize())
            .setTaskListActivitiesPerSecond(options.getTaskListActivitiesPerSecond())
            .setPollerOptions(options.getActivityPollerOptions())
            .setMetricsScope(metricsScope)
            .build();
    activityWorker =
        new SyncActivityWorker(
            client.getService(), shadowerConstants.LocalDomainName, this.taskList, activityOptions);
    activityWorker.setActivitiesImplementation(scanActivity, replayActivity);
  }

  public void start() throws Exception {
    if (!started.compareAndSet(false, true)) {
      return;
    }
    startShadowingWorkflow();
    activityWorker.start();
  }

  public void shutdown() {
    activityWorker.shutdown();
  }

  public void shutdownNow() {
    activityWorker.shutdownNow();
  }

  public boolean isTerminated() {
    return activityWorker.isTerminated();
  }

  public void awaitTermination(long timeout, TimeUnit unit) {
    InternalUtils.awaitTermination(activityWorker, unit.toMillis(timeout));
  }

  @Override
  public void suspendPolling() {
    activityWorker.suspendPolling();
  }

  @Override
  public void resumePolling() {
    activityWorker.resumePolling();
  }

  @Override
  public boolean isSuspended() {
    return activityWorker.isSuspended();
  }

  public void registerWorkflowImplementationTypes(Class<?>... workflowImplementationClasses) {
    replayActivity.registerWorkflowImplementationTypes(workflowImplementationClasses);
  }

  public void registerWorkflowImplementationTypes(
      WorkflowImplementationOptions options, Class<?>... workflowImplementationClasses) {
    replayActivity.registerWorkflowImplementationTypesWithOptions(
        options, workflowImplementationClasses);
  }

  public <R> void addWorkflowImplementationFactory(
      WorkflowImplementationOptions options,
      Class<R> workflowInterface,
      Functions.Func<R> factory) {
    replayActivity.addWorkflowImplementationFactoryWithOptions(options, workflowInterface, factory);
  }

  public <R> void addWorkflowImplementationFactory(
      Class<R> workflowInterface, Functions.Func<R> factory) {
    replayActivity.addWorkflowImplementationFactory(workflowInterface, factory);
  }

  protected void startShadowingWorkflow() throws Exception {
    TSerializer serializer = new TSerializer(new TSimpleJSONProtocol.Factory());
    WorkflowParams params =
        new WorkflowParams()
            .setDomain(shadowingOptions.getDomain())
            .setConcurrency(shadowingOptions.getConcurrency())
            .setExitCondition(shadowingOptions.getExitCondition())
            .setShadowMode(shadowingOptions.getShadowMode())
            .setSamplingRate(shadowingOptions.getSamplingRate())
            .setTaskList(taskList)
            .setWorkflowQuery(shadowingOptions.getWorkflowQuery());
    StartWorkflowExecutionRequest request =
        new StartWorkflowExecutionRequest()
            .setDomain(shadowerConstants.LocalDomainName)
            .setWorkflowId(shadowingOptions.getDomain() + shadowerConstants.WorkflowIDSuffix)
            .setTaskList(new TaskList().setName(shadowerConstants.TaskList))
            .setInput(serializer.serialize(params))
            .setWorkflowType(new WorkflowType().setName(shadowerConstants.WorkflowName))
            .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.AllowDuplicate)
            .setRequestId(UUID.randomUUID().toString())
            .setExecutionStartToCloseTimeoutSeconds(864000)
            .setTaskStartToCloseTimeoutSeconds(60);
    try {
      RpcRetryer.retryWithResult(
          RpcRetryer.DEFAULT_RPC_RETRY_OPTIONS, () -> service.StartWorkflowExecution(request));
    } catch (WorkflowExecutionAlreadyStartedError e) {
      // Ignore workflow execution already started error
    }
  }
}
