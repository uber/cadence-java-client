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
  @FunctionalInterface
  private interface Method<T> {
    T get() throws TException;
  }

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

    void testHelper(Method<R> method) throws TException, TChannelError {
      when(mockSubChannel.send(any(ThriftRequest.class)))
          .thenReturn(mockResponse(responseCode, responseBody));
      if (expectedException != null) {
        assertThrows(expectedException, method::get);
      } else {
        assertEquals(expectedResponse, method.get());
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
}
