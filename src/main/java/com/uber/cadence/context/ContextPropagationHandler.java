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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContextPropagationHandler {

  private List<ContextPropagator> contextPropagators;
  private Map<String, byte[]> context;
  public static ContextPropagationHandler noopPropagationHandler =
      new ContextPropagationHandler(new ArrayList<>(), null);

  public ContextPropagationHandler(
      List<ContextPropagator> contextPropagators, Map<String, byte[]> context) {
    this.contextPropagators = contextPropagators;
    this.context = context;
  }

  public void refreshContext() {
    contextPropagators.forEach(
        contextPropagator ->
            contextPropagator.setCurrentContext(contextPropagator.deserializeContext(context)));
  }
}
