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
package com.uber.cadence.worker;

import static org.junit.Assert.*;

import com.google.common.collect.Sets;
import java.util.Set;
import java.util.UUID;
import org.junit.Test;

public class ShadowingOptionsTest {

  @Test
  public void testShadowingOptions_DefaultOptions() {
    ShadowingOptions shadowingOptions = ShadowingOptions.defaultInstance();
    assertEquals("", shadowingOptions.getDomain());
    assertEquals("", shadowingOptions.getWorkflowQuery());
    assertEquals(0, shadowingOptions.getWorkflowTypes().size());
    assertEquals(1.0, shadowingOptions.getSamplingRate(), 0.0);
    assertNotNull(shadowingOptions.getExitCondition());
    assertNotNull(shadowingOptions.getWorkflowStartTimeFilter());
    assertEquals(1, shadowingOptions.getWorkflowStatuses().size());
    assertTrue(shadowingOptions.getWorkflowStatuses().contains(WorkflowStatus.OPEN));
  }

  @Test
  public void testShadowingOptions() {
    String domain = UUID.randomUUID().toString();
    String query = UUID.randomUUID().toString();
    Set<String> wfTypes = Sets.newHashSet("workflowType");
    double samplingRate = 0.5;
    ShadowingExitCondition exitCondition = ShadowingExitCondition.newBuilder().build();
    TimeFilter timeFilter = TimeFilter.newBuilder().build();
    Set<WorkflowStatus> workflowStatuses = Sets.newHashSet(WorkflowStatus.CLOSED);

    ShadowingOptions shadowingOptions =
        ShadowingOptions.newBuilder()
            .setDomain(domain)
            .setWorkflowQuery(query)
            .setWorkflowTypes(wfTypes)
            .setWorkflowSamplingRate(samplingRate)
            .setExitCondition(exitCondition)
            .setWorkflowStartTimeFilter(timeFilter)
            .setWorkflowStatuses(workflowStatuses)
            .build();
    assertEquals(domain, shadowingOptions.getDomain());
    assertEquals(query, shadowingOptions.getWorkflowQuery());
    assertTrue(shadowingOptions.getWorkflowTypes().contains("workflowType"));
    assertEquals(0.5, shadowingOptions.getSamplingRate(), 0.0);
    assertEquals(exitCondition, shadowingOptions.getExitCondition());
    assertEquals(timeFilter, shadowingOptions.getWorkflowStartTimeFilter());
    assertEquals(1, shadowingOptions.getWorkflowStatuses().size());
    assertTrue(shadowingOptions.getWorkflowStatuses().contains(WorkflowStatus.CLOSED));
  }

  @Test
  public void testShadowingOptions_setWorkflowStatuses_DefaultValue() {
    ShadowingOptions shadowingOptions =
        ShadowingOptions.newBuilder().setWorkflowStatuses(Sets.newHashSet()).build();
    assertTrue(shadowingOptions.getWorkflowStatuses().contains(WorkflowStatus.OPEN));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testShadowingOptions_setDomain_ExpectedIllegalArgumentException() {
    ShadowingOptions.newBuilder().setDomain("").build();
  }

    @Test(expected = IllegalArgumentException.class)
    public void testShadowingOptions_setSamplingRate_ExpectedIllegalArgumentException() {

        try {
            ShadowingOptions.newBuilder().setWorkflowSamplingRate(0.0).build();
        } catch (Exception e) {
            assertEquals(IllegalArgumentException.class.getCanonicalName(), e.g);
        }
    }
}
