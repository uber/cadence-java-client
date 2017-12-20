package com.amazonaws.services.simpleworkflow.flow.worker;

import java.util.function.Consumer;

public class OpenChildWorkflowRequestInfo extends OpenRequestInfo<byte[], String> {

    private final Consumer<String> runIdCallback;

    public OpenChildWorkflowRequestInfo(Consumer<String> runIdCallback) {
        this.runIdCallback = runIdCallback;
    }

    public Consumer<String> getRunIdCallback() {
        return runIdCallback;
    }
}