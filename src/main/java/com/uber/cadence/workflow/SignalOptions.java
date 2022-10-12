package com.uber.cadence.workflow;

public class SignalOptions {

  private String domain;
  private String signalName;

  private SignalOptions() {}

  public static SignalOptions.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private SignalOptions signalOptions = new SignalOptions();

    public SignalOptions.Builder setDomain(String domain) {
      signalOptions.setDomain(domain);
      return this;
    }

    public SignalOptions.Builder setSignalName(String signalName) {
      signalOptions.setSignalName(signalName);
      return this;
    }

    public SignalOptions build() {
      if (signalOptions.getSignalName() == null) {
        throw new IllegalArgumentException("Signal name must be provided");
      }

      return signalOptions;
    }
  }

  public SignalOptions setDomain(String domain) {
    this.domain = domain;
    return this;
  }

  public String getDomain() {
    return this.domain;
  }

  public SignalOptions setSignalName(String signalName) {
    this.signalName = signalName;
    return this;
  }

  public String getSignalName() {
    return this.signalName;
  }
}
