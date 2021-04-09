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
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.uber.cadence.internal.shadowing.ReplayWorkflowActivity;
import com.uber.cadence.internal.shadowing.ReplayWorkflowActivityResult;
import com.uber.cadence.internal.shadowing.ScanWorkflowActivity;
import com.uber.cadence.internal.shadowing.ScanWorkflowActivityParams;
import com.uber.cadence.internal.shadowing.ScanWorkflowActivityResult;
import com.uber.cadence.internal.shadowing.WorkflowExecution;
import com.uber.cadence.worker.ShadowingOptions;
import java.util.UUID;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

public class WorkflowShadowerTest {
  private ScanWorkflowActivity mockScanWorkflowActivity = mock(ScanWorkflowActivity.class);
  private ReplayWorkflowActivity mockReplayWorkflowActivity = mock(ReplayWorkflowActivity.class);

  @Test
  public void testRun_ReturnSuccess() throws Throwable {
    ShadowingOptions options = ShadowingOptions.defaultInstance();
    WorkflowExecution execution = new WorkflowExecution();
    execution.setWorkflowId(UUID.randomUUID().toString());
    execution.setRunId(UUID.randomUUID().toString());
    ScanWorkflowActivityResult result = new ScanWorkflowActivityResult();
    result.setExecutions(Lists.newArrayList(execution));
    when(mockScanWorkflowActivity.scan(any())).thenReturn(result);
    when(mockReplayWorkflowActivity.replayOneExecution(any(), any()))
        .thenReturn(new ReplayWorkflowActivityResult());
    WorkflowShadower shadower =
        new WorkflowShadower(options, mockScanWorkflowActivity, mockReplayWorkflowActivity);
    shadower.run();

    ScanWorkflowActivityParams scanParams = new ScanWorkflowActivityParams();
    scanParams.setDomain(options.getDomain());
    scanParams.setWorkflowQuery(options.getWorkflowQuery());
    scanParams.setSamplingRate(options.getSamplingRate());
    verify(mockScanWorkflowActivity, times(1))
        .scan(argThat(new ScanWorkflowActivityParamsMatcher(scanParams)));
    verify(mockReplayWorkflowActivity, times(1)).replayOneExecution("", execution);
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
    WorkflowExecution execution = new WorkflowExecution();
    execution.setWorkflowId(UUID.randomUUID().toString());
    execution.setRunId(UUID.randomUUID().toString());
    ScanWorkflowActivityResult result = new ScanWorkflowActivityResult();
    result.setExecutions(Lists.newArrayList(execution));
    when(mockScanWorkflowActivity.scan(any())).thenReturn(result);
    when(mockReplayWorkflowActivity.replay(any())).thenThrow(new Exception());
    WorkflowShadower shadower =
        new WorkflowShadower(options, mockScanWorkflowActivity, mockReplayWorkflowActivity);
    shadower.run();
  }

  private class ScanWorkflowActivityParamsMatcher
      extends ArgumentMatcher<ScanWorkflowActivityParams> {
    ScanWorkflowActivityParams params;

    public ScanWorkflowActivityParamsMatcher(ScanWorkflowActivityParams params) {
      this.params = params;
    }

    @Override
    public boolean matches(Object argument) {
      ScanWorkflowActivityParams newParams = (ScanWorkflowActivityParams) argument;
      return params.getDomain().equals(newParams.getDomain())
          && params.getWorkflowQuery().equals(newParams.getWorkflowQuery())
          && params.getSamplingRate() == newParams.getSamplingRate()
          && params.getPageSize() == newParams.getPageSize();
    }
  }
}
