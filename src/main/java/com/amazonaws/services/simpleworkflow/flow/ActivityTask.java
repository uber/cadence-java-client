package com.amazonaws.services.simpleworkflow.flow;

import com.uber.cadence.ActivityType;
import com.uber.cadence.PollForActivityTaskResponse;
import com.uber.cadence.WorkflowExecution;

public final class ActivityTask {
    private final PollForActivityTaskResponse response;

    public ActivityTask(PollForActivityTaskResponse response) {
        this.response = response;
    }

    public byte[] getTaskToken() {
        return response.getTaskToken();
    }

    public WorkflowExecution getWorkflowExecution() {
        return response.getWorkflowExecution();
    }

    public String getActivityId() {
        return response.getActivityId();
    }

    public ActivityType getActivityType() {
        return response.getActivityType();
    }

    public long getScheduledTimestamp() {
        return response.getScheduledTimestamp();
    }

    public int getScheduleToCloseTimeoutSeconds() {
        return response.getScheduleToCloseTimeoutSeconds();
    }

    public void setScheduleToCloseTimeoutSecondsIsSet(boolean value) {
        response.setScheduleToCloseTimeoutSecondsIsSet(value);
    }

    public int getStartToCloseTimeoutSeconds() {
        return response.getStartToCloseTimeoutSeconds();
    }

    public int getHeartbeatTimeoutSeconds() {
        return response.getHeartbeatTimeoutSeconds();
    }

    public byte[] getInput() {
        return response.getInput();
    }
}
