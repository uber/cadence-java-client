/*
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.uber.cadence.internal.worker;

import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowClientOptions;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.worker.WorkerFactory;
import com.uber.m3.tally.NoopScope;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WorkerShutDownHandlerTest {

    @Mock
    private WorkflowClient mockClient;

    @Mock
    private IWorkflowService mockService;

    @Before
    public void setup() {
        WorkflowClientOptions clientOptions =
                WorkflowClientOptions.newBuilder().setMetricsScope(new NoopScope()).build();
        when(mockClient.getOptions()).thenReturn(clientOptions);
        when(mockClient.getService()).thenReturn(mockService);
    }

    @Test
    public void shutDownHookShutsDownFactories() {
        WorkerShutDownHandler.registerHandler();

        WorkerFactory workerFactory = WorkerFactory.newInstance(mockClient);
        workerFactory.newWorker("TL1");
        workerFactory.newWorker("TL2");

        WorkerFactory workerFactory2 = WorkerFactory.newInstance(mockClient);
        workerFactory2.newWorker("TL3");

        WorkerShutDownHandler.execute();

        assertTrue(workerFactory.isShutdown());
        assertTrue(workerFactory2.isShutdown());
    }

}