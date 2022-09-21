package com.uber.cadence.internal.worker;

import com.uber.cadence.worker.WorkerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WorkerShutDownHandler {

    private static final List<WorkerFactory> workerFactories = new ArrayList<>();
    private static boolean registered = false;

    public static void registerHandler() {
        if (!registered) {
            registered = true;
            Runtime.getRuntime().addShutdownHook(new Thread("SHUTDOWN_WORKERS") {
                @Override
                public void run() {
                    for (WorkerFactory workerFactory : workerFactories) {
                        workerFactory.suspendPolling();
                    }

                    for (WorkerFactory workerFactory : workerFactories) {
                        workerFactory.shutdownNow();
                    }

                    for (WorkerFactory workerFactory: workerFactories) {
                        workerFactory.awaitTermination(1, TimeUnit.SECONDS);
                    }
                }
            });
        }
    }


    public static void registerWorkerFactory(WorkerFactory workerFactory) {
        if (workerFactory != null) {
            workerFactories.add(workerFactory);
        }
    }
}
