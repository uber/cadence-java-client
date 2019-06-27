package com.uber.cadence.workflow;

import java.time.Duration;

public class MyWorkflowImpl implements MyWorkflow {

    private Signal1Struct signal1;
    private Signal2Struct signal2;

    @Override
    public void main() {
        long start = Workflow.currentTimeMillis(); // must use workflow clock
        Duration timeout = Duration.ofMinutes(30);
        Workflow.await(timeout, () -> signal1 != null || signal2 != null);
        long duration = Workflow.currentTimeMillis() - start;
        if (timeout.toMillis() <= duration || (signal1 == null && signal2 == null)) {
            // handle timeout
        }
        // process signals
    }

    @Override
    public void signal1(Signal1Struct signal) {
        signal1 = signal;
    }

    @Override
    public void signal2(Signal2Struct signal) {
        signal2 = signal;
    }
}
