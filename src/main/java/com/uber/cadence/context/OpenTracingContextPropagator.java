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

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.util.GlobalTracer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Support for OpenTracing spans */
public class OpenTracingContextPropagator implements ContextPropagator {

  private static ThreadLocal<SpanContext> currentOpenTracingSpanContext = new ThreadLocal<>();

  public static void setCurrentOpenTracingSpanContext(SpanContext ctx) {
    if (ctx != null) {
      currentOpenTracingSpanContext.set(ctx);
    }
  }

  public static SpanContext getCurrentOpenTracingSpanContext() {
    return currentOpenTracingSpanContext.get();
  }

  @Override
  public String getName() {
    return "OpenTracing";
  }

  @Override
  public Map<String, byte[]> serializeContext(Object context) {
    Map<String, byte[]> serializedContext = new HashMap<>();
    Map<String, String> contextMap = (Map<String, String>) context;
    for (Map.Entry<String, String> entry : contextMap.entrySet()) {
      serializedContext.put(entry.getKey(), entry.getValue().getBytes(Charset.defaultCharset()));
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
    Tracer currentTracer = GlobalTracer.get();
    Span currentSpan = currentTracer.scopeManager().activeSpan();
    if (currentSpan != null) {
      HashMapTextMap contextTextMap = new HashMapTextMap();
      currentTracer.inject(currentSpan.context(), Format.Builtin.TEXT_MAP, contextTextMap);
      return contextTextMap.getBackingMap();
    } else {
      return null;
    }
  }

  @Override
  public void setCurrentContext(Object context) {
    Tracer currentTracer = GlobalTracer.get();
    Map<String, String> contextAsMap = (Map<String, String>) context;
    if (contextAsMap != null) {
      HashMapTextMap contextTextMap = new HashMapTextMap(contextAsMap);
      setCurrentOpenTracingSpanContext(
          currentTracer.extract(Format.Builtin.TEXT_MAP, contextTextMap));
    }
  }

  private class HashMapTextMap implements TextMap {

    private final HashMap<String, String> backingMap = new HashMap<>();

    public HashMapTextMap() {
      // Noop
    }

    public HashMapTextMap(Map<String, String> spanData) {
      backingMap.putAll(spanData);
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
      return backingMap.entrySet().iterator();
    }

    @Override
    public void put(String key, String value) {
      backingMap.put(key, value);
    }

    public HashMap<String, String> getBackingMap() {
      return backingMap;
    }
  }
}
