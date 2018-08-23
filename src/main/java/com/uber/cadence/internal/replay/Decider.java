package com.uber.cadence.internal.replay;

import com.uber.cadence.WorkflowQuery;
import com.uber.cadence.internal.worker.DecisionTaskWithHistoryIterator;

public interface Decider {

  void decide(DecisionTaskWithHistoryIterator iterator) throws Throwable;

  byte[] query(DecisionTaskWithHistoryIterator decisionTaskIterator, WorkflowQuery query)
      throws Throwable;

  void close();
}
