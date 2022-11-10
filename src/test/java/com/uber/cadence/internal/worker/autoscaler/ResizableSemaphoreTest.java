package com.uber.cadence.internal.worker.autoscaler;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
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
