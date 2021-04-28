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
package com.uber.cadence.internal.shadowing;

import java.util.Arrays;

/**
 * This class is the JSON serializable class of {@link
 * com.uber.cadence.shadower.ScanWorkflowActivityParams} Make sure this class is sync with auto
 * generated ScanWorkflowActivityParams
 */
public class ScanWorkflowActivityParams {
  private String domain;
  private String workflowQuery;
  private double samplingRate;
  private int pageSize;
  private byte[] nextPageToken;

  public ScanWorkflowActivityParams() {}

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public String getWorkflowQuery() {
    return workflowQuery;
  }

  public void setWorkflowQuery(String workflowQuery) {
    this.workflowQuery = workflowQuery;
  }

  public double getSamplingRate() {
    return samplingRate;
  }

  public void setSamplingRate(double samplingRate) {
    this.samplingRate = samplingRate;
  }

  public int getPageSize() {
    return pageSize;
  }

  public void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }

  public byte[] getNextPageToken() {
    return nextPageToken;
  }

  public void setNextPageToken(byte[] nextPageToken) {
    this.nextPageToken = nextPageToken;
  }

  @Override
  public String toString() {
    return "ScanWorkflowActivityParams{"
        + "domain='"
        + domain
        + '\''
        + ", workflowQuery='"
        + workflowQuery
        + '\''
        + ", samplingRate="
        + samplingRate
        + ", pageSize="
        + pageSize
        + ", nextPageToken="
        + Arrays.toString(nextPageToken)
        + '}';
  }
}
