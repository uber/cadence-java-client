/*
 *  Modifications Copyright (c) 2017-2020 Uber Technologies Inc.
 *  Portions of the Software are attributed to Copyright (c) 2020 Temporal Technologies Inc.
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

import com.uber.cadence.worker.WorkerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WorkerShutDownHandler {

  private static final List<WorkerFactory> workerFactories = new ArrayList<>();
  private static boolean registered = false;

  public static void registerHandler() {
    if (registered) {
      return;
    }

    registered = true;
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread("SHUTDOWN_WORKERS") {
              @Override
              public void run() {
                for (WorkerFactory workerFactory : workerFactories) {
                  workerFactory.suspendPolling();
                }

                for (WorkerFactory workerFactory : workerFactories) {
                  workerFactory.shutdownNow();
                }

                for (WorkerFactory workerFactory : workerFactories) {
                  workerFactory.awaitTermination(1, TimeUnit.SECONDS);
                }
              }
            });
  }

  public static synchronized void registerWorkerFactory(WorkerFactory workerFactory) {
    if (workerFactory != null) {
      workerFactories.add(workerFactory);
    }
  }
}
