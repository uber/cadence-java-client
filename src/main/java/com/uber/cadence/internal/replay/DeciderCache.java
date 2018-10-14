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
import com.google.common.cache.Weigher;
import com.uber.cadence.PollForDecisionTaskResponse;
import com.uber.cadence.internal.common.ThrowableFunc1;
import com.uber.cadence.internal.metrics.MetricsType;
import com.uber.m3.tally.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class DeciderCache {
  private final String evictionEntryId = UUID.randomUUID().toString();
  private final int maxCacheSize;
  private final Scope metricsScope;
  private LoadingCache<String, WeightedCacheEntry<Decider>> cache;
  private Lock evictionLock = new ReentrantLock();
  Random rand = new Random();

    private static final Logger log = LoggerFactory.getLogger(DeciderCache.class);

    public DeciderCache(int maxCacheSize, Scope scope) {
    Preconditions.checkArgument(maxCacheSize > 0, "Max cache size must be greater than 0");
    this.maxCacheSize = maxCacheSize;
    this.metricsScope = Objects.requireNonNull(scope);
    this.cache =
        CacheBuilder.newBuilder()
            .maximumWeight(maxCacheSize)
            .concurrencyLevel(1)
            .weigher(
                (Weigher<String, WeightedCacheEntry<Decider>>) (key, value) -> value.getWeight())
            .removalListener(
                e -> {
                  Decider entry = e.getValue().entry;
                  if (entry != null) {
                    entry.close();
                  }
                })
            .build(
                new CacheLoader<String, WeightedCacheEntry<Decider>>() {
                  @Override
                  public WeightedCacheEntry<Decider> load(String key) {
                    return null;
                  }
                });
  }

  public Decider getOrCreate(
      PollForDecisionTaskResponse decisionTask,
      ThrowableFunc1<PollForDecisionTaskResponse, Decider, Exception> createReplayDecider)
      throws Exception {
    String runId = decisionTask.getWorkflowExecution().getRunId();
    metricsScope.gauge(MetricsType.STICKY_CACHE_SIZE).update(size());
    if (isFullHistory(decisionTask)) {
      invalidate(decisionTask);
      return cache.get(
              runId, () -> new WeightedCacheEntry<>(createReplayDecider.apply(decisionTask), 1))
          .entry;
    }
    return getUnchecked(runId);
  }

  public Decider getUnchecked(String runId) throws Exception {
    try {
      Decider cachedDecider = cache.getUnchecked(runId).entry;
      metricsScope.counter(MetricsType.STICKY_CACHE_HIT).inc(1);
      return cachedDecider;
    } catch (CacheLoader.InvalidCacheLoadException e) {
      metricsScope.counter(MetricsType.STICKY_CACHE_MISS).inc(1);
      throw new EvictedException(runId);
    }
  }

  public void evictAny(String runId) throws InterruptedException {
    // Timeout is to guard against workflows trying to evict each other.
    if (!evictionLock.tryLock(rand.nextInt(4), TimeUnit.SECONDS)) {
      return;
    }
    try {
      metricsScope.gauge(MetricsType.STICKY_CACHE_SIZE).update(size());
      Set<String> set = cache.asMap().keySet();
      if (set.isEmpty()) {
        return;
      }
      Iterator<String> iter = cache.asMap().keySet().iterator();
      String key = null;
      while (iter.hasNext()) {
        key = iter.next();
        if (key != runId) {
          break;
        }
      }

      if (key == runId) {
        log.warn(String.format("%s attempted to self evict. Ignoring eviction", runId));
        return;
      }
      if (key != null) {
        cache.invalidate(key);
      }
      metricsScope.gauge(MetricsType.STICKY_CACHE_SIZE).update(size());
      metricsScope.counter(MetricsType.STICKY_CACHE_THREAD_FORCED_EVICTION).inc(1);
    } finally {
      evictionLock.unlock();
    }
  }

  public void invalidate(PollForDecisionTaskResponse decisionTask) throws InterruptedException {
    String runId = decisionTask.getWorkflowExecution().getRunId();
    invalidate(runId);
  }

  private void invalidate(String runId) throws InterruptedException {
    if (!evictionLock.tryLock(rand.nextInt(4), TimeUnit.SECONDS)) {
      return;
    }
    try {
      cache.invalidate(runId);
      metricsScope.counter(MetricsType.STICKY_CACHE_TOTAL_FORCED_EVICTION).inc(1);
    } finally {
      evictionLock.unlock();
    }
  }

  public long size() {
    return cache.size();
  }

  private boolean isFullHistory(PollForDecisionTaskResponse decisionTask) {
    return decisionTask.getHistory() != null
        && decisionTask.getHistory().getEvents().size() > 0
        && decisionTask.getHistory().getEvents().get(0).getEventId() == 1;
  }

  public void invalidateAll() {
    cache.invalidateAll();
  }

  // Used for eviction
  private static class WeightedCacheEntry<T> {
    private T entry;
    private int weight;

    private WeightedCacheEntry(T entry, int weight) {
      this.entry = entry;
      this.weight = weight;
    }

    public T getEntry() {
      return entry;
    }

    public int getWeight() {
      return weight;
    }
  }

  public static class EvictedException extends Exception {

    public EvictedException(String runId) {
      super(String.format("cache was evicted for the decisionTask. RunId: %s", runId));
    }
  }
}
