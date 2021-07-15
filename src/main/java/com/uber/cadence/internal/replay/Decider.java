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

import com.uber.cadence.Decision;
import com.uber.cadence.PollForDecisionTaskResponse;
import com.uber.cadence.WorkflowQuery;
import com.uber.cadence.WorkflowQueryResult;
import java.util.List;
import java.util.Map;

public interface Decider {

  DecisionResult decide(PollForDecisionTaskResponse decisionTask) throws Throwable;

  byte[] query(PollForDecisionTaskResponse decisionTask, WorkflowQuery query) throws Throwable;

  void close();

  class DecisionResult {
    private final List<Decision> decisions;
    private final Map<String, WorkflowQueryResult> queryResults;
    private final boolean forceCreateNewDecisionTask;

    public DecisionResult(
        List<Decision> decisions,
        Map<String, WorkflowQueryResult> queryResults,
        boolean forceCreateNewDecisionTask) {
      this.decisions = decisions;
      this.queryResults = queryResults;
      this.forceCreateNewDecisionTask = forceCreateNewDecisionTask;
    }

    public List<Decision> getDecisions() {
      return decisions;
    }

    public Map<String, WorkflowQueryResult> getQueryResults() {
      return queryResults;
    }

    public boolean getForceCreateNewDecisionTask() {
      return forceCreateNewDecisionTask;
    }
  }
}
