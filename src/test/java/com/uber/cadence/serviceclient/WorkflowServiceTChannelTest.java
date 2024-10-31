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
package com.uber.cadence.serviceclient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

import com.uber.cadence.*;
import com.uber.tchannel.api.ResponseCode;
import com.uber.tchannel.api.SubChannel;
import com.uber.tchannel.api.TFuture;
import com.uber.tchannel.api.errors.TChannelError;
import com.uber.tchannel.headers.ArgScheme;
import com.uber.tchannel.messages.ThriftRequest;
import com.uber.tchannel.messages.ThriftResponse;
import java.util.Arrays;
import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Enclosed.class)
public class WorkflowServiceTChannelTest {

  private static class BaseTest<T, R> {
    SubChannel mockSubChannel;
    WorkflowServiceTChannel service;

    @Before
    public void setUp() {
      mockSubChannel = mock(SubChannel.class);
      service = new WorkflowServiceTChannel(mockSubChannel, ClientOptions.newBuilder().build());
    }

    @Parameterized.Parameter(0)
    public ResponseCode responseCode;

    @Parameterized.Parameter(1)
    public T responseBody;

    @Parameterized.Parameter(2)
    public Class<? extends TException> expectedException;

    @Parameterized.Parameter(3)
    public R expectedResponse;

    TFuture<ThriftResponse<T>> mockResponse(ResponseCode responseCode, T body) {
      // TFuture
      TFuture<ThriftResponse<T>> tFuture = TFuture.create(ArgScheme.THRIFT, null);
      tFuture.set(
          new ThriftResponse.Builder<T>(new ThriftRequest.Builder<>("", "").build())
              .setResponseCode(responseCode)
              .setBody(body)
              .build());
      return tFuture;
    }

    void testHelper(WorkflowServiceTChannel.RemoteCall<R> method) throws TException, TChannelError {
      when(mockSubChannel.send(any(ThriftRequest.class)))
          .thenReturn(mockResponse(responseCode, responseBody));
      if (expectedException != null) {
        assertThrows(expectedException, method::apply);
      } else {
        assertEquals(expectedResponse, method.apply());
      }
    }
  }

