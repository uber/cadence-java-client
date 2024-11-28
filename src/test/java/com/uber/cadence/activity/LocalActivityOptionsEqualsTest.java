/*
 *
 *  *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *  *
 *  *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  *  use this file except in compliance with the License. A copy of the License is
 *  *  located at
 *  *
 *  *  http://aws.amazon.com/apache2.0
 *  *
 *  *  or in the "license" file accompanying this file. This file is distributed on
 *  *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  *  express or implied. See the License for the specific language governing
 *  *  permissions and limitations under the License.
 *
 */

package com.uber.cadence.activity;

import static org.junit.Assert.assertEquals;

import com.uber.cadence.common.RetryOptions;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

// This test is separated into its own file because it uses the Parameterized runner
@RunWith(Parameterized.class)
public class LocalActivityOptionsEqualsTest {

  private final LocalActivityOptions options1;
  // In the java equals method the `other` parameter is of type Object
  private final Object options2;
  private final boolean equals;

  public LocalActivityOptionsEqualsTest(
      String name, LocalActivityOptions options1, Object options2, boolean equals) {
    this.options1 = options1;
    this.options2 = options2;
    this.equals = equals;
  }

  @Parameterized.Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    LocalActivityOptions options = LocalActivityOptionsTest.createOptions();

    return Arrays.asList(
        new Object[] {
          "Same object", options, options, true,
        },
        new Object[] {
          "Other is null", options, null, false,
        },
        new Object[] {
          "Other is not instance of LocalActivityOptions", options, new Object(), false,
        },
        new Object[] {
          "Emtpy equals empty",
          new LocalActivityOptions.Builder().build(),
          new LocalActivityOptions.Builder().build(),
          true
        },
        new Object[] {
          "Unset property on localActivityOptions is checked not equal",
          new LocalActivityOptions.Builder().build(),
          new LocalActivityOptions.Builder()
              .setScheduleToCloseTimeout(Duration.ofSeconds(123))
              .build(),
          false
        },
        new Object[] {
          "Property on localActivityOptions is checked equals",
          new LocalActivityOptions.Builder()
              .setScheduleToCloseTimeout(Duration.ofSeconds(123))
              .build(),
          new LocalActivityOptions.Builder()
              .setScheduleToCloseTimeout(Duration.ofSeconds(123))
              .build(),
          true
        },
        new Object[] {
          "Property on localActivityOptions is checked not equal",
          new LocalActivityOptions.Builder()
              .setScheduleToCloseTimeout(Duration.ofSeconds(1))
              .build(),
          new LocalActivityOptions.Builder()
              .setScheduleToCloseTimeout(Duration.ofSeconds(2))
              .build(),
          false
        },
        new Object[] {
          "retryOptions property not equal",
          new LocalActivityOptions.Builder()
              .setRetryOptions(
                  new RetryOptions.Builder().setInitialInterval(Duration.ofSeconds(1)).build())
              .build(),
          new LocalActivityOptions.Builder()
              .setRetryOptions(
                  new RetryOptions.Builder().setInitialInterval(Duration.ofSeconds(2)).build())
              .build(),
          false
        });
  }

  @Test
  public void testEquals() {
    boolean got = options1.equals(options2);
    assertEquals(equals, got);
  }
}
