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

import java.time.Duration;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PollerAutoScaler implements AutoScaler {

  private static final Logger LOGGER = LoggerFactory.getLogger(PollerAutoScaler.class);

  private final Duration coolDownTime;
  private final PollerUsageEstimator pollerUsageEstimator;
  private final Recommender recommender;
  private final ResizableSemaphore semaphore;
  private int semaphoreSize;
  private boolean shuttingDown;

  public PollerAutoScaler(
      Duration coolDownTime, PollerUsageEstimator pollerUsageEstimator, Recommender recommender) {
    this.coolDownTime = coolDownTime;
    this.pollerUsageEstimator = pollerUsageEstimator;
    this.recommender = recommender;
    this.semaphore = new ResizableSemaphore(recommender.getUpperValue());
    this.semaphoreSize = recommender.getUpperValue();
  }

  // Sleep method which can be mocked in tests
  void sleep(long millis) throws InterruptedException {
    Thread.sleep(millis);
  }

  public void start() {
    Executors.newSingleThreadExecutor()
        .submit(
            new Runnable() {
              @Override
              public void run() {
                while (!shuttingDown) {
                  try {
                    sleep(coolDownTime.toMillis());
                    if (!shuttingDown) {
                      resizePollers();
                    }
                  } catch (InterruptedException e) {
                    LOGGER.info("interrupted wait for next poller scaling");
                  }
                }
              }
            });
  }

  public void stop() {
    LOGGER.info("shutting down poller autoscaler");
    shuttingDown = true;
  }

  protected void resizePollers() {
    PollerUsage pollerUsage = pollerUsageEstimator.estimate();
    int pollerCount =
        recommender.recommend(this.semaphoreSize, pollerUsage.getPollerUtilizationRate());

    int diff = this.semaphoreSize - pollerCount;
    if (diff < 0) {
      semaphore.release(diff * -1);
    } else {
      semaphore.decreasePermits(diff);
    }

    LOGGER.info(String.format("resized pollers to: %d", pollerCount));
    this.semaphoreSize = pollerCount;
  }

  public void acquire() throws InterruptedException {
    semaphore.acquire();
  }

  public void release() {
    semaphore.release();
  }

  @Override
  public void increaseNoopPollCount() {
    pollerUsageEstimator.increaseNoopTaskCount();
  }

  @Override
  public void increaseActionablePollCount() {
    pollerUsageEstimator.increaseActionableTaskCount();
  }

  // For testing
  protected int getSemaphoreSize() {
    return semaphoreSize;
  }
}
