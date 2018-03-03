package com.uber.cadence.internal.worker;

import com.uber.cadence.common.RetryOptions;
import com.uber.cadence.converter.DataConverter;
import com.uber.cadence.converter.JsonDataConverter;

public final class WorkflowWorkerOptions {

    public static final class Builder {

        private String identity;

        private DataConverter dataConverter;

        private int taskExecutorThreadPoolSize = 100;

        private PollerOptions pollerOptions;

        private RetryOptions reportCompletionRetryOptions;

        private RetryOptions reportFailureRetryOptions;

        public Builder setIdentity(String identity) {
            this.identity = identity;
            return this;
        }

        public Builder setDataConverter(DataConverter dataConverter) {
            this.dataConverter = dataConverter;
            return this;
        }

        public Builder setTaskExecutorThreadPoolSize(int taskExecutorThreadPoolSize) {
            this.taskExecutorThreadPoolSize = taskExecutorThreadPoolSize;
            return this;
        }

        public Builder setPollerOptions(PollerOptions pollerOptions) {
            this.pollerOptions = pollerOptions;
            return this;
        }

        public WorkflowWorkerOptions build() {
            PollerOptions po = pollerOptions == null ? new PollerOptions.Builder().build() : pollerOptions;
            DataConverter dc = dataConverter;
            if (dc == null) {
                dc = JsonDataConverter.getInstance();
            }
            return new WorkflowWorkerOptions(identity, dc, taskExecutorThreadPoolSize, po,
                    reportCompletionRetryOptions, reportFailureRetryOptions);
        }

        public Builder setReportCompletionRetryOptions(RetryOptions reportCompletionRetryOptions) {
            this.reportCompletionRetryOptions = reportCompletionRetryOptions;
            return this;
        }

        public Builder setReportFailureRetryOptions(RetryOptions reportFailureRetryOptions) {
            this.reportFailureRetryOptions = reportFailureRetryOptions;
            return this;
        }
    }

    private final String identity;

    private final DataConverter dataConverter;

    private final int taskExecutorThreadPoolSize;

    private final PollerOptions pollerOptions;

    private final RetryOptions reportCompletionRetryOptions;

    private final RetryOptions reportFailureRetryOptions;

    private WorkflowWorkerOptions(String identity, DataConverter dataConverter, int taskExecutorThreadPoolSize,
                                  PollerOptions pollerOptions, RetryOptions reportCompletionRetryOptions,
                                  RetryOptions reportFailureRetryOptions) {
        this.identity = identity;
        this.dataConverter = dataConverter;
        this.taskExecutorThreadPoolSize = taskExecutorThreadPoolSize;
        this.pollerOptions = pollerOptions;
        this.reportCompletionRetryOptions = reportCompletionRetryOptions;
        this.reportFailureRetryOptions = reportFailureRetryOptions;
    }

    public String getIdentity() {
        return identity;
    }

    public DataConverter getDataConverter() {
        return dataConverter;
    }

    public int getTaskExecutorThreadPoolSize() {
        return taskExecutorThreadPoolSize;
    }

    public PollerOptions getPollerOptions() {
        return pollerOptions;
    }

    public RetryOptions getReportCompletionRetryOptions() {
        return reportCompletionRetryOptions;
    }

    public RetryOptions getReportFailureRetryOptions() {
        return reportFailureRetryOptions;
    }
}
