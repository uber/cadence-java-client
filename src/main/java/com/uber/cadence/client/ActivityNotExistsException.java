package com.uber.cadence.client;

import com.uber.cadence.activity.ActivityTask;

/**
 * Usually indicates that activity was already completed (duplicated request to complete) or timed out
 * or workflow is closed.
 */
public final class ActivityNotExistsException extends ActivityCompletionException {

    public ActivityNotExistsException(Throwable cause) {
        super(cause);
    }

    public ActivityNotExistsException(ActivityTask task, Throwable cause) {
        super(task, cause);
    }

    public ActivityNotExistsException(String activityId, Throwable cause) {
        super(activityId, cause);
    }

}
