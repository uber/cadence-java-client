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

import static org.junit.Assert.assertEquals;

import java.time.ZonedDateTime;
import org.junit.Test;

public class TimeFilterTest {

  @Test
  public void testTimeFilter() {
    ZonedDateTime now = ZonedDateTime.now();
    TimeFilter timeFilter =
        TimeFilter.newBuilder().setMinTimestamp(now).setMaxTimestamp(now.plusHours(1)).build();
    assertEquals(now, timeFilter.getMinTimestamp());
    assertEquals(now.plusHours(1), timeFilter.getMaxTimestamp());
  }
}
