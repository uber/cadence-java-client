package com.uber.cadence.workflow;

import com.uber.cadence.ActivityType;
import com.uber.cadence.internal.ActivityException;

/**
 * Indicates that an activity implementation threw an unhandled exception.
 * Contains the unhandled exception as a cause. Note that an unhandled exception stack trace
 * might belong to a separate process or even program.
 */
public class ActivityFailureException extends ActivityException {

    public ActivityFailureException(String message, long eventId, ActivityType activityType, String activityId, Throwable cause) {
        super(message, eventId, activityType, activityId);
        initCause(cause);
    }
}
