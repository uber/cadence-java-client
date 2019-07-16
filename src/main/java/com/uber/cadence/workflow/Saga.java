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

import com.uber.cadence.client.BatchRequest;
import java.util.ArrayList;
import java.util.List;

public final class Saga implements BatchRequest {
  private final Options options;
  private final List<Functions.Func<Promise>> requests = new ArrayList<>();

  public static final class Options {
    private final boolean parallelCompensation;
    private final boolean continueWithError;

    private Options(boolean parallelCompensation, boolean continueWithError) {
      this.parallelCompensation = parallelCompensation;
      this.continueWithError = continueWithError;
    }

    public static final class Builder {
      private boolean parallelCompensation = true;
      private boolean continueWithError = true;

      public Builder setParallelCompensation(boolean parallelCompensation) {
        this.parallelCompensation = parallelCompensation;
        return this;
      }

      public Builder setContinueWithError(boolean continueWithError) {
        this.continueWithError = continueWithError;
        return this;
      }

      public Options build() {
        return new Options(parallelCompensation, continueWithError);
      }
    }
  }

  public Saga(Options options) {
    this.options = options;
  }

  public void compensate() {
    if (options.parallelCompensation) {
      List<Promise> results = new ArrayList<>();
      for (Functions.Func<Promise> f : requests) {
        results.add(f.apply());
      }

      RuntimeException sagaException = null;
      for (Promise p : results) {
        try {
          p.get();
        } catch (Exception e) {
          if (sagaException == null) {
            sagaException = new RuntimeException("Exception from saga compensate", e);
          } else {
            sagaException.addSuppressed(e);
          }
        }
      }

      if (sagaException != null) {
        throw sagaException;
      }
    } else {
      for (int i = requests.size() - 1; i >= 0; i--) {
        Functions.Func<Promise> f = requests.get(i);
        try {
          Promise result = f.apply();
          result.get();
        } catch (Exception e) {
          if (!options.continueWithError) {
            throw e;
          }
        }
      }
    }
  }

  @Override
  public void add(Functions.Proc request) {
    requests.add(() -> Async.procedure(request));
  }

  @Override
  public <A1> void add(Functions.Proc1<A1> request, A1 arg1) {
    requests.add(() -> Async.procedure(request, arg1));
  }

  @Override
  public <A1, A2> void add(Functions.Proc2<A1, A2> request, A1 arg1, A2 arg2) {
    requests.add(() -> Async.procedure(request, arg1, arg2));
  }

  @Override
  public <A1, A2, A3> void add(Functions.Proc3<A1, A2, A3> request, A1 arg1, A2 arg2, A3 arg3) {
    requests.add(() -> Async.procedure(request, arg1, arg2, arg3));
  }

  @Override
  public <A1, A2, A3, A4> void add(
      Functions.Proc4<A1, A2, A3, A4> request, A1 arg1, A2 arg2, A3 arg3, A4 arg4) {
    requests.add(() -> Async.procedure(request, arg1, arg2, arg3, arg4));
  }

  @Override
  public <A1, A2, A3, A4, A5> void add(
      Functions.Proc5<A1, A2, A3, A4, A5> request, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5) {
    requests.add(() -> Async.procedure(request, arg1, arg2, arg3, arg4, arg5));
  }

  @Override
  public <A1, A2, A3, A4, A5, A6> void add(
      Functions.Proc6<A1, A2, A3, A4, A5, A6> request,
      A1 arg1,
      A2 arg2,
      A3 arg3,
      A4 arg4,
      A5 arg5,
      A6 arg6) {
    requests.add(() -> Async.procedure(request, arg1, arg2, arg3, arg4, arg5, arg6));
  }

  @Override
  public void add(Functions.Func<?> request) {
    requests.add(() -> Async.function(request));
  }

  @Override
  public <A1> void add(Functions.Func1<A1, ?> request, A1 arg1) {
    requests.add(() -> Async.function(request, arg1));
  }

  @Override
  public <A1, A2> void add(Functions.Func2<A1, A2, ?> request, A1 arg1, A2 arg2) {
    requests.add(() -> Async.function(request, arg1, arg2));
  }

  @Override
  public <A1, A2, A3> void add(Functions.Func3<A1, A2, A3, ?> request, A1 arg1, A2 arg2, A3 arg3) {
    requests.add(() -> Async.function(request, arg1, arg2, arg3));
  }

  @Override
  public <A1, A2, A3, A4> void add(
      Functions.Func4<A1, A2, A3, A4, ?> request, A1 arg1, A2 arg2, A3 arg3, A4 arg4) {
    requests.add(() -> Async.function(request, arg1, arg2, arg3, arg4));
  }

  @Override
  public <A1, A2, A3, A4, A5> void add(
      Functions.Func5<A1, A2, A3, A4, A5, ?> request, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5) {
    requests.add(() -> Async.function(request, arg1, arg2, arg3, arg4, arg5));
  }

  @Override
  public <A1, A2, A3, A4, A5, A6> void add(
      Functions.Func6<A1, A2, A3, A4, A5, A6, ?> request,
      A1 arg1,
      A2 arg2,
      A3 arg3,
      A4 arg4,
      A5 arg5,
      A6 arg6) {
    requests.add(() -> Async.function(request, arg1, arg2, arg3, arg4, arg5, arg6));
  }
}
