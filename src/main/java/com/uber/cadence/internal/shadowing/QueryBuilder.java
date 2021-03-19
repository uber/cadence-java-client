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

import com.google.common.collect.Lists;
import com.uber.cadence.worker.ShadowingOptions;
import com.uber.cadence.worker.TimeFilter;
import com.uber.cadence.worker.WorkflowStatus;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.stream.Collectors;

public class QueryBuilder {
  public static QueryBuilder newQueryBuilder() {
    return new QueryBuilder();
  }

  public static QueryBuilder newQueryBuilder(ShadowingOptions options) {
    return new QueryBuilder()
        .setWorkflowTypes(options.getWorkflowTypes())
        .setWorkflowStartTime(options.getWorkflowStartTimeFilter())
        .setWorkflowStatuses(options.getWorkflowStatuses());
  }

  public QueryBuilder setWorkflowTypes(Collection<String> workflowTypes) {
    if (workflowTypes == null || workflowTypes.isEmpty()) {
      return this;
    }

    Collection<String> types =
        workflowTypes
            .stream()
            .map((wfType) -> WORKFLOW_TYPE_PLACEHOLDER + wfType)
            .collect(Collectors.toList());

    String query = String.join(OR_QUERY, types);
    this.appendPartialQuery(query);
    return this;
  }

  public QueryBuilder setWorkflowStatuses(Collection<WorkflowStatus> workflowStatuses) {
    if (workflowStatuses == null || workflowStatuses.isEmpty()) {
      return this;
    }

    Collection<String> wfStatuses = Lists.newArrayListWithCapacity(workflowStatuses.size());
    for (WorkflowStatus workflowStatus : workflowStatuses) {
      switch (workflowStatus) {
        case OPEN:
          wfStatuses.add(CLOSE_TIME_PLACEHOLDER + " = " + MISSING_QUERY);
          break;
        case CLOSED:
          wfStatuses.add(CLOSE_TIME_PLACEHOLDER + " != " + MISSING_QUERY);
          break;
        default:
          wfStatuses.add(WORKFLOW_STATUS_PLACEHOLDER + '"' + workflowStatus + '"');
          break;
      }
    }

    String query = String.join(OR_QUERY, wfStatuses);
    this.appendPartialQuery(query);
    return this;
  }

  public QueryBuilder setWorkflowStartTime(TimeFilter timeFilter) {
    if (timeFilter == null || timeFilter.isEmpty()) {
      return this;
    }

    Collection<String> timerFilters = Lists.newArrayListWithCapacity(2);
    if (timeFilter.getMinTimestamp() != null) {
      timerFilters.add(
          START_TIME_PLACEHOLDER + " >= " + toNanoSeconds(timeFilter.getMinTimestamp()));
    }

    if (timeFilter.getMaxTimestamp() != null) {
      timerFilters.add(
          START_TIME_PLACEHOLDER + " <= " + toNanoSeconds(timeFilter.getMaxTimestamp()));
    }

    String query = String.join(AND_QUERY, timerFilters);
    this.appendPartialQuery(query);
    return this;
  }

  public String build() {
    return this.stringBuffer.toString();
  }

  private static final String OR_QUERY = " or ";
  private static final String AND_QUERY = " and ";
  private static final String LEFT_PARENTHESES = "(";
  private static final String RIGHT_PARENTHESES = ")";
  private static final String MISSING_QUERY = "missing";
  private static final String WORKFLOW_TYPE_PLACEHOLDER = "WorkflowType = ";
  private static final String WORKFLOW_STATUS_PLACEHOLDER = "CloseStatus = ";
  private static final String START_TIME_PLACEHOLDER = "StartTime";
  private static final String CLOSE_TIME_PLACEHOLDER = "CloseTime";
  private static final long TIMESTAMP_SCALE = 1_000_000_000L;
  private StringBuffer stringBuffer;

  private QueryBuilder() {
    this.stringBuffer = new StringBuffer();
  }

  private void appendPartialQuery(String query) {
    if (query == null || query.length() == 0) {
      return;
    }

    if (this.stringBuffer.length() != 0) {
      this.stringBuffer.append(AND_QUERY);
    }

    this.stringBuffer.append(LEFT_PARENTHESES);
    this.stringBuffer.append(query);
    this.stringBuffer.append(RIGHT_PARENTHESES);
  }

  protected static long toNanoSeconds(ZonedDateTime time) {
    return time.toEpochSecond() * TIMESTAMP_SCALE + time.getNano();
  }
}
