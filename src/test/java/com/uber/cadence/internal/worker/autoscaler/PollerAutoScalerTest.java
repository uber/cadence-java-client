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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import org.junit.Test;

public class PollerAutoScalerTest {

  @Test
  public void testAutoScalerScalesPollers() {
    PollerUsageEstimator pollerUsageEstimator = new PollerUsageEstimator();
    Recommender recommender = new Recommender(0.5f, 100, 10);
    PollerAutoScaler pollerAutoScaler =
        new PollerAutoScaler(Duration.ofSeconds(1), pollerUsageEstimator, recommender);

    assertEquals(100, pollerAutoScaler.getSemaphoreSize());

    pollerUsageEstimator.increaseActionableTaskCount();
    pollerAutoScaler.resizePollers();

    assertEquals(100, pollerAutoScaler.getSemaphoreSize());

    pollerUsageEstimator.increaseNoopTaskCount();

    pollerAutoScaler.resizePollers();

    assertEquals(10, pollerAutoScaler.getSemaphoreSize());
  }

  @Test
  public void testStart() throws Exception {
    // Get a partial mock of PollerAutoScaler
    PollerAutoScaler pollerAutoScaler =
        spy(new PollerAutoScaler(Duration.ofSeconds(10), null, new Recommender(0.5f, 100, 10)));

    // We want to test what resizePollers() is called, we don't want to test
    // the implementation of the resizePollers() method
    doNothing().when(pollerAutoScaler).resizePollers();

    // We let the loop run 3 times on the 3rd iteration we stop the pollerAutoScaler
    // so we expect resizePollers() to be called 2 times and stop() to be called once
    CountDownLatch latch = new CountDownLatch(3);
    doAnswer(
            invocation -> {
              latch.countDown();
              if (latch.getCount() == 0) {
                pollerAutoScaler.stop();
              }
              return null;
            })
        .when(pollerAutoScaler)
        .sleep(anyLong());

    pollerAutoScaler.start();

    // Wait for the latch to be 0
    latch.await();
    assertEquals(0, latch.getCount());

    // Verify that resizePollers() was called 2 times and stop() was called once
    verify(pollerAutoScaler, times(2)).resizePollers();
    verify(pollerAutoScaler, times(1)).stop();
  }

  @Test
  public void testAquireRelease() throws Exception {
    PollerAutoScaler pollerAutoScaler =
        new PollerAutoScaler(Duration.ofSeconds(10), null, new Recommender(0.5f, 100, 10));

    // access the private semaphore field
    Field semaphoreField = PollerAutoScaler.class.getDeclaredField("semaphore");
    semaphoreField.setAccessible(true);
    ResizableSemaphore semaphore = (ResizableSemaphore) semaphoreField.get(pollerAutoScaler);

    assertEquals(100, semaphore.availablePermits());

    pollerAutoScaler.acquire();
    assertEquals(99, semaphore.availablePermits());

    pollerAutoScaler.release();
    assertEquals(100, semaphore.availablePermits());
  }

  @Test
  public void testPollerIndirection() throws Exception {
    PollerUsageEstimator pollerUsageEstimator = mock(PollerUsageEstimator.class);

    PollerAutoScaler pollerAutoScaler =
        new PollerAutoScaler(
            Duration.ofSeconds(10), pollerUsageEstimator, new Recommender(0.5f, 100, 10));

    pollerAutoScaler.increaseNoopPollCount();
    verify(pollerUsageEstimator, times(1)).increaseNoopTaskCount();

    pollerAutoScaler.increaseActionablePollCount();
    verify(pollerUsageEstimator, times(1)).increaseActionableTaskCount();
  }
}
