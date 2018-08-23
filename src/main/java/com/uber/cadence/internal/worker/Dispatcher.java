package com.uber.cadence.internal.worker;

import java.util.function.Consumer;

public interface Dispatcher<TTopic, TMessage> extends Consumer<TMessage> {
  void subscribe(TTopic topic, Consumer<TMessage> consumer);
}
