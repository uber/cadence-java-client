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
package com.uber.cadence.internal.compatibility.thrift;

import com.uber.cadence.AccessDeniedError;
import com.uber.cadence.CancellationAlreadyRequestedError;
import com.uber.cadence.ClientVersionNotSupportedError;
import com.uber.cadence.DomainAlreadyExistsError;
import com.uber.cadence.DomainNotActiveError;
import com.uber.cadence.EntityNotExistsError;
import com.uber.cadence.FeatureNotEnabledError;
import com.uber.cadence.InternalDataInconsistencyError;
import com.uber.cadence.InternalServiceError;
import com.uber.cadence.LimitExceededError;
import com.uber.cadence.ServiceBusyError;
import com.uber.cadence.WorkflowExecutionAlreadyCompletedError;
import com.uber.cadence.WorkflowExecutionAlreadyStartedError;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import org.apache.thrift.TException;

public class ErrorMapper {

  public static TException Error(StatusRuntimeException ex) {
    String details = getErrorDetails(ex);
    switch (ex.getStatus().getCode()) {
      case PERMISSION_DENIED:
        return new AccessDeniedError(ex.getMessage());
      case INTERNAL:
        return new InternalServiceError(ex.getMessage());
      case NOT_FOUND: {
        switch (details) {
          case "EntityNotExistsError":
            // TODO add cluster info
            return new EntityNotExistsError(ex.getMessage());
          case "WorkflowExecutionAlreadyCompletedError":
            return new WorkflowExecutionAlreadyCompletedError(ex.getMessage());
        }
      }
      case ALREADY_EXISTS: {
        switch (details) {
          case "CancellationAlreadyRequestedError":
            return new CancellationAlreadyRequestedError(ex.getMessage());
          case "DomainAlreadyExistsError":
            return new DomainAlreadyExistsError(ex.getMessage());
          case "WorkflowExecutionAlreadyStartedError": {
            // TODO add started wf info
            WorkflowExecutionAlreadyStartedError e = new WorkflowExecutionAlreadyStartedError();
            e.setMessage(ex.getMessage());
            return e;
          }
        }
      }
      case DATA_LOSS:
        return new InternalDataInconsistencyError(ex.getMessage());
      case FAILED_PRECONDITION:
        switch (details) {
          // TODO add infos
          case "ClientVersionNotSupportedError":
            return new ClientVersionNotSupportedError();
          case "FeatureNotEnabledError":
            return new FeatureNotEnabledError();
          case "DomainNotActiveError": {
            DomainNotActiveError e = new DomainNotActiveError();
            e.setMessage(ex.getMessage());
            return e;
          }
        }
      case RESOURCE_EXHAUSTED:
        switch (details) {
          case "LimitExceededError":
            return new LimitExceededError(ex.getMessage());
          case "ServiceBusyError":
            return new ServiceBusyError(ex.getMessage());
        }
      case UNKNOWN:
        return new TException(ex);
      default:
        // If error does not match anything, return raw grpc status error
        // There are some code that casts error to grpc status to check for deadline exceeded status
        return new TException(ex);
    }
  }

  static String getErrorDetails(StatusRuntimeException ex) {
    {
      Metadata trailer = ex.getTrailers();
      Metadata.Key<String> key =
          Metadata.Key.of("rpc-application-error-name", Metadata.ASCII_STRING_MARSHALLER);
      if (trailer != null && trailer.containsKey(key)) {
        return trailer.get(key);
      } else {
        return "";
      }
    }
  }
}
