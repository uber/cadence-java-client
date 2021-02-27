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

import java.time.Duration;
import java.util.Objects;

public class ShadowingExitCondition {

    public static ShadowingExitCondition.Builder newBuilder() {
        return new ShadowingExitCondition.Builder();
    }

    public static ShadowingExitCondition.Builder newBuilder(ShadowingExitCondition options) {
        return new ShadowingExitCondition.Builder(options);
    }

    public static ShadowingExitCondition defaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final ShadowingExitCondition DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = ShadowingExitCondition.newBuilder().build();
    }

    public static final class Builder {
        private int shadowingCount;
        private Duration shadowingDuration;

        private Builder() {}

        private Builder(ShadowingExitCondition options) {
            this.shadowingCount = options.shadowingCount;
            this.shadowingDuration = options.shadowingDuration;
        }

        public Builder setShadowingCount(int shadowingCount) {
            if (shadowingCount < 0) {
                throw new IllegalArgumentException("Negative value: " + shadowingCount);
            }
            this.shadowingCount = shadowingCount;
            return this;
        }

        public Builder setShadowingDuration(Duration shadowingDuration) {
            Objects.requireNonNull(shadowingDuration);
            if (shadowingDuration.isNegative() || shadowingDuration.isZero()) {
                throw new IllegalArgumentException("Negative or zero value : " + shadowingDuration);
            }
            this.shadowingDuration = shadowingDuration;
            return this;
        }

        public ShadowingExitCondition build() {
            return new ShadowingExitCondition(shadowingCount, shadowingDuration);
        }
    }

    private final int shadowingCount;
    private final Duration shadowingDuration;

    private ShadowingExitCondition(int shadowingCount, Duration shadowingDuration) {
        this.shadowingCount = shadowingCount;
        this.shadowingDuration = shadowingDuration;
    }

    public int getShadowingCount() {
        return shadowingCount;
    }

    public Duration getShadowingDuration() {
        return shadowingDuration;
    }

    @Override
    public String toString() {
        return "ShadowingExitCondition{"
                + "shadowingCount="
                + shadowingCount
                + ", shadowingDuration="
                + shadowingDuration
                + '}';
    }
}
