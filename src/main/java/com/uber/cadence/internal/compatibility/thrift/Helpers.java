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
package com.uber.cadence.internal.compatibility.thrift;

import com.google.protobuf.ByteString;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Duration;
import com.google.protobuf.Int64Value;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;

class Helpers {

  static double toDoubleValue(DoubleValue v) {
    return v.getValue();
  }

  static long toInt64Value(Int64Value v) {
    return v.getValue();
  }

  static long timeToUnixNano(Timestamp t) {
    return Timestamps.toNanos(t);
  }

  static int durationToDays(Duration d) {
    return (int) Durations.toDays(d);
  }

  static int durationToSeconds(Duration d) {
    return (int) Durations.toSeconds(d);
  }

  static byte[] byteStringToArray(ByteString t) {
    if (t == null) {
      return null;
    }
    return t.toByteArray();
  }

}
