package com.uber.cadence.internal.activity;

import com.uber.cadence.activity.ActivityExecutionContext;

public final class ActivityInternal {

    private ActivityInternal() {
    }

    public static ActivityExecutionContext getContext() {
        return CurrentActivityExecutionContext.get();
    }
}
