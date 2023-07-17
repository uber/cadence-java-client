package com.uber.cadence.migration;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.uber.cadence.StartWorkflowExecutionRequest;
import com.uber.cadence.StartWorkflowExecutionResponse;
import com.uber.cadence.WorkflowType;
import com.uber.cadence.serviceclient.IWorkflowService;
import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MigrationIWorkflowServiceTest {

  @Mock private IWorkflowService serviceOld;

  @Mock private IWorkflowService serviceNew;

  @Captor private ArgumentCaptor<StartWorkflowExecutionRequest> requestCaptor;

  private MigrationIWorkflowService migrationService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    migrationService =
        new MigrationIWorkflowService(serviceOld, "domainOld", serviceNew, "domainNew");
  }

  @Test
  public void testStartWorkflowExecution_WhenShouldStartInNew_ReturnsResponseFromServiceNew()
      throws TException {
    // Arrange
    StartWorkflowExecutionRequest startRequest =
        new StartWorkflowExecutionRequest()
            .setWorkflowId("123")
            .setWorkflowType(new WorkflowType().setName("sampleWorkflow"))
            .setRequestId("123");

    when(migrationService.shouldStartInNew(startRequest.getWorkflowId())).thenReturn(true);

    StartWorkflowExecutionResponse responseNew = new StartWorkflowExecutionResponse();
    when(serviceNew.StartWorkflowExecution(startRequest)).thenReturn(responseNew);

    // Act
    StartWorkflowExecutionResponse response = migrationService.StartWorkflowExecution(startRequest);

    // Assert
    assertEquals(responseNew, response);
    verify(serviceNew, times(1)).StartWorkflowExecution(requestCaptor.capture());
    assertEquals(startRequest, requestCaptor.getValue());
  }

  //  @Test
  //  public void testStartWorkflowExecution_WhenShouldStartInOld_ReturnsResponseFromServiceOld()
  //      throws TException {
  //    // Arrange
  //    StartWorkflowExecutionRequest startRequest =
  //        new StartWorkflowExecutionRequest().setWorkflowId("123").setWorkflowType("wfType");
  //
  //    when(migrationService.shouldStartInNew(startRequest.getWorkflowId())).thenReturn(false);
  //
  //    StartWorkflowExecutionResponse responseOld = new StartWorkflowExecutionResponse();
  //    when(serviceOld.StartWorkflowExecution(startRequest)).thenReturn(responseOld);
  //
  //    // Act
  //    StartWorkflowExecutionResponse response =
  // migrationService.StartWorkflowExecution(startRequest);
  //
  //    // Assert
  //    assertEquals(responseOld, response);
  //    verify(serviceOld, times(1)).StartWorkflowExecution(requestCaptor.capture());
  //    assertEquals(startRequest, requestCaptor.getValue());
  //  }
}
