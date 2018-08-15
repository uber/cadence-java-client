package com.uber.cadence.internal.replay;

import com.google.common.cache.LoadingCache;
import com.uber.cadence.PollForDecisionTaskResponse;
import com.uber.cadence.internal.common.ThrowableFunc1;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class ReplayDeciderCache {

  private LoadingCache<String, ReplayDecider> cache;
  private ThrowableFunc1<PollForDecisionTaskResponse, ReplayDecider, Exception> createReplayDecider;

  public ReplayDeciderCache(
      LoadingCache<String, ReplayDecider> cache,
      ThrowableFunc1<PollForDecisionTaskResponse, ReplayDecider, Exception> createReplayDecider) {
    Objects.requireNonNull(cache);
    Objects.requireNonNull(createReplayDecider);

    this.cache = cache;
    this.createReplayDecider = createReplayDecider;
  }

  public ReplayDecider getOrCreate(PollForDecisionTaskResponse decisionTask)
      throws Exception {
    synchronized (this) {
      String runId = decisionTask.getWorkflowExecution().getRunId();
      if (isFullHistory(decisionTask)) {
        cache.invalidate(runId);
      }
      ReplayDecider decider = cache.get(runId, ()-> createReplayDecider.apply(decisionTask));
      return decider;
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
}
