/*
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

package com.uber.cadence.internal.largeblob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.uber.cadence.internal.largeblob.impl.InMemoryStorage;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FutureTest {

  private Storage storage = new InMemoryStorage();

  @Test
  public void testPutSmallElement() throws Exception {
    Future future = new Future(storage, 10);

    String testValue = "testValue";
    future.put("test", testValue.getBytes(StandardCharsets.UTF_8));

    assertEquals("testValue", new String(future.get(), StandardCharsets.UTF_8));
    assertNull(storage.get("test"));
  }

  @Test
  public void testLargerValuesGetPutInStorage() throws Exception {
    Future future = new Future(storage, 10);

    String testValue = "testValuetestValuetestValue";
    future.put("test", testValue.getBytes(StandardCharsets.UTF_8));

    assertEquals("testValuetestValuetestValue", new String(future.get(), StandardCharsets.UTF_8));
    assertEquals(
        "testValuetestValuetestValue", new String(storage.get("test"), StandardCharsets.UTF_8));
  }

  @Test
  public void testDeleteValueFromStorage() throws Exception {
    Future future = new Future(storage, 10);

    String testValue = "testValuetestValuetestValue";
    future.put("test", testValue.getBytes(StandardCharsets.UTF_8));

    assertEquals("testValuetestValuetestValue", new String(future.get(), StandardCharsets.UTF_8));
    assertEquals(
        "testValuetestValuetestValue", new String(storage.get("test"), StandardCharsets.UTF_8));

    future.delete();

    assertNull(storage.get("test"));

    assertNull(future.get());
  }

  @Test
  public void testSmallValueIsDelete() throws Exception {
    Future future = new Future(storage, 10);

    String testValue = "testValue";
    future.put("test", testValue.getBytes(StandardCharsets.UTF_8));

    assertEquals("testValue", new String(future.get(), StandardCharsets.UTF_8));
    assertNull(storage.get("test"));

    future.delete();
    assertNull(future.get());
  }
}
