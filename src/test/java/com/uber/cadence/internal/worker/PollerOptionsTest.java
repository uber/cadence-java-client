/**
 * Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * <p>Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. A copy of the License is located at
 *
 * <p>http://aws.amazon.com/apache2.0
 *
 * <p>or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.uber.cadence.internal.worker;

import static org.junit.Assert.*;

import java.time.Duration;
import org.junit.Test;

public class PollerOptionsTest {
  @Test
  public void testNewBuilder() {
    PollerOptions.newBuilder();
  }

  @Test
  public void testNewBuilderWithOptions() {
    PollerOptions.Builder builder = PollerOptions.newBuilder();
    PollerOptions options = builder.build();

    assertEquals(1000, options.getMaximumPollRateIntervalMilliseconds());
    assertEquals(0.0, options.getMaximumPollRatePerSecond(), 0.0);
    assertEquals(2.0, options.getPollBackoffCoefficient(), 0.0);
    assertEquals(100, options.getPollBackoffInitialInterval().toMillis());
    assertEquals(60000, options.getPollBackoffMaximumInterval().toMillis());
    assertEquals(1, options.getPollThreadCount());
    assertNotNull(options.getUncaughtExceptionHandler());
    assertNull(options.getPollThreadNamePrefix());
    assertFalse(options.getPollOnlyIfExecutorHasCapacity());
    assertNull(options.getPollerAutoScalerOptions());
  }

  @Test
  public void testGetDefaultInstance() {
    PollerOptions options = PollerOptions.getDefaultInstance();

    assertEquals(1000, options.getMaximumPollRateIntervalMilliseconds());
    assertEquals(0.0, options.getMaximumPollRatePerSecond(), 0.0);
    assertEquals(2.0, options.getPollBackoffCoefficient(), 0.0);
    assertEquals(100, options.getPollBackoffInitialInterval().toMillis());
    assertEquals(60000, options.getPollBackoffMaximumInterval().toMillis());
    assertEquals(1, options.getPollThreadCount());
    assertNotNull(options.getUncaughtExceptionHandler());
    assertNull(options.getPollThreadNamePrefix());
    assertFalse(options.getPollOnlyIfExecutorHasCapacity());
    assertNull(options.getPollerAutoScalerOptions());
  }

  @Test
  public void testSetters() {
    PollerOptions.Builder builder = PollerOptions.newBuilder(null);
    builder.setMaximumPollRateIntervalMilliseconds(2);
    builder.setMaximumPollRatePerSecond(3.0);
    builder.setPollBackoffCoefficient(4.0);
    builder.setPollBackoffInitialInterval(Duration.ofMillis(5));
    builder.setPollBackoffMaximumInterval(Duration.ofMillis(6));
    builder.setPollThreadCount(7);
    builder.setUncaughtExceptionHandler((t, e) -> {});
    builder.setPollThreadNamePrefix("prefix");
    builder.setPollOnlyIfExecutorHasCapacity(true);
    PollerAutoScalerOptions autoScalerOptions =
        PollerAutoScalerOptions.Builder.newBuilder()
            .setPollerScalingInterval(Duration.ofMinutes(1))
            .setMinConcurrentPollers(1)
            .setTargetPollerUtilisation(0.6f)
            .build();
    builder.setPollerAutoScalerOptions(autoScalerOptions);

    PollerOptions options = builder.build();
    PollerOptions.Builder newBuilder = PollerOptions.newBuilder(options);
    PollerOptions newOptions = newBuilder.build();

    assertEquals(2, newOptions.getMaximumPollRateIntervalMilliseconds());
    assertEquals(3.0, newOptions.getMaximumPollRatePerSecond(), 0.0);
    assertEquals(4.0, newOptions.getPollBackoffCoefficient(), 0.0);
    assertEquals(5, newOptions.getPollBackoffInitialInterval().toMillis());
    assertEquals(6, newOptions.getPollBackoffMaximumInterval().toMillis());
    assertEquals(7, newOptions.getPollThreadCount());
    assertNotNull(newOptions.getUncaughtExceptionHandler());
    assertEquals("prefix", newOptions.getPollThreadNamePrefix());
    assertTrue(newOptions.getPollOnlyIfExecutorHasCapacity());
    assertEquals(autoScalerOptions, newOptions.getPollerAutoScalerOptions());
  }

  @Test
  public void testToString() {
    PollerOptions options = PollerOptions.getDefaultInstance();
    assertEquals(
        "PollerOptions{maximumPollRateIntervalMilliseconds=1000, maximumPollRatePerSecond=0.0, pollBackoffCoefficient=2.0, pollBackoffInitialInterval=PT0.1S, pollBackoffMaximumInterval=PT1M, pollThreadCount=1, pollThreadNamePrefix='null, pollOnlyIfExecutorHasCapacity='false, pollerAutoScalerOptions='null'}",
        options.toString());
  }
}
