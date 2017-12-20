package com.uber.cadence.internal.dispatcher;

public interface WorkflowClock {

    long currentTimeMillis();

}
