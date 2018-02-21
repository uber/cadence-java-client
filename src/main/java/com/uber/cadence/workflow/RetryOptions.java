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
package com.uber.cadence.workflow;

import java.time.Duration;
import java.util.Objects;

public final class RetryOptions {

    public final static class Builder {

        private Duration interval = Duration.ofSeconds(1);

        private double backoffCoefficient = 2;

        private int maximumRetries = Integer.MAX_VALUE;

        private Duration expiration = Duration.ZERO;

        private Duration maximumInterval = Duration.ofMinutes(1);

        private int minimumRetries;

        private Functions.Func1<Exception, Boolean> exceptionFilter = (e) -> true;

        public Builder setInterval(Duration interval) {
            this.interval = interval;
            return this;
        }

        public Builder setBackoffCoefficient(double backoffCoefficient) {
            this.backoffCoefficient = backoffCoefficient;
            return this;
        }

        public Builder setMaximumRetries(int maximumRetries) {
            this.maximumRetries = maximumRetries;
            return this;
        }

        public Builder setExpiration(Duration expiration) {
            Objects.requireNonNull(expiration);
            this.expiration = expiration;
            return this;
        }

        /**
         * Maximum time to keep retrying. Default is {@link Duration#ZERO} which means retry forever.
         */
        public Builder setMaximumInterval(Duration maximumInterval) {
            Objects.requireNonNull(maximumInterval);
            this.maximumInterval = maximumInterval;
            return this;
        }

        /**
         * Minimum number of retries before bailing out independently of maximum interval value.
         */
        public Builder setMinimumRetries(int minimumRetries) {
            this.minimumRetries = minimumRetries;
            return this;
        }

        /**
         * Returns true if exception should retried.
         * {@link Error} and {@link java.util.concurrent.CancellationException} are never retried and
         * are not even passed to this filter. The default filter always returns true.
         *
         * @param exceptionFilter non null
         */
        public Builder setExceptionFilter(Functions.Func1<Exception, Boolean> exceptionFilter) {
            Objects.requireNonNull(exceptionFilter);
            this.exceptionFilter = exceptionFilter;
            return this;
        }

        public RetryOptions build() {
            return new RetryOptions(interval, backoffCoefficient, maximumRetries, expiration, maximumInterval,
                    minimumRetries, exceptionFilter);
        }
    }

    private RetryOptions(Duration interval, double backoffCoefficient, int maximumRetries, Duration expiration,
                         Duration maximumInterval, int minimumRetries, Functions.Func1<Exception, Boolean> exceptionFilter) {
        this.interval = interval;
        this.backoffCoefficient = backoffCoefficient;
        this.maximumRetries = maximumRetries;
        this.expiration = expiration;
        this.maximumInterval = maximumInterval;
        this.minimumRetries = minimumRetries;
        this.exceptionFilter = exceptionFilter;
    }

    private final Duration interval;

    private final double backoffCoefficient;

    private final int maximumRetries;

    private final Duration expiration;

    private final Duration maximumInterval;

    private final int minimumRetries;

    private final Functions.Func1<Exception, Boolean> exceptionFilter;

    public Duration getInterval() {
        return interval;
    }

    public double getBackoffCoefficient() {
        return backoffCoefficient;
    }

    public int getMaximumRetries() {
        return maximumRetries;
    }

    public Duration getExpiration() {
        return expiration;
    }

    public Duration getMaximumInterval() {
        return maximumInterval;
    }

    public int getMinimumRetries() {
        return minimumRetries;
    }

    public Functions.Func1<Exception, Boolean> getExceptionFilter() {
        return exceptionFilter;
    }
}
