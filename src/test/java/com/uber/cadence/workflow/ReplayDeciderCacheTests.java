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

package com.uber.cadence.workflow;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotSame;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.uber.cadence.*;
import com.uber.cadence.internal.replay.*;
import com.uber.cadence.internal.testservice.TestWorkflowService;
import com.uber.cadence.internal.worker.WorkflowExecutionException;
import com.uber.cadence.worker.WorkerOptions;
import junit.framework.TestCase;
import org.junit.Test;

public class ReplayDeciderCacheTests {

  @Test
  public void whenHistoryIsFullNewReplayDeciderIsReturnedAndCached_InitiallyEmpty()
      throws Exception {
    // Arrange
    LoadingCache<String, ReplayDecider> cache = buildCache();
    ReplayDeciderCache replayDeciderCache = new ReplayDeciderCache(cache);
    PollForDecisionTaskResponse decisionTask =
        HistoryUtils.generateDecisionTaskWithInitialHistory();

    String runId = decisionTask.getWorkflowExecution().getRunId();

    AssertCacheIsEmpty(cache, runId);

    // Act
    ReplayDecider decider = replayDeciderCache.getOrCreate(decisionTask, this::createFakeDecider);

    // Assert
    assertEquals(decider, cache.getUnchecked(runId));
  }

  @Test
  public void whenHistoryIsFullNewReplayDeciderIsReturned_InitiallyCached() throws Exception {
    // Arrange
    LoadingCache<String, ReplayDecider> cache = buildCache();
    ReplayDeciderCache replayDeciderCache = new ReplayDeciderCache(cache);
    PollForDecisionTaskResponse decisionTask =
        HistoryUtils.generateDecisionTaskWithInitialHistory();

    String runId = decisionTask.getWorkflowExecution().getRunId();
    ReplayDecider decider = replayDeciderCache.getOrCreate(decisionTask, this::createFakeDecider);
    assertEquals(decider, cache.getUnchecked(runId));

    // Act
    ReplayDecider decider2 = replayDeciderCache.getOrCreate(decisionTask, this::createFakeDecider);

    // Assert
    assertEquals(decider2, cache.getUnchecked(runId));
    assertNotSame(decider2, decider);
  }

  @Test
  public void whenHistoryIsPartialCachedEntryIsReturned() throws Exception {
    // Arrange
    LoadingCache<String, ReplayDecider> cache = buildCache();
    ReplayDeciderCache replayDeciderCache = new ReplayDeciderCache(cache);
    TestWorkflowService service = new TestWorkflowService();
    PollForDecisionTaskResponse decisionTask =
        HistoryUtils.generateDecisionTaskWithInitialHistory(
            "domain", "taskList", "workflowType", service);

    String runId = decisionTask.getWorkflowExecution().getRunId();
    ReplayDecider decider = replayDeciderCache.getOrCreate(decisionTask, this::createFakeDecider);
    assertEquals(decider, cache.getUnchecked(runId));

    // Act
    decisionTask =
        HistoryUtils.generateDecisionTaskWithPartialHistoryFromExistingTask(decisionTask, service);
    ReplayDecider decider2 = replayDeciderCache.getOrCreate(decisionTask, this::createFakeDecider);

    // Assert
    assertEquals(decider2, cache.getUnchecked(runId));
    assertEquals(decider2, decider);
  }

  @Test
  public void whenHistoryIsPartialAndCacheIsEmptyThenCacheEvictedExceptionIsThrown()
      throws Exception {
    // Arrange
    LoadingCache<String, ReplayDecider> cache = buildCache();
    ReplayDeciderCache replayDeciderCache = new ReplayDeciderCache(cache);

    // Act
    PollForDecisionTaskResponse decisionTask =
        HistoryUtils.generateDecisionTaskWithPartialHistory();

    try {
      replayDeciderCache.getOrCreate(decisionTask, this::createFakeDecider);
    } catch (ReplayDeciderCache.EvictedException ex) {
      return;
    }

    TestCase.fail(
        "Expected replayDeciderCache.getOrCreate to throw ReplayDeciderCache.EvictedException but no exception was thrown");
  }

  private void AssertCacheIsEmpty(LoadingCache<String, ReplayDecider> cache, String runId) {
    CacheLoader.InvalidCacheLoadException ex = null;
    try {
      cache.getUnchecked(runId);
    } catch (CacheLoader.InvalidCacheLoadException e) {
      ex = e;
    }
    assertNotNull(ex);
  }

  private LoadingCache<String, ReplayDecider> buildCache() {
    return CacheBuilder.newBuilder()
        .maximumSize(1000)
        .build(
            new CacheLoader<String, ReplayDecider>() {
              @Override
              public ReplayDecider load(String key) {
                return null;
              }
            });
  }

  private ReplayDecider createFakeDecider(PollForDecisionTaskResponse response) {
    return new ReplayDecider(
        "domain",
        new ReplayWorkflow() {
          @Override
          public void start(HistoryEvent event, DecisionContext context) throws Exception {}

          @Override
          public void handleSignal(String signalName, byte[] input, long eventId) {}

          @Override
          public boolean eventLoop() throws Throwable {
            return false;
          }

          @Override
          public byte[] getOutput() {
            return new byte[0];
          }

          @Override
          public void cancel(String reason) {}

          @Override
          public void close() {}

          @Override
          public long getNextWakeUpTime() {
            return 0;
          }

          @Override
          public byte[] query(WorkflowQuery query) {
            return new byte[0];
          }

          @Override
          public WorkflowExecutionException mapUnexpectedException(Exception failure) {
            return null;
          }
        },
        new DecisionsHelper(response),
        new WorkerOptions.Builder().build().getMetricsScope(),
        false);
  }
}
