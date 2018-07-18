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

package com.uber.cadence.common;

public class Validate {

  public static <T> void ArgumentNotNull(T arg, String argName) {
    if (arg == null) {
      throw new IllegalArgumentException(String.format("%s should not be null", argName));
    }
  }

  public static void ArgumentNotEmpty(String arg, String argName) {
    if (arg == null || "".equals(arg)) {
      throw new IllegalArgumentException(String.format("%s should not be null or empty", argName));
    }
  }

  public static <T> void Argument(boolean condition, String argName, String errorMessage) {
    if (!condition) {
      throw new IllegalArgumentException(
          String.format("Argument '%s' Validation Failed: %s", argName, errorMessage));
    }
  }

  public static void Condition(boolean condition, String message) {
    if (!condition) {
      throw new IllegalStateException(message);
    }
  }
}
