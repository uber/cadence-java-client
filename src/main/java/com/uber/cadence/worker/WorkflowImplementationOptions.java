package com.uber.cadence.worker;

import static com.uber.cadence.worker.NonDeterministicWorkflowPolicy.NonDeterministicWorkflowPolicyBlockWorkflow;

import java.util.Objects;

public final class WorkflowImplementationOptions {

  public static final class Builder {

    private NonDeterministicWorkflowPolicy nonDeterministicWorkflowPolicy =
        NonDeterministicWorkflowPolicyBlockWorkflow;

    /**
     * Optional: Sets how decision worker deals with non-deterministic history events (presumably
     * arising from non-deterministic workflow definitions or non-backward compatible workflow
     * definition changes). default: NonDeterministicWorkflowPolicyBlockWorkflow, which just logs
     * error but reply nothing back to server.
     */
    public Builder setNonDeterministicWorkflowPolicy(
        NonDeterministicWorkflowPolicy nonDeterministicWorkflowPolicy) {
      this.nonDeterministicWorkflowPolicy = Objects.requireNonNull(nonDeterministicWorkflowPolicy);
      return this;
    }

    public WorkflowImplementationOptions build() {
      return new WorkflowImplementationOptions(nonDeterministicWorkflowPolicy);
    }
  }

  private final NonDeterministicWorkflowPolicy nonDeterministicWorkflowPolicy;

  public WorkflowImplementationOptions(
      NonDeterministicWorkflowPolicy nonDeterministicWorkflowPolicy) {
    this.nonDeterministicWorkflowPolicy = nonDeterministicWorkflowPolicy;
  }

  public NonDeterministicWorkflowPolicy getNonDeterministicWorkflowPolicy() {
    return nonDeterministicWorkflowPolicy;
  }

  @Override
  public String toString() {
    return "WorkflowImplementationOptions{"
        + "nonDeterministicWorkflowPolicy="
        + nonDeterministicWorkflowPolicy
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    WorkflowImplementationOptions that = (WorkflowImplementationOptions) o;
    return nonDeterministicWorkflowPolicy == that.nonDeterministicWorkflowPolicy;
  }

  @Override
  public int hashCode() {
    return Objects.hash(nonDeterministicWorkflowPolicy);
  }
}
