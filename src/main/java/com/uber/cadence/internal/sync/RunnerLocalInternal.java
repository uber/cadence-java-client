package com.uber.cadence.internal.sync;

import java.util.Optional;
import java.util.function.Supplier;

public final class RunnerLocalInternal<T> {

  public T get(Supplier<? extends T> supplier) {
    Optional<T> result =
        DeterministicRunnerImpl.currentThreadInternal().getRunner().getRunnerLocal(this);
    return result.orElse(supplier.get());
  }

  public void set(T value) {
    DeterministicRunnerImpl.currentThreadInternal().getRunner().setRunnerLocal(this, value);
  }
}
