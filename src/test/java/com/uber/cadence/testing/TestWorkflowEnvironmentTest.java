/**
 * Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * <p>Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. A copy of the License is located at
 *
 * <p>http://aws.amazon.com/apache2.0
 *
 * <p>or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.uber.cadence.testing;

import static org.junit.Assert.assertThrows;

import com.uber.cadence.*;
import com.uber.cadence.serviceclient.IWorkflowService;
import junit.framework.TestCase;

public class TestWorkflowEnvironmentTest extends TestCase {
  TestWorkflowEnvironment testEnvironment;

  public void setUp() throws Exception {
    testEnvironment = TestWorkflowEnvironment.newInstance();
  }

  public void testWorkflowService() {
    IWorkflowService service = testEnvironment.getWorkflowService();
    // unimplemented
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.RegisterDomain(new RegisterDomainRequest()));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.DescribeDomain(new DescribeDomainRequest()));
    assertThrows(
        UnsupportedOperationException.class, () -> service.ListDomains(new ListDomainsRequest()));
    assertThrows(
        UnsupportedOperationException.class, () -> service.UpdateDomain(new UpdateDomainRequest()));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.DeprecateDomain(new DeprecateDomainRequest()));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.RestartWorkflowExecution(new RestartWorkflowExecutionRequest()));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.GetTaskListsByDomain(new GetTaskListsByDomainRequest()));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.TerminateWorkflowExecution(new TerminateWorkflowExecutionRequest()));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.ListWorkflowExecutions(new ListWorkflowExecutionsRequest()));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.ListArchivedWorkflowExecutions(new ListArchivedWorkflowExecutionsRequest()));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.ScanWorkflowExecutions(new ListWorkflowExecutionsRequest()));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.CountWorkflowExecutions(new CountWorkflowExecutionsRequest()));
    assertThrows(UnsupportedOperationException.class, () -> service.GetSearchAttributes());
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.ResetStickyTaskList(new ResetStickyTaskListRequest()));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.DescribeWorkflowExecution(new DescribeWorkflowExecutionRequest()));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.DescribeTaskList(new DescribeTaskListRequest()));
    assertThrows(UnsupportedOperationException.class, () -> service.GetClusterInfo());
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.ResetStickyTaskList(new ResetStickyTaskListRequest()));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.ListTaskListPartitions(new ListTaskListPartitionsRequest()));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.RefreshWorkflowTasks(new RefreshWorkflowTasksRequest()));

    assertThrows(
        UnsupportedOperationException.class,
        () -> service.RegisterDomain(new RegisterDomainRequest(), null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.DescribeDomain(new DescribeDomainRequest(), null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.ListDomains(new ListDomainsRequest(), null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.UpdateDomain(new UpdateDomainRequest(), null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.DeprecateDomain(new DeprecateDomainRequest(), null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.RestartWorkflowExecution(new RestartWorkflowExecutionRequest(), null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.GetTaskListsByDomain(new GetTaskListsByDomainRequest(), null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.TerminateWorkflowExecution(new TerminateWorkflowExecutionRequest(), null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.ListWorkflowExecutions(new ListWorkflowExecutionsRequest(), null));
    assertThrows(
        UnsupportedOperationException.class,
        () ->
            service.ListArchivedWorkflowExecutions(
                new ListArchivedWorkflowExecutionsRequest(), null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.ScanWorkflowExecutions(new ListWorkflowExecutionsRequest(), null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.CountWorkflowExecutions(new CountWorkflowExecutionsRequest(), null));
    assertThrows(UnsupportedOperationException.class, () -> service.GetSearchAttributes(null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.ResetStickyTaskList(new ResetStickyTaskListRequest(), null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.DescribeWorkflowExecution(new DescribeWorkflowExecutionRequest(), null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.DescribeTaskList(new DescribeTaskListRequest(), null));
    assertThrows(UnsupportedOperationException.class, () -> service.GetClusterInfo(null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.ResetStickyTaskList(new ResetStickyTaskListRequest(), null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.ListTaskListPartitions(new ListTaskListPartitionsRequest(), null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.RefreshWorkflowTasks(new RefreshWorkflowTasksRequest(), null));
  }
}
