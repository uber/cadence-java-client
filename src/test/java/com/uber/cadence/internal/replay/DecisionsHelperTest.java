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

import com.google.common.collect.ImmutableList;
import com.uber.cadence.Decision;
import com.uber.cadence.DecisionType;
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.ScheduleActivityTaskDecisionAttributes;
import java.util.Collections;
import java.util.List;
import junit.framework.TestCase;
import org.junit.Test;

public class DecisionsHelperTest extends TestCase {
  @Test
  public void testGetDecisionsSimple() {
    // Start the decision
    DecisionsHelper decisionsHelper = new DecisionsHelper(null, null);

    HistoryHelper.DecisionEvents decisionEvents =
        new HistoryHelper.DecisionEvents(
            Collections.emptyList(), ImmutableList.of(new HistoryEvent()), false, 123, 456);

    decisionsHelper.handleDecisionTaskStartedEvent(decisionEvents);

    // Schedule 2 activity tasks
    ScheduleActivityTaskDecisionAttributes scheduleActivityTaskDecisionAttributes =
        new ScheduleActivityTaskDecisionAttributes();
    scheduleActivityTaskDecisionAttributes.setActivityId("testActivityId");

    decisionsHelper.scheduleActivityTask(scheduleActivityTaskDecisionAttributes);
    decisionsHelper.scheduleActivityTask(scheduleActivityTaskDecisionAttributes);

    // Act
    List<Decision> decisions = decisionsHelper.getDecisions();

    // Assert
    assertEquals(2, decisions.size());
  }

  private static final int MAXIMUM_DECISIONS_PER_COMPLETION = 10000;

  @Test
  public void testGetDecisionsManyDecisions() {
    // Start the decision
    DecisionsHelper decisionsHelper = new DecisionsHelper(null, null);

    HistoryHelper.DecisionEvents decisionEvents =
        new HistoryHelper.DecisionEvents(
            Collections.emptyList(), ImmutableList.of(new HistoryEvent()), false, 123, 456);

    decisionsHelper.handleDecisionTaskStartedEvent(decisionEvents);

    // Schedule more than MAXIMUM_DECISIONS_PER_COMPLETION activity tasks
    ScheduleActivityTaskDecisionAttributes scheduleActivityTaskDecisionAttributes =
        new ScheduleActivityTaskDecisionAttributes();
    scheduleActivityTaskDecisionAttributes.setActivityId("testActivityId");

    for (int i = 0; i < MAXIMUM_DECISIONS_PER_COMPLETION * 2; i++) {
      decisionsHelper.scheduleActivityTask(scheduleActivityTaskDecisionAttributes);
    }

    // Act
    List<Decision> decisions = decisionsHelper.getDecisions();

    // Assert
    assertEquals(MAXIMUM_DECISIONS_PER_COMPLETION, decisions.size());

    // Check that the last decision is a FORCE_IMMEDIATE_DECISION_TIMER
    Decision lastDecision = decisions.get(decisions.size() - 1);
    assertEquals(DecisionType.StartTimer, lastDecision.getDecisionType());
    String timerId = lastDecision.getStartTimerDecisionAttributes().getTimerId();
    assertEquals(timerId, DecisionsHelper.FORCE_IMMEDIATE_DECISION_TIMER);
  }

  @Test
  public void testNotifyDecisionSent() {
    DecisionsHelper decisionsHelper = new DecisionsHelper(null, null);

    // Start the decision
    HistoryHelper.DecisionEvents decisionEvents =
        new HistoryHelper.DecisionEvents(
            Collections.emptyList(), ImmutableList.of(new HistoryEvent()), false, 123, 456);

    decisionsHelper.handleDecisionTaskStartedEvent(decisionEvents);

    // Schedule activity task
    ScheduleActivityTaskDecisionAttributes scheduleActivityTaskDecisionAttributes =
        new ScheduleActivityTaskDecisionAttributes();
    scheduleActivityTaskDecisionAttributes.setActivityId("testActivityId");

    decisionsHelper.scheduleActivityTask(scheduleActivityTaskDecisionAttributes);

    // Act
    decisionsHelper.notifyDecisionSent();

    // The decision is sent so it's not returned anymore
    assertEquals(0, decisionsHelper.getDecisions().size());
  }
}
