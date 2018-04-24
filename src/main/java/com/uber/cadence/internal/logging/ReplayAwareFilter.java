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

package com.uber.cadence.internal.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.uber.cadence.internal.replay.ReplayAware;
import com.uber.cadence.internal.sync.WorkflowInternal;
import io.netty.util.internal.ConcurrentSet;
import java.util.Iterator;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplayAwareFilter extends Filter<ILoggingEvent> {
  static {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    ch.qos.logback.classic.Logger log = context.getLogger(Logger.ROOT_LOGGER_NAME);

    Iterator<Appender<ILoggingEvent>> appenderIter = log.iteratorForAppenders();
    while (appenderIter.hasNext()) {
      appenderIter
          .next()
          .addFilter(
              new ReplayAwareFilter(
                  WorkflowInternal::isReplaying, WorkflowInternal::getEnableLoggingInReplay));
    }
  }

  private ReplayAware context;
  private Supplier<Boolean> enableLoggingInReplay;
  public static ConcurrentSet<String> workflowLoggers = new ConcurrentSet<>();

  public ReplayAwareFilter(ReplayAware context, Supplier<Boolean> enableLoggingInReplay) {
    this.context = context;
    this.enableLoggingInReplay = enableLoggingInReplay;
  }

  @Override
  public FilterReply decide(ILoggingEvent event) {
    if (!workflowLoggers.contains(event.getLoggerName())) {
      return FilterReply.NEUTRAL;
    }

    if (!context.isReplaying() || enableLoggingInReplay.get()) {
      return FilterReply.NEUTRAL;
    }

    return FilterReply.DENY;
  }
}
