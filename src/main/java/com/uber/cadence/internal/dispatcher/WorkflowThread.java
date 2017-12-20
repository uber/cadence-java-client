package com.uber.cadence.internal.dispatcher;

import java.util.function.Supplier;

public interface WorkflowThread {

    void start();

    void join() throws InterruptedException;

    void join(long millis) throws InterruptedException;

    void interrupt();

    boolean isInterrupted();

    boolean isAlive();

    void setName(String name);

    String getName();

    long getId();

    Thread.State getState();

    static WorkflowThread currentThread() {
        return WorkflowThreadImpl.currentThread();
    }

    static void sleep(long millis) throws InterruptedException {
        WorkflowThreadImpl.yield(millis, "sleep", () -> false   );
    }

    static boolean interrupted() {
        return WorkflowThreadImpl.currentThread().resetInterrupted();
    }
}