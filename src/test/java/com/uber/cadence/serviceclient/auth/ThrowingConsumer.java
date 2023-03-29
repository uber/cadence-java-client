package com.uber.cadence.serviceclient.auth;

@FunctionalInterface
public interface ThrowingConsumer<T> {

  void acceptThrows(T elem) throws Exception;
}
