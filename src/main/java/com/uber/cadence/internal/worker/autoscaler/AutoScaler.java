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

package com.uber.cadence.internal.worker.autoscaler;

/**
 * Poller autoscaler interface for acquiring and releasing locks. In order to control number of
 * concurrent operations.
 */
public interface AutoScaler {

  void start();

  void stop();

  /**
   * Reduce the number of available locks. Intended to be blocking operation until lock is acquired.
   */
  void acquire() throws InterruptedException;

  /**
   * Releases lock into the autoscaler pool. Release should be always called in same process,
   * failing to do so is considered a usage error.
   */
  void release();

  void increaseNoopPollCount();

  void increaseActionablePollCount();
}
