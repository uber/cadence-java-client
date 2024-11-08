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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import org.junit.Before;
import org.junit.Test;

public class ThrottlerTest {

  private Throttler throttler;

  @Before
  public void setUp() {
    // Initialize Throttler with a name, rate of 10 messages per second, and interval of 1000ms (1
    // second)
    throttler = new Throttler("TestResource", 10, 1000);
  }

  @Test
  public void testConstructorWithValidParameters() {
    // Ensure no exceptions are thrown with valid constructor parameters
    Throttler throttler = new Throttler("ResourceName", 5.0, 1000);
    assertNotNull(throttler);
  }

  @Test
  public void testConstructorWithNullName() {
    // Ensure that constructing Throttler with a null name throws IllegalArgumentException
    assertThrows(IllegalArgumentException.class, () -> new Throttler(null, 10, 1000));
  }

  @Test
  public void testConstructorWithZeroRate() {
    // Ensure that a zero max rate throws an IllegalArgumentException
    assertThrows(IllegalArgumentException.class, () -> new Throttler("ResourceName", 0, 1000));
  }

  @Test
  public void testConstructorWithNegativeRate() {
    // Ensure that a negative max rate throws an IllegalArgumentException
    assertThrows(IllegalArgumentException.class, () -> new Throttler("ResourceName", -5, 1000));
  }

  @Test
  public void testConstructorWithZeroRateInterval() {
    // Ensure that a zero rate interval throws an IllegalArgumentException
    assertThrows(IllegalArgumentException.class, () -> new Throttler("ResourceName", 10, 0));
  }

  @Test
  public void testConstructorWithNegativeRateInterval() {
    // Ensure that a negative rate interval throws an IllegalArgumentException
    assertThrows(IllegalArgumentException.class, () -> new Throttler("ResourceName", 10, -1000));
  }

  @Test
  public void testSetMaxRatePerSecond() {
    // Verify that setting a valid max rate per second does not throw exceptions and changes the
    // internal rate
    throttler.setMaxRatePerSecond(0);

    throttler.setMaxRatePerSecond(5.0);
    // No explicit output to assert; this test checks that no exceptions occur
  }

  @Test
  public void testThrottle() throws InterruptedException {
    // Test throttle by calling the method several times and verifying no exceptions
    for (int i = 0; i < 10; i++) {
      throttler.throttle(); // This test will run without explicit assertions
    }
  }

  @Test
  public void testThrottleAtHighRate() throws InterruptedException {
    throttler.setMaxRatePerSecond(100000.0); // High rate
    for (int i = 0; i < 100; i++) {
      throttler.throttle();
    }
  }

  @Test
  public void testThrottleAtLowRate() throws InterruptedException {
    throttler.setMaxRatePerSecond(1); // Low rate
    for (int i = 0; i < 10; i++) {
      throttler.throttle();
    }
  }
}
