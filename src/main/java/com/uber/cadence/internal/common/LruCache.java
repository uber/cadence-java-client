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

package com.uber.cadence.internal.common;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class LruCache<T> implements Cache<T> {

  private int MAX_ENTRIES;
  private Consumer<T> actOnEvictedEntry = e -> {};
  private Lock lock = new ReentrantLock();

  private LinkedHashMap<String, T> map =
      new LinkedHashMap<String, T>(MAX_ENTRIES + 1, 0.75F, true) {
        protected @Override boolean removeEldestEntry(Map.Entry eldest) {
          if (size() > MAX_ENTRIES) {
            @SuppressWarnings("unchecked")
            T entry = (T) (eldest.getValue());
            actOnEvictedEntry.accept(entry);
            return true;
          }
          return false;
        }
      };

  public LruCache(int maxEntries) {
    if (maxEntries <= 0) {
      throw new IllegalArgumentException("Cache Max Entries must be greater than 0");
    }
    MAX_ENTRIES = maxEntries;
  }

  public LruCache(int maxEntries, Consumer<T> actOnEvictedEntry) {
    if (maxEntries <= 0) {
      throw new IllegalArgumentException("Cache Max Entries must be greater than 0");
    }
    if (actOnEvictedEntry == null) {
      throw new IllegalArgumentException("actOnEvictedEntryMethod must not be null");
    }
    MAX_ENTRIES = maxEntries;
    this.actOnEvictedEntry = actOnEvictedEntry;
  }

  @Override
  public T Get(String key) {
    lock.lock();
    try {
      return map.get(key);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void Put(String key, T value) {
    lock.lock();
    try {
      map.put(key, value);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void PutIfNotExist(String key, T value) {
    lock.lock();
    try {
      map.putIfAbsent(key, value);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void Delete(String key) {
    lock.lock();
    try {
      deleteNoLock(key);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void EvictAll() {
    lock.lock();
    try {
      while (map.size() > 0) {
        evictNextNoLock();
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void EvictNext() {
    lock.lock();
    try {
      evictNextNoLock();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public int Size() {
    lock.lock();
    try {
      return map.size();
    } finally {
      lock.unlock();
    }
  }

  private void deleteNoLock(String key) {
    T value = map.remove(key);
    actOnEvictedEntry.accept(value);
  }

  private void evictNextNoLock() {
    Iterator<Map.Entry<String, T>> iterator = map.entrySet().iterator();
    if (iterator.hasNext()) {
      Map.Entry<String, T> next = iterator.next();
      deleteNoLock(next.getKey());
    }
  }
}
