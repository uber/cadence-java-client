package com.uber.cadence.workflow;

import com.uber.cadence.internal.sync.WorkflowThreadLocalInternal;
import java.util.Objects;
import java.util.function.Supplier;

/** {@link ThreadLocal} analog for workflow code. */
public final class WorkflowThreadLocal<T> {

  private final WorkflowThreadLocalInternal<T> impl = new WorkflowThreadLocalInternal<>();
  private Supplier<? extends T> supplier;

  private WorkflowThreadLocal(Supplier<? extends T> supplier) {
    this.supplier = Objects.requireNonNull(supplier);
  }

  public WorkflowThreadLocal() {
    this.supplier = () -> null;
  }

  public static <S> WorkflowThreadLocal<S> withInitial(Supplier<? extends S> supplier) {
    return new WorkflowThreadLocal<>(supplier);
  }

  public T get() {
    return impl.get(supplier);
  }

  public void set(T value) {
    impl.set(value);
  }
}
