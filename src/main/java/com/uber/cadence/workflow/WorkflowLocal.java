package com.uber.cadence.workflow;

import com.uber.cadence.internal.sync.RunnerLocalInternal;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A value that is local to a single workflow execution. So it can act as a <i>global</i> variable
 * for the workflow code. For example the {@code Workflow.isSignalled()} static method returns the
 * correct value even if there are multiple workflows executing on the same machine simultaneously.
 * It would be invalid if the {@code signaled} was a {@code static boolean} variable.
 *
 * <pre>{@code
 * public class Workflow {
 *
 *   private static final WorkflowLocal<Boolean> signaled = WorkflowLocal.withInitial(() -> false);
 *
 *   public static boolean isSignalled() {
 *     return signaled.get();
 *   }
 *
 *   public void signal() {
 *     signaled.set(true);
 *   }
 * }
 * }</pre>
 *
 * @see WorkflowThreadLocal for thread local that can be used inside workflow code.
 */
public final class WorkflowLocal<T> {

  private final RunnerLocalInternal<T> impl = new RunnerLocalInternal<>();
  private Supplier<? extends T> supplier;

  private WorkflowLocal(Supplier<? extends T> supplier) {
    this.supplier = Objects.requireNonNull(supplier);
  }

  public WorkflowLocal() {
    this.supplier = () -> null;
  }

  public static <S> WorkflowLocal<S> withInitial(Supplier<? extends S> supplier) {
    return new WorkflowLocal<>(supplier);
  }

  public T get() {
    return impl.get(supplier);
  }

  public void set(T value) {
    impl.set(value);
  }
}
