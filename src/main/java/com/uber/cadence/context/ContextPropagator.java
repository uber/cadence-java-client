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

import java.util.Map;

public interface ContextPropagator {

  /** Returns the name of the context propagator (for use in serialization and transfer) */
  String getName();

  /** Given context data, serialize it for transmission in the Cadence header */
  Map<String, byte[]> serializeContext(Object context);

  /** Turn the serialized header data into context object(s) */
  Object deserializeContext(Map<String, byte[]> context);

  /** Returns the current context in object form */
  Object getCurrentContext();

  /** Sets the current context */
  void setCurrentContext(Object context);
}
