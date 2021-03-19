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
package com.uber.cadence.worker;

import java.time.ZonedDateTime;
import java.util.Objects;

public class TimeFilter {

  public static TimeFilter.Builder newBuilder() {
    return new TimeFilter.Builder();
  }

  public static TimeFilter.Builder newBuilder(TimeFilter options) {
    return new TimeFilter.Builder(options);
  }

  public static TimeFilter defaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final TimeFilter DEFAULT_INSTANCE;

  static {
    DEFAULT_INSTANCE = TimeFilter.newBuilder().build();
  }

  public static final class Builder {
    private ZonedDateTime minTimestamp;
    private ZonedDateTime maxTimestamp;

    private Builder() {}

    private Builder(TimeFilter options) {
      this.minTimestamp = options.minTimestamp;
      this.maxTimestamp = options.maxTimestamp;
    }

    public Builder setMinTimestamp(ZonedDateTime minTimestamp) {
      this.minTimestamp = Objects.requireNonNull(minTimestamp);
      return this;
    }

    public Builder setMaxTimestamp(ZonedDateTime maxTimestamp) {
      this.maxTimestamp = Objects.requireNonNull(maxTimestamp);
      return this;
    }

    public TimeFilter build() {
      return new TimeFilter(minTimestamp, maxTimestamp);
    }
  }

  private final ZonedDateTime minTimestamp;
  private final ZonedDateTime maxTimestamp;

  private TimeFilter(ZonedDateTime minTimestamp, ZonedDateTime maxTimestamp) {
    this.minTimestamp = minTimestamp;
    this.maxTimestamp = maxTimestamp;
  }

  public ZonedDateTime getMinTimestamp() {
    return minTimestamp;
  }

  public ZonedDateTime getMaxTimestamp() {
    return maxTimestamp;
  }

  public boolean isEmpty() {
    return this.minTimestamp == null && this.maxTimestamp == null;
  }

  @Override
  public String toString() {
    return "TimeFilter{" + "minTimestamp=" + minTimestamp + ", maxTimestamp=" + maxTimestamp + '}';
  }
}
