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

import com.uber.cadence.PollForActivityTaskRequest;
import com.uber.cadence.PollForActivityTaskResponse;
import com.uber.cadence.TaskList;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.WorkflowService;
import com.uber.cadence.common.RetryOptions;
import com.uber.cadence.internal.common.SynchronousRetryer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.thrift.TException;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Assumes that there is only one instance of ActivityPollTask per worker as it contains thread pool and semaphore.
 */
final class ActivityPollTask implements Poller.ThrowingRunnable {

    private static final Log log = LogFactory.getLog(ActivityPollTask.class);
    private static final String ACTIVITY_THREAD_NAME_PREFIX = "SWF Activity ";

    private final WorkflowService.Iface service;
    private final String domain;
    private final String taskList;
    private final ActivityTaskHandler handler;
    private final ActivityWorkerOptions options;
    private ThreadPoolExecutor taskExecutor;
    private Semaphore pollSemaphore;

    private UncaughtExceptionHandler uncaughtExceptionHandler = (t, e) -> log.error("Failure in thread " + t.getName(), e);

    ActivityPollTask(WorkflowService.Iface service, String domain, String taskList, ActivityWorkerOptions options,
                     ActivityTaskHandler handler) {
        this.service = service;
        this.domain = domain;
        this.taskList = taskList;
        this.options = options;
        this.handler = handler;
        taskExecutor = new ThreadPoolExecutor(options.getTaskExecutorThreadPoolSize(),
                options.getTaskExecutorThreadPoolSize(), 10, TimeUnit.SECONDS,
                new SynchronousQueue<>());
        taskExecutor.setThreadFactory(new ExecutorThreadFactory(
                ACTIVITY_THREAD_NAME_PREFIX + " " + taskList + " ",
                options.getPollerOptions().getUncaughtExceptionHandler()));
        taskExecutor.setRejectedExecutionHandler(new BlockCallerPolicy());
        this.pollSemaphore = new Semaphore(options.getTaskExecutorThreadPoolSize());
    }

    /**
     * Poll for a activity task and execute correspondent implementation using
     * provided executor service.
     */
    @Override
    public void run() throws TException, InterruptedException {
        boolean synchronousSemaphoreRelease = false;
        try {
            pollSemaphore.acquire();
            // we will release the semaphore in a finally clause
            synchronousSemaphoreRelease = true;
            final PollForActivityTaskResponse task = poll();
            if (task == null) {
                return;
            }
            synchronousSemaphoreRelease = false; // released by the task
            try {
                taskExecutor.execute(() -> {
                    try {
                        ActivityTaskHandler.Result result = handler.handle(service, domain, task);
                        sendReply(result);
                    } catch (Throwable ee) {
                        uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), wrapFailure(task, ee));
                    } finally {
                        pollSemaphore.release();
                    }
                });
            } catch (Error | Exception e) {
                synchronousSemaphoreRelease = true;
                throw e;
            }
        } finally {
            if (synchronousSemaphoreRelease) {
                pollSemaphore.release();
            }
        }
    }

    private void sendReply(ActivityTaskHandler.Result response) throws TException {
        RetryOptions ro = response.getRequestRetryOptions();
        if (response.getTaskCompleted() != null) {
            ro = options.getReportCompletionRetryOptions().merge(ro);
            SynchronousRetryer.retry(ro,
                    () -> service.RespondActivityTaskCompleted(response.getTaskCompleted()));
        } else if (response.getTaskFailed() != null) {
            ro = options.getReportFailureRetryOptions().merge(ro);
            SynchronousRetryer.retry(ro,
                    () -> service.RespondActivityTaskFailed(response.getTaskFailed()));
        } else if (response.getTaskCancelled() != null) {
            ro = options.getReportFailureRetryOptions().merge(ro);
            SynchronousRetryer.retry(ro,
                    () -> service.RespondActivityTaskCanceled(response.getTaskCancelled()));
        }
        // Manual activity completion
    }

    private PollForActivityTaskResponse poll() throws TException {
        PollForActivityTaskRequest pollRequest = new PollForActivityTaskRequest();
        pollRequest.setDomain(domain);
        pollRequest.setIdentity(options.getIdentity());
        pollRequest.setTaskList(new TaskList().setName(taskList));
        if (log.isDebugEnabled()) {
            log.debug("poll request begin: " + pollRequest);
        }
        PollForActivityTaskResponse result = service.PollForActivityTask(pollRequest);
        if (result == null || result.getTaskToken() == null) {
            if (log.isDebugEnabled()) {
                log.debug("poll request returned no task");
            }
            return null;
        }
        if (log.isTraceEnabled()) {
            log.trace("poll request returned " + result);
        }
        return result;
    }

    private Exception wrapFailure(final PollForActivityTaskResponse task, Throwable failure) {
        WorkflowExecution execution = task.getWorkflowExecution();
        return new RuntimeException(
                "Failure taskId=\"" + "\" workflowExecutionRunId=\"" + execution.getRunId()
                        + "\" workflowExecutionId=\"" + execution.getWorkflowId() + "\"", failure);
    }
}
