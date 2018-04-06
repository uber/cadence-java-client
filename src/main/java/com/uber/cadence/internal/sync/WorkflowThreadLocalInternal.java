package com.uber.cadence.internal.sync;

import java.util.Optional;
import java.util.function.Supplier;

public final class WorkflowThreadLocalInternal<T> {

  public T get(Supplier<? extends T> supplier) {
    Optional<T> result = DeterministicRunnerImpl.currentThreadInternal().getThreadLocal(this);
    return result.orElse(supplier.get());
  }

  public void set(T value) {
    DeterministicRunnerImpl.currentThreadInternal().setThreadLocal(this, value);
  }
}
