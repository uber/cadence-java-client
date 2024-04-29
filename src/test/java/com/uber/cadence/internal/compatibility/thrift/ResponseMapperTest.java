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

package com.uber.cadence.internal.compatibility.thrift;

import static com.uber.cadence.internal.compatibility.MapperTestUtil.assertNoMissingFields;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.uber.cadence.api.v1.DescribeDomainResponse;
import com.uber.cadence.api.v1.Domain;
import com.uber.cadence.api.v1.StartWorkflowExecutionAsyncResponse;
import com.uber.cadence.api.v1.StartWorkflowExecutionResponse;
import com.uber.cadence.api.v1.UpdateDomainResponse;
import org.junit.Assert;
import org.junit.Test;

public class ResponseMapperTest {

  @Test
  public void DescribeDomainContainsConfigurationInfo() {
    com.uber.cadence.api.v1.DescribeDomainResponse describeDomainResponse =
        DescribeDomainResponse.newBuilder().setDomain(Domain.newBuilder().build()).build();
    com.uber.cadence.DescribeDomainResponse response =
        ResponseMapper.describeDomainResponse(describeDomainResponse);
    Assert.assertNotNull(response.configuration);
    Assert.assertNotNull(response.replicationConfiguration);
  }

  @Test
  public void UpdateDomainContainsConfigurationInfo() {
    com.uber.cadence.api.v1.UpdateDomainResponse updateDomainResponse =
        UpdateDomainResponse.newBuilder().setDomain(Domain.newBuilder().build()).build();

    com.uber.cadence.UpdateDomainResponse response =
        ResponseMapper.updateDomainResponse(updateDomainResponse);

    Assert.assertNotNull(response.configuration);
    Assert.assertNotNull(response.replicationConfiguration);
  }

  @Test
  public void testStartWorkflowExecutionResponse() {
    com.uber.cadence.api.v1.StartWorkflowExecutionResponse startWorkflowExecutionResponse =
        StartWorkflowExecutionResponse.newBuilder().setRunId("runId").build();

    com.uber.cadence.StartWorkflowExecutionResponse response =
        ResponseMapper.startWorkflowExecutionResponse(startWorkflowExecutionResponse);

    assertNoMissingFields(response, com.uber.cadence.StartWorkflowExecutionResponse._Fields.class);

    assertEquals("runId", response.getRunId());
  }

  @Test
  public void testStartWorkflowExecutionAsyncResponse() {
    com.uber.cadence.api.v1.StartWorkflowExecutionAsyncResponse startWorkflowExecutionResponse =
        StartWorkflowExecutionAsyncResponse.newBuilder().build();

    com.uber.cadence.StartWorkflowExecutionAsyncResponse response =
        ResponseMapper.startWorkflowExecutionAsyncResponse(startWorkflowExecutionResponse);

    assertNoMissingFields(
        response, com.uber.cadence.StartWorkflowExecutionAsyncResponse._Fields.class);

    // No fields to test
    assertNotNull(response);
  }
}
