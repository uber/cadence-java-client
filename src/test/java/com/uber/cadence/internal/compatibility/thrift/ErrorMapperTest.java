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
package com.uber.cadence.internal.compatibility.thrift;

import static org.junit.Assert.assertTrue;

import com.uber.cadence.AccessDeniedError;
import com.uber.cadence.CancellationAlreadyRequestedError;
import com.uber.cadence.ClientVersionNotSupportedError;
import com.uber.cadence.DomainAlreadyExistsError;
import com.uber.cadence.DomainNotActiveError;
import com.uber.cadence.EntityNotExistsError;
import com.uber.cadence.FeatureNotEnabledError;
import com.uber.cadence.InternalServiceError;
import com.uber.cadence.LimitExceededError;
import com.uber.cadence.ServiceBusyError;
import com.uber.cadence.WorkflowExecutionAlreadyCompletedError;
import com.uber.cadence.WorkflowExecutionAlreadyStartedError;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.apache.thrift.TException;
import org.junit.Test;

public class ErrorMapperTest {

  @Test
  public void testPermissionDeniedError() {
    StatusRuntimeException ex =
        new StatusRuntimeException(Status.PERMISSION_DENIED.withDescription("Access denied"));
    TException result = ErrorMapper.Error(ex);
    assertTrue(result instanceof AccessDeniedError);
  }

  @Test
  public void testInternalServiceError() {
    StatusRuntimeException ex =
        new StatusRuntimeException(Status.INTERNAL.withDescription("Internal error"));
    TException result = ErrorMapper.Error(ex);
    assertTrue(result instanceof InternalServiceError);
  }

  @Test
  public void testWorkflowExecutionAlreadyCompletedError() {
    Metadata metadata = new Metadata();
    metadata.put(
        Metadata.Key.of("rpc-application-error-name", Metadata.ASCII_STRING_MARSHALLER),
        "EntityNotExistsError");
    StatusRuntimeException ex =
        new StatusRuntimeException(
            Status.NOT_FOUND.withDescription("already completed."), metadata);
    TException result = ErrorMapper.Error(ex);
    assertTrue(result instanceof WorkflowExecutionAlreadyCompletedError);
  }

  @Test
  public void testEntityNotExistsError() {
    StatusRuntimeException ex =
        new StatusRuntimeException(Status.NOT_FOUND.withDescription("Entity not found"));
    TException result = ErrorMapper.Error(ex);
    assertTrue(result instanceof EntityNotExistsError);
  }

  @Test
  public void testCancellationAlreadyRequestedError() {
    Metadata metadata = new Metadata();
    metadata.put(
        Metadata.Key.of("rpc-application-error-name", Metadata.ASCII_STRING_MARSHALLER),
        "CancellationAlreadyRequestedError");
    StatusRuntimeException ex =
        new StatusRuntimeException(
            Status.ALREADY_EXISTS.withDescription("Cancellation already requested"), metadata);
    TException result = ErrorMapper.Error(ex);
    assertTrue(result instanceof CancellationAlreadyRequestedError);
  }

  @Test
  public void testDomainAlreadyExistsError() {
    Metadata metadata = new Metadata();
    metadata.put(
        Metadata.Key.of("rpc-application-error-name", Metadata.ASCII_STRING_MARSHALLER),
        "DomainAlreadyExistsError");
    StatusRuntimeException ex =
        new StatusRuntimeException(
            Status.ALREADY_EXISTS.withDescription("Domain already exists"), metadata);
    TException result = ErrorMapper.Error(ex);
    assertTrue(result instanceof DomainAlreadyExistsError);
  }

  @Test
  public void testWorkflowExecutionAlreadyStartedError() {
    Metadata metadata = new Metadata();
    metadata.put(
        Metadata.Key.of("rpc-application-error-name", Metadata.ASCII_STRING_MARSHALLER),
        "WorkflowExecutionAlreadyStartedError");
    StatusRuntimeException ex =
        new StatusRuntimeException(
            Status.ALREADY_EXISTS.withDescription("Workflow already started"), metadata);
    TException result = ErrorMapper.Error(ex);
    assertTrue(result instanceof WorkflowExecutionAlreadyStartedError);
  }

  @Test
  public void testClientVersionNotSupportedError() {
    Metadata metadata = new Metadata();
    metadata.put(
        Metadata.Key.of("rpc-application-error-name", Metadata.ASCII_STRING_MARSHALLER),
        "ClientVersionNotSupportedError");
    StatusRuntimeException ex =
        new StatusRuntimeException(
            Status.FAILED_PRECONDITION.withDescription("Client version not supported"), metadata);
    TException result = ErrorMapper.Error(ex);
    assertTrue(result instanceof ClientVersionNotSupportedError);
  }

  @Test
  public void testFeatureNotEnabledError() {
    Metadata metadata = new Metadata();
    metadata.put(
        Metadata.Key.of("rpc-application-error-name", Metadata.ASCII_STRING_MARSHALLER),
        "FeatureNotEnabledError");
    StatusRuntimeException ex =
        new StatusRuntimeException(
            Status.FAILED_PRECONDITION.withDescription("Feature not enabled"), metadata);
    TException result = ErrorMapper.Error(ex);
    assertTrue(result instanceof FeatureNotEnabledError);
  }

  @Test
  public void testDomainNotActiveError() {
    Metadata metadata = new Metadata();
    metadata.put(
        Metadata.Key.of("rpc-application-error-name", Metadata.ASCII_STRING_MARSHALLER),
        "DomainNotActiveError");
    StatusRuntimeException ex =
        new StatusRuntimeException(
            Status.FAILED_PRECONDITION.withDescription("Domain not active"), metadata);
    TException result = ErrorMapper.Error(ex);
    assertTrue(result instanceof DomainNotActiveError);
  }

  @Test
  public void testLimitExceededError() {
    Metadata metadata = new Metadata();
    metadata.put(
        Metadata.Key.of("rpc-application-error-name", Metadata.ASCII_STRING_MARSHALLER),
        "LimitExceededError");
    StatusRuntimeException ex =
        new StatusRuntimeException(
            Status.RESOURCE_EXHAUSTED.withDescription("Limit exceeded"), metadata);
    TException result = ErrorMapper.Error(ex);
    assertTrue(result instanceof LimitExceededError);
  }

  @Test
  public void testServiceBusyError() {
    Metadata metadata = new Metadata();
    metadata.put(
        Metadata.Key.of("rpc-application-error-name", Metadata.ASCII_STRING_MARSHALLER),
        "ServiceBusyError");
    StatusRuntimeException ex =
        new StatusRuntimeException(
            Status.RESOURCE_EXHAUSTED.withDescription("Service busy"), metadata);
    TException result = ErrorMapper.Error(ex);
    assertTrue(result instanceof ServiceBusyError);
  }

  @Test
  public void testUnknownError() {
    StatusRuntimeException ex =
        new StatusRuntimeException(Status.UNKNOWN.withDescription("Unknown error"));
    TException result = ErrorMapper.Error(ex);
    assertTrue(result instanceof TException);
  }
}
