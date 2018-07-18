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

package com.uber.cadence.worker;

import com.uber.cadence.common.Validate;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.serviceclient.WorkflowServiceTChannel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.function.Supplier;

public final class WorkerFactory {

  private final WorkerFactoryOptions factoryOptions;
  private final ArrayList<Worker> workers = new ArrayList<>();
  private final Supplier<IWorkflowService> getWorkFlowService;
  private String domain;
  private State state = State.Initial;

  private final String statusErrorMessage = "attempted to %s in non %s state. Current State: %s";

  public WorkerFactory(String domain) {
    this(domain, null);
  }

  public WorkerFactory(String domain, WorkerFactoryOptions options) {
    this(() -> new WorkflowServiceTChannel(), domain, options);
  }

  public WorkerFactory(String host, int port, String domain, WorkerFactoryOptions options) {
    this(() -> new WorkflowServiceTChannel(host, port), domain, options);
  }

  public WorkerFactory(
      Supplier<IWorkflowService> getWorkFlowService, String domain, WorkerFactoryOptions options) {
    Validate.ArgumentNotNull(getWorkFlowService, "getWorkFlowService");
    Validate.ArgumentNotEmpty(domain, "domain");

    this.getWorkFlowService = getWorkFlowService;
    this.domain = domain;
    this.factoryOptions = options;
  }

  public Worker newWorker(String taskList, WorkerOptions options) {
    Validate.ArgumentNotEmpty(taskList, "taskList");

    synchronized (this) {
      Validate.Condition(
          state == State.Started,
          String.format(
              statusErrorMessage, "create new worker", State.Started.name(), state.name()));
    }

    Worker worker = new Worker(getWorkFlowService.get(), domain, taskList, options);
    workers.add(worker);
    return worker;
  }

  public void start() {
    synchronized (this) {
      Validate.Condition(
          state == State.Initial,
          String.format(
              statusErrorMessage, "start WorkerFactory", State.Initial.name(), state.name()));
      state = State.Started;
    }

    for (Worker worker : workers) {
      worker.start();
    }
  }

  public void shutdown(Duration timeout) {
    synchronized (this) {
      Validate.Condition(
          state == State.Started,
          String.format(
              statusErrorMessage, "shutdown WorkerFactory", State.Started.name(), state.name()));
      state = State.Shutdown;
    }

    for (Worker worker : workers) {
      worker.shutdown(timeout);
    }
  }

  enum State {
    Initial,
    Started,
    Shutdown
  }
}
