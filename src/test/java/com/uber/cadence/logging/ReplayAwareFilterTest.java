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

package com.uber.cadence.logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import com.uber.cadence.internal.logging.ReplayAwareFilter;
import com.uber.cadence.metrics.ReplayAwareScopeTest.TestContext;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplayAwareFilterTest {

  private Logger logger;
  private File logFile;
  private FileAppender<ILoggingEvent> fileAppender;
  private PatternLayoutEncoder encoder;

  @Before
  public void setUp() throws IOException {
    logFile = File.createTempFile("phase-aware-logging-", "-test");
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

    encoder = new PatternLayoutEncoder();
    encoder.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
    encoder.setContext(lc);
    encoder.start();

    fileAppender = new FileAppender<>();
    fileAppender.setFile(logFile.getCanonicalPath());
    fileAppender.setEncoder(encoder);
    fileAppender.setContext(lc);
    fileAppender.setImmediateFlush(true);
    fileAppender.start();

    ch.qos.logback.classic.Logger log = lc.getLogger(ReplayAwareFilterTest.class);
    log.addAppender(fileAppender);
    log.setLevel(Level.INFO);
    log.setAdditive(false);

    ReplayAwareFilter.workflowLoggers.add(log.getName());
    logger = log;
  }

  @After
  public void tearDown() {
    encoder.stop();
    fileAppender.stop();
  }

  @Test
  public void testReplayAwareFilterReplayLoggingDisabled() throws IOException {
    TestContext context = new TestContext(false);

    ch.qos.logback.classic.Logger log = (ch.qos.logback.classic.Logger) logger;

    Iterator<Appender<ILoggingEvent>> appenderIter = log.iteratorForAppenders();
    while (appenderIter.hasNext()) {
      appenderIter.next().addFilter(new ReplayAwareFilter(context, () -> false));
    }

    log.info("normal info");

    context.setReplaying(true);
    log.info("replay info");

    List<String> logs = Files.readAllLines(logFile.toPath());
    assertEquals(1, logs.size());
    assertTrue(logs.get(0).contains("normal info"));
  }

  @Test
  public void testReplayAwareFilterReplayLoggingEnabled() throws IOException {
    TestContext context = new TestContext(false);

    ch.qos.logback.classic.Logger log = (ch.qos.logback.classic.Logger) logger;

    Iterator<Appender<ILoggingEvent>> appenderIter = log.iteratorForAppenders();
    while (appenderIter.hasNext()) {
      appenderIter.next().addFilter(new ReplayAwareFilter(context, () -> true));
    }

    log.info("normal info");

    context.setReplaying(true);
    log.info("replay info");

    List<String> logs = Files.readAllLines(logFile.toPath());
    assertEquals(2, logs.size());
    assertTrue(logs.get(0).contains("normal info"));
    assertTrue(logs.get(1).contains("replay info"));
  }
}
