package com.uber.cadence.internal.replay;

import com.uber.cadence.Decision;
import com.uber.cadence.HistoryEvent;

public class MarkerStateMachine implements DecisionStateMachine {

  private Decision decision;
  private final DecisionId id;
  boolean done;

  public MarkerStateMachine(DecisionId id, Decision decision) {
    this.id = id;
    this.decision = decision;
  }

  @Override
  public DecisionId getId() {
    return id;
  }

  @Override
  public com.uber.cadence.Decision getDecision() {
    return decision;
  }

  @Override
  public void cancel(Runnable immediateCancellationCallback) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void handleStartedEvent(HistoryEvent event) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void handleCancellationInitiatedEvent() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void handleCancellationEvent() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void handleCancellationFailureEvent(HistoryEvent event) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void handleCompletionEvent() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void handleInitiationFailedEvent(HistoryEvent event) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void handleInitiatedEvent(HistoryEvent event) {
    done = true;
  }

  @Override
  public void handleDecisionTaskStartedEvent() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public DecisionState getState() {
    return done ? DecisionState.COMPLETED : DecisionState.CREATED;
  }

  @Override
  public boolean isDone() {
    return done;
  }
}
