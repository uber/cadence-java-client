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

package com.uber.cadence.internal.worker;

import java.time.Duration;
import java.util.Objects;

public class PollerAutoScalerOptions {

  private Duration pollerScalingInterval;
  private int minConcurrentPollers;
  private float targetPollerUtilisation;

  private PollerAutoScalerOptions() {}

  public static class Builder {

    private Duration pollerScalingInterval = Duration.ofMinutes(1);
    private int minConcurrentPollers = 1;
    private float targetPollerUtilisation = 0.6f;

    private Builder() {}

    public static Builder newBuilder() {
      return new Builder();
    }

    public Builder setPollerScalingInterval(Duration duration) {
      this.pollerScalingInterval = duration;
      return this;
    }

    public Builder setMinConcurrentPollers(int minConcurrentPollers) {
      this.minConcurrentPollers = minConcurrentPollers;
      return this;
    }

    public Builder setTargetPollerUtilisation(float targetPollerUtilisation) {
      this.targetPollerUtilisation = targetPollerUtilisation;
      return this;
    }

    public PollerAutoScalerOptions build() {
      PollerAutoScalerOptions pollerAutoScalerOptions = new PollerAutoScalerOptions();
      pollerAutoScalerOptions.pollerScalingInterval = this.pollerScalingInterval;
      pollerAutoScalerOptions.minConcurrentPollers = this.minConcurrentPollers;
      pollerAutoScalerOptions.targetPollerUtilisation = this.targetPollerUtilisation;
      return pollerAutoScalerOptions;
    }
  }

  public Duration getPollerScalingInterval() {
    return pollerScalingInterval;
  }

  public int getMinConcurrentPollers() {
    return minConcurrentPollers;
  }

  public float getTargetPollerUtilisation() {
    return targetPollerUtilisation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PollerAutoScalerOptions that = (PollerAutoScalerOptions) o;
    return minConcurrentPollers == that.minConcurrentPollers
        && Float.compare(that.targetPollerUtilisation, targetPollerUtilisation) == 0
        && Objects.equals(pollerScalingInterval, that.pollerScalingInterval);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pollerScalingInterval, minConcurrentPollers, targetPollerUtilisation);
  }

  @Override
  public String toString() {
    return "PollerAutoScalerOptions{"
        + "pollerScalingInterval="
        + pollerScalingInterval
        + ", minConcurrentPollers="
        + minConcurrentPollers
        + ", targetPollerUtilisation="
        + targetPollerUtilisation
        + '}';
  }
}
