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

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.uber.cadence.PollForDecisionTaskResponse;
import com.uber.cadence.internal.common.ThrowableFunc1;
import java.util.Objects;

public final class ReplayDeciderCache {
  private LoadingCache<String, ReplayDecider> cache;

  public ReplayDeciderCache(LoadingCache<String, ReplayDecider> cache) {
    Objects.requireNonNull(cache);

    this.cache = cache;
  }

  public ReplayDecider getOrCreate(
      PollForDecisionTaskResponse decisionTask,
      ThrowableFunc1<PollForDecisionTaskResponse, ReplayDecider, Exception> createReplayDecider)
      throws Exception {
    synchronized (this) {
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
  }

  public void invalidate(PollForDecisionTaskResponse decisionTask) {
    synchronized (this) {
      String runId = decisionTask.getWorkflowExecution().getRunId();
      invalidate(runId);
    }
  }

  public void invalidate(String runId) {
    synchronized (this) {
      cache.invalidate(runId);
    }
  }

  public long size() {
    synchronized (this) {
      return cache.size();
    }
  }

  private boolean isFullHistory(PollForDecisionTaskResponse decisionTask) {
    return decisionTask.history.events.get(0).getEventId() == 1;
  }

  public void invalidateAll() {
    synchronized (this) {
      cache.invalidateAll();
    }
  }

  public static class EvictedException extends Exception {

    public EvictedException(PollForDecisionTaskResponse task) {
      super(
          String.format(
              "cache was evicted for the decisionTask. WorkflowId: %s - RunId: %s",
              task.getWorkflowExecution().workflowId, task.getWorkflowExecution().runId));
    }
  }
}
