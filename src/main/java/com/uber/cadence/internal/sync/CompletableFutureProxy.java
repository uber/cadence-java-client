package com.uber.cadence.internal.sync;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

class CompletableFutureProxy<T> extends CompletableFuture<T> {
  private final CompletableFuture<T> impl = new CompletableFuture<>();

  @Override
  public boolean isDone() {
    return impl.isDone();
  }

  @Override
  public T get() throws InterruptedException, ExecutionException {
    return impl.get();
  }

  @Override
  public T get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return impl.get(timeout, unit);
  }

  @Override
  public T join() {
    return impl.join();
  }

  @Override
  public T getNow(T valueIfAbsent) {
    return impl.getNow(valueIfAbsent);
  }

  @Override
  public boolean complete(T value) {
    return impl.complete(value);
  }

  @Override
  public boolean completeExceptionally(Throwable ex) {
    return impl.completeExceptionally(ex);
  }

  @Override
  public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> fn) {
    return impl.thenApply(fn);
  }

  @Override
  public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
    return impl.thenApplyAsync(fn);
  }

  @Override
  public <U> CompletableFuture<U> thenApplyAsync(
      Function<? super T, ? extends U> fn, Executor executor) {
    return impl.thenApplyAsync(fn, executor);
  }

  @Override
  public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
    return impl.thenAccept(action);
  }

  @Override
  public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
    return impl.thenAcceptAsync(action);
  }

  @Override
  public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
    return impl.thenAcceptAsync(action, executor);
  }

  @Override
  public CompletableFuture<Void> thenRun(Runnable action) {
    return impl.thenRun(action);
  }

  @Override
  public CompletableFuture<Void> thenRunAsync(Runnable action) {
    return impl.thenRunAsync(action);
  }

  @Override
  public CompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
    return impl.thenRunAsync(action, executor);
  }

  @Override
  public <U, V> CompletableFuture<V> thenCombine(
      CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
    return impl.thenCombine(other, fn);
  }

  @Override
  public <U, V> CompletableFuture<V> thenCombineAsync(
      CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
    return impl.thenCombineAsync(other, fn);
  }

  @Override
  public <U, V> CompletableFuture<V> thenCombineAsync(
      CompletionStage<? extends U> other,
      BiFunction<? super T, ? super U, ? extends V> fn,
      Executor executor) {
    return impl.thenCombineAsync(other, fn, executor);
  }

  @Override
  public <U> CompletableFuture<Void> thenAcceptBoth(
      CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
    return impl.thenAcceptBoth(other, action);
  }

  @Override
  public <U> CompletableFuture<Void> thenAcceptBothAsync(
      CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
    return impl.thenAcceptBothAsync(other, action);
  }

  @Override
  public <U> CompletableFuture<Void> thenAcceptBothAsync(
      CompletionStage<? extends U> other,
      BiConsumer<? super T, ? super U> action,
      Executor executor) {
    return impl.thenAcceptBothAsync(other, action, executor);
  }

  @Override
  public CompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
    return impl.runAfterBoth(other, action);
  }

  @Override
  public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
    return impl.runAfterBothAsync(other, action);
  }

  @Override
  public CompletableFuture<Void> runAfterBothAsync(
      CompletionStage<?> other, Runnable action, Executor executor) {
    return impl.runAfterBothAsync(other, action, executor);
  }

  @Override
  public <U> CompletableFuture<U> applyToEither(
      CompletionStage<? extends T> other, Function<? super T, U> fn) {
    return impl.applyToEither(other, fn);
  }

  @Override
  public <U> CompletableFuture<U> applyToEitherAsync(
      CompletionStage<? extends T> other, Function<? super T, U> fn) {
    return impl.applyToEitherAsync(other, fn);
  }

  @Override
  public <U> CompletableFuture<U> applyToEitherAsync(
      CompletionStage<? extends T> other, Function<? super T, U> fn, Executor executor) {
    return impl.applyToEitherAsync(other, fn, executor);
  }

  @Override
  public CompletableFuture<Void> acceptEither(
      CompletionStage<? extends T> other, Consumer<? super T> action) {
    return impl.acceptEither(other, action);
  }

  @Override
  public CompletableFuture<Void> acceptEitherAsync(
      CompletionStage<? extends T> other, Consumer<? super T> action) {
    return impl.acceptEitherAsync(other, action);
  }

  @Override
  public CompletableFuture<Void> acceptEitherAsync(
      CompletionStage<? extends T> other, Consumer<? super T> action, Executor executor) {
    return impl.acceptEitherAsync(other, action, executor);
  }

  @Override
  public CompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
    return impl.runAfterEither(other, action);
  }

  @Override
  public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
    return impl.runAfterEitherAsync(other, action);
  }

  @Override
  public CompletableFuture<Void> runAfterEitherAsync(
      CompletionStage<?> other, Runnable action, Executor executor) {
    return impl.runAfterEitherAsync(other, action, executor);
  }

  @Override
  public <U> CompletableFuture<U> thenCompose(
      Function<? super T, ? extends CompletionStage<U>> fn) {
    return impl.thenCompose(fn);
  }

  @Override
  public <U> CompletableFuture<U> thenComposeAsync(
      Function<? super T, ? extends CompletionStage<U>> fn) {
    return impl.thenComposeAsync(fn);
  }

  @Override
  public <U> CompletableFuture<U> thenComposeAsync(
      Function<? super T, ? extends CompletionStage<U>> fn, Executor executor) {
    return impl.thenComposeAsync(fn, executor);
  }

  @Override
  public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
    return impl.whenComplete(action);
  }

  @Override
  public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
    return impl.whenCompleteAsync(action);
  }

  @Override
  public CompletableFuture<T> whenCompleteAsync(
      BiConsumer<? super T, ? super Throwable> action, Executor executor) {
    return impl.whenCompleteAsync(action, executor);
  }

  @Override
  public <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
    return impl.handle(fn);
  }

  @Override
  public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
    return impl.handleAsync(fn);
  }

  @Override
  public <U> CompletableFuture<U> handleAsync(
      BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
    return impl.handleAsync(fn, executor);
  }

  @Override
  public CompletableFuture<T> toCompletableFuture() {
    return impl.toCompletableFuture();
  }

  @Override
  public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
    return impl.exceptionally(fn);
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return impl.cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean isCancelled() {
    return impl.isCancelled();
  }

  @Override
  public boolean isCompletedExceptionally() {
    return impl.isCompletedExceptionally();
  }

  @Override
  public void obtrudeValue(T value) {
    impl.obtrudeValue(value);
  }

  @Override
  public void obtrudeException(Throwable ex) {
    impl.obtrudeException(ex);
  }

  @Override
  public int getNumberOfDependents() {
    return impl.getNumberOfDependents();
  }

  @Override
  public String toString() {
    return impl.toString();
  }
}
