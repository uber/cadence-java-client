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

package com.uber.cadence.internal.worker;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

final class WorkflowRunLockManager {

  private static class CountableLock {
    private ReentrantLock lock;
    private int count;

    CountableLock() {
      this.lock = new ReentrantLock();
      this.count = 1;
    }

    void incrementCount() {
      count++;
    }

    void decrementCount() {
      count--;
    }

    int getCount() {
      return count;
    }

    ReentrantLock getLock() {
      return lock;
    }
  }

  private final ReentrantLock mapLock = new ReentrantLock();
  private final HashMap<String, CountableLock> perRunLock = new HashMap<>();

  ReentrantLock getLockForLocking(String runId) {
    mapLock.lock();

    try {
      CountableLock cl;
      if (perRunLock.containsKey(runId)) {
        cl = perRunLock.get(runId);
        cl.incrementCount();
      } else {
        cl = new CountableLock();
        perRunLock.put(runId, cl);
      }

      return cl.getLock();
    } finally {
      mapLock.unlock();
    }
  }

  void unlock(String runId) {
    mapLock.lock();

    try {
      CountableLock cl = perRunLock.get(runId);
      if (cl == null) {
        throw new RuntimeException("lock for run " + runId + " does not exist.");
      }

      cl.decrementCount();
      if (cl.getCount() == 0) {
        perRunLock.remove(runId);
      }

      cl.getLock().unlock();
    } finally {
      mapLock.unlock();
    }
  }

  int totalLocks() {
    mapLock.lock();

    try {
      return perRunLock.size();
    } finally {
      mapLock.unlock();
    }
  }
}
