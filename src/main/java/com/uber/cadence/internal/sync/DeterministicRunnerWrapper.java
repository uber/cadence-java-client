package com.uber.cadence.internal.sync;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

class DeterministicRunnerWrapper implements InvocationHandler {

  private final InvocationHandler invocationHandler;

  DeterministicRunnerWrapper(InvocationHandler invocationHandler) {
    this.invocationHandler = Objects.requireNonNull(invocationHandler);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    CompletableFuture<Object> result = new CompletableFuture<>();
    DeterministicRunner runner = new DeterministicRunnerImpl(() -> {
      try {
        result.complete(invocationHandler.invoke(proxy, method, args));
      } catch (Throwable throwable) {
        result.completeExceptionally(throwable);
      }
    });
    runner.runUntilAllBlocked();
    try {
      return result.get();
    } catch (ExecutionException e) {
      throw e.getCause();
    }
  }
}
