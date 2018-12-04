package com.uber.cadence.worker;

public enum NonDeterministicWorkflowPolicy {

  /**
   * Fails decision tasks, blocking workflow progress until problem is fixed usually by rollback.
   */
  NonDeterministicWorkflowPolicyBlockWorkflow,

  /**
   * Fails a workflow instance on the first non deterministic error. Useful when workflow doesn't
   * have any important state (like cron) and is restarted automatically through {@link
   * com.uber.cadence.common.RetryOptions}.
   */
  NonDeterministicWorkflowPolicyFailWorkflow,
}
