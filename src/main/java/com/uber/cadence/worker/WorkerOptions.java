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
package com.uber.cadence.worker;

import com.uber.cadence.converter.DataConverter;
import com.uber.cadence.converter.JsonDataConverter;

public final class WorkerOptions {

    public final static class Builder {

        boolean disableWorkflowWorker;
        boolean disableActivityWorker;
        double workerActivitiesPerSecond;
        String identity;
        DataConverter dataConverter = JsonDataConverter.getInstance();
        int maxConcurrentActivityExecutionSize;
        int maxWorkflowThreads;

        /**
         * When set to true doesn't poll on workflow task list even if there are registered workflows with a worker.
         * For clarity prefer not registing workflow types with a {@link Worker} to setting this option.
         * But it can be useful for disabling polling through configuration without a code change.
         */
        public Builder setDisableWorkflowWorker(boolean disableWorkflowWorker) {
            this.disableWorkflowWorker = disableWorkflowWorker;
            return this;
        }

        /**
         * When set to true doesn't poll on activity task list even if there are registered activities with a worker.
         * For clarity prefer not registing activity implementations with a {@link Worker} to setting this option.
         * But it can be useful for disabling polling through configuration without a code change.
         */
        public Builder setDisableActivityWorker(boolean disableActivityWorker) {
            this.disableActivityWorker = disableActivityWorker;
            return this;
        }

        /**
         * Override human readable identity of the worker. Identity is used to identify a worker and is recorded in
         * the workflow history events.
         * For example when a worker gets an activity task the correspondent ActivityTaskStarted event contains
         * the worker identity as a field.
         * Default is whatever <code>(ManagementFactory.getRuntimeMXBean().getName()</code> returns.
         */
        public Builder setIdentity(String identity) {
            this.identity = identity;
            return this;
        }

        /**
         * Override a data converter implementation used by workflows and activities executed by this worker.
         * Default is {@link com.uber.cadence.converter.JsonDataConverter} data converter.
         */
        public Builder setDataConverter(DataConverter dataConverter) {
            if (dataConverter == null) {
                throw new IllegalArgumentException("null");
            }
            this.dataConverter = dataConverter;
            return this;
        }

        /**
         * Maximum number of activities started per second.
         * Default is 0 which means unlimited.
         */
        public Builder setWorkerActivitiesPerSecond(double workerActivitiesPerSecond) {
            this.workerActivitiesPerSecond = workerActivitiesPerSecond;
            return this;
        }

        /**
         * Maximum number of parallely executed activities.
         */
        public Builder setMaxConcurrentActivityExecutionSize(int maxConcurrentActivityExecutionSize) {
            this.maxConcurrentActivityExecutionSize = maxConcurrentActivityExecutionSize;
            return this;
        }

        /**
         * Maximum size of a thread pool used to execute workflows.
         */
        public Builder setMaxWorkflowThreads(int maxWorkflowThreads) {
            this.maxWorkflowThreads = maxWorkflowThreads;
            return this;
        }

        public WorkerOptions build() {
            return new WorkerOptions(disableWorkflowWorker, disableActivityWorker, workerActivitiesPerSecond, identity,
                    dataConverter, maxConcurrentActivityExecutionSize, maxWorkflowThreads);
        }
    }

    private final boolean disableWorkflowWorker;
    private final boolean disableActivityWorker;
    private final double workerActivitiesPerSecond;
    private final String identity;
    private final DataConverter dataConverter;
    private final int maxConcurrentActivityExecutionSize;
    private final int maxWorkflowThreads;

    private WorkerOptions(boolean disableWorkflowWorker, boolean disableActivityWorker, double workerActivitiesPerSecond,
                          String identity, DataConverter dataConverter, int maxConcurrentActivityExecutionSize, int maxWorkflowThreads) {
        this.disableWorkflowWorker = disableWorkflowWorker;
        this.disableActivityWorker = disableActivityWorker;
        this.workerActivitiesPerSecond = workerActivitiesPerSecond;
        this.identity = identity;
        this.dataConverter = dataConverter;
        this.maxConcurrentActivityExecutionSize = maxConcurrentActivityExecutionSize;
        this.maxWorkflowThreads = maxWorkflowThreads;
    }

    public boolean isDisableWorkflowWorker() {
        return disableWorkflowWorker;
    }

    public boolean isDisableActivityWorker() {
        return disableActivityWorker;
    }

    public double getWorkerActivitiesPerSecond() {
        return workerActivitiesPerSecond;
    }

    public String getIdentity() {
        return identity;
    }

    public DataConverter getDataConverter() {
        return dataConverter;
    }

    public int getMaxConcurrentActivityExecutionSize() {
        return maxConcurrentActivityExecutionSize;
    }

    public int getMaxWorkflowThreads() {
        return maxWorkflowThreads;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkerOptions that = (WorkerOptions) o;

        if (disableWorkflowWorker != that.disableWorkflowWorker) return false;
        if (disableActivityWorker != that.disableActivityWorker) return false;
        if (Double.compare(that.workerActivitiesPerSecond, workerActivitiesPerSecond) != 0) return false;
        if (maxConcurrentActivityExecutionSize != that.maxConcurrentActivityExecutionSize) return false;
        if (maxWorkflowThreads != that.maxWorkflowThreads) return false;
        if (identity != null ? !identity.equals(that.identity) : that.identity != null) return false;
        return dataConverter != null ? dataConverter.equals(that.dataConverter) : that.dataConverter == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = (disableWorkflowWorker ? 1 : 0);
        result = 31 * result + (disableActivityWorker ? 1 : 0);
        temp = Double.doubleToLongBits(workerActivitiesPerSecond);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (identity != null ? identity.hashCode() : 0);
        result = 31 * result + (dataConverter != null ? dataConverter.hashCode() : 0);
        result = 31 * result + maxConcurrentActivityExecutionSize;
        result = 31 * result + maxWorkflowThreads;
        return result;
    }

    @Override
    public String toString() {
        return "WorkerOptions{" +
                "disableWorkflowWorker=" + disableWorkflowWorker +
                ", disableActivityWorker=" + disableActivityWorker +
                ", workerActivitiesPerSecond=" + workerActivitiesPerSecond +
                ", identity='" + identity + '\'' +
                ", dataConverter=" + dataConverter +
                ", maxConcurrentActivityExecutionSize=" + maxConcurrentActivityExecutionSize +
                ", maxWorkflowThreads=" + maxWorkflowThreads +
                '}';
    }
}
