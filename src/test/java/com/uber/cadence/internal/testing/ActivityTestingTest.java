package com.uber.cadence.internal.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.uber.cadence.activity.Activity;
import com.uber.cadence.workflow.ActivityFailureException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class ActivityTestingTest {

  public interface TestActivity {

    String activity1(String input);
  }

  private static class ActivityImpl implements TestActivity {

    @Override
    public String activity1(String input) {
      return Activity.getTask().getActivityType().getName() + "-" + input;
    }
  }

  @Test
  public void testSuccess() {
    TestEnvironment env = TestEnvironment.newInstance(null);
    env.registerActivitiesImplementations(new ActivityImpl());
    TestActivity activity = env.newActivityStub(TestActivity.class);
    String result = activity.activity1("input1");
    assertEquals("TestActivity::activity1-input1", result);
  }

  private static class AngryActivityImpl implements TestActivity {

    @Override
    public String activity1(String input) {
      throw Activity.wrap(new IOException("simulated"));
    }
  }

  @Test
  public void testFailure() {
    TestEnvironment env = TestEnvironment.newInstance(null);
    env.registerActivitiesImplementations(new AngryActivityImpl());
    TestActivity activity = env.newActivityStub(TestActivity.class);
    try {
      activity.activity1("input1");
      fail("unreachable");
    } catch (ActivityFailureException e) {
      assertTrue(e.getMessage().contains("TestActivity::activity1"));
      assertTrue(e.getCause() instanceof IOException);
      assertEquals("simulated", e.getCause().getMessage());
    }
  }

  private static class HeartbeatActivityImpl implements TestActivity {

    @Override
    public String activity1(String input) {
      Activity.heartbeat("details1");
      return input;
    }
  }

  @Test
  public void testHeartbeat() {
    TestEnvironment env = TestEnvironment.newInstance(null);
    env.registerActivitiesImplementations(new HeartbeatActivityImpl());
    AtomicReference<String> details = new AtomicReference<>();
    env.setActivityHeartbeatListener(String.class, (d) -> {
      details.set(d);
    });
    TestActivity activity = env.newActivityStub(TestActivity.class);
    String result = activity.activity1("input1");
    assertEquals("input1", result);
    assertEquals("details1", details.get());
  }
}
