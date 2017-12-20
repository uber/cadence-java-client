package com.uber.cadence.internal.dispatcher;

/**
 * Executes code passed to {@link #newRunner(Runnable)} as well as threads created from it using
 * {@link Workflow#newThread(Runnable)} deterministically. Requires use of provided wrappers for synchronization
 * and notification instead of native ones.
 */
public interface DeterministicRunner {

    static DeterministicRunner newRunner(Runnable root) {
        return new DeterministicRunnerImpl(root);
    }

    static DeterministicRunner newRunner(SyncDecisionContext decisionContext, WorkflowClock clock, Runnable root) {
        return new DeterministicRunnerImpl(decisionContext, clock, root);
    }

    /**
     * ExecuteUntilAllBlocked executes threads one by one in deterministic order
     * until all of them are completed or blocked.
     *
     * @return nearest time when at least one of the threads is expected to wake up.
     * @throws Throwable if one of the threads didn't handle an exception.
     */
    void runUntilAllBlocked() throws Throwable;

    /**
     * IsDone returns true when all of threads are completed
     */
    boolean isDone();

    /**
     * * Destroys all threads by throwing {@link DestroyWorkflowThreadError} without waiting for their completion
     */
    void close();

    /**
     * Stack trace of all threads owned by the DeterministicRunner instance
     */
    String stackTrace();

    /**
     * @return time according to a {@link WorkflowClock} configured with the Runner.
     */
    long currentTimeMillis();

    /**
     * @return time at which workflow can make progress.
     * For example when {@link WorkflowThread#sleep(long)} expires.
     * 0 means that no time related blockages.
     */
    long getNextWakeUpTime();
}
