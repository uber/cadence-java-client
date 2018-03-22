package com.uber.cadence.client;

import com.uber.cadence.WorkflowExecution;

public final class WorkflowServiceException extends WorkflowException {

  public WorkflowServiceException(WorkflowExecution execution, String message) {
    super(message, execution, null, null);
  }
}
