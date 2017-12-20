package com.uber.cadence.internal.dispatcher;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

class LockImpl implements Lock {

    private WorkflowThread owner;
    private int holdCount;

    @Override
    public void lock() {
        boolean interrupted = false;
        do {
            try {
                WorkflowThreadImpl.yield("lock", () -> tryLock());
            } catch (InterruptedException e) {
                interrupted = true;
            }
        } while (interrupted);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        WorkflowThreadImpl.yield("lockInterruptibly", () -> tryLock());
    }

    @Override
    public boolean tryLock() {
        WorkflowThread currentThread = WorkflowThread.currentThread();
        if (owner == null || owner == currentThread) {
            owner = currentThread;
            holdCount++;
        }
        return owner == currentThread;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return WorkflowThreadImpl.yield(unit.toMillis(time), "tryLock", () -> tryLock());
    }

    @Override
    public void unlock() {
        if (owner == null || holdCount == 0) {
            throw new IllegalMonitorStateException("not locked");
        }
        if (owner != WorkflowThread.currentThread()) {
            throw new IllegalMonitorStateException(
                    WorkflowThread.currentThread().getName() + " is not an owner. " +
                            "Owner is " + owner.getName());
        }
        holdCount--;
        owner = null;
    }

    @Override
    public Condition newCondition() {
        return null;
    }
}
