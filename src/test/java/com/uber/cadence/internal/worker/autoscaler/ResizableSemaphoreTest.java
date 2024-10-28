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

import org.junit.Test;

public class ResizableSemaphoreTest {

  @Test
  public void releaseIncreasesNumberOfAvailablePermits() {
    ResizableSemaphore resizableSemaphore = new ResizableSemaphore(10);

    resizableSemaphore.release(2);

    assertEquals(12, resizableSemaphore.availablePermits());
  }

  @Test
  public void decreasePermitsDecreasesNumberOfAvailablePermits() {
    ResizableSemaphore resizableSemaphore = new ResizableSemaphore(10);

    resizableSemaphore.decreasePermits(2);

    assertEquals(8, resizableSemaphore.availablePermits());
  }
}
