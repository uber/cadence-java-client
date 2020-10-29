/*
 *  Modifications Copyright (c) 2017-2020 Uber Technologies Inc.
 *  Portions of the Software are attributed to Copyright (c) 2020 Temporal Technologies Inc.
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.uber.cadence.internal.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.uber.cadence.RecordActivityTaskHeartbeatResponse;
import com.uber.cadence.activity.Activity;
import com.uber.cadence.activity.ActivityInterface;
import com.uber.cadence.client.ActivityCancelledException;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.testing.TestActivityEnvironment;
import com.uber.cadence.workflow.ActivityFailureException;
import io.netty.util.internal.ConcurrentSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Test;

public class ActivityTestingTest {

  private TestActivityEnvironment testEnvironment;

  @Before
  public void setUp() {
    testEnvironment = TestActivityEnvironment.newInstance();
  }

  @ActivityInterface
  public interface TestActivity {

    String activity1(String input);
  }

  private static class ActivityImpl implements TestActivity {

    @Override
    public String activity1(String input) {
      return Activity.getTask().getActivityType() + "-" + input;
    }
  }

  @Test
  public void testSuccess() {
    testEnvironment.registerActivitiesImplementations(new ActivityImpl());
    TestActivity activity = testEnvironment.newActivityStub(TestActivity.class);
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
    testEnvironment.registerActivitiesImplementations(new AngryActivityImpl());
    TestActivity activity = testEnvironment.newActivityStub(TestActivity.class);
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
    testEnvironment.registerActivitiesImplementations(new HeartbeatActivityImpl());
    AtomicReference<String> details = new AtomicReference<>();
    testEnvironment.setActivityHeartbeatListener(String.class, details::set);
    TestActivity activity = testEnvironment.newActivityStub(TestActivity.class);
    String result = activity.activity1("input1");
    assertEquals("input1", result);
    assertEquals("details1", details.get());
  }

  @ActivityInterface
  public interface InterruptibleTestActivity {
    void activity1() throws InterruptedException;
  }

  private static class BurstHeartbeatActivityImpl implements InterruptibleTestActivity {

    @Override
    public void activity1() throws InterruptedException {
      for (int i = 0; i < 10; i++) {
        Activity.heartbeat(i);
      }
      Thread.sleep(1000);
      for (int i = 10; i < 20; i++) {
        Activity.heartbeat(i);
      }
    }
  }

  @Test
  public void testHeartbeatThrottling() throws InterruptedException {
    testEnvironment.registerActivitiesImplementations(new BurstHeartbeatActivityImpl());
    ConcurrentSet<Integer> details = new ConcurrentSet<>();
    testEnvironment.setActivityHeartbeatListener(Integer.class, details::add);
    InterruptibleTestActivity activity =
        testEnvironment.newActivityStub(InterruptibleTestActivity.class);
    activity.activity1();
    assertEquals(2, details.size());
  }

  private static class BurstHeartbeatActivity2Impl implements InterruptibleTestActivity {

    @Override
    public void activity1() throws InterruptedException {
      for (int i = 0; i < 10; i++) {
        Activity.heartbeat(null);
      }
      Thread.sleep(1200);
    }
  }

  // This test covers the logic where another heartbeat request is sent by the background thread,
  // after wait period expires.
  @Test
  public void testHeartbeatThrottling2() throws InterruptedException {
    testEnvironment.registerActivitiesImplementations(new BurstHeartbeatActivity2Impl());
    AtomicInteger count = new AtomicInteger();
    testEnvironment.setActivityHeartbeatListener(Void.class, i -> count.incrementAndGet());
    InterruptibleTestActivity activity =
        testEnvironment.newActivityStub(InterruptibleTestActivity.class);
    activity.activity1();
    assertEquals(2, count.get());
  }

  private static class HeartbeatCancellationActivityImpl implements InterruptibleTestActivity {

    @Override
    public void activity1() throws InterruptedException {
      try {
        Activity.heartbeat(null);
        fail("unreachable");
      } catch (ActivityCancelledException e) {
        System.out.println("activity cancelled");
      }
    }
  }

  @Test
  public void testHeartbeatCancellation() throws InterruptedException, TException {
    testEnvironment.registerActivitiesImplementations(new HeartbeatCancellationActivityImpl());
    IWorkflowService workflowService = mock(IWorkflowService.class);
    RecordActivityTaskHeartbeatResponse resp = new RecordActivityTaskHeartbeatResponse();
    resp.setCancelRequested(true);
    when(workflowService.RecordActivityTaskHeartbeat(any())).thenReturn(resp);
    testEnvironment.setWorkflowService(workflowService);
    InterruptibleTestActivity activity =
        testEnvironment.newActivityStub(InterruptibleTestActivity.class);
    activity.activity1();
  }

  private static class CancellationOnNextHeartbeatActivityImpl
      implements InterruptibleTestActivity {

    @Override
    public void activity1() throws InterruptedException {
      Activity.heartbeat(null);
      Thread.sleep(100);
      Activity.heartbeat(null);
      Thread.sleep(1000);
      try {
        Activity.heartbeat(null);
        fail("unreachable");
      } catch (ActivityCancelledException e) {
        System.out.println("activity cancelled");
      }
    }
  }

  @Test
  public void testCancellationOnNextHeartbeat() throws InterruptedException, TException {
    testEnvironment.registerActivitiesImplementations(
        new CancellationOnNextHeartbeatActivityImpl());
    IWorkflowService workflowService = mock(IWorkflowService.class);
    RecordActivityTaskHeartbeatResponse resp = new RecordActivityTaskHeartbeatResponse();
    resp.setCancelRequested(true);
    when(workflowService.RecordActivityTaskHeartbeat(any()))
        .thenReturn(new RecordActivityTaskHeartbeatResponse())
        .thenReturn(resp);
    testEnvironment.setWorkflowService(workflowService);
    InterruptibleTestActivity activity =
        testEnvironment.newActivityStub(InterruptibleTestActivity.class);
    activity.activity1();
  }

  private static class SimpleHeartbeatActivityImpl implements InterruptibleTestActivity {

    @Override
    public void activity1() throws InterruptedException {
      Activity.heartbeat(null);
      // Make sure that the activity lasts longer than the retry period.
      Thread.sleep(3000);
    }
  }

  @Test
  public void testHeartbeatIntermittentError() throws TException, InterruptedException {
    testEnvironment.registerActivitiesImplementations(new SimpleHeartbeatActivityImpl());
    IWorkflowService workflowService = mock(IWorkflowService.class);
    when(workflowService.RecordActivityTaskHeartbeat(any()))
        .thenThrow(new TException("intermittent error"))
        .thenThrow(new TException("intermittent error"))
        .thenReturn(new RecordActivityTaskHeartbeatResponse());
    testEnvironment.setWorkflowService(workflowService);
    AtomicInteger count = new AtomicInteger();
    testEnvironment.setActivityHeartbeatListener(Void.class, i -> count.incrementAndGet());
    InterruptibleTestActivity activity =
        testEnvironment.newActivityStub(InterruptibleTestActivity.class);
    activity.activity1();
    assertEquals(3, count.get());
  }

  public interface A {
    void a();
  }

  @ActivityInterface
  public interface B extends A {
    void b();
  }

  @ActivityInterface
  public interface C extends B, A {
    void c();
  }

  public class CImpl implements C {
    private List<String> invocations = new ArrayList<>();

    @Override
    public void a() {
      invocations.add("a");
    }

    @Override
    public void b() {
      invocations.add("b");
    }

    @Override
    public void c() {
      invocations.add("c");
    }
  }

  @Test
  public void testInvokingActivityByBaseInterface() {
    CImpl impl = new CImpl();
    testEnvironment.registerActivitiesImplementations(impl);
    try {
      testEnvironment.newActivityStub(A.class);
      fail("A doesn't implement activity");
    } catch (IllegalArgumentException e) {
      // expected as A doesn't implement any activity
    }
    B b = testEnvironment.newActivityStub(B.class);
    b.a();
    b.b();
    C c = testEnvironment.newActivityStub(C.class);
    c.a();
    c.b();
    c.c();
    List<String> expected = new ArrayList<>();
    expected.add("a");
    expected.add("b");
    expected.add("a");
    expected.add("b");
    expected.add("c");
    assertEquals(expected, impl.invocations);
  }
}
