/*
 *  Modifications Copyright (c) 2017-2021 Uber Technologies Inc.
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.uber.cadence.internal.shadowing;

/**
 * This class is the JSON serializable class of {@link
 * com.uber.cadence.shadower.ReplayWorkflowActivityResult} Make sure this class is sync with auto
 * generated ReplayWorkflowActivityResult
 */
public class ReplayWorkflowActivityResult {
  private int succeeded;
  private int skipped;
  private int failed;

  public ReplayWorkflowActivityResult() {}

  public int getSucceeded() {
    return succeeded;
  }

  public void setSucceeded(int succeeded) {
    this.succeeded = succeeded;
  }

  public int getSkipped() {
    return skipped;
  }

  public void setSkipped(int skipped) {
    this.skipped = skipped;
  }

  public int getFailed() {
    return failed;
  }

  public void setFailed(int failed) {
    this.failed = failed;
  }

  @Override
  public String toString() {
    return "ReplayWorkflowActivityResult{"
        + "succeeded="
        + succeeded
        + ", skipped="
        + skipped
        + ", failed="
        + failed
        + '}';
  }
}
