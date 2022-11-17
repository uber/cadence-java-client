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

package com.uber.cadence.internal.largeblob.impl;

import com.uber.cadence.internal.largeblob.Storage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class InMemoryStorage implements Storage {

  private final Map<String, byte[]> storage = new HashMap<>();

  @Override
  public byte[] get(String uri) throws IOException {
    return storage.get(uri);
  }

  @Override
  public void put(String uri, byte[] bytes) throws IOException {
    storage.put(uri, bytes);
  }

  @Override
  public void delete(String uri) throws IOException {
    storage.remove(uri);
  }
}
