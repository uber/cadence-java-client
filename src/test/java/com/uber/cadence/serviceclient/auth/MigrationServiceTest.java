package com.uber.cadence.serviceclient.auth;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import com.uber.cadence.ServiceBusyError;
import com.uber.cadence.StartWorkflowExecutionRequest;
import com.uber.cadence.StartWorkflowExecutionResponse;
import com.uber.cadence.internal.metrics.NoopScope;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.serviceclient.MigrationService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class MigrationServiceTest {

  @Mock private IWorkflowService from = Mockito.mock(IWorkflowService.class);

  @Mock private IWorkflowService to = Mockito.mock(IWorkflowService.class);

  @Test
  public void testStartWorkflow() throws Exception {

    StartWorkflowExecutionResponse mockResponse = new StartWorkflowExecutionResponse();

    // Migration enabled, happy path, contact 'to' cluster
    testStartWorkflowParametrised(
        client -> {},
        client -> doReturn(mockResponse).when(client).StartWorkflowExecution(any()),
        new StartWorkflowExecutionRequest(),
        mockResponse,
        MigrationService.MigrationState.ENABLED);

    // Migration enabled, but 'to' cluster throws exception
    testStartWorkflowParametrised(
        client -> {},
        client -> doThrow(new ServiceBusyError()).when(client).StartWorkflowExecution(any()),
        new StartWorkflowExecutionRequest(),
        mockResponse,
        MigrationService.MigrationState.ENABLED);
  }

  private void testStartWorkflowParametrised(
      ThrowingConsumer<IWorkflowService> fromClientMock,
      ThrowingConsumer<IWorkflowService> toClientMock,
      StartWorkflowExecutionRequest request,
      StartWorkflowExecutionResponse expectedResponse,
      MigrationService.MigrationState migrationState)
      throws Exception {

    fromClientMock.accept(from);
    toClientMock.accept(to);

    MigrationService migrationService = new MigrationService(from, to, NoopScope.getInstance());
    migrationService.setMigrationState(migrationState);

    StartWorkflowExecutionResponse actualResponse =
        migrationService.StartWorkflowExecution(request);
    Assert.assertEquals(expectedResponse, actualResponse);
  }
}
