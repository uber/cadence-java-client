/*
 *  Modifications Copyright (c) 2017-2021 Uber Technologies Inc.
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.uber.cadence.testing;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.internal.shadowing.ReplayWorkflowActivity;
import com.uber.cadence.internal.shadowing.ScanWorkflowActivity;
import com.uber.cadence.shadower.ReplayWorkflowActivityParams;
import com.uber.cadence.shadower.ReplayWorkflowActivityResult;
import com.uber.cadence.shadower.ScanWorkflowActivityParams;
import com.uber.cadence.shadower.ScanWorkflowActivityResult;
import com.uber.cadence.worker.ShadowingOptions;
import java.util.List;
import java.util.UUID;
import org.junit.Test;

public class WorkflowShadowerTest {
  private ScanWorkflowActivity mockScanWorkflowActivity = mock(ScanWorkflowActivity.class);
  private ReplayWorkflowActivity mockReplayWorkflowActivity = mock(ReplayWorkflowActivity.class);

  @Test
  public void testRun_ReturnSuccess() throws Throwable {
    ShadowingOptions options = ShadowingOptions.defaultInstance();
    List<WorkflowExecution> workflowExecutions =
        Lists.newArrayList(
            new WorkflowExecution()
                .setWorkflowId(UUID.randomUUID().toString())
                .setRunId(UUID.randomUUID().toString()));
    when(mockScanWorkflowActivity.scan(any()))
        .thenReturn(new ScanWorkflowActivityResult().setExecutions(workflowExecutions));
    when(mockReplayWorkflowActivity.replay(any())).thenReturn(new ReplayWorkflowActivityResult());
    WorkflowShadower shadower =
        new WorkflowShadower(options, mockScanWorkflowActivity, mockReplayWorkflowActivity);
    shadower.run();

    ScanWorkflowActivityParams scanParams =
        new ScanWorkflowActivityParams()
            .setDomain(options.getDomain())
            .setWorkflowQuery(options.getWorkflowQuery())
            .setSamplingRate(options.getSamplingRate());
    verify(mockScanWorkflowActivity, times(1)).scan(scanParams);
    ReplayWorkflowActivityParams replayParams =
        new ReplayWorkflowActivityParams()
            .setDomain(options.getDomain())
            .setExecutions(workflowExecutions);
    verify(mockReplayWorkflowActivity, times(1)).replay(replayParams);
  }

  @Test(expected = Exception.class)
  public void testRun_CallScan_ThrowsException() throws Throwable {
    ShadowingOptions options = ShadowingOptions.defaultInstance();
    when(mockScanWorkflowActivity.scan(any())).thenThrow(new Exception());
    WorkflowShadower shadower =
        new WorkflowShadower(options, mockScanWorkflowActivity, mockReplayWorkflowActivity);
    shadower.run();
  }

  @Test(expected = Exception.class)
  public void testRun_CallReplay_ThrowsException() throws Throwable {
    ShadowingOptions options = ShadowingOptions.defaultInstance();
    List<WorkflowExecution> workflowExecutions =
        Lists.newArrayList(
            new WorkflowExecution()
                .setWorkflowId(UUID.randomUUID().toString())
                .setRunId(UUID.randomUUID().toString()));
    when(mockScanWorkflowActivity.scan(any()))
        .thenReturn(new ScanWorkflowActivityResult().setExecutions(workflowExecutions));
    when(mockReplayWorkflowActivity.replay(any())).thenThrow(new Exception());
    WorkflowShadower shadower =
        new WorkflowShadower(options, mockScanWorkflowActivity, mockReplayWorkflowActivity);
    shadower.run();
  }
}
