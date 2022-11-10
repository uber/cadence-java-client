package com.uber.cadence.internal.worker.autoscaler;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PollerUsageEstimatorTest {

  @Test
  public void testUsageIsZero() {
    PollerUsageEstimator pollerUsageEstimator = new PollerUsageEstimator();
    PollerUsage pollerUsage = pollerUsageEstimator.estimate();
    assertEquals(0f, pollerUsage.getPollerUtilizationRate(), 0);
  }

  @Test
  public void testUsagesIs100Percent() {
    PollerUsageEstimator pollerUsageEstimator = new PollerUsageEstimator();
    pollerUsageEstimator.increaseActionableTaskCount();
    pollerUsageEstimator.increaseActionableTaskCount();
    PollerUsage pollerUsage = pollerUsageEstimator.estimate();
    assertEquals(1f, pollerUsage.getPollerUtilizationRate(), 0);
  }

  @Test
  public void testUsageCalculatedCorrectly() {
    PollerUsageEstimator pollerUsageEstimator = new PollerUsageEstimator();
    pollerUsageEstimator.increaseNoopTaskCount();
    pollerUsageEstimator.increaseActionableTaskCount();
    pollerUsageEstimator.increaseNoopTaskCount();
    pollerUsageEstimator.increaseNoopTaskCount();

    PollerUsage pollerUsage = pollerUsageEstimator.estimate();
    assertEquals(0.25f, pollerUsage.getPollerUtilizationRate(), 0);
  }

  @Test
  public void estimationIsReset() {
    PollerUsageEstimator pollerUsageEstimator = new PollerUsageEstimator();
    pollerUsageEstimator.increaseActionableTaskCount();

    PollerUsage pollerUsage = pollerUsageEstimator.estimate();
    assertEquals(1f, pollerUsage.getPollerUtilizationRate(), 0);

    pollerUsage = pollerUsageEstimator.estimate();
    assertEquals(0f, pollerUsage.getPollerUtilizationRate(), 0);
  }
}
