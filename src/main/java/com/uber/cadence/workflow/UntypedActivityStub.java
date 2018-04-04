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

package com.uber.cadence.workflow;

/**
 * UntypedActivityStub is used to call an activity without referencing an interface it implements.
 * Useful to call activities when their type is not known at compile time or activities implemented
 * in other languages.
 */
public interface UntypedActivityStub {

  /**
   * Executes activity by type name and arguments.
   *
   * @param activityName name of activity type to execute.
   * @param returnType expected return type of the activity. Use Void.class for activities that
   *     return void type.
   * @param args arguments of the activity.
   * @param <R> return type.
   * @return Promise to the activity result.
   */
  <R> Promise<R> execute(String activityName, Class<R> returnType, Object... args);
}
