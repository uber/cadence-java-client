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

package com.uber.cadence.internal.metrics;

import com.uber.cadence.internal.Version;
import com.uber.m3.tally.Scope;
import com.uber.m3.util.ImmutableMap;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

public class ClientVersionEmitter implements Runnable {

  private Scope metricScope;

  public ClientVersionEmitter(Scope metricScope, String domain) {
    if (domain == null) {
      domain = "UNKNOWN";
    }

    Properties prop = new Properties();
    InputStream in = Version.class.getResourceAsStream("/com/uber/cadence/version.properties");
    String version = "";
    try {
      prop.load(in);
      version = prop.getProperty("cadence-client-version");
    } catch (IOException exception) {
      version = "UNKNOWN";
    }

    Map<String, String> tags =
        new ImmutableMap.Builder<String, String>(2)
            .put(MetricsTag.VERSION, version)
            .put(MetricsTag.DOMAIN, domain)
            .build();

    if (metricScope == null) {
      this.metricScope = NoopScope.getInstance();
    } else {
      this.metricScope = metricScope.tagged(tags);
    }
  }

  @Override
  public void run() {
    metricScope.counter(MetricsType.JAVA_CLIENT_VERSION).inc(1);
  }
}
