/*
 *  Modifications Copyright (c) 2017-2020 Uber Technologies Inc.
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

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

public final class ShadowingOptions {
    public static ShadowingOptions.Builder newBuilder() {
        return new ShadowingOptions.Builder();
    }

    public static ShadowingOptions.Builder newBuilder(ShadowingOptions options) {
        return new ShadowingOptions.Builder(options);
    }

    public static ShadowingOptions defaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final ShadowingOptions DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = ShadowingOptions.newBuilder().build();
    }

    public static final class Builder {
        private String domain = "";
        private String workflowQuery = "";
        private Set<String> workflowTypes = Sets.newHashSet();
        private TimeFilter workflowStartTimeFilter;
        private Set<WorkflowStatus> workflowStatuses = Sets.newHashSet(WorkflowStatus.OPEN);
        private double samplingRate = 1.0;
        private ShadowingExitCondition exitCondition = ShadowingExitCondition.defaultInstance();

        private Builder() {}

        private Builder(ShadowingOptions options) {
            this.domain = options.domain;
            this.workflowQuery = options.getWorkflowQuery();
            this.workflowTypes = options.workflowTypes;
            this.workflowStartTimeFilter = options.workflowStartTimeFilter;
            this.workflowStatuses = options.workflowStatuses;
            this.samplingRate = options.samplingRate;
            this.exitCondition = options.exitCondition;
        }

        public Builder setDomain(String domain) {
            this.domain = domain;
            return this;
        }

        public Builder setWorkflowQuery(String workflowQuery) {
            this.workflowQuery = Objects.requireNonNull(workflowQuery);
            return this;
        }

        public Builder setWorkflowTypes(Collection<String> workflowTypes) {
            Objects.requireNonNull(workflowTypes);
            this.workflowTypes = Sets.newHashSet(workflowTypes);
            return this;
        }

        public Builder setWorkflowStartTimeFilter(TimeFilter workflowStartTimeFilter) {
            this.workflowStartTimeFilter = Objects.requireNonNull(workflowStartTimeFilter);
            return this;
        }

        public Builder setWorkflowClosedStatuses(Collection<WorkflowStatus> workflowStatuses) {
            Objects.requireNonNull(workflowStatuses);
            this.workflowStatuses = Sets.newHashSet(workflowStatuses);
            if (workflowStatuses.size() == 0) {
                this.workflowStatuses.add(WorkflowStatus.OPEN);
            }
            return this;
        }

        public Builder setWorkflowSamplingRate(double samplingRate) {
            if (samplingRate < 0.0 || samplingRate > 1.0) {
                throw new IllegalArgumentException("Negative or larger than one: " + samplingRate);
            }
            this.samplingRate = samplingRate;
            return this;
        }

        public Builder setExitCondition(ShadowingExitCondition shadowingExitCondition) {
            this.exitCondition = Objects.requireNonNull(shadowingExitCondition);
            return this;
        }

        public ShadowingOptions build() {
            return new ShadowingOptions(
                    domain,
                    workflowQuery,
                    workflowTypes,
                    workflowStartTimeFilter,
                    workflowStatuses,
                    samplingRate,
                    exitCondition);
        }
    }

    private final String domain;
    private final String workflowQuery;
    private final Set<String> workflowTypes;
    private final TimeFilter workflowStartTimeFilter;
    private final Set<WorkflowStatus> workflowStatuses;
    private final double samplingRate;
    private final ShadowingExitCondition exitCondition;

    private ShadowingOptions(
            String domain,
            String workflowQuery,
            Set<String> workflowTypes,
            TimeFilter workflowStartTimeFilter,
            Set<WorkflowStatus> workflowStatuses,
            double samplingRate,
            ShadowingExitCondition exitCondition) {
        this.domain = domain;
        this.workflowQuery = workflowQuery;
        this.workflowTypes = workflowTypes;
        this.workflowStartTimeFilter = workflowStartTimeFilter;
        this.workflowStatuses = workflowStatuses;
        this.samplingRate = samplingRate;
        this.exitCondition = exitCondition;
    }

    public String getDomain() {
        return domain;
    }

    public String getWorkflowQuery() {
        return workflowQuery;
    }

    public Collection<String> getWorkflowTypes() {
        return workflowTypes;
    }

    public TimeFilter getWorkflowStartTimeFilter() {
        return workflowStartTimeFilter;
    }

    public Collection<WorkflowStatus> getWorkflowClosedStatuses() {
        return workflowStatuses;
    }

    public double getSamplingRate() {
        return samplingRate;
    }

    public ShadowingExitCondition getExitCondition() {
        return exitCondition;
    }

    @Override
    public String toString() {
        return "ShadowOptions{"
                + ", domain="
                + domain
                + ", workflowQuery="
                + workflowQuery
                + ", workflowTypes="
                + workflowTypes.toString()
                + ", workflowClosedStatusesFilter="
                + workflowStatuses.toString()
                + ", samplingRate="
                + samplingRate
                + ", exitCondition="
                + exitCondition.toString()
                + '}';
    }
}
