package com.uber.cadence.client;

import com.uber.cadence.WorkflowExecution;

public final class WorkflowQueryException extends WorkflowException {

  public WorkflowQueryException(WorkflowExecution execution, String message) {
    super(message, execution, null, null);
  }
}
