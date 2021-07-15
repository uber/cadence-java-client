/*
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.uber.cadence.context;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.slf4j.MDC;

public class OpenTelemetryContextPropagator implements ContextPropagator {

  private static final TextMapPropagator w3cTraceContextPropagator =
      W3CTraceContextPropagator.getInstance();
  private static final TextMapPropagator w3cBaggagePropagator = W3CBaggagePropagator.getInstance();
  private static ThreadLocal<Scope> currentContextOtelScope = new ThreadLocal<>();
  private static ThreadLocal<Span> currentOtelSpan = new ThreadLocal<>();
  private static ThreadLocal<Scope> currentOtelScope = new ThreadLocal<>();
  private static ThreadLocal<Iterable<String>> otelKeySet = new ThreadLocal<>();
  private static final TextMapSetter<Map<String, String>> setter = Map::put;
  private static final TextMapGetter<Map<String, String>> getter =
      new TextMapGetter<Map<String, String>>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
          return otelKeySet.get();
        }

        @Nullable
        @Override
        public String get(Map<String, String> carrier, String key) {
          return MDC.get(key);
        }
      };

  @Override
  public String getName() {
    return "OpenTelemetry";
  }

  @Override
  public Map<String, byte[]> serializeContext(Object context) {
    Map<String, byte[]> serializedContext = new HashMap<>();
    Map<String, String> contextMap = (Map<String, String>) context;
    if (contextMap != null) {
      for (Map.Entry<String, String> entry : contextMap.entrySet()) {
        serializedContext.put(entry.getKey(), entry.getValue().getBytes(Charset.defaultCharset()));
      }
    }
    return serializedContext;
  }

  @Override
  public Object deserializeContext(Map<String, byte[]> context) {
    Map<String, String> contextMap = new HashMap<>();
    for (Map.Entry<String, byte[]> entry : context.entrySet()) {
      contextMap.put(entry.getKey(), new String(entry.getValue(), Charset.defaultCharset()));
    }
    return contextMap;
  }

  @Override
  public Object getCurrentContext() {
    Map<String, String> carrier = new HashMap<>();
    w3cTraceContextPropagator.inject(Context.current(), carrier, setter);
    w3cBaggagePropagator.inject(Context.current(), carrier, setter);
    return carrier;
  }

  @Override
  public void setCurrentContext(Object context) {
    Map<String, String> contextMap = (Map<String, String>) context;
    if (contextMap != null) {
      for (Map.Entry<String, String> entry : contextMap.entrySet()) {
        MDC.put(entry.getKey(), entry.getValue());
      }
      otelKeySet.set(contextMap.keySet());
    }
  }

  @Override
  @SuppressWarnings("MustBeClosedChecker")
  public void setUp() {
    Context context =
        Baggage.fromContext(w3cBaggagePropagator.extract(Context.current(), null, getter))
            .toBuilder()
            .build()
            .storeInContext(w3cTraceContextPropagator.extract(Context.current(), null, getter));

    currentContextOtelScope.set(context.makeCurrent());

    Span span =
        GlobalOpenTelemetry.getTracer("cadence-client")
            .spanBuilder("cadence.workflow")
            .setParent(context)
            .setSpanKind(SpanKind.CLIENT)
            .startSpan();

    Scope scope = span.makeCurrent();
    currentOtelSpan.set(span);
    currentOtelScope.set(scope);
  }

  @Override
  public void finish() {
    Scope scope = currentOtelScope.get();
    if (scope != null) {
      scope.close();
    }
    Span span = currentOtelSpan.get();
    if (span != null) {
      span.end();
    }
    Scope contextScope = currentContextOtelScope.get();
    if (contextScope != null) {
      contextScope.close();
    }
  }
}
