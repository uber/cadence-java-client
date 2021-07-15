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

package com.uber.cadence.internal.context;

import com.uber.cadence.context.ContextPropagator;
import com.uber.cadence.workflow.WorkflowThreadLocal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class holds the current set of context propagators. */
public class ContextThreadLocal {

  private static final Logger log = LoggerFactory.getLogger(ContextThreadLocal.class);

  private static WorkflowThreadLocal<List<ContextPropagator>> contextPropagators =
      WorkflowThreadLocal.withInitial(
          new Supplier<List<ContextPropagator>>() {
            @Override
            public List<ContextPropagator> get() {
              return new ArrayList<>();
            }
          });

  /** Sets the list of context propagators for the thread. */
  public static void setContextPropagators(List<ContextPropagator> propagators) {
    if (propagators == null || propagators.isEmpty()) {
      return;
    }
    contextPropagators.set(propagators);
  }

  public static List<ContextPropagator> getContextPropagators() {
    return contextPropagators.get();
  }

  public static Map<String, Object> getCurrentContextForPropagation() {
    Map<String, Object> contextData = new HashMap<>();
    for (ContextPropagator propagator : contextPropagators.get()) {
      contextData.put(propagator.getName(), propagator.getCurrentContext());
    }
    return contextData;
  }

  /**
   * Injects the context data into the thread for each configured context propagator.
   *
   * @param contextData The context data received from the server.
   */
  public static void propagateContextToCurrentThread(Map<String, Object> contextData) {
    if (contextData == null || contextData.isEmpty()) {
      return;
    }
    for (ContextPropagator propagator : contextPropagators.get()) {
      if (contextData.containsKey(propagator.getName())) {
        propagator.setCurrentContext(contextData.get(propagator.getName()));
      }
    }
  }

  /** Calls {@link ContextPropagator#setUp()} for each propagator. */
  public static void setUpContextPropagators() {
    for (ContextPropagator propagator : contextPropagators.get()) {
      try {
        propagator.setUp();
      } catch (Throwable t) {
        // Don't let an error in one propagator block the others
        log.error("Error calling setUp() on a contextpropagator", t);
      }
    }
  }

  /**
   * Calls {@link ContextPropagator#onError(Throwable)} for each propagator.
   *
   * @param t The Throwable that caused the workflow/activity to finish.
   */
  public static void onErrorContextPropagators(Throwable t) {
    for (ContextPropagator propagator : contextPropagators.get()) {
      try {
        propagator.onError(t);
      } catch (Throwable t1) {
        // Don't let an error in one propagator block the others
        log.error("Error calling onError() on a contextpropagator", t1);
      }
    }
  }

  /** Calls {@link ContextPropagator#finish()} for each propagator. */
  public static void finishContextPropagators() {
    for (ContextPropagator propagator : contextPropagators.get()) {
      try {
        propagator.finish();
      } catch (Throwable t) {
        // Don't let an error in one propagator block the others
        log.error("Error calling finish() on a contextpropagator", t);
      }
    }
  }
}
