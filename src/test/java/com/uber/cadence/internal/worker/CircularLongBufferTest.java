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
package com.uber.cadence.internal.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Before;
import org.junit.Test;

public class CircularLongBufferTest {

  private CircularLongBuffer buffer;
  private CircularLongBuffer buffer2;

  @Before
  public void setUp() {
    // Initialize a buffer with specific values for testing
    buffer = new CircularLongBuffer(new long[] {1, 2, 3, 4, 5});
    buffer2 = new CircularLongBuffer(new long[] {});
  }

  @Test
  public void testCopyZeroLength() {
    // Copy with zero length should result in an empty buffer
    CircularLongBuffer copyBuffer = buffer.copy(2, 0);
    assertEquals(0, copyBuffer.size());
  }

  @Test
  public void testEdgeCase() {
    // Copy with zero length should result in an empty buffer
    CircularLongBuffer copyBuffer = buffer.copy(100, 3);
    assertEquals(3, copyBuffer.size());
  }

  @Test
  public void testValuesZero() {
    // Copy with zero length should result in an empty buffer
    assertThrows(IllegalStateException.class, () -> buffer2.copy(2, 3));
  }
}
