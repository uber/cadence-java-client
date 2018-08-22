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

package com.uber.cadence.internal.replay;

import static com.uber.cadence.internal.common.InternalUtils.createStickyTaskList;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static org.mockito.Mockito.*;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.uber.cadence.HistoryUtils;
import com.uber.cadence.PollForDecisionTaskResponse;
import com.uber.cadence.StickyExecutionAttributes;
import com.uber.cadence.internal.worker.DecisionTaskHandler;
import com.uber.cadence.internal.worker.DecisionTaskWithHistoryIterator;
import com.uber.cadence.internal.worker.SingleWorkerOptions;
import org.junit.Test;

public class ReplayDeciderTaskHandlerTests {

  @Test
  public void ifStickyExecutionAttributesAreNotSetThenWorkflowsAreNotCached() throws Throwable {
    // Arrange
    LoadingCache<String, ReplayDecider> lruCache =
        CacheBuilder.newBuilder()
            .maximumSize(10)
            .build(
                new CacheLoader<String, ReplayDecider>() {
                  @Override
                  public ReplayDecider load(String key) {
                    return null;
                  }
                });
    ReplayDeciderCache cache = new ReplayDeciderCache(lruCache);
    DecisionTaskHandler taskHandler =
        new ReplayDecisionTaskHandler(
            "domain",
            setUpMockWorkflowFactory(),
            cache,
            new SingleWorkerOptions.Builder().build(),
            null);

    DecisionTaskWithHistoryIterator mockIterator =
        setUpMockIterator(HistoryUtils.generateDecisionTaskWithInitialHistory());

    // Act
    DecisionTaskHandler.Result result = taskHandler.handleDecisionTask(mockIterator);

    // Assert
    assertEquals(0, lruCache.size());
    assertNotNull(result.getTaskCompleted());
    assertNull(result.getTaskCompleted().getStickyAttributes());
  }

  @Test
  public void ifStickyExecutionAttributesAreSetThenWorkflowsAreCached() throws Throwable {
    // Arrange
    LoadingCache<String, ReplayDecider> lruCache =
        CacheBuilder.newBuilder()
            .maximumSize(10)
            .build(
                new CacheLoader<String, ReplayDecider>() {
                  @Override
                  public ReplayDecider load(String key) {
                    return null;
                  }
                });
    ReplayDeciderCache cache = new ReplayDeciderCache(lruCache);

    DecisionTaskHandler taskHandler =
        new ReplayDecisionTaskHandler(
            "domain",
            setUpMockWorkflowFactory(),
            cache,
            new SingleWorkerOptions.Builder().build(),
            "sticky");

    PollForDecisionTaskResponse decisionTask =
        HistoryUtils.generateDecisionTaskWithInitialHistory();
    DecisionTaskWithHistoryIterator mockIterator = setUpMockIterator(decisionTask);

    // Act
    DecisionTaskHandler.Result result = taskHandler.handleDecisionTask(mockIterator);

    // Assert
    assertEquals(1, lruCache.size());
    assertNotNull(result.getTaskCompleted());
    StickyExecutionAttributes attributes = result.getTaskCompleted().getStickyAttributes();
    assertEquals("sticky", attributes.getWorkerTaskList().name);
    assertEquals(
        decisionTask
            .history
            .events
            .get(0)
            .getWorkflowExecutionStartedEventAttributes()
            .taskStartToCloseTimeoutSeconds,
        attributes.getScheduleToStartTimeoutSeconds());
  }

  @Test
  public void ifCacheIsEvictedAndPartialHistoryIsReceivedThenTaskFailedIsReturned()
      throws Throwable {
    // Arrange
    LoadingCache<String, ReplayDecider> lruCache =
        CacheBuilder.newBuilder()
            .maximumSize(10)
            .build(
                new CacheLoader<String, ReplayDecider>() {
                  @Override
                  public ReplayDecider load(String key) {
                    return null;
                  }
                });
    ReplayDeciderCache cache = new ReplayDeciderCache(lruCache);
    StickyExecutionAttributes attributes = new StickyExecutionAttributes();
    attributes.setWorkerTaskList(createStickyTaskList("sticky"));
    DecisionTaskHandler taskHandler =
        new ReplayDecisionTaskHandler(
            "domain",
            setUpMockWorkflowFactory(),
            cache,
            new SingleWorkerOptions.Builder().build(),
            "sticky");
    PollForDecisionTaskResponse response = HistoryUtils.generateDecisionTaskWithPartialHistory();

    DecisionTaskWithHistoryIterator mockIterator =
        setUpMockIterator(HistoryUtils.generateDecisionTaskWithPartialHistory());

    // Act
    DecisionTaskHandler.Result result = taskHandler.handleDecisionTask(mockIterator);

    // Assert
    assertEquals(0, lruCache.size());
    assertNull(result.getTaskCompleted());
    assertNotNull(result.getTaskFailed());
  }

  private DecisionTaskWithHistoryIterator setUpMockIterator(
      PollForDecisionTaskResponse decisionTask) {
    DecisionTaskWithHistoryIterator mockIterator = mock(DecisionTaskWithHistoryIterator.class);
    when(mockIterator.getDecisionTask()).thenReturn(decisionTask);
    when(mockIterator.getHistory()).thenReturn(decisionTask.history.getEventsIterator());
    return mockIterator;
  }

  private ReplayWorkflowFactory setUpMockWorkflowFactory() throws Throwable {
    ReplayWorkflow mockWorkflow = mock(ReplayWorkflow.class);
    ReplayWorkflowFactory mockFactory = mock(ReplayWorkflowFactory.class);

    when(mockFactory.getWorkflow(any())).thenReturn(mockWorkflow);
    when(mockWorkflow.eventLoop()).thenReturn(true);
    return mockFactory;
  }
}
