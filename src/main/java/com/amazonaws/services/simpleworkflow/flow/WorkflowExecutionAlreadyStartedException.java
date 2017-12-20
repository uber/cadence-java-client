package com.amazonaws.services.simpleworkflow.flow;

public class WorkflowExecutionAlreadyStartedException extends Exception {
    public WorkflowExecutionAlreadyStartedException(String message, Throwable cause) {
        super(message, cause);
    }
}
