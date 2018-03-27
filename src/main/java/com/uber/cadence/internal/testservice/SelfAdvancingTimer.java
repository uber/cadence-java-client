package com.uber.cadence.internal.testservice;

import java.time.Duration;
import java.util.function.LongSupplier;

/**
 * Timer service that automatically forwards current time to the next task time when is not locked
 * through {@link #lockTimeSkipping()}.
 */
public interface SelfAdvancingTimer {

  /**
   * Schedule a task with a specified delay. The actual wait time is defined by the
   * internal clock that might advance much faster than the wall clock.
   */
  void schedule(Duration delay, Runnable task);

  /**
   * Supplier that returns current time of the timer when called.
   */
  LongSupplier getClock();

  /**
   * Prohibit automatic time skipping until {@link #unlockTimeSkipping()} is called.
   * Locks and unlocks are counted.
   */
  void lockTimeSkipping();

  void unlockTimeSkipping();

  void shutdown();
}
