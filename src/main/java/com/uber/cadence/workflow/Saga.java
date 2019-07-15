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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class Saga {
  private final Deque<Functions.Proc> compensations = new ArrayDeque<>();
  private final boolean parallelCompensation;

  public Saga(boolean parallelCompensation) {
    this.parallelCompensation = parallelCompensation;
  }

  public void addCompensation(Functions.Proc func) {
    compensations.addFirst(func);
  }

  public void compensate() {
    if (parallelCompensation) {
      List<Promise<Void>> results = new ArrayList<>();
      for (Functions.Proc f : compensations) {
        results.add(Async.procedure(f));
      }

      for (Promise<Void> p : results) {
        p.get();
      }
    } else {
      for (Functions.Proc f : compensations) {
        f.apply();
      }
    }
  }
}
