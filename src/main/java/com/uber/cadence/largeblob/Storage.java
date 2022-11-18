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

package com.uber.cadence.largeblob;

import java.io.IOException;

/**
 * Storage is an abstraction for storing large parameters to access inside of activities.0
 */
public interface Storage {

  /**
   * Gets the data based on uri provided
   * @param uri uri.
   * @return the data as a byte array.
   * @throws IOException should be thrown in any implementation class in case of problems accessing the datastore.
   */
  byte[] get(String uri) throws IOException;

  /**
   * Stores data based on uri provided.
   * @param uri uri.
   * @param bytes bytes.
   * @throws IOException should be thrown in any implementation class in case of problems with the datastore
   */
  void put(String uri, byte[] bytes) throws IOException;

  /**
   * Deletes data based on uri provided.
   * @param uri uri.
   * @throws IOException should be thrown in any implementation class in case of problems with the datastore
   */
  void delete(String uri) throws IOException;
}
