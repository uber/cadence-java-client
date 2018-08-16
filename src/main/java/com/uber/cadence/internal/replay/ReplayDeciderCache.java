package com.uber.cadence.internal.replay;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.uber.cadence.PollForDecisionTaskResponse;
import com.uber.cadence.internal.common.ThrowableFunc1;

import java.util.Objects;

public final class ReplayDeciderCache {

  private LoadingCache<String, ReplayDecider> cache;

  public ReplayDeciderCache(
      LoadingCache<String, ReplayDecider> cache) {
    Objects.requireNonNull(cache);

    this.cache = cache;
  }

  public ReplayDecider getOrCreate(PollForDecisionTaskResponse decisionTask, ThrowableFunc1<PollForDecisionTaskResponse, ReplayDecider, Exception> createReplayDecider) throws Exception {
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

  private boolean isFullHistory(PollForDecisionTaskResponse decisionTask) {
    return decisionTask.history.events.get(0).getEventId() == 1;
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
