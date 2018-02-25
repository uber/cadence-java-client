package com.uber.cadence.client;

import com.uber.cadence.activity.ActivityTask;

/**
 * Usually indicates that activity was already completed (duplicated request to complete) or timed out
 * or workflow is closed.
 */
public final class ActivityCancelledException extends ActivityCompletionException {

    public ActivityCancelledException(ActivityTask task) {
        super(task);
    }

    public ActivityCancelledException() {
        super();
    }
}
