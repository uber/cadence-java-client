package com.uber.cadence.migration;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.uber.cadence.*;
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

  @Captor private ArgumentCaptor<StartWorkflowExecutionRequest> startRequestCaptor;
  @Captor private ArgumentCaptor<ListWorkflowExecutionsRequest> listRequestCaptor;

  private MigrationIWorkflowService migrationService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    migrationService =
        new MigrationIWorkflowService(serviceOld, "domainOld", serviceNew, "domainNew");
  }

  @Test
  public void testStartWorkflowExecution_startNewWorkflow() throws TException {

    StartWorkflowExecutionRequest startRequest =
        new StartWorkflowExecutionRequest()
            .setWorkflowId("123")
            .setWorkflowType(new WorkflowType().setName("sampleWorkflow"))
            .setRequestId("123");

    StartWorkflowExecutionResponse responseNew = new StartWorkflowExecutionResponse();
    when(serviceNew.StartWorkflowExecution(startRequest)).thenReturn(responseNew);

    StartWorkflowExecutionResponse response = migrationService.StartWorkflowExecution(startRequest);

    assertEquals(responseNew, response);
    verify(serviceNew, times(1)).StartWorkflowExecution(startRequestCaptor.capture());
    assertEquals(startRequest, startRequestCaptor.getValue());
  }

  @Test
  public void testStartWorkflowExecution_startOldWorkflow() throws TException {

    StartWorkflowExecutionRequest startRequest =
        new StartWorkflowExecutionRequest()
            .setWorkflowId("1234")
            .setWorkflowType(new WorkflowType().setName("sampleWorkflow"))
            .setRequestId("123");

    // when(migrationService.shouldStartInNew(startRequest.getWorkflowId())).thenReturn(false);

    StartWorkflowExecutionResponse responseOld = new StartWorkflowExecutionResponse();
    when(serviceOld.StartWorkflowExecution(startRequest)).thenReturn(responseOld);

    // Act
    StartWorkflowExecutionResponse response = migrationService.StartWorkflowExecution(startRequest);
    // response = serviceOld.StartWorkflowExecution(startRequest);
    // Assert
    assertEquals(responseOld, response);
    verify(serviceOld, times(1)).StartWorkflowExecution(startRequestCaptor.capture());
    assertEquals(startRequest, startRequestCaptor.getValue());
  }

  @Test
  public void testListWorkflowExecutions_startNewWorkflow() throws TException {

    ListWorkflowExecutionsRequest listRequest =
        new ListWorkflowExecutionsRequest().setDomain("domain").setPageSize(10);
    listRequest.setNextPageToken("sample workflow".getBytes());

    ListWorkflowExecutionsResponse responseNew = new ListWorkflowExecutionsResponse();
    when(serviceNew.ListWorkflowExecutions(listRequest)).thenReturn(responseNew);

    ListWorkflowExecutionsResponse response = migrationService.ListWorkflowExecutions(listRequest);

    // assertNotNull("Response is null", response);
    assertEquals(responseNew, response);
    verify(serviceNew, times(1)).ListWorkflowExecutions(listRequestCaptor.capture());
    //    assertEquals(listRequest, listRequestCaptor.getValue());
  }

  @Test
  public void testListWorkflowExecutions_startOldWorkflow() throws TException {

    ListWorkflowExecutionsRequest listRequest =
        new ListWorkflowExecutionsRequest().setDomain("domain").setPageSize(10);

    ListWorkflowExecutionsResponse responseOld = new ListWorkflowExecutionsResponse();
    when(serviceOld.ListWorkflowExecutions(listRequest)).thenReturn(responseOld);

    ListWorkflowExecutionsResponse response = migrationService.ListWorkflowExecutions(listRequest);

    assertEquals(responseOld, response);
    verify(serviceOld, times(1)).ListWorkflowExecutions(listRequestCaptor.capture());
    assertEquals(listRequest, listRequestCaptor.getValue());
  }
}
