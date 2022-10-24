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

package com.uber.cadence.context;

import io.opentracing.propagation.TextMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HashMapTextMap implements TextMap {

  private final HashMap<String, String> backingMap = new HashMap<>();

  public HashMapTextMap() {
    // Noop
  }

  public HashMapTextMap(Map<String, String> spanData) {
    backingMap.putAll(spanData);
  }

  @Override
  public Iterator<Map.Entry<String, String>> iterator() {
    return backingMap.entrySet().iterator();
  }

  @Override
  public void put(String key, String value) {
    backingMap.put(key, value);
  }

  public HashMap<String, String> getBackingMap() {
    return backingMap;
  }
}
