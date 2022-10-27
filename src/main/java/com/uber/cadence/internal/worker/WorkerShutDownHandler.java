/*
 *  Modifications Copyright (c) 2020-2022 Uber Technologies Inc.
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.uber.cadence.internal.common.InternalUtils;
import com.uber.cadence.worker.WorkerFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WorkerShutDownHandler {

  private static final List<WorkerFactory> workerFactories = new ArrayList<>();
  private static Thread registeredHandler;

  public static void registerHandler(Duration workerShutdownTimeout) {
    if (registeredHandler != null) {
      return;
    }

    registeredHandler =
        new Thread("SHUTDOWN_WORKERS") {
          @Override
          public void run() {
            for (WorkerFactory workerFactory : workerFactories) {
              workerFactory.suspendPolling();
            }

            for (WorkerFactory workerFactory : workerFactories) {
              workerFactory.shutdownNow();
            }

            long remainingTimeoutMillis =
                TimeUnit.SECONDS.toMillis(workerShutdownTimeout.getSeconds())
                    + TimeUnit.NANOSECONDS.toMillis(workerShutdownTimeout.getNano());

            for (WorkerFactory workerFactory : workerFactories) {
              final long timeoutMillis = remainingTimeoutMillis;
              remainingTimeoutMillis =
                  InternalUtils.awaitTermination(
                      timeoutMillis,
                      () -> workerFactory.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS));
            }
          }
        };

    Runtime.getRuntime().addShutdownHook(registeredHandler);
  }

  public static synchronized void registerWorkerFactory(WorkerFactory workerFactory) {
    if (workerFactory != null) {
      workerFactories.add(workerFactory);
    }
  }

  // Only for tests
  protected static void execute() {
    registeredHandler.run();
  }
}
