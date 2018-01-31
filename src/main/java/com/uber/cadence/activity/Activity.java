package com.uber.cadence.activity;

import com.uber.cadence.internal.activity.ActivityInternal;

public final class Activity {

    public static ActivityExecutionContext getContext() {
        return ActivityInternal.getContext();
    }

    /**
     * Prohibit instantiation
     */
    private Activity() {

    }
}
