package com.uber.cadence.serviceclient.auth;

import com.uber.cadence.StartWorkflowExecutionRequest;
import com.uber.cadence.StartWorkflowExecutionResponse;
import com.uber.cadence.internal.metrics.NoopScope;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.serviceclient.MigrationService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

public class MigrationServiceTest {

    @Mock
    private IWorkflowService from = Mockito.mock(IWorkflowService.class);

    @Mock
    private IWorkflowService to = Mockito.mock(IWorkflowService.class);

    @Test
    public void testStartWorkflow() throws Exception {
        MigrationService migrationService = new MigrationService(from, to, NoopScope.getInstance());

        StartWorkflowExecutionResponse mockResponse = new StartWorkflowExecutionResponse();
        doReturn(mockResponse).when(to).StartWorkflowExecution(any());

        StartWorkflowExecutionRequest startRequest = new StartWorkflowExecutionRequest();
        StartWorkflowExecutionResponse response = migrationService.StartWorkflowExecution(startRequest);
        Assert.assertNotNull(response);
    }
}
