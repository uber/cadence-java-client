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

public class PollerAutoScalerOptionsTest {
  @Test
  public void testNewBuilder() {
    PollerAutoScalerOptions.Builder builder = PollerAutoScalerOptions.Builder.newBuilder();

    assertNotNull(builder);
  }

  @Test
  public void testNewBuilderWithOptions() {
    PollerAutoScalerOptions.Builder builder = PollerAutoScalerOptions.Builder.newBuilder();
    PollerAutoScalerOptions options = builder.build();

    assertNotNull(options);
    assertEquals(Duration.ofMinutes(1), options.getPollerScalingInterval());
    assertEquals(1, options.getMinConcurrentPollers());
    assertEquals(0.6f, options.getTargetPollerUtilisation(), 0.0);
  }

  @Test
  public void testSetPollerScalingInterval() {
    PollerAutoScalerOptions.Builder builder = PollerAutoScalerOptions.Builder.newBuilder();
    builder.setPollerScalingInterval(Duration.ofMinutes(2));
    PollerAutoScalerOptions options = builder.build();

    assertNotNull(options);
    assertEquals(Duration.ofMinutes(2), options.getPollerScalingInterval());
    assertEquals(1, options.getMinConcurrentPollers());
    assertEquals(0.6f, options.getTargetPollerUtilisation(), 0.0);
  }

  @Test
  public void testSetMinConcurrentPollers() {
    PollerAutoScalerOptions.Builder builder = PollerAutoScalerOptions.Builder.newBuilder();
    builder.setMinConcurrentPollers(2);
    PollerAutoScalerOptions options = builder.build();

    assertNotNull(options);
    assertEquals(Duration.ofMinutes(1), options.getPollerScalingInterval());
    assertEquals(2, options.getMinConcurrentPollers());
    assertEquals(0.6f, options.getTargetPollerUtilisation(), 0.0);
  }

  @Test
  public void testSetTargetPollerUtilisation() {
    PollerAutoScalerOptions.Builder builder = PollerAutoScalerOptions.Builder.newBuilder();
    builder.setTargetPollerUtilisation(0.7f);
    PollerAutoScalerOptions options = builder.build();

    assertNotNull(options);
    assertEquals(Duration.ofMinutes(1), options.getPollerScalingInterval());
    assertEquals(1, options.getMinConcurrentPollers());
    assertEquals(0.7f, options.getTargetPollerUtilisation(), 0.0);
  }

  @Test
  public void testEquals() {
    PollerAutoScalerOptions.Builder builder = PollerAutoScalerOptions.Builder.newBuilder();
    builder.setPollerScalingInterval(Duration.ofMinutes(2));
    builder.setMinConcurrentPollers(2);
    builder.setTargetPollerUtilisation(0.7f);
    PollerAutoScalerOptions options = builder.build();
    PollerAutoScalerOptions options2 = builder.build();

    assertTrue(options.equals(options));
    assertFalse(options.equals(null));
    assertTrue(options.equals(options2));
  }

  @Test
  public void testHashCode() {
    PollerAutoScalerOptions options = PollerAutoScalerOptions.Builder.newBuilder().build();

    assertEquals(1058729812, options.hashCode());
  }

  @Test
  public void testToString() {
    PollerAutoScalerOptions options = PollerAutoScalerOptions.Builder.newBuilder().build();

    assertEquals(
        "PollerAutoScalerOptions{pollerScalingInterval=PT1M, minConcurrentPollers=1, targetPollerUtilisation=0.6}",
        options.toString());
  }
}
