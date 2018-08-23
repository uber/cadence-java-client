package com.uber.cadence.internal.worker;

public interface DispatcherFactory<TTopic, TMessage> {
  Dispatcher<TTopic, TMessage> create();
}
