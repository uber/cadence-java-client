/*
 *  Modifications Copyright (c) 2017-2021 Uber Technologies Inc.
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.uber.cadence.internal.shadowing;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import com.uber.cadence.worker.TimeFilter;
import com.uber.cadence.worker.WorkflowStatus;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.Test;

public class QueryBuilderTest {
  @Test
  public void testQueryBuilder_SetWorkflowTypes() {
    List<String> types = Lists.newArrayList("test1", "test2");
    String query = QueryBuilder.newQueryBuilder().setWorkflowTypes(types).build();
    assertEquals("(WorkflowType = test1 or WorkflowType = test2)", query);
  }

  @Test
  public void testQueryBuilder_SetWorkflowStatuses() {
    List<WorkflowStatus> statuses =
        Lists.newArrayList(WorkflowStatus.OPEN, WorkflowStatus.CLOSED, WorkflowStatus.COMPLETED);
    String query = QueryBuilder.newQueryBuilder().setWorkflowStatuses(statuses).build();
    assertEquals(
        "(CloseTime = missing or CloseTime != missing or CloseStatus = "
            + '"'
            + "COMPLETED"
            + '"'
            + ")",
        query);
  }

  @Test
  public void testQueryBuilder_SetWorkflowStartTime() {
    ZonedDateTime now = ZonedDateTime.now();
    TimeFilter timeFilter =
        TimeFilter.newBuilder().setMinTimestamp(now).setMaxTimestamp(now.plusHours(1)).build();
    String query = QueryBuilder.newQueryBuilder().setWorkflowStartTime(timeFilter).build();
    assertEquals(
        "(StartTime >= "
            + QueryBuilder.toNanoSeconds(now)
            + " and StartTime <= "
            + QueryBuilder.toNanoSeconds(now.plusHours(1))
            + ")",
        query);
  }

  @Test
  public void testQueryBuilder() {
    ZonedDateTime now = ZonedDateTime.now();
    TimeFilter timeFilter =
        TimeFilter.newBuilder().setMinTimestamp(now).setMaxTimestamp(now.plusHours(1)).build();
    List<String> types = Lists.newArrayList("test1", "test2");
    String query =
        QueryBuilder.newQueryBuilder()
            .setWorkflowStartTime(timeFilter)
            .setWorkflowTypes(types)
            .build();
    assertEquals(
        "(StartTime >= "
            + QueryBuilder.toNanoSeconds(now)
            + " and StartTime <= "
            + QueryBuilder.toNanoSeconds(now.plusHours(1))
            + ")"
            + " and "
            + "(WorkflowType = test1 or WorkflowType = test2)",
        query);
  }
}
