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

import java.util.ArrayList;
import java.util.List;
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

  @Test
  public void testThreadSafety() throws Exception {
    PollerUsageEstimator pollerUsageEstimator = new PollerUsageEstimator();
    List<Thread> threadList = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      threadList.add(
          new Thread(
              new Runnable() {
                @Override
                public void run() {
                  for (int i = 0; i < 100; i++) {
                    pollerUsageEstimator.increaseActionableTaskCount();
                  }
                }
              }));
    }

    threadList.forEach(Thread::start);
    for (Thread thread : threadList) {
      thread.join();
    }

    pollerUsageEstimator.increaseNoopTaskCount();
    assertEquals(10000f / 10001, pollerUsageEstimator.estimate().getPollerUtilizationRate(), 0);
  }
}
