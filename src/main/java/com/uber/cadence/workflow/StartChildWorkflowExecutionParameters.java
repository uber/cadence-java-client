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

import com.uber.cadence.WorkflowIdReusePolicy;
import com.uber.cadence.internal.WorkflowOptions;
import com.uber.cadence.ChildPolicy;
import com.uber.cadence.WorkflowType;

import java.util.Arrays;

public final class StartChildWorkflowExecutionParameters {

    public final static class Builder {

        private String domain;

        private String control;

        private int executionStartToCloseTimeoutSeconds;

        private byte[] input;

        private String taskList;

        private int taskStartToCloseTimeoutSeconds;

        private String workflowId;

        private WorkflowType workflowType;

        private ChildPolicy childPolicy;

        private WorkflowIdReusePolicy workflowIdReusePolicy;

        public Builder setDomain(String domain) {
            this.domain = domain;
            return this;
        }

        public Builder setControl(String control) {
            this.control = control;
            return this;
        }

        public Builder setExecutionStartToCloseTimeoutSeconds(int executionStartToCloseTimeoutSeconds) {
            this.executionStartToCloseTimeoutSeconds = executionStartToCloseTimeoutSeconds;
            return this;
        }

        public Builder setInput(byte[] input) {
            this.input = input;
            return this;
        }

        public Builder setTaskList(String taskList) {
            this.taskList = taskList;
            return this;
        }

        public Builder setTaskStartToCloseTimeoutSeconds(int taskStartToCloseTimeoutSeconds) {
            this.taskStartToCloseTimeoutSeconds = taskStartToCloseTimeoutSeconds;
            return this;
        }

        public Builder setWorkflowId(String workflowId) {
            this.workflowId = workflowId;
            return this;
        }

        public Builder setWorkflowType(WorkflowType workflowType) {
            this.workflowType = workflowType;
            return this;
        }

        public Builder setChildPolicy(ChildPolicy childPolicy) {
            this.childPolicy = childPolicy;
            return this;
        }

        public Builder setWorkflowIdReusePolicy(WorkflowIdReusePolicy workflowIdReusePolicy) {
            this.workflowIdReusePolicy = workflowIdReusePolicy;
            return this;
        }

        public StartChildWorkflowExecutionParameters build() {
            return new StartChildWorkflowExecutionParameters(domain, input, control, executionStartToCloseTimeoutSeconds,
                    taskList, taskStartToCloseTimeoutSeconds, workflowId, workflowType, childPolicy, workflowIdReusePolicy);
        }
    }

    private final String domain;

    private final String control;

    private final int executionStartToCloseTimeoutSeconds;

    private final byte[] input;

    private final String taskList;

    private final int taskStartToCloseTimeoutSeconds;

    private final String workflowId;

    private final WorkflowType workflowType;

    private final ChildPolicy childPolicy;

    private final WorkflowIdReusePolicy workflowIdReusePolicy;

    public StartChildWorkflowExecutionParameters(String domain, byte[] input, String control, int executionStartToCloseTimeoutSeconds,
                                                 String taskList, int taskStartToCloseTimeoutSeconds,
                                                 String workflowId, WorkflowType workflowType, ChildPolicy childPolicy,
                                                 WorkflowIdReusePolicy workflowIdReusePolicy) {
        this.domain = domain;
        this.input = input;
        this.control = control;
        this.executionStartToCloseTimeoutSeconds = executionStartToCloseTimeoutSeconds;
        this.taskList = taskList;
        this.taskStartToCloseTimeoutSeconds = taskStartToCloseTimeoutSeconds;
        this.workflowId = workflowId;
        this.workflowType = workflowType;
        this.childPolicy = childPolicy;
        this.workflowIdReusePolicy = workflowIdReusePolicy;
    }

    public String getDomain() {
        return domain;
    }

    public String getControl() {
        return control;
    }

    public int getExecutionStartToCloseTimeoutSeconds() {
        return executionStartToCloseTimeoutSeconds;
    }

    public byte[] getInput() {
        return input;
    }

    public String getTaskList() {
        return taskList;
    }

    public int getTaskStartToCloseTimeoutSeconds() {
        return taskStartToCloseTimeoutSeconds;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public WorkflowType getWorkflowType() {
        return workflowType;
    }

    public ChildPolicy getChildPolicy() {
        return childPolicy;
    }

    public WorkflowIdReusePolicy getWorkflowIdReusePolicy() {
        return workflowIdReusePolicy;
    }

    @Override
    public String toString() {
        return "StartChildWorkflowExecutionParameters{" +
                "domain='" + domain + '\'' +
                ", control='" + control + '\'' +
                ", executionStartToCloseTimeoutSeconds=" + executionStartToCloseTimeoutSeconds +
                ", input=" + Arrays.toString(input) +
                ", taskList='" + taskList + '\'' +
                ", taskStartToCloseTimeoutSeconds=" + taskStartToCloseTimeoutSeconds +
                ", workflowId='" + workflowId + '\'' +
                ", workflowType=" + workflowType +
                ", childPolicy=" + childPolicy +
                ", workflowIdReusePolicy=" + workflowIdReusePolicy +
                '}';
    }
}
