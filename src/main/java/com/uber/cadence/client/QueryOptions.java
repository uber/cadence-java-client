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

package com.uber.cadence.client;

import com.uber.cadence.QueryConsistencyLevel;
import com.uber.cadence.QueryRejectCondition;
import java.util.Objects;

public final class QueryOptions {

  public static final class Builder {

    private QueryRejectCondition queryRejectCondition = null; // default to empty condition
    private QueryConsistencyLevel queryConsistencyLevel =
        QueryConsistencyLevel.EVENTUAL; // default to eventual consistent query

    public Builder() {}

    public Builder(QueryOptions o) {
      if (o == null) {
        return;
      }
      this.queryConsistencyLevel = o.queryConsistencyLevel;
      this.queryRejectCondition = o.queryRejectCondition;
    }

    /** queryRejectCondition to decide condition to reject the query */
    public Builder setQueryRejectCondition(QueryRejectCondition queryRejectCondition) {
      this.queryRejectCondition = queryRejectCondition;
      return this;
    }

    public Builder setQueryConsistencyLevel(QueryConsistencyLevel queryConsistencyLevel) {
      this.queryConsistencyLevel = queryConsistencyLevel;
      return this;
    }

    public QueryOptions build() {
      return new QueryOptions(queryRejectCondition, queryConsistencyLevel);
    }
  }

  private QueryRejectCondition queryRejectCondition;
  private QueryConsistencyLevel queryConsistencyLevel;

  private QueryOptions(
      QueryRejectCondition queryRejectCondition, QueryConsistencyLevel queryConsistencyLevel) {
    this.queryConsistencyLevel = queryConsistencyLevel;
    this.queryRejectCondition = queryRejectCondition;
  }

  public QueryRejectCondition getQueryRejectCondition() {
    return queryRejectCondition;
  }

  public QueryConsistencyLevel getQueryConsistencyLevel() {
    return queryConsistencyLevel;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    QueryOptions that = (QueryOptions) o;
    return Objects.equals(queryRejectCondition, that.queryRejectCondition)
        && queryConsistencyLevel == that.queryConsistencyLevel;
  }

  @Override
  public int hashCode() {
    return Objects.hash(queryRejectCondition, queryConsistencyLevel);
  }

  @Override
  public String toString() {
    return "QueryOptions{"
        + "queryRejectCondition='"
        + queryRejectCondition
        + '\''
        + ", queryConsistencyLevel="
        + queryConsistencyLevel
        + '}';
  }
}
