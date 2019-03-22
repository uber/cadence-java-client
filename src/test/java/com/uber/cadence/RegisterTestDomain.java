package com.uber.cadence;

import static com.uber.cadence.workflow.WorkflowTest.DOMAIN;

import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.serviceclient.WorkflowServiceTChannel;
import org.apache.thrift.TException;

public class RegisterTestDomain {

  public static void main(String[] args) throws TException {
    IWorkflowService service = new WorkflowServiceTChannel();
    RegisterDomainRequest request =
        new RegisterDomainRequest().setName(DOMAIN).setWorkflowExecutionRetentionPeriodInDays(0);
    try {
      service.RegisterDomain(request);
    } catch (DomainAlreadyExistsError e) {
    } catch (Throwable e) {
        e.printStackTrace();
        System.exit(1);
    }
    System.exit(0);
  }
}
