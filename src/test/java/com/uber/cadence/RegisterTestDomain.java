package com.uber.cadence;

import static com.uber.cadence.workflow.WorkflowTest.DOMAIN;

import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.serviceclient.WorkflowServiceTChannel;
import org.apache.thrift.TException;

/** Waits for local service to become available and registers UnitTest domain. */
public class RegisterTestDomain {

  public static void main(String[] args) throws TException, InterruptedException {
    IWorkflowService service = new WorkflowServiceTChannel();
    RegisterDomainRequest request =
        new RegisterDomainRequest().setName(DOMAIN).setWorkflowExecutionRetentionPeriodInDays(0);
    while (true) {
      try {
        service.RegisterDomain(request);
        break;
      } catch (DomainAlreadyExistsError e) {
        break;
      } catch (TException e) {
        String message = e.getMessage();
        if (message != null
            && !message.contains("Failed to connect to the host")
            && !message.contains("Connection timeout on identification")) {
          e.printStackTrace();
        }
        Thread.sleep(500);
        continue;
      } catch (Throwable e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
    System.exit(0);
  }
}
