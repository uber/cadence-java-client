package com.uber.cadence.testing;

import com.uber.cadence.converter.DataConverter;
import com.uber.cadence.converter.JsonDataConverter;
import com.uber.cadence.serviceclient.IWorkflowService;
import java.util.Objects;

public class TestEnvironmentOptions {

  public static class Builder {

    private DataConverter dataConverter = JsonDataConverter.getInstance();

    private String domain = "unit-test";

    private String taskList = "unit-test";

    private IWorkflowService service;

    public Builder setDataConverter(DataConverter dataConverter) {
      Objects.requireNonNull(dataConverter);
      this.dataConverter = dataConverter;
      return this;
    }

    public Builder setDomain(String domain) {
      Objects.requireNonNull(domain);
      this.domain = domain;
      return this;
    }

    public Builder setTaskList(String taskList) {
      Objects.requireNonNull(taskList);
      this.taskList = taskList;
      return this;
    }

    public Builder setService(IWorkflowService service) {
      this.service = service;
      return this;
    }

    public TestEnvironmentOptions build() {
      return new TestEnvironmentOptions(dataConverter, domain, taskList, service);
    }
  }

  private final DataConverter dataConverter;

  private final String domain;

  private final String taskList;

  private final IWorkflowService service;

  private TestEnvironmentOptions(DataConverter dataConverter, String domain, String taskList,
      IWorkflowService service) {
    this.dataConverter = dataConverter;
    this.domain = domain;
    this.taskList = taskList;
    this.service = service;
  }

  public DataConverter getDataConverter() {
    return dataConverter;
  }

  public String getDomain() {
    return domain;
  }

  public String getTaskList() {
    return taskList;
  }

  public IWorkflowService getService() {
    return service;
  }

  @Override
  public String toString() {
    return "TestEnvironmentOptions{" +
        "dataConverter=" + dataConverter +
        ", domain='" + domain + '\'' +
        ", taskList='" + taskList + '\'' +
        ", service=" + service +
        '}';
  }
}
