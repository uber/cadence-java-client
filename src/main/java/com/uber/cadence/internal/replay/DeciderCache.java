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

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.uber.cadence.PollForDecisionTaskResponse;
import com.uber.cadence.internal.common.ThrowableFunc1;

public final class DeciderCache {
  private LoadingCache<String, Decider> cache;

  public DeciderCache(int maxCacheSize) {
    Preconditions.checkArgument(maxCacheSize > 0, "Max cache size must be greater than 0");
    this.cache =
        CacheBuilder.newBuilder()
            .maximumSize(maxCacheSize)
            .build(
                new CacheLoader<String, Decider>() {
                  @Override
                  public ReplayDecider load(String key) {
                    return null;
                  }
                });
  }

  public Decider getOrCreate(
      PollForDecisionTaskResponse decisionTask,
      ThrowableFunc1<PollForDecisionTaskResponse, Decider, Exception> createReplayDecider)
      throws Exception {
    String runId = decisionTask.getWorkflowExecution().getRunId();
    if (isFullHistory(decisionTask)) {
      cache.invalidate(runId);
      return cache.get(runId, () -> createReplayDecider.apply(decisionTask));
    }
    try {
      return cache.getUnchecked(runId);
    } catch (CacheLoader.InvalidCacheLoadException e) {
      throw new EvictedException(decisionTask);
    }
  }

  public void invalidate(PollForDecisionTaskResponse decisionTask) {
    synchronized (this) {
      String runId = decisionTask.getWorkflowExecution().getRunId();
      invalidate(runId);
    }
  }

  public void invalidate(String runId) {
    cache.invalidate(runId);
  }

  public long size() {
    return cache.size();
  }

  private boolean isFullHistory(PollForDecisionTaskResponse decisionTask) {
    return decisionTask.history.events.get(0).getEventId() == 1;
  }

  public void invalidateAll() {
    cache.invalidateAll();
  }

  public static class EvictedException extends Exception {

    public EvictedException(PollForDecisionTaskResponse task) {
      super(
          String.format(
              "cache was evicted for the decisionTask. WorkflowId: %s - RunId: %s",
              task.getWorkflowExecution().workflowId, task.getWorkflowExecution().runId));
    }
  }

  // For testing purposes
  LoadingCache<String, Decider> getInternalCache() {
    return cache;
  }
}
