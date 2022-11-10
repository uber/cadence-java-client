package com.uber.cadence.internal.worker.autoscaler;

import static org.junit.Assert.*;

import java.time.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PollerAutoScalerTest {

  @Test
  public void testAutoScalerScalesPollers() {
    PollerUsageEstimator pollerUsageEstimator = new PollerUsageEstimator();
    Recommender recommender = new Recommender(0.5f, 100, 10, "test");
    PollerAutoScaler pollerAutoScaler =
        new PollerAutoScaler(Duration.ofSeconds(1), pollerUsageEstimator, recommender);

    assertEquals(100, pollerAutoScaler.getSemaphoreSize());

    pollerUsageEstimator.increaseActionableTaskCount();
    pollerAutoScaler.resizePollers();

    assertEquals(100, pollerAutoScaler.getSemaphoreSize());

    pollerUsageEstimator.increaseNoopTaskCount();

    pollerAutoScaler.resizePollers();

    assertEquals(10, pollerAutoScaler.getSemaphoreSize());
  }
}
