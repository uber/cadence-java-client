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

package com.uber.cadence.internal.replay;

import com.google.common.collect.PeekingIterator;
import com.uber.cadence.EventType;
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.PollForDecisionTaskResponse;
import com.uber.cadence.WorkflowExecutionStartedEventAttributes;
import com.uber.cadence.internal.common.WorkflowExecutionUtils;
import com.uber.cadence.internal.worker.DecisionTaskWithHistoryIterator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

class HistoryHelper {

  static final class DecisionEvents {

    private final List<HistoryEvent> events;
    private final List<HistoryEvent> decisionEvents;
    private final boolean replay;
    private final long replayCurrentTimeMilliseconds;
    private final long nextDecisionEventId;

    DecisionEvents(
        List<HistoryEvent> events,
        List<HistoryEvent> decisionEvents,
        boolean replay,
        long replayCurrentTimeMilliseconds,
        long nextDecisionEventId) {
      if (replayCurrentTimeMilliseconds <= 0) {
        throw new Error("replayCurrentTimeMilliseconds is not set");
      }
      if (nextDecisionEventId <= 0) {
        throw new Error("nextDecisionEventId is not set");
      }
      this.events = events;
      this.decisionEvents = decisionEvents;
      this.replay = replay;
      this.replayCurrentTimeMilliseconds = replayCurrentTimeMilliseconds;
      this.nextDecisionEventId = nextDecisionEventId;
    }

    public List<HistoryEvent> getEvents() {
      return events;
    }

    public List<HistoryEvent> getDecisionEvents() {
      return decisionEvents;
    }

    public boolean isReplay() {
      return replay;
    }

    public long getReplayCurrentTimeMilliseconds() {
      return replayCurrentTimeMilliseconds;
    }

    public long getNextDecisionEventId() {
      return nextDecisionEventId;
    }
  }

  private static final class EventsIterator implements PeekingIterator<HistoryEvent> {

    private Iterator<HistoryEvent> events;
    private HistoryEvent next;

    EventsIterator(Iterator<HistoryEvent> events) {
      this.events = events;
      if (events.hasNext()) {
        next = events.next();
      }
    }

    @Override
    public HistoryEvent peek() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return next;
    }

    @Override
    public boolean hasNext() {
      return next != null;
    }

    @Override
    public HistoryEvent next() {
      HistoryEvent result = next;
      if (events.hasNext()) {
        next = events.next();
      } else {
        next = null;
      }
      return result;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("not implemented");
    }
  }

  static class DecisionEventsIterator implements Iterator<DecisionEvents> {

    private EventsIterator events;

    private Queue<HistoryEvent> bufferedEvents = new ArrayDeque<>();

    DecisionEventsIterator(DecisionTaskWithHistoryIterator decisionTaskWithHistoryIterator) {
      this.events = new EventsIterator(decisionTaskWithHistoryIterator.getHistory());
    }

    @Override
    public boolean hasNext() {
      return !bufferedEvents.isEmpty() || events.hasNext();
    }

    @Override
    public DecisionEvents next() {
      List<HistoryEvent> decisionEvents = new ArrayList<>();
      List<HistoryEvent> newEvents = new ArrayList<>();
      boolean replay = true;
      long replayCurrentTimeMilliseconds = -1;
      long nextDecisionEventId = -1;
      while (events.hasNext()) {
        HistoryEvent event = events.next();
        replayCurrentTimeMilliseconds = TimeUnit.MICROSECONDS.toMillis(event.getTimestamp());
        EventType eventType = event.getEventType();
        if (eventType == EventType.DecisionTaskStarted) {
          if (!events.hasNext()) {
            replay = false;
            nextDecisionEventId = event.getEventId() + 2; // +1 for next, +1 for DecisionCompleted
            break;
          }
          HistoryEvent peeked = events.peek();
          EventType peekedType = peeked.getEventType();
          if (peekedType == EventType.DecisionTaskTimedOut
              || peekedType == EventType.DecisionTaskFailed) {
            continue;
          } else if (peekedType == EventType.DecisionTaskCompleted) {
            events.next(); // consume DecisionTaskCompleted
            nextDecisionEventId = peeked.getEventId() + 1; // +1 for next
            break;
          } else {
            throw new Error("Unexpected event after DecisionTaskStarted: " + peeked);
          }
        }
        newEvents.add(event);
      }
      while (events.hasNext()) {
        HistoryEvent event = events.next();
        if (!WorkflowExecutionUtils.isDecisionEvent(event)) {
          break;
        }
        decisionEvents.add(event);
      }
      return new DecisionEvents(
          newEvents, decisionEvents, replay, replayCurrentTimeMilliseconds, nextDecisionEventId);
    }
  }

  private final DecisionTaskWithHistoryIterator decisionTaskWithHistoryIterator;
  private final DecisionEventsIterator iterator;

  HistoryHelper(DecisionTaskWithHistoryIterator decisionTasks) {
    this.decisionTaskWithHistoryIterator = decisionTasks;
    this.iterator = new DecisionEventsIterator(decisionTasks);
  }

  public DecisionEventsIterator getIterator() {
    return iterator;
  }

  public PollForDecisionTaskResponse getDecisionTask() {
    return decisionTaskWithHistoryIterator.getDecisionTask();
  }

  public WorkflowExecutionStartedEventAttributes getWorkflowExecutionStartedEventAttributes() {
    return decisionTaskWithHistoryIterator.getStartedEvent();
  }

  @Override
  public String toString() {
    return WorkflowExecutionUtils.prettyPrintHistory(
        decisionTaskWithHistoryIterator.getDecisionTask().getHistory().getEvents().iterator(),
        true);
  }

  long getPreviousStartedEventId() {
    return getDecisionTask().getPreviousStartedEventId();
  }
}
