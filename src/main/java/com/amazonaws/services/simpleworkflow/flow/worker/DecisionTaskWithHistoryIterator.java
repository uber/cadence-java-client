package com.amazonaws.services.simpleworkflow.flow.worker;

import com.uber.cadence.HistoryEvent;
import com.uber.cadence.PollForDecisionTaskResponse;

import java.util.Iterator;

/**
 * Contains DecisionTask and history iterator that paginates history behind the scene.
 */
public interface DecisionTaskWithHistoryIterator {
    PollForDecisionTaskResponse getDecisionTask();
    Iterator<HistoryEvent> getHistory();
}
