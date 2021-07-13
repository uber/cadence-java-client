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

package com.uber.cadence.serviceclient;

import com.uber.cadence.GetWorkflowExecutionHistoryRequest;
import com.uber.cadence.GetWorkflowExecutionHistoryResponse;
import com.uber.cadence.SignalWorkflowExecutionRequest;
import com.uber.cadence.StartWorkflowExecutionRequest;
import com.uber.cadence.WorkflowService.AsyncIface;
import com.uber.cadence.WorkflowService.Iface;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

public interface IWorkflowService extends Iface, AsyncIface {
  void close();

  /**
   * StartWorkflowExecutionWithTimeout start workflow same as StartWorkflowExecution but with
   * timeout
   *
   * @param startRequest
   * @param resultHandler
   * @param timeoutInMillis
   * @throws TException
   */
  void StartWorkflowExecutionWithTimeout(
      StartWorkflowExecutionRequest startRequest,
      AsyncMethodCallback resultHandler,
      Long timeoutInMillis)
      throws TException;

  /**
   * GetWorkflowExecutionHistoryWithTimeout get workflow history same as GetWorkflowExecutionHistory
   * but with timeout.
   *
   * @param getRequest
   * @param timeoutInMillis
   * @return GetWorkflowExecutionHistoryResponse
   * @throws TException
   */
  GetWorkflowExecutionHistoryResponse GetWorkflowExecutionHistoryWithTimeout(
      GetWorkflowExecutionHistoryRequest getRequest, Long timeoutInMillis) throws TException;

  /**
   * GetWorkflowExecutionHistoryWithTimeout get workflow history asynchronously same as
   * GetWorkflowExecutionHistory but with timeout.
   *
   * @param getRequest
   * @param resultHandler
   * @param timeoutInMillis
   * @throws org.apache.thrift.TException
   */
  void GetWorkflowExecutionHistoryWithTimeout(
      GetWorkflowExecutionHistoryRequest getRequest,
      AsyncMethodCallback resultHandler,
      Long timeoutInMillis)
      throws TException;
  /**
   * SignalWorkflowExecutionWithTimeout signal workflow same as SignalWorkflowExecution but with
   * timeout
   *
   * @param signalRequest
   * @param resultHandler
   * @param timeoutInMillis
   * @throws TException
   */
  void SignalWorkflowExecutionWithTimeout(
      SignalWorkflowExecutionRequest signalRequest,
      AsyncMethodCallback resultHandler,
      Long timeoutInMillis)
      throws TException;
}
