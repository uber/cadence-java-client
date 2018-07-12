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

// A Cache is a generalized interface to a cache
public interface Cache<T> {

  // Get retrieves an element based on a key, returning nil if the element
  // does not exist
  T Get(String key);

  // Put adds an element to the cache
  void Put(String key, T value);

  // PutIfNotExist puts a value associated with a given key if it does not exist
  void PutIfNotExist(String key, T value);

  // Delete deletes an element in the cache
  void Delete(String key);

  // Deletes all elements from the cache
  void EvictAll();

  // Deletes the least recently used element in the cache
  void EvictNext();

  // Size returns the number of entries currently stored in the Cache
  int Size();
}
