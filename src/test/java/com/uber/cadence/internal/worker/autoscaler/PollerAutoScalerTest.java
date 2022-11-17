/*
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
    Recommender recommender = new Recommender(0.5f, 100, 10);
    PollerAutoScaler pollerAutoScaler =
        new PollerAutoScaler(Duration.ofSeconds(1), pollerUsageEstimator, recommender);

    assertEquals(100, pollerAutoScaler.getSemaphoreSize());

    pollerUsageEstimator.increaseActionableTaskCount();
    pollerAutoScaler.resizePollers();

    assertEquals(100, pollerAutoScaler.getSemaphoreSize());

    pollerUsageEstimator.increaseNoopTaskCount();

    pollerAutoScaler.resizePollers();

    assertEquals(10, pollerAutoScaler.getSemaphoreSize());

    pollerUsageEstimator.increaseActionableTaskCount();
    pollerUsageEstimator.increaseActionableTaskCount();

    pollerAutoScaler.resizePollers();

    assertEquals(100, pollerAutoScaler.getSemaphoreSize());

    pollerUsageEstimator.increaseActionableTaskCount();
    pollerUsageEstimator.increaseActionableTaskCount();
    pollerUsageEstimator.increaseNoopTaskCount();
    pollerUsageEstimator.increaseNoopTaskCount();
    pollerUsageEstimator.increaseNoopTaskCount();

    pollerAutoScaler.resizePollers();

    assertEquals(80, pollerAutoScaler.getSemaphoreSize());
  }
}
