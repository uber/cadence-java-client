/*
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.uber.cadence.internal.testservice;

import com.uber.cadence.EntityNotExistsError;
import com.uber.cadence.InternalServiceError;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class StateMachine<Data> {

  enum State {
    NONE,
    SCHEDULED,
    INITIATED,
    STARTED,
    FAILED,
    TIMED_OUT,
    CANCELED,
    COMPLETED
  }

  @FunctionalInterface
  interface Callback<D, R> {

    void apply(RequestContext ctx, D data, R request) throws InternalServiceError;
  }

  private static class Transition {

    final State from;
    final State to;

    public Transition(State from, State to) {
      this.from = Objects.requireNonNull(from);
      this.to = Objects.requireNonNull(to);
    }

    public State getFrom() {
      return from;
    }

    public State getTo() {
      return to;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Transition that = (Transition) o;

      if (from != that.from) {
        return false;
      }
      return to == that.to;
    }

    @Override
    public int hashCode() {
      int result = from.hashCode();
      result = 31 * result + to.hashCode();
      return result;
    }

    @Override
    public String toString() {
      return "Transition{" + from + "->" + to + '}';
    }
  }

  private final List<State> transitionHistory = new ArrayList<>();
  private final Map<Transition, Callback<Data, ?>> allowedTransitions = new HashMap<>();

  private State state = State.NONE;

  private final Data data;

  StateMachine(Data data) {
    this.data = data;
  }

  public State getState() {
    return state;
  }

  <V> void addTransition(State from, State to, Callback<Data, V> callback) {
    allowedTransitions.put(new Transition(from, to), callback);
  }

  public <V> void schedule(RequestContext ctx, V request) throws InternalServiceError {
    applyEvent(State.SCHEDULED, ctx, request);
  }

  public <V> void start(RequestContext ctx, V request) throws InternalServiceError {
    applyEvent(State.STARTED, ctx, request);
  }

  public <V> void fail(RequestContext ctx, V request) throws InternalServiceError {
    applyEvent(State.FAILED, ctx, request);
  }

  public <V> void complete(RequestContext ctx, V request) throws InternalServiceError {
    applyEvent(State.COMPLETED, ctx, request);
  }

  public <V> void timeout(RequestContext ctx, V timeoutType) throws InternalServiceError {
    applyEvent(State.TIMED_OUT, ctx, timeoutType);
  }

  public <V> void update(V request) throws EntityNotExistsError, InternalServiceError {
    Transition transition = new Transition(State.STARTED, State.STARTED);
    @SuppressWarnings("unchecked")
    Callback<Data, V> callback = (Callback<Data, V>) allowedTransitions.get(transition);
    if (callback == null) {
      throw new EntityNotExistsError("Not in running state: " + state);
    }
    callback.apply(null, data, request);
  }

  private <V> void applyEvent(State toState, RequestContext context, V request)
      throws InternalServiceError {
    Transition transition = new Transition(state, toState);
    @SuppressWarnings("unchecked")
    Callback<Data, V> callback = (Callback<Data, V>) allowedTransitions.get(transition);
    if (callback == null) {
      throw new InternalServiceError(
          "Invalid transition " + transition + ", history: " + transitionHistory);
    }
    callback.apply(context, data, request);
    transitionHistory.add(transition.getTo());
    state = transition.getTo();
  }
}
