package com.uber.cadence.activity;

import com.uber.cadence.common.RetryOptions;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;

public class ActivityOptionsTest {

    @ActivityMethod
    public void defaultActivityOptions() {
    }

    @Test
    public void testOnlyOptionsPresent() throws NoSuchMethodException {
        ActivityOptions o = new ActivityOptions.Builder()
                .setTaskList("foo")
                .setHeartbeatTimeoutSeconds(123)
                .setScheduleToCloseTimeoutSeconds(321)
                .setScheduleToStartTimeoutSeconds(333)
                .setStartToCloseTimeoutSeconds(345)
                .setRetryOptions(new RetryOptions.Builder()
                        .setDoNotRetry(IllegalArgumentException.class)
                        .setMaximumAttempts(11111)
                        .setBackoffCoefficient(1.55)
                        .setMaximumInterval(Duration.ofDays(3))
                        .setExpiration(Duration.ofDays(365))
                        .setInitialInterval(Duration.ofMinutes(12))
                        .setMinimumAttempts(2)
                        .build())
                .build();
        ActivityMethod a = ActivityOptionsTest.class
                .getMethod("defaultActivityOptions")
                .getAnnotation(ActivityMethod.class);
        Assert.assertEquals(o, ActivityOptions.merge(a, null, o));
    }

    @MethodRetry(initialIntervalSeconds = 3)
    @ActivityMethod
    public void defaultActivityAndRetryOptions() {
    }

    @Test
    public void testOnlyOptionsAndEmptyAnnotationsPresent() throws NoSuchMethodException {
        ActivityOptions o = new ActivityOptions.Builder()
                .setTaskList("foo")
                .setHeartbeatTimeoutSeconds(123)
                .setScheduleToCloseTimeoutSeconds(321)
                .setScheduleToStartTimeoutSeconds(333)
                .setStartToCloseTimeoutSeconds(345)
                .setRetryOptions(new RetryOptions.Builder()
                        .setDoNotRetry(IllegalArgumentException.class)
                        .setMaximumAttempts(11111)
                        .setBackoffCoefficient(1.55)
                        .setMaximumInterval(Duration.ofDays(3))
                        .setExpiration(Duration.ofDays(365))
                        .setInitialInterval(Duration.ofMinutes(12))
                        .setMinimumAttempts(2)
                        .build())
                .build();
        ActivityMethod a = ActivityOptionsTest.class
                .getMethod("defaultActivityAndRetryOptions")
                .getAnnotation(ActivityMethod.class);
        Assert.assertEquals(o, ActivityOptions.merge(a, null, o));
    }

    @MethodRetry(initialIntervalSeconds = 12, backoffCoefficient = 1.97, expirationSeconds = 1231423, maximumAttempts = 234567,
            maximumIntervalSeconds = 22, doNotRetry = {NullPointerException.class, UnsupportedOperationException.class})
    @ActivityMethod(startToCloseTimeoutSeconds = 1135, taskList = "bar", heartbeatTimeoutSeconds = 4567,
            scheduleToCloseTimeoutSeconds = 2342, scheduleToStartTimeoutSeconds = 9879)
    public void activityAndRetryOptions() {
    }

    @Test
    public void testOnlyAnnotationsPresent() throws NoSuchMethodException {
        Method method = ActivityOptionsTest.class
                .getMethod("activityAndRetryOptions");
        ActivityMethod a = method
                .getAnnotation(ActivityMethod.class);
        MethodRetry r = method.getAnnotation(MethodRetry.class);
        ActivityOptions o = new ActivityOptions.Builder()
                .build();
        ActivityOptions merged = ActivityOptions.merge(a, r, o);
        Assert.assertEquals(a.taskList(), merged.getTaskList());
        Assert.assertEquals(a.heartbeatTimeoutSeconds(), merged.getHeartbeatTimeoutSeconds());
        Assert.assertEquals(a.scheduleToCloseTimeoutSeconds(), merged.getScheduleToCloseTimeoutSeconds());
        Assert.assertEquals(a.scheduleToStartTimeoutSeconds(), merged.getScheduleToStartTimeoutSeconds());
        Assert.assertEquals(a.startToCloseTimeoutSeconds(), merged.getStartToCloseTimeoutSeconds());

        RetryOptions rMerged = merged.getRetryOptions();
        Assert.assertEquals(r.maximumAttempts(), rMerged.getMaximumAttempts());
        Assert.assertEquals(r.minimumAttempts(), rMerged.getMinimumAttempts());
        Assert.assertEquals(r.backoffCoefficient(), rMerged.getBackoffCoefficient(), 0.0);
        Assert.assertEquals(Duration.ofSeconds(r.expirationSeconds()), rMerged.getExpiration());
        Assert.assertEquals(Duration.ofSeconds(r.initialIntervalSeconds()), rMerged.getInitialInterval());
        Assert.assertEquals(Duration.ofSeconds(r.maximumIntervalSeconds()), rMerged.getMaximumInterval());
        Assert.assertEquals(Arrays.asList(r.doNotRetry()), rMerged.getDoNotRetry());
    }

    @Test
    public void testBothPresent() throws NoSuchMethodException {
        ActivityOptions o = new ActivityOptions.Builder()
                .setTaskList("foo")
                .setHeartbeatTimeoutSeconds(123)
                .setScheduleToCloseTimeoutSeconds(321)
                .setScheduleToStartTimeoutSeconds(333)
                .setStartToCloseTimeoutSeconds(345)
                .setRetryOptions(new RetryOptions.Builder()
                        .setDoNotRetry(IllegalArgumentException.class)
                        .setMaximumAttempts(11111)
                        .setBackoffCoefficient(1.55)
                        .setMaximumInterval(Duration.ofDays(3))
                        .setExpiration(Duration.ofDays(365))
                        .setInitialInterval(Duration.ofMinutes(12))
                        .setMinimumAttempts(2)
                        .build())
                .build();
        ActivityMethod a = ActivityOptionsTest.class
                .getMethod("activityAndRetryOptions")
                .getAnnotation(ActivityMethod.class);
        Assert.assertEquals(o, ActivityOptions.merge(a, null, o));
    }
}
