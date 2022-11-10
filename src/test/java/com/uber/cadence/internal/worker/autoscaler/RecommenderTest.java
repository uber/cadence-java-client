package com.uber.cadence.internal.worker.autoscaler;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RecommenderTest {

  @Test
  public void pollerUnderutilizedShouldReduceToLowerBound() {
    Recommender recommender = new Recommender(0.5f, 100, 1);

    int recommendedPollerCount = recommender.recommend(100, 0);
    assertEquals(1, recommendedPollerCount);
  }

  @Test
  public void pollerUnderUtilsedShouldReduce() {
    Recommender recommender = new Recommender(0.5f, 100, 1);

    int recommendedPollerCount = recommender.recommend(100, 0.1f);
    assertEquals(20, recommendedPollerCount);
  }

  @Test
  public void polleratTargetRateShouldRemainUnchanged() {
    Recommender recommender = new Recommender(0.5f, 100, 1);
    int recommendedPollerCount = recommender.recommend(25, 0.5f);
    assertEquals(25, recommendedPollerCount);
  }

  @Test
  public void pollerOverUtilised100PercentShouldAddPollersToMax() {
    Recommender recommender = new Recommender(0.5f, 100, 1);

    int recommendedPollerCount = recommender.recommend(5, 1);
    assertEquals(100, recommendedPollerCount);
  }

  @Test
  public void pollerOverUtilisedShouldAddPollers() {
    Recommender recommender = new Recommender(0.4f, 100, 1);
    int recommendedPollerCount = recommender.recommend(10, 0.8f);
    assertEquals(20, recommendedPollerCount);
  }

  @Test
  public void pollerOverUtilisedUpperBound() {
    Recommender recommender = new Recommender(0.5f, 100, 1);

    int recommendedPollerCount = recommender.recommend(99, 1);
    assertEquals(100, recommendedPollerCount);
  }
}
