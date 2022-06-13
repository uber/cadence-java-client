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
package com.uber.cadence.internal.compatibility.proto.serviceclient;

import com.google.common.collect.ImmutableMap;
import com.uber.cadence.api.v1.DomainAPIGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolver;
import java.util.Map;
import java.util.function.Function;

public class GrpcServiceStubsOptions {

  private static final String LOCAL_DOCKER_TARGET = "127.0.0.1:7233";

  /** Default RPC timeout used for all non long poll calls. */
  private static final long DEFAULT_RPC_TIMEOUT_MILLIS = 1000;
  /** Default RPC timeout used for all long poll calls. */
  private static final long DEFAULT_POLL_RPC_TIMEOUT_MILLIS = 121 * 1000;
  /** Default RPC timeout for QueryWorkflow */
  private static final long DEFAULT_QUERY_RPC_TIMEOUT_MILLIS = 10000;

  private static final GrpcServiceStubsOptions DEFAULT_INSTANCE;

  static {
    DEFAULT_INSTANCE = GrpcServiceStubsOptions.newBuilder().build();
  }

  private final ManagedChannel channel;
  private final String target;
  /** The tChannel timeout in milliseconds */
  private final long rpcTimeoutMillis;
  /** The tChannel timeout for long poll calls in milliseconds */
  private final long rpcLongPollTimeoutMillis;
  /** The tChannel timeout for query workflow call in milliseconds */
  private final long rpcQueryTimeoutMillis;
  /** Optional TChannel headers */
  private final Map<String, String> headers;

  private GrpcServiceStubsOptions(Builder builder) {
    this.target = builder.target;
    this.channel = builder.channel;
    this.rpcLongPollTimeoutMillis = builder.rpcLongPollTimeoutMillis;
    this.rpcQueryTimeoutMillis = builder.rpcQueryTimeoutMillis;
    this.rpcTimeoutMillis = builder.rpcTimeoutMillis;
    this.headers = builder.headers;
  }

  private GrpcServiceStubsOptions(Builder builder, boolean validate) {
    if (builder.target != null && builder.channel != null) {
      throw new IllegalStateException(
          "Only one of the target and channel options can be set at a time");
    }
    if (builder.target == null && builder.channel == null) {
      this.target = LOCAL_DOCKER_TARGET;
    } else {
      this.target = builder.target;
    }
    this.channel = builder.channel;
    this.rpcLongPollTimeoutMillis = builder.rpcLongPollTimeoutMillis;
    this.rpcQueryTimeoutMillis = builder.rpcQueryTimeoutMillis;
    this.rpcTimeoutMillis = builder.rpcTimeoutMillis;

    if (builder.headers != null) {
      this.headers = ImmutableMap.copyOf(builder.headers);
    } else {
      this.headers = ImmutableMap.of();
    }
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static Builder newBuilder(GrpcServiceStubsOptions options) {
    return new Builder(options);
  }

  public static GrpcServiceStubsOptions getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  public ManagedChannel getChannel() {
    return channel;
  }

  public String getTarget() {
    return target;
  }

  /** @return Returns the rpc timeout value in millis. */
  public long getRpcTimeoutMillis() {
    return rpcTimeoutMillis;
  }

  /** @return Returns the rpc timout for long poll requests in millis. */
  public long getRpcLongPollTimeoutMillis() {
    return rpcLongPollTimeoutMillis;
  }

  /** @return Returns the rpc timout for query workflow requests in millis. */
  public long getRpcQueryTimeoutMillis() {
    return rpcQueryTimeoutMillis;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  /**
   * Builder is the builder for ClientOptions.
   *
   * @author venkat
   */
  public static class Builder {

    public long rpcQueryTimeoutMillis = DEFAULT_QUERY_RPC_TIMEOUT_MILLIS;
    private ManagedChannel channel;
    private String target;
    private long rpcTimeoutMillis = DEFAULT_RPC_TIMEOUT_MILLIS;
    private long rpcLongPollTimeoutMillis = DEFAULT_POLL_RPC_TIMEOUT_MILLIS;
    private Map<String, String> headers;
    private Function<DomainAPIGrpc.DomainAPIBlockingStub, DomainAPIGrpc.DomainAPIBlockingStub>
        blockingStubInterceptor;
    private Function<DomainAPIGrpc.DomainAPIFutureStub, DomainAPIGrpc.DomainAPIFutureStub>
        futureStubInterceptor;

    private Builder() {}

    public Builder(GrpcServiceStubsOptions options) {
      this.target = options.target;
      this.channel = options.channel;
      this.rpcLongPollTimeoutMillis = options.rpcLongPollTimeoutMillis;
      this.rpcQueryTimeoutMillis = options.rpcQueryTimeoutMillis;
      this.rpcTimeoutMillis = options.rpcTimeoutMillis;
      this.headers = options.headers;
    }

    /** Sets gRPC channel to use. Exclusive with target. */
    public Builder setChannel(ManagedChannel channel) {
      this.channel = channel;
      return this;
    }

    /**
     * Sets a target string, which can be either a valid {@link NameResolver}-compliant URI, or an
     * authority string. See {@link ManagedChannelBuilder#forTarget(String)} for more information
     * about parameter format.
     *
     * <p>Exclusive with channel.
     */
    public Builder setTarget(String target) {
      this.target = target;
      return this;
    }

    /**
     * Sets the rpc timeout value for non query and non long poll calls. Default is 1000.
     *
     * @param timeoutMillis timeout, in millis.
     */
    public Builder setRpcTimeout(long timeoutMillis) {
      this.rpcTimeoutMillis = timeoutMillis;
      return this;
    }

    /**
     * Sets the rpc timeout value for the following long poll based operations: PollForDecisionTask,
     * PollForActivityTask, GetWorkflowExecutionHistory. Should never be below 60000 as this is
     * server side timeout for the long poll. Default is 61000.
     *
     * @param timeoutMillis timeout, in millis.
     */
    public Builder setRpcLongPollTimeout(long timeoutMillis) {
      this.rpcLongPollTimeoutMillis = timeoutMillis;
      return this;
    }

    /**
     * Sets the rpc timeout value for query calls. Default is 10000.
     *
     * @param timeoutMillis timeout, in millis.
     */
    public Builder setQueryRpcTimeout(long timeoutMillis) {
      this.rpcQueryTimeoutMillis = timeoutMillis;
      return this;
    }

    public Builder setHeaders(Map<String, String> headers) {
      this.headers = headers;
      return this;
    }

    /**
     * Builds and returns a ClientOptions object.
     *
     * @return ClientOptions object with the specified params.
     */
    public GrpcServiceStubsOptions build() {
      return new GrpcServiceStubsOptions(this);
    }

    public GrpcServiceStubsOptions validateAndBuildWithDefaults() {
      return new GrpcServiceStubsOptions(this, true);
    }
  }
}
