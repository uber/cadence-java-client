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

import com.uber.cadence.internal.worker.PollerAutoScalerOptions;
import com.uber.cadence.internal.worker.PollerOptions;

public class AutoScalerFactory {

  private static final AutoScalerFactory INSTANCE = new AutoScalerFactory();

  private AutoScalerFactory() {}

  public AutoScaler createAutoScaler(PollerOptions pollerOptions) {
    if (pollerOptions == null || pollerOptions.getPollerAutoScalerOptions() == null) {
      return new NoopAutoScaler();
    }

    PollerAutoScalerOptions autoScalerOptions = pollerOptions.getPollerAutoScalerOptions();
    return new PollerAutoScaler(
        autoScalerOptions.getPollerScalingInterval(),
        new PollerUsageEstimator(),
        new Recommender(
            autoScalerOptions.getTargetPollerUtilisation(),
            pollerOptions.getPollThreadCount(),
            autoScalerOptions.getMinConcurrentPollers()));
  }

  public static AutoScalerFactory getInstance() {
    return INSTANCE;
  }
}
