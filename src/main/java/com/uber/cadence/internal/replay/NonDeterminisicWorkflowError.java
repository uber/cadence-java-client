package com.uber.cadence.internal.replay;

final class NonDeterminisicWorkflowError extends Error {

  NonDeterminisicWorkflowError(String message) {
    super(message);
  }
}
