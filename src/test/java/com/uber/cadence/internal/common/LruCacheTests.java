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

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public class LruCacheTests {

  @Test
  public void ItemsCanBeCached() {
    LruCache<String> c = new LruCache<>(3);
    c.Put("key1", "a");
    String shouldExist = c.Get("key1");

    assertEquals(1, c.Size());
    assertEquals("a", shouldExist);
  }

  @Test
  public void PutIfNotExistDoesNotOverwriteCache() {
    LruCache<String> c = new LruCache<>(3);
    c.Put("key1", "a");
    c.PutIfNotExist("key1", "b");
    String shouldExist = c.Get("key1");

    assertEquals(1, c.Size());
    assertEquals("a", shouldExist);
  }

  @Test
  public void ItemsCanBeDeleted() {
    AtomicInteger elementsEvicted = new AtomicInteger(0);
    LruCache<String> c = new LruCache<String>(3, e -> elementsEvicted.getAndIncrement());
    c.Put("key1", "a");
    c.Delete("key1");
    String shouldNotExist = c.Get("key1");

    assertEquals(0, c.Size());
    assertEquals(null, shouldNotExist);

    // Verify actOnEvict is called on eviction
    assertEquals(1, elementsEvicted.get());
  }

  @Test
  public void LeastRecentlyUsedEntryIsEvictedOnPut() {
    AtomicInteger elementsEvicted = new AtomicInteger(0);
    LruCache<String> c = new LruCache<>(3, e -> elementsEvicted.getAndIncrement());
    c.Put("key1", "a");
    c.Put("key2", "b");
    c.Put("key3", "c");
    c.Get("key1");
    c.Put("key4", "c");

    String shouldBeEvicted = c.Get("key2");
    String shouldNotBeEvicted = c.Get("key1");

    assertEquals(3, c.Size());
    assertEquals("a", shouldNotBeEvicted);
    assertEquals(null, shouldBeEvicted);

    // Verify actOnEvict is called on eviction
    assertEquals(1, elementsEvicted.get());
  }

  @Test
  public void LeastRecentlyUsedEntryIsEvictedOnEvictNext() {
    AtomicInteger elementsEvicted = new AtomicInteger(0);
    LruCache<String> c = new LruCache<>(4, e -> elementsEvicted.getAndIncrement());
    c.Put("key1", "a");
    c.Put("key2", "b");
    c.Put("key3", "c");
    c.Get("key1");
    c.EvictNext();

    String shouldBeEvicted = c.Get("key2");
    String shouldNotBeEvicted = c.Get("key1");

    assertEquals(2, c.Size());
    assertEquals(null, shouldBeEvicted);
    assertEquals("a", shouldNotBeEvicted);

    // Verify actOnEvict is called on evictNext
    assertEquals(1, elementsEvicted.get());
  }

  @Test
  public void EvictAllRemovesAllElements() {
    AtomicInteger elementsEvicted = new AtomicInteger(0);
    LruCache<String> c = new LruCache<>(4, e -> elementsEvicted.getAndIncrement());
    c.Put("key1", "a");
    c.Put("key2", "b");
    c.Put("key3", "c");
    c.EvictAll();

    assertEquals(0, c.Size());

    // Verify actOnEvict is called on evictAll
    assertEquals(3, elementsEvicted.get());
  }
}
