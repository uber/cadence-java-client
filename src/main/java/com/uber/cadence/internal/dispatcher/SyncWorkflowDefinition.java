package com.uber.cadence.internal.dispatcher;

public interface SyncWorkflowDefinition {
    byte[] execute(byte[] input);
}
