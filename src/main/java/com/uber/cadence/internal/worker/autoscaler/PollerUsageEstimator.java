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

public class PollerUsageEstimator {

  private int noopTaskCount;
  private int actionableTaskCount;

  public void increaseNoopTaskCount() {
    noopTaskCount += 1;
  }

  public void increaseActionableTaskCount() {
    actionableTaskCount += 1;
  }

  public PollerUsage estimate() {
    if (noopTaskCount + actionableTaskCount == 0) {
      return new PollerUsage(0);
    }
    PollerUsage result =
        new PollerUsage((actionableTaskCount * 1f) / (noopTaskCount + actionableTaskCount));
    reset();
    return result;
  }

  public void reset() {
    noopTaskCount = 0;
    actionableTaskCount = 0;
  }
}
