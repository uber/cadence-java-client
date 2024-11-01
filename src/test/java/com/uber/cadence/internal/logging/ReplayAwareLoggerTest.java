/**
 * Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * <p>Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. A copy of the License is located at
 *
 * <p>http://aws.amazon.com/apache2.0
 *
 * <p>or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.uber.cadence.internal.logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uber.cadence.internal.replay.ReplayAware;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.Marker;

public class ReplayAwareLoggerTest {

  @Mock private Logger mockLogger;

  @Mock private ReplayAware mockReplayAware;

  @Mock private Supplier<Boolean> enableLoggingInReplay;

  private ReplayAwareLogger replayAwareLogger;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    replayAwareLogger = new ReplayAwareLogger(mockLogger, mockReplayAware, enableLoggingInReplay);
  }

  private void setReplayMode(boolean isReplaying, boolean loggingEnabledInReplay) {
    when(mockReplayAware.isReplaying()).thenReturn(isReplaying);
    when(enableLoggingInReplay.get()).thenReturn(loggingEnabledInReplay);
  }

  // ===========================
  // Tests for is*Enabled Methods
  // ===========================

  @Test
  public void testIsTraceEnabledWhenLoggingAllowed() {
    setReplayMode(false, false);
    when(mockLogger.isTraceEnabled()).thenReturn(true);
    assertTrue(replayAwareLogger.isTraceEnabled());
  }

  @Test
  public void testIsTraceEnabledWhenLoggingSkipped() {
    setReplayMode(true, false);
    assertFalse(replayAwareLogger.isTraceEnabled());
  }

  @Test
  public void testIsTraceEnabledWhenReplayAndEnabled() {
    setReplayMode(true, true);
    when(mockLogger.isTraceEnabled()).thenReturn(true);
    assertTrue(replayAwareLogger.isTraceEnabled());
  }

  @Test
  public void testIsDebugEnabledWhenLoggingAllowed() {
    setReplayMode(false, false);
    when(mockLogger.isDebugEnabled()).thenReturn(true);
    assertTrue(replayAwareLogger.isDebugEnabled());
  }

  @Test
  public void testIsDebugEnabledWhenLoggingSkipped() {
    setReplayMode(true, false);
    assertFalse(replayAwareLogger.isDebugEnabled());
  }

  @Test
  public void testIsDebugEnabledWhenReplayAndEnabled() {
    setReplayMode(true, true);
    when(mockLogger.isDebugEnabled()).thenReturn(true);
    assertTrue(replayAwareLogger.isDebugEnabled());
  }

  @Test
  public void testIsInfoEnabledWhenLoggingAllowed() {
    setReplayMode(false, false);
    when(mockLogger.isInfoEnabled()).thenReturn(true);
    assertTrue(replayAwareLogger.isInfoEnabled());
  }

  @Test
  public void testIsInfoEnabledWhenLoggingSkipped() {
    setReplayMode(true, false);
    assertFalse(replayAwareLogger.isInfoEnabled());
  }

  @Test
  public void testIsInfoEnabledWhenReplayAndEnabled() {
    setReplayMode(true, true);
    when(mockLogger.isInfoEnabled()).thenReturn(true);
    assertTrue(replayAwareLogger.isInfoEnabled());
  }

  @Test
  public void testIsWarnEnabledWhenLoggingAllowed() {
    setReplayMode(false, false);
    when(mockLogger.isWarnEnabled()).thenReturn(true);
    assertTrue(replayAwareLogger.isWarnEnabled());
  }

  @Test
  public void testIsWarnEnabledWhenLoggingSkipped() {
    setReplayMode(true, false);
    assertFalse(replayAwareLogger.isWarnEnabled());
  }

  @Test
  public void testIsWarnEnabledWhenReplayAndEnabled() {
    setReplayMode(true, true);
    when(mockLogger.isWarnEnabled()).thenReturn(true);
    assertTrue(replayAwareLogger.isWarnEnabled());
  }

  @Test
  public void testIsErrorEnabledWhenLoggingAllowed() {
    setReplayMode(false, false);
    when(mockLogger.isErrorEnabled()).thenReturn(true);
    assertTrue(replayAwareLogger.isErrorEnabled());
  }

  @Test
  public void testIsErrorEnabledWhenLoggingSkipped() {
    setReplayMode(true, false);
    assertFalse(replayAwareLogger.isErrorEnabled());
  }

  @Test
  public void testIsErrorEnabledWhenReplayAndEnabled() {
    setReplayMode(true, true);
    when(mockLogger.isErrorEnabled()).thenReturn(true);
    assertTrue(replayAwareLogger.isErrorEnabled());
  }

  // ===========================
  // Tests for Each Log Level
  // ===========================

  // TRACE Level Methods
  @Test
  public void testTraceMethods() {
    setReplayMode(false, false);
    replayAwareLogger.trace("Trace message");
    replayAwareLogger.trace("Trace format {}", "arg");
    replayAwareLogger.trace("Trace format {} {}", "arg1", "arg2");
    replayAwareLogger.trace("Trace format {}", "arg1", "arg2", "arg3");
    Throwable exception = new RuntimeException("Test exception");
    replayAwareLogger.trace("Trace with exception", exception);
    Marker marker = mock(Marker.class);
    replayAwareLogger.trace(marker, "Marker trace");

    verify(mockLogger).trace("Trace message");
    verify(mockLogger).trace("Trace format {}", "arg");
    verify(mockLogger).trace("Trace format {} {}", "arg1", "arg2");
    verify(mockLogger).trace("Trace format {}", "arg1", "arg2", "arg3");
    verify(mockLogger).trace("Trace with exception", exception);
    verify(mockLogger).trace(marker, "Marker trace");
  }

  // DEBUG Level Methods
  @Test
  public void testDebugMethods() {
    setReplayMode(false, false);
    replayAwareLogger.debug("Debug message");
    replayAwareLogger.debug("Debug format {}", "arg");
    replayAwareLogger.debug("Debug format {} {}", "arg1", "arg2");
    replayAwareLogger.debug("Debug format {}", "arg1", "arg2", "arg3");
    Throwable exception = new RuntimeException("Test exception");
    replayAwareLogger.debug("Debug with exception", exception);
    Marker marker = mock(Marker.class);
    replayAwareLogger.debug(marker, "Marker debug");

    verify(mockLogger).debug("Debug message");
    verify(mockLogger).debug("Debug format {}", "arg");
    verify(mockLogger).debug("Debug format {} {}", "arg1", "arg2");
    verify(mockLogger).debug("Debug format {}", "arg1", "arg2", "arg3");
    verify(mockLogger).debug("Debug with exception", exception);
    verify(mockLogger).debug(marker, "Marker debug");
  }

  // INFO Level Methods
  @Test
  public void testInfoMethods() {
    setReplayMode(false, false);
    replayAwareLogger.info("Info message");
    replayAwareLogger.info("Info format {}", "arg");
    replayAwareLogger.info("Info format {} {}", "arg1", "arg2");
    replayAwareLogger.info("Info format {}", "arg1", "arg2", "arg3");
    Throwable exception = new RuntimeException("Test exception");
    replayAwareLogger.info("Info with exception", exception);
    Marker marker = mock(Marker.class);
    replayAwareLogger.info(marker, "Marker info");

    verify(mockLogger).info("Info message");
    verify(mockLogger).info("Info format {}", "arg");
    verify(mockLogger).info("Info format {} {}", "arg1", "arg2");
    verify(mockLogger).info("Info format {}", "arg1", "arg2", "arg3");
    verify(mockLogger).info("Info with exception", exception);
    verify(mockLogger).info(marker, "Marker info");
  }

  // WARN Level Methods
  @Test
  public void testWarnMethods() {
    setReplayMode(false, false);
    replayAwareLogger.warn("Warn message");
    replayAwareLogger.warn("Warn format {}", "arg");
    replayAwareLogger.warn("Warn format {} {}", "arg1", "arg2");
    replayAwareLogger.warn("Warn format {}", "arg1", "arg2", "arg3");
    Throwable exception = new RuntimeException("Test exception");
    replayAwareLogger.warn("Warn with exception", exception);
    Marker marker = mock(Marker.class);
    replayAwareLogger.warn(marker, "Marker warn");

    verify(mockLogger).warn("Warn message");
    verify(mockLogger).warn("Warn format {}", "arg");
    verify(mockLogger).warn("Warn format {} {}", "arg1", "arg2");
    verify(mockLogger).warn("Warn format {}", "arg1", "arg2", "arg3");
    verify(mockLogger).warn("Warn with exception", exception);
    verify(mockLogger).warn(marker, "Marker warn");
  }

  // ERROR Level Methods
  @Test
  public void testErrorMethods() {
    setReplayMode(false, false);
    replayAwareLogger.error("Error message");
    replayAwareLogger.error("Error format {}", "arg");
    replayAwareLogger.error("Error format {} {}", "arg1", "arg2");
    replayAwareLogger.error("Error format {}", "arg1", "arg2", "arg3");
    Throwable exception = new RuntimeException("Test exception");
    replayAwareLogger.error("Error with exception", exception);
    Marker marker = mock(Marker.class);
    replayAwareLogger.error(marker, "Marker error");

    verify(mockLogger).error("Error message");
    verify(mockLogger).error("Error format {}", "arg");
    verify(mockLogger).error("Error format {} {}", "arg1", "arg2");
    verify(mockLogger).error("Error format {}", "arg1", "arg2", "arg3");
    verify(mockLogger).error("Error with exception", exception);
    verify(mockLogger).error(marker, "Marker error");
  }

  // ===========================
  // Test getName Delegation
  // ===========================
  @Test
  public void testGetNameDelegation() {
    when(mockLogger.getName()).thenReturn("TestLogger");
    assertEquals("TestLogger", replayAwareLogger.getName());
    verify(mockLogger).getName();
  }
}
