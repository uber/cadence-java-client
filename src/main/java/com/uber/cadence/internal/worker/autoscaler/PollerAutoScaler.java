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

public class PollerAutoScaler {

  private static final Logger LOGGER = LoggerFactory.getLogger(PollerAutoScaler.class);

  private Duration coolDownTime;
  private PollerUsageEstimator pollerUsageEstimator;
  private Recommender recommender;
  private ResizableSemaphore semaphore;
  private int semaphoreSize;
  private boolean shutingDown;

  public PollerAutoScaler(
      Duration coolDownTime, PollerUsageEstimator pollerUsageEstimator, Recommender recommender) {
    this.coolDownTime = coolDownTime;
    this.pollerUsageEstimator = pollerUsageEstimator;
    this.recommender = recommender;
    this.semaphore = new ResizableSemaphore(recommender.getUpperValue());
    this.semaphoreSize = recommender.getUpperValue();
  }

  public void start() {
    Executors.newSingleThreadExecutor()
        .submit(
            new Runnable() {
              @Override
              public void run() {
                while (!shutingDown) {
                  try {
                    Thread.sleep(coolDownTime.toMillis());
                    if (!shutingDown) {
                      resizePollers();
                    }
                  } catch (InterruptedException e) {
                  }
                }
              }
            });
  }

  public void stop() {
    LOGGER.info("shutting down poller autoscaler");
    shutingDown = true;
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

  public int getLowerPollerAmount() {
    return recommender.getLowerValue();
  }

  public int getUpperPollerAmount() {
    return recommender.getUpperValue();
  }

  public PollerUsageEstimator getPollerUsageEstimator() {
    return pollerUsageEstimator;
  }

  public Recommender getRecommender() {
    return recommender;
  }

  public void acquire() throws InterruptedException {
    semaphore.acquire();
  }

  public void release() {
    semaphore.release();
  }

  // For testing
  protected int getSemaphoreSize() {
    return semaphoreSize;
  }
}
