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

import com.uber.cadence.internal.logging.LoggerTag;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/** Support for OpenTracing spans */
public class OpenTracingContextPropagator implements ContextPropagator {

  private static final Logger log = LoggerFactory.getLogger(OpenTracingContextPropagator.class);

  private static ThreadLocal<SpanContext> currentOpenTracingSpanContext = new ThreadLocal<>();
  private static ThreadLocal<Span> currentOpenTracingSpan = new ThreadLocal<>();
  private static ThreadLocal<Scope> currentOpenTracingScope = new ThreadLocal<>();

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
    log.debug("Getting current context");
    Tracer currentTracer = GlobalTracer.get();
    Span currentSpan = currentTracer.scopeManager().activeSpan();
    if (currentSpan != null) {
      HashMapTextMap contextTextMap = new HashMapTextMap();
      currentTracer.inject(currentSpan.context(), Format.Builtin.TEXT_MAP, contextTextMap);
      log.debug(
          "Retrieving current span data as current context: " + contextTextMap.getBackingMap());
      return contextTextMap.getBackingMap();
    } else {
      return null;
    }
  }

  @Override
  public void setCurrentContext(Object context) {
    log.debug("Setting current context");
    Tracer currentTracer = GlobalTracer.get();
    Map<String, String> contextAsMap = (Map<String, String>) context;
    if (contextAsMap != null) {
      log.debug("setting current context to " + contextAsMap);
      HashMapTextMap contextTextMap = new HashMapTextMap(contextAsMap);
      setCurrentOpenTracingSpanContext(
          currentTracer.extract(Format.Builtin.TEXT_MAP, contextTextMap));
    }
  }

  @Override
  public void setUp() {
    log.debug("Starting a new opentracing span");
    Tracer openTracingTracer = GlobalTracer.get();
    Tracer.SpanBuilder builder =
        openTracingTracer
            .buildSpan("cadence.workflow")
            .withTag("resource.name", MDC.get(LoggerTag.WORKFLOW_TYPE));

    if (getCurrentOpenTracingSpanContext() != null) {
      builder.asChildOf(getCurrentOpenTracingSpanContext());
    }

    Span span = builder.start();
    log.debug("New span: " + span);
    openTracingTracer.activateSpan(span);
    currentOpenTracingSpan.set(span);
    Scope scope = openTracingTracer.activateSpan(span);
    currentOpenTracingScope.set(scope);
  }

  @Override
  public void onError(Throwable t) {
    Span span = currentOpenTracingSpan.get();
    Tags.ERROR.set(span, true);
    Map<String, Object> errorData = new HashMap<>();
    errorData.put(Fields.EVENT, "error");
    if (t != null) {
      errorData.put(Fields.ERROR_OBJECT, t);
      errorData.put(Fields.MESSAGE, t.getMessage());
    }
    span.log(errorData);
  }

  @Override
  public void finish(boolean successful) {
    Scope currentScope = currentOpenTracingScope.get();
    Span currentSpan = currentOpenTracingSpan.get();

    log.debug("Closing currently open span " + currentSpan.context().toSpanId());
    currentScope.close();
    currentSpan.finish();
    currentOpenTracingScope.remove();
    currentOpenTracingSpan.remove();
    currentOpenTracingSpanContext.remove();
  }

  /** Just check for other instances of the same class */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }

    if (this == obj) {
      return true;
    }

    if (this.getClass().equals(obj.getClass())) {
      return true;
    }

    return false;
  }

  @Override
  public int hashCode() {
    return this.getClass().hashCode();
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
