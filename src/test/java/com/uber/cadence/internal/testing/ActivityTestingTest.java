package com.uber.cadence.internal.testing;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ActivityTestingTest {

  public interface Activity {

    String activity1(String input);
  }

  private static class ActivityImpl implements Activity {

    @Override
    public String activity1(String input) {
      return input;
    }
  }

  @Test
  public void test() {
    TestEnvironment env = TestEnvironment.newInstance(null);
    env.registerActivitiesImplementations(new ActivityImpl());
    Activity activity = env.newActivityStub(Activity.class);
    String result = activity.activity1("input1");
    assertEquals("input1", result);
  }

}
