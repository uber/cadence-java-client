/*
 *  Modifications Copyright (c) 2017-2020 Uber Technologies Inc.
 *  Portions of the Software are attributed to Copyright (c) 2020 Temporal Technologies Inc.
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

package com.uber.cadence.client;

import com.uber.cadence.QueryRejectCondition;
import com.uber.cadence.context.ContextPropagator;
import com.uber.cadence.converter.DataConverter;
import com.uber.cadence.converter.JsonDataConverter;
import com.uber.cadence.internal.metrics.MetricsTag;
import com.uber.cadence.internal.metrics.NoopScope;
import com.uber.m3.tally.Scope;
import com.uber.m3.util.ImmutableMap;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Options for WorkflowClient configuration. */
public final class WorkflowClientOptions {
  private static final String DEFAULT_DOMAIN = "default";
  private static final WorkflowClientOptions DEFAULT_INSTANCE;
  private static final WorkflowClientInterceptor[] EMPTY_INTERCEPTOR_ARRAY =
      new WorkflowClientInterceptor[0];
  private static final List<ContextPropagator> EMPTY_CONTEXT_PROPAGATORS = Arrays.asList();

  static {
    DEFAULT_INSTANCE = new Builder().build();
  }

  public static WorkflowClientOptions defaultInstance() {
    return DEFAULT_INSTANCE;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static Builder newBuilder(WorkflowClientOptions options) {
    return new Builder(options);
  }

  public static final class Builder {
    private String domain = DEFAULT_DOMAIN;
    private DataConverter dataConverter = JsonDataConverter.getInstance();
    private WorkflowClientInterceptor[] interceptors = EMPTY_INTERCEPTOR_ARRAY;
    private Scope metricsScope = NoopScope.getInstance();
    private String identity = ManagementFactory.getRuntimeMXBean().getName();;
    private List<ContextPropagator> contextPropagators = EMPTY_CONTEXT_PROPAGATORS;
    private QueryRejectCondition queryRejectCondition;

    private Builder() {}

    private Builder(WorkflowClientOptions options) {
      domain = options.getDomain();
      dataConverter = options.getDataConverter();
      interceptors = options.getInterceptors();
      metricsScope = options.getMetricsScope();
      identity = options.getIdentity();
      queryRejectCondition = options.getQueryRejectCondition();
    }

    public Builder setDomain(String domain) {
      this.domain = domain;
      return this;
    }

    /**
     * Used to override default (JSON) data converter implementation.
     *
     * @param dataConverter data converter to serialize and deserialize arguments and return values.
     *     Not null.
     */
    public Builder setDataConverter(DataConverter dataConverter) {
      this.dataConverter = Objects.requireNonNull(dataConverter);
      return this;
    }

    /**
     * Interceptor used to intercept workflow client calls.
     *
     * @param interceptors not null
     */
    public Builder setInterceptors(WorkflowClientInterceptor... interceptors) {
      this.interceptors = Objects.requireNonNull(interceptors);
      return this;
    }

    /**
     * Sets the scope to be used for the workflow client for metrics reporting.
     *
     * @param metricsScope the scope to be used. Not null.
     */
    public Builder setMetricsScope(Scope metricsScope) {
      this.metricsScope = Objects.requireNonNull(metricsScope);
      return this;
    }

    /**
     * Override human readable identity of the worker. Identity is used to identify a worker and is
     * recorded in the workflow history events. For example when a worker gets an activity task the
     * correspondent ActivityTaskStarted event contains the worker identity as a field. Default is
     * whatever <code>(ManagementFactory.getRuntimeMXBean().getName()</code> returns.
     */
    public Builder setIdentity(String identity) {
      this.identity = Objects.requireNonNull(identity);
      return this;
    }

    public Builder setContextPropagators(List<ContextPropagator> contextPropagators) {
      this.contextPropagators = contextPropagators;
      return this;
    }

    /**
     * QueryRejectCondition is an optional field used to reject queries based on workflow state.
     * QueryRejectConditionNotOpen will reject queries to workflows that are not open.
     * QueryRejectConditionNotCompletedCleanly will reject queries to workflows that are not
     * completed successfully. (e.g. terminated, canceled timeout etc...)
     *
     * <p>Default is null, which means that queries will not be rejected based on workflow state.
     */
    public Builder setQueryRejectCondition(QueryRejectCondition queryRejectCondition) {
      this.queryRejectCondition = queryRejectCondition;
      return this;
    }

    public WorkflowClientOptions build() {
      metricsScope = metricsScope.tagged(ImmutableMap.of(MetricsTag.DOMAIN, domain));
      return new WorkflowClientOptions(
          domain,
          dataConverter,
          interceptors,
          metricsScope,
          identity,
          contextPropagators,
          queryRejectCondition);
    }
  }

  private final String domain;
  private final DataConverter dataConverter;
  private final WorkflowClientInterceptor[] interceptors;
  private final Scope metricsScope;
  private final String identity;
  private final List<ContextPropagator> contextPropagators;
  private final QueryRejectCondition queryRejectCondition;

  private WorkflowClientOptions(
      String domain,
      DataConverter dataConverter,
      WorkflowClientInterceptor[] interceptors,
      Scope metricsScope,
      String identity,
      List<ContextPropagator> contextPropagators,
      QueryRejectCondition queryRejectCondition) {
    this.domain = domain;
    this.dataConverter = dataConverter;
    this.interceptors = interceptors;
    this.metricsScope = metricsScope;
    this.identity = identity;
    this.contextPropagators = contextPropagators;
    this.queryRejectCondition = queryRejectCondition;
  }

  public String getDomain() {
    return domain;
  }

  public DataConverter getDataConverter() {
    return dataConverter;
  }

  public WorkflowClientInterceptor[] getInterceptors() {
    return interceptors;
  }

  public Scope getMetricsScope() {
    return metricsScope;
  }

  public String getIdentity() {
    return identity;
  }

  public List<ContextPropagator> getContextPropagators() {
    return contextPropagators;
  }

  public QueryRejectCondition getQueryRejectCondition() {
    return queryRejectCondition;
  }

  @Override
  public String toString() {
    return "WorkflowClientOptions{"
        + "domain='"
        + domain
        + '\''
        + ", dataConverter="
        + dataConverter
        + ", interceptors="
        + Arrays.toString(interceptors)
        + ", identity='"
        + identity
        + '\''
        + ", contextPropagators="
        + contextPropagators
        + ", queryRejectCondition="
        + queryRejectCondition
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    WorkflowClientOptions that = (WorkflowClientOptions) o;
    return com.google.common.base.Objects.equal(domain, that.domain)
        && com.google.common.base.Objects.equal(dataConverter, that.dataConverter)
        && Arrays.equals(interceptors, that.interceptors)
        && com.google.common.base.Objects.equal(identity, that.identity)
        && com.google.common.base.Objects.equal(contextPropagators, that.contextPropagators)
        && queryRejectCondition == that.queryRejectCondition;
  }

  @Override
  public int hashCode() {
    return com.google.common.base.Objects.hashCode(
        domain,
        dataConverter,
        Arrays.hashCode(interceptors),
        identity,
        contextPropagators,
        queryRejectCondition);
  }
}
