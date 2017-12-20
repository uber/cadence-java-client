package com.uber.cadence.internal.dispatcher;

/**
 * Used to interrupt deterministic thread execution. Assumption is that none of the code
 * that thread executes catches it.
 */
public class DestroyWorkflowThreadError extends Error {
}
