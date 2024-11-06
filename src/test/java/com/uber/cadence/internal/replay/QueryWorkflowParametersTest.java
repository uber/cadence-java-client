/**
 * Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * <p>Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. A copy of the License is located at
 *
 * <p>http://aws.amazon.com/apache2.0
 *
 * <p>or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.uber.cadence.internal.replay;

import com.uber.cadence.QueryConsistencyLevel;
import com.uber.cadence.QueryRejectCondition;
import junit.framework.TestCase;

public class QueryWorkflowParametersTest extends TestCase {
  QueryWorkflowParameters p;

  public void setUp() throws Exception {
    p = new QueryWorkflowParameters();
    p.setInput("input".getBytes());
    p.setWorkflowId("workflowid");
    p.setQueryType("querytype");
    p.setRunId("runid");
    p.setQueryConsistencyLevel(QueryConsistencyLevel.EVENTUAL);
    p.setQueryRejectCondition(QueryRejectCondition.NOT_OPEN);
  }

  public void testCopy() {
    QueryWorkflowParameters copied = p.copy();
    assertEquals(copied.toString(), p.toString());
  }
}