  @RunWith(Parameterized.class)
  public static class RegisterDomainTest
      extends BaseTest<WorkflowService.RegisterDomain_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {ResponseCode.OK, new WorkflowService.RegisterDomain_result(), null, null},
            {
              ResponseCode.Error,
              new WorkflowService.RegisterDomain_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.RegisterDomain_result()
                  .setDomainExistsError(new DomainAlreadyExistsError("")),
              DomainAlreadyExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RegisterDomain_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RegisterDomain_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(
          () -> {
            service.RegisterDomain(new RegisterDomainRequest());
            return null;
          });
    }
  }

  @RunWith(Parameterized.class)
  public static class DescribeDomainTest
      extends BaseTest<WorkflowService.DescribeDomain_result, DescribeDomainResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.DescribeDomain_result().setSuccess(new DescribeDomainResponse()),
              null,
              new DescribeDomainResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.DescribeDomain_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              new DescribeDomainResponse(),
            },
            {
              ResponseCode.Error,
              new WorkflowService.DescribeDomain_result()
                  .setSuccess(new DescribeDomainResponse())
                  .setEntityNotExistError(new EntityNotExistsError("")),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.DescribeDomain_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.DescribeDomain_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.DescribeDomain(new DescribeDomainRequest()));
    }
  }

  @RunWith(Parameterized.class)
  public static class ListDomainsTest
      extends BaseTest<WorkflowService.ListDomains_result, ListDomainsResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.ListDomains_result().setSuccess(new ListDomainsResponse()),
              null,
              new ListDomainsResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListDomains_result().setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListDomains_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {ResponseCode.Error, new WorkflowService.ListDomains_result(), TException.class, null},
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.ListDomains(new ListDomainsRequest()));
    }
  }

  @RunWith(Parameterized.class)
  public static class ResetWorkflowExecutionTest
      extends BaseTest<
          WorkflowService.ResetWorkflowExecution_result, ResetWorkflowExecutionResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.ResetWorkflowExecution_result()
                  .setSuccess(new ResetWorkflowExecutionResponse()),
              null,
              new ResetWorkflowExecutionResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.ResetWorkflowExecution_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.ResetWorkflowExecution_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ResetWorkflowExecution_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.ResetWorkflowExecution(new ResetWorkflowExecutionRequest()));
    }
  }

  @RunWith(Parameterized.class)
  public static class TerminateWorkflowExecutionTest
      extends BaseTest<WorkflowService.TerminateWorkflowExecution_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {ResponseCode.OK, new WorkflowService.TerminateWorkflowExecution_result(), null, null},
            {
              ResponseCode.Error,
              new WorkflowService.TerminateWorkflowExecution_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.TerminateWorkflowExecution_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.TerminateWorkflowExecution_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(
          () -> {
            service.TerminateWorkflowExecution(new TerminateWorkflowExecutionRequest());
            return null;
          });
    }
  }

  @RunWith(Parameterized.class)
  public static class ListOpenWorkflowExecutionsTest
      extends BaseTest<
          WorkflowService.ListOpenWorkflowExecutions_result, ListOpenWorkflowExecutionsResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.ListOpenWorkflowExecutions_result()
                  .setSuccess(new ListOpenWorkflowExecutionsResponse()),
              null,
              new ListOpenWorkflowExecutionsResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListOpenWorkflowExecutions_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListOpenWorkflowExecutions_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListOpenWorkflowExecutions_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.ListOpenWorkflowExecutions(new ListOpenWorkflowExecutionsRequest()));
    }
  }

  @RunWith(Parameterized.class)
  public static class ListClosedWorkflowExecutionsTest
      extends BaseTest<
          WorkflowService.ListClosedWorkflowExecutions_result,
          ListClosedWorkflowExecutionsResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.ListClosedWorkflowExecutions_result()
                  .setSuccess(new ListClosedWorkflowExecutionsResponse()),
              null,
              new ListClosedWorkflowExecutionsResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListClosedWorkflowExecutions_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListClosedWorkflowExecutions_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListClosedWorkflowExecutions_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(
          () -> service.ListClosedWorkflowExecutions(new ListClosedWorkflowExecutionsRequest()));
    }
  }

  @RunWith(Parameterized.class)
  public static class ListWorkflowExecutionsTest
      extends BaseTest<
          WorkflowService.ListWorkflowExecutions_result, ListWorkflowExecutionsResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.ListWorkflowExecutions_result()
                  .setSuccess(new ListWorkflowExecutionsResponse()),
              null,
              new ListWorkflowExecutionsResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListWorkflowExecutions_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListWorkflowExecutions_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListWorkflowExecutions_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.ListWorkflowExecutions(new ListWorkflowExecutionsRequest()));
    }
  }

  @RunWith(Parameterized.class)
  public static class ListArchivedWorkflowExecutionsTest
      extends BaseTest<
          WorkflowService.ListArchivedWorkflowExecutions_result,
          ListArchivedWorkflowExecutionsResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.ListArchivedWorkflowExecutions_result()
                  .setSuccess(new ListArchivedWorkflowExecutionsResponse()),
              null,
              new ListArchivedWorkflowExecutionsResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListArchivedWorkflowExecutions_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListArchivedWorkflowExecutions_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListArchivedWorkflowExecutions_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(
          () ->
              service.ListArchivedWorkflowExecutions(new ListArchivedWorkflowExecutionsRequest()));
    }
  }

  @RunWith(Parameterized.class)
  public static class ScanWorkflowExecutionsTest
      extends BaseTest<
          WorkflowService.ScanWorkflowExecutions_result, ListWorkflowExecutionsResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.ScanWorkflowExecutions_result()
                  .setSuccess(new ListWorkflowExecutionsResponse()),
              null,
              new ListWorkflowExecutionsResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.ScanWorkflowExecutions_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.ScanWorkflowExecutions_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ScanWorkflowExecutions_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.ScanWorkflowExecutions(new ListWorkflowExecutionsRequest()));
    }
  }

  @RunWith(Parameterized.class)
  public static class CountWorkflowExecutionsTest
      extends BaseTest<
          WorkflowService.CountWorkflowExecutions_result, CountWorkflowExecutionsResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.CountWorkflowExecutions_result()
                  .setSuccess(new CountWorkflowExecutionsResponse()),
              null,
              new CountWorkflowExecutionsResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.CountWorkflowExecutions_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.CountWorkflowExecutions_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.CountWorkflowExecutions_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.CountWorkflowExecutions(new CountWorkflowExecutionsRequest()));
    }
  }

  @RunWith(Parameterized.class)
  public static class GetSearchAttributesTest
      extends BaseTest<WorkflowService.GetSearchAttributes_result, GetSearchAttributesResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.GetSearchAttributes_result()
                  .setSuccess(new GetSearchAttributesResponse()),
              null,
              new GetSearchAttributesResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetSearchAttributes_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetSearchAttributes_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetSearchAttributes_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.GetSearchAttributes());
    }
  }

  @RunWith(Parameterized.class)
  public static class RespondQueryTaskCompletedTest
      extends BaseTest<WorkflowService.RespondQueryTaskCompleted_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {ResponseCode.OK, new WorkflowService.RespondQueryTaskCompleted_result(), null, null},
            {
              ResponseCode.Error,
              new WorkflowService.RespondQueryTaskCompleted_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondQueryTaskCompleted_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondQueryTaskCompleted_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(
          () -> {
            service.RespondQueryTaskCompleted(new RespondQueryTaskCompletedRequest());
            return null;
          });
    }
  }

  @RunWith(Parameterized.class)
  public static class ResetStickyTaskListTest
      extends BaseTest<WorkflowService.ResetStickyTaskList_result, ResetStickyTaskListResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.ResetStickyTaskList_result()
                  .setSuccess(new ResetStickyTaskListResponse()),
              null,
              new ResetStickyTaskListResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.ResetStickyTaskList_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.ResetStickyTaskList_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ResetStickyTaskList_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.ResetStickyTaskList(new ResetStickyTaskListRequest()));
    }
  }

  @RunWith(Parameterized.class)
  public static class QueryWorkflowTest
      extends BaseTest<WorkflowService.QueryWorkflow_result, QueryWorkflowResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.QueryWorkflow_result().setSuccess(new QueryWorkflowResponse()),
              null,
              new QueryWorkflowResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.QueryWorkflow_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.QueryWorkflow_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error, new WorkflowService.QueryWorkflow_result(), TException.class, null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.QueryWorkflow(new QueryWorkflowRequest()));
    }
  }

  @RunWith(Parameterized.class)
  public static class DescribeWorkflowExecutionTest
      extends BaseTest<
          WorkflowService.DescribeWorkflowExecution_result, DescribeWorkflowExecutionResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.DescribeWorkflowExecution_result()
                  .setSuccess(new DescribeWorkflowExecutionResponse()),
              null,
              new DescribeWorkflowExecutionResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.DescribeWorkflowExecution_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.DescribeWorkflowExecution_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.DescribeWorkflowExecution_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.DescribeWorkflowExecution(new DescribeWorkflowExecutionRequest()));
    }
  }

  @RunWith(Parameterized.class)
  public static class DescribeTaskListTest
      extends BaseTest<WorkflowService.DescribeTaskList_result, DescribeTaskListResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.DescribeTaskList_result()
                  .setSuccess(new DescribeTaskListResponse()),
              null,
              new DescribeTaskListResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.DescribeTaskList_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.DescribeTaskList_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.DescribeTaskList_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.DescribeTaskList(new DescribeTaskListRequest()));
    }
  }

  @RunWith(Parameterized.class)
  public static class GetClusterInfoTest
      extends BaseTest<WorkflowService.GetClusterInfo_result, ClusterInfo> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.GetClusterInfo_result().setSuccess(new ClusterInfo()),
              null,
              new ClusterInfo()
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetClusterInfo_result()
                  .setInternalServiceError(new InternalServiceError("")),
              InternalServiceError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetClusterInfo_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetClusterInfo_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.GetClusterInfo());
    }
  }

  @RunWith(Parameterized.class)
  public static class ListTaskListPartitionsTest
      extends BaseTest<
          WorkflowService.ListTaskListPartitions_result, ListTaskListPartitionsResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.ListTaskListPartitions_result()
                  .setSuccess(new ListTaskListPartitionsResponse()),
              null,
              new ListTaskListPartitionsResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListTaskListPartitions_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListTaskListPartitions_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListTaskListPartitions_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.ListTaskListPartitions(new ListTaskListPartitionsRequest()));
    }
  }

  @RunWith(Parameterized.class)
  public static class UpdateDomainTest
      extends BaseTest<WorkflowService.UpdateDomain_result, UpdateDomainResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.UpdateDomain_result().setSuccess(new UpdateDomainResponse()),
              null,
              new UpdateDomainResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.UpdateDomain_result().setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.UpdateDomain_result()
                  .setEntityNotExistError(new EntityNotExistsError("")),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.UpdateDomain_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.UpdateDomain_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {ResponseCode.Error, new WorkflowService.UpdateDomain_result(), TException.class, null},
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(() -> service.UpdateDomain(new UpdateDomainRequest()));
    }
  }

  @RunWith(Parameterized.class)
  public static class GetWorkflowExecutionHistoryWithTimeoutTest
      extends BaseTest<
          WorkflowService.GetWorkflowExecutionHistory_result, GetWorkflowExecutionHistoryResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.GetWorkflowExecutionHistory_result()
                  .setSuccess(new GetWorkflowExecutionHistoryResponse()),
              null,
              new GetWorkflowExecutionHistoryResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetWorkflowExecutionHistory_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetWorkflowExecutionHistory_result()
                  .setEntityNotExistError(new EntityNotExistsError("")),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetWorkflowExecutionHistory_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetWorkflowExecutionHistory_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetWorkflowExecutionHistory_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () ->
              service.GetWorkflowExecutionHistoryWithTimeout(
                  new GetWorkflowExecutionHistoryRequest(), 1000L));
    }
  }

  @RunWith(Parameterized.class)
  public static class StartWorkflowExecutionAsyncTest
      extends BaseTest<
          WorkflowService.StartWorkflowExecutionAsync_result, StartWorkflowExecutionAsyncResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.StartWorkflowExecutionAsync_result()
                  .setSuccess(new StartWorkflowExecutionAsyncResponse()),
              null,
              new StartWorkflowExecutionAsyncResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecutionAsync_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecutionAsync_result()
                  .setSessionAlreadyExistError(new WorkflowExecutionAlreadyStartedError()),
              WorkflowExecutionAlreadyStartedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecutionAsync_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecutionAsync_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecutionAsync_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecutionAsync_result()
                  .setEntityNotExistError(new EntityNotExistsError("")),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecutionAsync_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecutionAsync_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () ->
              service.StartWorkflowExecutionAsync(
                  new StartWorkflowExecutionAsyncRequest()
                      .setRequest(new StartWorkflowExecutionRequest())));
    }
  }

  @RunWith(Parameterized.class)
  public static class StartWorkflowExecutionTest
      extends BaseTest<
          WorkflowService.StartWorkflowExecution_result, StartWorkflowExecutionResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.StartWorkflowExecution_result()
                  .setSuccess(new StartWorkflowExecutionResponse()),
              null,
              new StartWorkflowExecutionResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecution_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecution_result()
                  .setSessionAlreadyExistError(new WorkflowExecutionAlreadyStartedError()),
              WorkflowExecutionAlreadyStartedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecution_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecution_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecution_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecution_result()
                  .setEntityNotExistError(new EntityNotExistsError("")),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecution_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecution_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(() -> service.StartWorkflowExecution(new StartWorkflowExecutionRequest()));
    }
  }

  @RunWith(Parameterized.class)
  public static class GetTaskListsByDomainTest
      extends BaseTest<WorkflowService.GetTaskListsByDomain_result, GetTaskListsByDomainResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.GetTaskListsByDomain_result()
                  .setSuccess(new GetTaskListsByDomainResponse()),
              null,
              new GetTaskListsByDomainResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetTaskListsByDomain_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetTaskListsByDomain_result()
                  .setEntityNotExistError(new EntityNotExistsError("")),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetTaskListsByDomain_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetTaskListsByDomain_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetTaskListsByDomain_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetTaskListsByDomain_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(() -> service.GetTaskListsByDomain(new GetTaskListsByDomainRequest()));
    }
  }

  @RunWith(Parameterized.class)
  public static class DeprecateDomainTest
      extends BaseTest<WorkflowService.DeprecateDomain_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {ResponseCode.OK, new WorkflowService.DeprecateDomain_result(), null, null},
            {
              ResponseCode.Error,
              new WorkflowService.DeprecateDomain_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.DeprecateDomain_result()
                  .setEntityNotExistError(new EntityNotExistsError("")),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.DeprecateDomain_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.DeprecateDomain_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.DeprecateDomain_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> {
            service.DeprecateDomain(new DeprecateDomainRequest());
            return null;
          });
    }
  }

  @RunWith(Parameterized.class)
  public static class PollForDecisionTaskTest
      extends BaseTest<WorkflowService.PollForDecisionTask_result, PollForDecisionTaskResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.PollForDecisionTask_result()
                  .setSuccess(new PollForDecisionTaskResponse()),
              null,
              new PollForDecisionTaskResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.PollForDecisionTask_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.PollForDecisionTask_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.PollForDecisionTask_result()
                  .setEntityNotExistError(new EntityNotExistsError("")),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.PollForDecisionTask_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(() -> service.PollForDecisionTask(new PollForDecisionTaskRequest()));
    }
  }

  @RunWith(Parameterized.class)
  public static class RespondDecisionTaskCompletedTest
      extends BaseTest<
          WorkflowService.RespondDecisionTaskCompleted_result,
          RespondDecisionTaskCompletedResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.RespondDecisionTaskCompleted_result()
                  .setSuccess(new RespondDecisionTaskCompletedResponse()),
              null,
              new RespondDecisionTaskCompletedResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondDecisionTaskCompleted_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondDecisionTaskCompleted_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondDecisionTaskCompleted_result()
                  .setEntityNotExistError(new EntityNotExistsError("")),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondDecisionTaskCompleted_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> service.RespondDecisionTaskCompleted(new RespondDecisionTaskCompletedRequest()));
    }
  }

  @RunWith(Parameterized.class)
  public static class PollForActivityTaskTest
      extends BaseTest<WorkflowService.PollForActivityTask_result, PollForActivityTaskResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.PollForActivityTask_result()
                  .setSuccess(new PollForActivityTaskResponse()),
              null,
              new PollForActivityTaskResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.PollForActivityTask_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.PollForActivityTask_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.PollForActivityTask_result()
                  .setEntityNotExistError(new EntityNotExistsError("")),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.PollForActivityTask_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(() -> service.PollForActivityTask(new PollForActivityTaskRequest()));
    }
  }

  @RunWith(Parameterized.class)
  public static class RecordActivityTaskHeartbeatTest
      extends BaseTest<
          WorkflowService.RecordActivityTaskHeartbeat_result, RecordActivityTaskHeartbeatResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.RecordActivityTaskHeartbeat_result()
                  .setSuccess(new RecordActivityTaskHeartbeatResponse()),
              null,
              new RecordActivityTaskHeartbeatResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.RecordActivityTaskHeartbeat_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RecordActivityTaskHeartbeat_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RecordActivityTaskHeartbeat_result()
                  .setEntityNotExistError(new EntityNotExistsError("")),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RecordActivityTaskHeartbeat_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> service.RecordActivityTaskHeartbeat(new RecordActivityTaskHeartbeatRequest()));
    }
  }

  @RunWith(Parameterized.class)
  public static class RecordActivityTaskHeartbeatByIDTest
      extends BaseTest<
          WorkflowService.RecordActivityTaskHeartbeatByID_result,
          RecordActivityTaskHeartbeatResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.RecordActivityTaskHeartbeatByID_result()
                  .setSuccess(new RecordActivityTaskHeartbeatResponse()),
              null,
              new RecordActivityTaskHeartbeatResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.RecordActivityTaskHeartbeatByID_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RecordActivityTaskHeartbeatByID_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RecordActivityTaskHeartbeatByID_result()
                  .setEntityNotExistError(new EntityNotExistsError("")),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RecordActivityTaskHeartbeatByID_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () ->
              service.RecordActivityTaskHeartbeatByID(
                  new RecordActivityTaskHeartbeatByIDRequest()));
    }
  }

  @RunWith(Parameterized.class)
  public static class RespondActivityTaskCompletedTest
      extends BaseTest<WorkflowService.RespondActivityTaskCompleted_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK, new WorkflowService.RespondActivityTaskCompleted_result(), null, null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCompleted_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCompleted_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCompleted_result()
                  .setEntityNotExistError(new EntityNotExistsError("")),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCompleted_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> {
            service.RespondActivityTaskCompleted(new RespondActivityTaskCompletedRequest());
            return null;
          });
    }
  }

  @RunWith(Parameterized.class)
  public static class RespondActivityTaskCompletedByIDTest
      extends BaseTest<WorkflowService.RespondActivityTaskCompletedByID_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.RespondActivityTaskCompletedByID_result(),
              null,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCompletedByID_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCompletedByID_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCompletedByID_result()
                  .setEntityNotExistError(new EntityNotExistsError("")),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCompletedByID_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> {
            service.RespondActivityTaskCompletedByID(new RespondActivityTaskCompletedByIDRequest());
            return null;
          });
    }
  }

  @RunWith(Parameterized.class)
  public static class RespondActivityTaskFailedTest
      extends BaseTest<WorkflowService.RespondActivityTaskFailed_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {ResponseCode.OK, new WorkflowService.RespondActivityTaskFailed_result(), null, null},
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskFailed_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskFailed_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskFailed_result()
                  .setEntityNotExistError(new EntityNotExistsError("")),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskFailed_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> {
            service.RespondActivityTaskFailed(new RespondActivityTaskFailedRequest());
            return null;
          });
    }
  }

  @RunWith(Parameterized.class)
  public static class RespondActivityTaskFailedByIDTest
      extends BaseTest<WorkflowService.RespondActivityTaskFailedByID_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.RespondActivityTaskFailedByID_result(),
              null,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskFailedByID_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskFailedByID_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskFailedByID_result()
                  .setEntityNotExistError(new EntityNotExistsError("")),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskFailedByID_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> {
            service.RespondActivityTaskFailedByID(new RespondActivityTaskFailedByIDRequest());
            return null;
          });
    }
  }

  @RunWith(Parameterized.class)
  public static class RespondActivityTaskCanceledTest
      extends BaseTest<WorkflowService.RespondActivityTaskCanceled_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {ResponseCode.OK, new WorkflowService.RespondActivityTaskCanceled_result(), null, null},
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCanceled_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCanceled_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCanceled_result()
                  .setEntityNotExistError(new EntityNotExistsError("")),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCanceled_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> {
            service.RespondActivityTaskCanceled(new RespondActivityTaskCanceledRequest());
            return null;
          });
    }
  }

  @RunWith(Parameterized.class)
  public static class RespondActivityTaskCanceledByIDTest
      extends BaseTest<WorkflowService.RespondActivityTaskCanceledByID_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.RespondActivityTaskCanceledByID_result(),
              null,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCanceledByID_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCanceledByID_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCanceledByID_result()
                  .setEntityNotExistError(new EntityNotExistsError("")),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCanceledByID_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> {
            service.RespondActivityTaskCanceledByID(new RespondActivityTaskCanceledByIDRequest());
            return null;
          });
    }
  }

  @RunWith(Parameterized.class)
  public static class RequestCancelWorkflowExecutionTest
      extends BaseTest<WorkflowService.RequestCancelWorkflowExecution_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.RequestCancelWorkflowExecution_result(),
              null,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RequestCancelWorkflowExecution_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RequestCancelWorkflowExecution_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RequestCancelWorkflowExecution_result()
                  .setEntityNotExistError(new EntityNotExistsError("")),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RequestCancelWorkflowExecution_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> {
            service.RequestCancelWorkflowExecution(new RequestCancelWorkflowExecutionRequest());
            return null;
          });
    }
  }

  @RunWith(Parameterized.class)
  public static class SignalWorkflowExecutionTest
      extends BaseTest<WorkflowService.SignalWorkflowExecution_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {ResponseCode.OK, new WorkflowService.SignalWorkflowExecution_result(), null, null},
            {
              ResponseCode.Error,
              new WorkflowService.SignalWorkflowExecution_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWorkflowExecution_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWorkflowExecution_result()
                  .setEntityNotExistError(new EntityNotExistsError("")),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWorkflowExecution_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> {
            service.SignalWorkflowExecution(new SignalWorkflowExecutionRequest());
            return null;
          });
    }
  }

  @RunWith(Parameterized.class)
  public static class SignalWithStartWorkflowExecutionTest
      extends BaseTest<
          WorkflowService.SignalWithStartWorkflowExecution_result, StartWorkflowExecutionResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.SignalWithStartWorkflowExecution_result()
                  .setSuccess(new StartWorkflowExecutionResponse()),
              null,
              new StartWorkflowExecutionResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWithStartWorkflowExecution_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWithStartWorkflowExecution_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWithStartWorkflowExecution_result()
                  .setEntityNotExistError(new EntityNotExistsError("")),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWithStartWorkflowExecution_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () ->
              service.SignalWithStartWorkflowExecution(
                  new SignalWithStartWorkflowExecutionRequest()));
    }
  }

  @RunWith(Parameterized.class)
  public static class RefreshWorkflowTasksTest
      extends BaseTest<WorkflowService.RefreshWorkflowTasks_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {ResponseCode.OK, new WorkflowService.RefreshWorkflowTasks_result(), null, null},
            {
              ResponseCode.Error,
              new WorkflowService.RefreshWorkflowTasks_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RefreshWorkflowTasks_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RefreshWorkflowTasks_result()
                  .setEntityNotExistError(new EntityNotExistsError("")),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RefreshWorkflowTasks_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RefreshWorkflowTasks_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> {
            service.RefreshWorkflowTasks(new RefreshWorkflowTasksRequest());
            return null;
          });
    }
  }

  @RunWith(Parameterized.class)
  public static class SignalWithStartWorkflowExecutionAsyncTest
      extends BaseTest<
          WorkflowService.SignalWithStartWorkflowExecutionAsync_result,
          SignalWithStartWorkflowExecutionAsyncResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.SignalWithStartWorkflowExecutionAsync_result()
                  .setSuccess(new SignalWithStartWorkflowExecutionAsyncResponse()),
              null,
              new SignalWithStartWorkflowExecutionAsyncResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWithStartWorkflowExecutionAsync_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWithStartWorkflowExecutionAsync_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWithStartWorkflowExecutionAsync_result()
                  .setEntityNotExistError(new EntityNotExistsError("")),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWithStartWorkflowExecutionAsync_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () ->
              service.SignalWithStartWorkflowExecutionAsync(
                  new SignalWithStartWorkflowExecutionAsyncRequest()
                      .setRequest(new SignalWithStartWorkflowExecutionRequest())));
    }
  }

  @RunWith(Parameterized.class)
  public static class RespondDecisionTaskFailedTest
      extends BaseTest<WorkflowService.RespondDecisionTaskFailed_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {ResponseCode.OK, new WorkflowService.RespondDecisionTaskFailed_result(), null, null},
            {
              ResponseCode.Error,
              new WorkflowService.RespondDecisionTaskFailed_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondDecisionTaskFailed_result()
                  .setEntityNotExistError(new EntityNotExistsError("")),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondDecisionTaskFailed_result()
                  .setWorkflowExecutionAlreadyCompletedError(
                      new WorkflowExecutionAlreadyCompletedError("")),
              WorkflowExecutionAlreadyCompletedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondDecisionTaskFailed_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondDecisionTaskFailed_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> {
            service.RespondDecisionTaskFailed(new RespondDecisionTaskFailedRequest());
            return null;
          });
    }
  }
}
