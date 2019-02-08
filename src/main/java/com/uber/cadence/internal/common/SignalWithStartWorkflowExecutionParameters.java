package com.uber.cadence.internal.common;

public class SignalWithStartWorkflowExecutionParameters {

  private final StartWorkflowExecutionParameters startParameters;
  private final String signalName;
  private final byte[] signalInput;

  public SignalWithStartWorkflowExecutionParameters(
      StartWorkflowExecutionParameters startParameters, String signalName, byte[] signalInput) {
    this.startParameters = startParameters;
    this.signalName = signalName;
    this.signalInput = signalInput;
  }

  public StartWorkflowExecutionParameters getStartParameters() {
    return startParameters;
  }

  public String getSignalName() {
    return signalName;
  }

  public byte[] getSignalInput() {
    return signalInput;
  }
}
