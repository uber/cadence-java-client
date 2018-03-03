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
package com.uber.cadence.internal.worker;

import com.uber.cadence.WorkflowService;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class WorkflowWorker {

    private static final String POLL_THREAD_NAME_PREFIX = "SWF Activity Poll ";

    private Poller poller;
    private final DecisionTaskHandler handler;
    private final WorkflowService.Iface service;
    private final String domain;
    private final String taskList;
    private final WorkflowWorkerOptions options;

    public WorkflowWorker(WorkflowService.Iface service, String domain, String taskList,
                          WorkflowWorkerOptions options, DecisionTaskHandler handler) {
        Objects.requireNonNull(service);
        Objects.requireNonNull(domain);
        Objects.requireNonNull(taskList);
        this.service = service;
        this.domain = domain;
        this.taskList = taskList;
        this.options = options;
        this.handler = handler;
    }

    public void start() {
        if (handler.isAnyTypeSupported()) {
            PollerOptions pollerOptions = options.getPollerOptions();
            if (pollerOptions.getPollThreadNamePrefix() == null) {
                pollerOptions = new PollerOptions.Builder(pollerOptions)
                        .setPollThreadNamePrefix(POLL_THREAD_NAME_PREFIX)
                        .build();
            }
            Poller.ThrowingRunnable pollTask = new ActivityPollTask(service, domain, taskList, options, handler);
            poller = new Poller(pollerOptions, pollTask);
            poller.start();
        }
    }

    public void shutdown() {
        if (poller != null) {
            poller.shutdown();
        }
    }

    public void shutdownNow() {
        if (poller != null) {
            poller.shutdownNow();
        }
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (poller == null) {
            return true;
        }
        return poller.awaitTermination(timeout, unit);
    }

    public boolean shutdownAndAwaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (poller == null) {
            return true;
        }
        return poller.shutdownAndAwaitTermination(timeout, unit);
    }

    public boolean isRunning() {
        if (poller == null) {
            return false;
        }
        return poller.isRunning();
    }

    public void suspendPolling() {
        if (poller != null) {
            poller.suspendPolling();
        }
    }

    public void resumePolling() {
        if (poller != null) {
            poller.resumePolling();
        }
    }
}
