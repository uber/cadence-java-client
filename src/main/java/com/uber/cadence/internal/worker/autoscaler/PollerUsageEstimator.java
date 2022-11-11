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

import java.util.concurrent.atomic.AtomicInteger;

public class PollerUsageEstimator {

  private AtomicInteger noopTaskCount = new AtomicInteger();
  private AtomicInteger actionableTaskCount = new AtomicInteger();

  public void increaseNoopTaskCount() {
    noopTaskCount.addAndGet(1);
  }

  public void increaseActionableTaskCount() {
    actionableTaskCount.addAndGet(1);
  }

  public PollerUsage estimate() {
    int actionableTasks = actionableTaskCount.get();
    int noopTasks = noopTaskCount.get();
    if (noopTasks + actionableTasks == 0) {
      return new PollerUsage(0);
    }
    PollerUsage result = new PollerUsage((actionableTasks * 1f) / (noopTasks + actionableTasks));
    reset();
    return result;
  }

  private void reset() {
    noopTaskCount.set(0);
    actionableTaskCount.set(0);
  }
}
