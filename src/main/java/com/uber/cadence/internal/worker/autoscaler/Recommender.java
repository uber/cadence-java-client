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

public class Recommender {

  private final float targetPollerUtilRate;
  private final int upperValue;
  private final int lowerValue;

  public Recommender(float targetPollerUtilRate, int upperValue, int lowerValue) {
    this.targetPollerUtilRate = targetPollerUtilRate;
    this.upperValue = upperValue;
    this.lowerValue = lowerValue;
  }

  public int recommend(int currentPollers, float pollerUtilizationRate) {
    int recommended = 0;

    if (pollerUtilizationRate == 1) {
      return upperValue;
    }

    float r = currentPollers * pollerUtilizationRate / targetPollerUtilRate;
    r = Math.min(upperValue, Math.max(lowerValue, r));
    recommended += r;
    return recommended;
  }

  public int getUpperValue() {
    return upperValue;
  }

  public int getLowerValue() {
    return lowerValue;
  }
}
