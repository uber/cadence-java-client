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
