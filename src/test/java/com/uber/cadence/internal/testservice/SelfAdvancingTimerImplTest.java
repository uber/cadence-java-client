package com.uber.cadence.internal.testservice;

import static org.junit.Assert.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SelfAdvancingTimerImplTest {

  public static final int INITIAL_TIME = 1;
  private SelfAdvancingTimer timer;
  private LongSupplier clock;

  @Before
  public void setUp() throws Exception {
    timer = new SelfAdvancingTimerImpl(INITIAL_TIME);
    clock = timer.getClock();
  }

  @After
  public void tearDown() throws Exception {
    timer.shutdown();
  }

  @Test
  public void testSchedule() throws InterruptedException {
    AtomicLong captured = new AtomicLong();
    timer.schedule(Duration.ofDays(1), () -> captured.set(clock.getAsLong()));
    Thread.sleep(100);
    assertTrue(Duration.ofDays(1).toMillis() + INITIAL_TIME <= captured.get());
    assertTrue(Duration.ofDays(1).toMillis() + INITIAL_TIME <= clock.getAsLong());
    long start = clock.getAsLong();
    timer.schedule(Duration.ofSeconds(123), () -> captured.set(clock.getAsLong()));
    assertTrue(clock.getAsLong() - start >= Duration.ofSeconds(123).toMillis());
  }

  @Test
  public void testOrdering() throws InterruptedException {
    List<Long> captured = Collections.synchronizedList(new ArrayList<>());
    timer.lockTimeSkipping();
    timer.schedule(Duration.ofSeconds(100), () -> captured.add(clock.getAsLong()));
    timer.schedule(Duration.ofSeconds(20), () -> captured.add(clock.getAsLong()));
    timer.schedule(Duration.ofSeconds(10), () -> captured.add(clock.getAsLong()));
    timer.schedule(Duration.ofSeconds(1), () -> captured.add(clock.getAsLong()));
    timer.unlockTimeSkipping();
    Thread.sleep(100);
    List<Long> expected = Arrays
        .asList(Duration.ofSeconds(1).toMillis(), Duration.ofSeconds(10).toMillis(),
            Duration.ofSeconds(20).toMillis(), Duration.ofSeconds(100).toMillis());
    for (int i = 0; i < captured.size(); i++) {
      assertEquals(expected.get(i), captured.get(i), 50.0);
    }
  }
}