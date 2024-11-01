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
import static org.mockito.Mockito.never;
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
  // Tests for trace(String)
  // ===========================
  @Test
  public void testTraceMessage_Normal() {
    setReplayMode(false, false);
    replayAwareLogger.trace("Trace message");
    verify(mockLogger).trace("Trace message");
  }

  @Test
  public void testTraceMessage_ReplayModeDisabled() {
    setReplayMode(true, false);
    replayAwareLogger.trace("Trace message");
    verify(mockLogger, never()).trace("Trace message");
  }

  @Test
  public void testTraceMessage_ReplayModeEnabled() {
    setReplayMode(true, true);
    replayAwareLogger.trace("Trace message");
    verify(mockLogger).trace("Trace message");
  }

  // ===========================
  // Tests for trace(String, Object)
  // ===========================
  @Test
  public void testTraceMessageWithOneArg_Normal() {
    setReplayMode(false, false);
    replayAwareLogger.trace("Trace format {}", "arg");
    verify(mockLogger).trace("Trace format {}", "arg");
  }

  @Test
  public void testTraceMessageWithOneArg_ReplayModeDisabled() {
    setReplayMode(true, false);
    replayAwareLogger.trace("Trace format {}", "arg");
    verify(mockLogger, never()).trace("Trace format {}", "arg");
  }

  @Test
  public void testTraceMessageWithOneArg_ReplayModeEnabled() {
    setReplayMode(true, true);
    replayAwareLogger.trace("Trace format {}", "arg");
    verify(mockLogger).trace("Trace format {}", "arg");
  }

  // ===========================
  // Tests for trace(String, Object, Object)
  // ===========================
  @Test
  public void testTraceMessageWithTwoArgs_Normal() {
    setReplayMode(false, false);
    replayAwareLogger.trace("Trace format {} {}", "arg1", "arg2");
    verify(mockLogger).trace("Trace format {} {}", "arg1", "arg2");
  }

  @Test
  public void testTraceMessageWithTwoArgs_ReplayModeDisabled() {
    setReplayMode(true, false);
    replayAwareLogger.trace("Trace format {} {}", "arg1", "arg2");
    verify(mockLogger, never()).trace("Trace format {} {}", "arg1", "arg2");
  }

  @Test
  public void testTraceMessageWithTwoArgs_ReplayModeEnabled() {
    setReplayMode(true, true);
    replayAwareLogger.trace("Trace format {} {}", "arg1", "arg2");
    verify(mockLogger).trace("Trace format {} {}", "arg1", "arg2");
  }

  // ===========================
  // Tests for trace(String, Object...)
  // ===========================
  @Test
  public void testTraceMessageWithMultipleArgs_Normal() {
    setReplayMode(false, false);
    replayAwareLogger.trace("Trace format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger).trace("Trace format {} {} {}", "arg1", "arg2", "arg3");
  }

  @Test
  public void testTraceMessageWithMultipleArgs_ReplayModeDisabled() {
    setReplayMode(true, false);
    replayAwareLogger.trace("Trace format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger, never()).trace("Trace format {} {} {}", "arg1", "arg2", "arg3");
  }

  @Test
  public void testTraceMessageWithMultipleArgs_ReplayModeEnabled() {
    setReplayMode(true, true);
    replayAwareLogger.trace("Trace format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger).trace("Trace format {} {} {}", "arg1", "arg2", "arg3");
  }

  // ===========================
  // Tests for trace(String, Throwable)
  // ===========================
  @Test
  public void testTraceMessageWithThrowable_Normal() {
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(false, false);
    replayAwareLogger.trace("Trace message with throwable", exception);
    verify(mockLogger).trace("Trace message with throwable", exception);
  }

  @Test
  public void testTraceMessageWithThrowable_ReplayModeDisabled() {
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(true, false);
    replayAwareLogger.trace("Trace message with throwable", exception);
    verify(mockLogger, never()).trace("Trace message with throwable", exception);
  }

  @Test
  public void testTraceMessageWithThrowable_ReplayModeEnabled() {
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(true, true);
    replayAwareLogger.trace("Trace message with throwable", exception);
    verify(mockLogger).trace("Trace message with throwable", exception);
  }

  // ===========================
  // Tests for trace(Marker, String)
  // ===========================
  @Test
  public void testTraceWithMarkerAndMessage_Normal() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false);
    replayAwareLogger.trace(marker, "Marker trace message");
    verify(mockLogger).trace(marker, "Marker trace message");
  }

  @Test
  public void testTraceWithMarkerAndMessage_ReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, false);
    replayAwareLogger.trace(marker, "Marker trace message");
    verify(mockLogger, never()).trace(marker, "Marker trace message");
  }

  @Test
  public void testTraceWithMarkerAndMessage_ReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true);
    replayAwareLogger.trace(marker, "Marker trace message");
    verify(mockLogger).trace(marker, "Marker trace message");
  }

  // ===========================
  // Tests for trace(Marker, String, Object)
  // ===========================
  @Test
  public void testTraceWithMarkerAndOneArg_Normal() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false);
    replayAwareLogger.trace(marker, "Marker trace format {}", "arg");
    verify(mockLogger).trace(marker, "Marker trace format {}", "arg");
  }

  @Test
  public void testTraceWithMarkerAndOneArg_ReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, false);
    replayAwareLogger.trace(marker, "Marker trace format {}", "arg");
    verify(mockLogger, never()).trace(marker, "Marker trace format {}", "arg");
  }

  @Test
  public void testTraceWithMarkerAndOneArg_ReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true);
    replayAwareLogger.trace(marker, "Marker trace format {}", "arg");
    verify(mockLogger).trace(marker, "Marker trace format {}", "arg");
  }

  // ===========================
  // Tests for trace(Marker, String, Object, Object)
  // ===========================
  @Test
  public void testTraceWithMarkerAndTwoArgs_Normal() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false);
    replayAwareLogger.trace(marker, "Marker trace format {} {}", "arg1", "arg2");
    verify(mockLogger).trace(marker, "Marker trace format {} {}", "arg1", "arg2");
  }

  @Test
  public void testTraceWithMarkerAndTwoArgs_ReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, false);
    replayAwareLogger.trace(marker, "Marker trace format {} {}", "arg1", "arg2");
    verify(mockLogger, never()).trace(marker, "Marker trace format {} {}", "arg1", "arg2");
  }

  @Test
  public void testTraceWithMarkerAndTwoArgs_ReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true);
    replayAwareLogger.trace(marker, "Marker trace format {} {}", "arg1", "arg2");
    verify(mockLogger).trace(marker, "Marker trace format {} {}", "arg1", "arg2");
  }

  // ===========================
  // Tests for trace(Marker, String, Object...)
  // ===========================
  @Test
  public void testTraceWithMarkerAndMultipleArgs_Normal() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false);
    replayAwareLogger.trace(marker, "Marker trace format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger).trace(marker, "Marker trace format {} {} {}", "arg1", "arg2", "arg3");
  }

  @Test
  public void testTraceWithMarkerAndMultipleArgs_ReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, false);
    replayAwareLogger.trace(marker, "Marker trace format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger, never())
        .trace(marker, "Marker trace format {} {} {}", "arg1", "arg2", "arg3");
  }

  @Test
  public void testTraceWithMarkerAndMultipleArgs_ReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true);
    replayAwareLogger.trace(marker, "Marker trace format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger).trace(marker, "Marker trace format {} {} {}", "arg1", "arg2", "arg3");
  }

  // ===========================
  // Tests for trace(Marker, String, Throwable)
  // ===========================
  @Test
  public void testTraceWithMarkerAndThrowable_Normal() {
    Marker marker = mock(Marker.class);
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(false, false);
    replayAwareLogger.trace(marker, "Marker trace with throwable", exception);
    verify(mockLogger).trace(marker, "Marker trace with throwable", exception);
  }

  @Test
  public void testTraceWithMarkerAndThrowable_ReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(true, false);
    replayAwareLogger.trace(marker, "Marker trace with throwable", exception);
    verify(mockLogger, never()).trace(marker, "Marker trace with throwable", exception);
  }

  @Test
  public void testTraceWithMarkerAndThrowable_ReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(true, true);
    replayAwareLogger.trace(marker, "Marker trace with throwable", exception);
    verify(mockLogger).trace(marker, "Marker trace with throwable", exception);
  }

  // ===========================
  // Tests for isTraceEnabled()
  // ===========================

  @Test
  public void testIsTraceEnabled_Normal() {
    setReplayMode(false, false); // Replay mode off
    when(mockLogger.isTraceEnabled()).thenReturn(true);
    assertTrue(replayAwareLogger.isTraceEnabled());
  }

  @Test
  public void testIsTraceEnabled_InReplayModeDisabled() {
    setReplayMode(true, false); // Replay mode on, logging disabled
    assertFalse(replayAwareLogger.isTraceEnabled());
  }

  @Test
  public void testIsTraceEnabled_InReplayModeEnabled() {
    setReplayMode(true, true); // Replay mode on, logging enabled
    when(mockLogger.isTraceEnabled()).thenReturn(true);
    assertTrue(replayAwareLogger.isTraceEnabled());
  }

  @Test
  public void testIsTraceEnabled_NormalWhenDisabled() {
    setReplayMode(false, false); // Replay mode off
    when(mockLogger.isTraceEnabled()).thenReturn(false);
    assertFalse(replayAwareLogger.isTraceEnabled());
  }

  @Test
  public void testIsTraceEnabled_InReplayModeEnabledWhenLoggerDisabled() {
    setReplayMode(true, true); // Replay mode on, logging enabled
    when(mockLogger.isTraceEnabled()).thenReturn(false);
    assertFalse(replayAwareLogger.isTraceEnabled());
  }

  // ===========================
  // Tests for isTraceEnabled(Marker)
  // ===========================

  @Test
  public void testIsTraceEnabledWithMarker_Normal() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false); // Replay mode off
    when(mockLogger.isTraceEnabled(marker)).thenReturn(true);
    assertTrue(replayAwareLogger.isTraceEnabled(marker));
  }

  @Test
  public void testIsTraceEnabledWithMarker_InReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, false); // Replay mode on, logging disabled
    assertFalse(replayAwareLogger.isTraceEnabled(marker));
  }

  @Test
  public void testIsTraceEnabledWithMarker_InReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true); // Replay mode on, logging enabled
    when(mockLogger.isTraceEnabled(marker)).thenReturn(true);
    assertTrue(replayAwareLogger.isTraceEnabled(marker));
  }

  @Test
  public void testIsTraceEnabledWithMarker_NormalWhenDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false); // Replay mode off
    when(mockLogger.isTraceEnabled(marker)).thenReturn(false);
    assertFalse(replayAwareLogger.isTraceEnabled(marker));
  }

  @Test
  public void testIsTraceEnabledWithMarker_InReplayModeEnabledWhenLoggerDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true); // Replay mode on, logging enabled
    when(mockLogger.isTraceEnabled(marker)).thenReturn(false);
    assertFalse(replayAwareLogger.isTraceEnabled(marker));
  }
  // ===========================
  // Tests for debug(String)
  // ===========================
  @Test
  public void testDebugMessage_Normal() {
    setReplayMode(false, false);
    replayAwareLogger.debug("Debug message");
    verify(mockLogger).debug("Debug message");
  }

  @Test
  public void testDebugMessage_ReplayModeDisabled() {
    setReplayMode(true, false);
    replayAwareLogger.debug("Debug message");
    verify(mockLogger, never()).debug("Debug message");
  }

  @Test
  public void testDebugMessage_ReplayModeEnabled() {
    setReplayMode(true, true);
    replayAwareLogger.debug("Debug message");
    verify(mockLogger).debug("Debug message");
  }

  // ===========================
  // Tests for debug(String, Object)
  // ===========================
  @Test
  public void testDebugMessageWithOneArg_Normal() {
    setReplayMode(false, false);
    replayAwareLogger.debug("Debug format {}", "arg");
    verify(mockLogger).debug("Debug format {}", "arg");
  }

  @Test
  public void testDebugMessageWithOneArg_ReplayModeDisabled() {
    setReplayMode(true, false);
    replayAwareLogger.debug("Debug format {}", "arg");
    verify(mockLogger, never()).debug("Debug format {}", "arg");
  }

  @Test
  public void testDebugMessageWithOneArg_ReplayModeEnabled() {
    setReplayMode(true, true);
    replayAwareLogger.debug("Debug format {}", "arg");
    verify(mockLogger).debug("Debug format {}", "arg");
  }

  // ===========================
  // Tests for debug(String, Object, Object)
  // ===========================
  @Test
  public void testDebugMessageWithTwoArgs_Normal() {
    setReplayMode(false, false);
    replayAwareLogger.debug("Debug format {} {}", "arg1", "arg2");
    verify(mockLogger).debug("Debug format {} {}", "arg1", "arg2");
  }

  @Test
  public void testDebugMessageWithTwoArgs_ReplayModeDisabled() {
    setReplayMode(true, false);
    replayAwareLogger.debug("Debug format {} {}", "arg1", "arg2");
    verify(mockLogger, never()).debug("Debug format {} {}", "arg1", "arg2");
  }

  @Test
  public void testDebugMessageWithTwoArgs_ReplayModeEnabled() {
    setReplayMode(true, true);
    replayAwareLogger.debug("Debug format {} {}", "arg1", "arg2");
    verify(mockLogger).debug("Debug format {} {}", "arg1", "arg2");
  }

  // ===========================
  // Tests for debug(String, Object...)
  // ===========================
  @Test
  public void testDebugMessageWithMultipleArgs_Normal() {
    setReplayMode(false, false);
    replayAwareLogger.debug("Debug format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger).debug("Debug format {} {} {}", "arg1", "arg2", "arg3");
  }

  @Test
  public void testDebugMessageWithMultipleArgs_ReplayModeDisabled() {
    setReplayMode(true, false);
    replayAwareLogger.debug("Debug format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger, never()).debug("Debug format {} {} {}", "arg1", "arg2", "arg3");
  }

  @Test
  public void testDebugMessageWithMultipleArgs_ReplayModeEnabled() {
    setReplayMode(true, true);
    replayAwareLogger.debug("Debug format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger).debug("Debug format {} {} {}", "arg1", "arg2", "arg3");
  }

  // ===========================
  // Tests for debug(String, Throwable)
  // ===========================
  @Test
  public void testDebugMessageWithThrowable_Normal() {
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(false, false);
    replayAwareLogger.debug("Debug message with throwable", exception);
    verify(mockLogger).debug("Debug message with throwable", exception);
  }

  @Test
  public void testDebugMessageWithThrowable_ReplayModeDisabled() {
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(true, false);
    replayAwareLogger.debug("Debug message with throwable", exception);
    verify(mockLogger, never()).debug("Debug message with throwable", exception);
  }

  @Test
  public void testDebugMessageWithThrowable_ReplayModeEnabled() {
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(true, true);
    replayAwareLogger.debug("Debug message with throwable", exception);
    verify(mockLogger).debug("Debug message with throwable", exception);
  }

  // ===========================
  // Tests for debug(Marker, String)
  // ===========================
  @Test
  public void testDebugWithMarkerAndMessage_Normal() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false);
    replayAwareLogger.debug(marker, "Marker debug message");
    verify(mockLogger).debug(marker, "Marker debug message");
  }

  @Test
  public void testDebugWithMarkerAndMessage_ReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, false);
    replayAwareLogger.debug(marker, "Marker debug message");
    verify(mockLogger, never()).debug(marker, "Marker debug message");
  }

  @Test
  public void testDebugWithMarkerAndMessage_ReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true);
    replayAwareLogger.debug(marker, "Marker debug message");
    verify(mockLogger).debug(marker, "Marker debug message");
  }

  // ===========================
  // Tests for debug(Marker, String, Object)
  // ===========================
  @Test
  public void testDebugWithMarkerAndOneArg_Normal() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false);
    replayAwareLogger.debug(marker, "Marker debug format {}", "arg");
    verify(mockLogger).debug(marker, "Marker debug format {}", "arg");
  }

  @Test
  public void testDebugWithMarkerAndOneArg_ReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, false);
    replayAwareLogger.debug(marker, "Marker debug format {}", "arg");
    verify(mockLogger, never()).debug(marker, "Marker debug format {}", "arg");
  }

  @Test
  public void testDebugWithMarkerAndOneArg_ReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true);
    replayAwareLogger.debug(marker, "Marker debug format {}", "arg");
    verify(mockLogger).debug(marker, "Marker debug format {}", "arg");
  }

  // ===========================
  // Tests for debug(Marker, String, Object, Object)
  // ===========================
  @Test
  public void testDebugWithMarkerAndTwoArgs_Normal() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false);
    replayAwareLogger.debug(marker, "Marker debug format {} {}", "arg1", "arg2");
    verify(mockLogger).debug(marker, "Marker debug format {} {}", "arg1", "arg2");
  }

  @Test
  public void testDebugWithMarkerAndTwoArgs_ReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, false);
    replayAwareLogger.debug(marker, "Marker debug format {} {}", "arg1", "arg2");
    verify(mockLogger, never()).debug(marker, "Marker debug format {} {}", "arg1", "arg2");
  }

  @Test
  public void testDebugWithMarkerAndTwoArgs_ReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true);
    replayAwareLogger.debug(marker, "Marker debug format {} {}", "arg1", "arg2");
    verify(mockLogger).debug(marker, "Marker debug format {} {}", "arg1", "arg2");
  }

  // ===========================
  // Tests for debug(Marker, String, Object...)
  // ===========================
  @Test
  public void testDebugWithMarkerAndMultipleArgs_Normal() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false);
    replayAwareLogger.debug(marker, "Marker debug format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger).debug(marker, "Marker debug format {} {} {}", "arg1", "arg2", "arg3");
  }

  @Test
  public void testDebugWithMarkerAndMultipleArgs_ReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, false);
    replayAwareLogger.debug(marker, "Marker debug format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger, never())
        .debug(marker, "Marker debug format {} {} {}", "arg1", "arg2", "arg3");
  }

  @Test
  public void testDebugWithMarkerAndMultipleArgs_ReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true);
    replayAwareLogger.debug(marker, "Marker debug format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger).debug(marker, "Marker debug format {} {} {}", "arg1", "arg2", "arg3");
  }

  // ===========================
  // Tests for debug(Marker, String, Throwable)
  // ===========================
  @Test
  public void testDebugWithMarkerAndThrowable_Normal() {
    Marker marker = mock(Marker.class);
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(false, false);
    replayAwareLogger.debug(marker, "Marker debug with throwable", exception);
    verify(mockLogger).debug(marker, "Marker debug with throwable", exception);
  }

  @Test
  public void testDebugWithMarkerAndThrowable_ReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(true, false);
    replayAwareLogger.debug(marker, "Marker debug with throwable", exception);
    verify(mockLogger, never()).debug(marker, "Marker debug with throwable", exception);
  }

  @Test
  public void testDebugWithMarkerAndThrowable_ReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(true, true);
    replayAwareLogger.debug(marker, "Marker debug with throwable", exception);
    verify(mockLogger).debug(marker, "Marker debug with throwable", exception);
  }

  // ===========================
  // Tests for isDebugEnabled()
  // ===========================

  @Test
  public void testIsDebugEnabled_Normal() {
    setReplayMode(false, false); // Replay mode off
    when(mockLogger.isDebugEnabled()).thenReturn(true);
    assertTrue(replayAwareLogger.isDebugEnabled());
  }

  @Test
  public void testIsDebugEnabled_InReplayModeDisabled() {
    setReplayMode(true, false); // Replay mode on, logging disabled
    assertFalse(replayAwareLogger.isDebugEnabled());
  }

  @Test
  public void testIsDebugEnabled_InReplayModeEnabled() {
    setReplayMode(true, true); // Replay mode on, logging enabled
    when(mockLogger.isDebugEnabled()).thenReturn(true);
    assertTrue(replayAwareLogger.isDebugEnabled());
  }

  @Test
  public void testIsDebugEnabled_NormalWhenDisabled() {
    setReplayMode(false, false); // Replay mode off
    when(mockLogger.isDebugEnabled()).thenReturn(false);
    assertFalse(replayAwareLogger.isDebugEnabled());
  }

  @Test
  public void testIsDebugEnabled_InReplayModeEnabledWhenLoggerDisabled() {
    setReplayMode(true, true); // Replay mode on, logging enabled
    when(mockLogger.isDebugEnabled()).thenReturn(false);
    assertFalse(replayAwareLogger.isDebugEnabled());
  }

  // ===========================
  // Tests for isDebugEnabled(Marker)
  // ===========================

  @Test
  public void testIsDebugEnabledWithMarker_Normal() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false); // Replay mode off
    when(mockLogger.isDebugEnabled(marker)).thenReturn(true);
    assertTrue(replayAwareLogger.isDebugEnabled(marker));
  }

  @Test
  public void testIsDebugEnabledWithMarker_InReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, false); // Replay mode on, logging disabled
    assertFalse(replayAwareLogger.isDebugEnabled(marker));
  }

  @Test
  public void testIsDebugEnabledWithMarker_InReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true); // Replay mode on, logging enabled
    when(mockLogger.isDebugEnabled(marker)).thenReturn(true);
    assertTrue(replayAwareLogger.isDebugEnabled(marker));
  }

  @Test
  public void testIsDebugEnabledWithMarker_NormalWhenDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false); // Replay mode off
    when(mockLogger.isDebugEnabled(marker)).thenReturn(false);
    assertFalse(replayAwareLogger.isDebugEnabled(marker));
  }

  @Test
  public void testIsDebugEnabledWithMarker_InReplayModeEnabledWhenLoggerDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true); // Replay mode on, logging enabled
    when(mockLogger.isDebugEnabled(marker)).thenReturn(false);
    assertFalse(replayAwareLogger.isDebugEnabled(marker));
  }
  // ===========================
  // Tests for info(String)
  // ===========================
  @Test
  public void testInfoMessage_Normal() {
    setReplayMode(false, false);
    replayAwareLogger.info("Info message");
    verify(mockLogger).info("Info message");
  }

  @Test
  public void testInfoMessage_ReplayModeDisabled() {
    setReplayMode(true, false);
    replayAwareLogger.info("Info message");
    verify(mockLogger, never()).info("Info message");
  }

  @Test
  public void testInfoMessage_ReplayModeEnabled() {
    setReplayMode(true, true);
    replayAwareLogger.info("Info message");
    verify(mockLogger).info("Info message");
  }

  // ===========================
  // Tests for info(String, Object)
  // ===========================
  @Test
  public void testInfoMessageWithOneArg_Normal() {
    setReplayMode(false, false);
    replayAwareLogger.info("Info format {}", "arg");
    verify(mockLogger).info("Info format {}", "arg");
  }

  @Test
  public void testInfoMessageWithOneArg_ReplayModeDisabled() {
    setReplayMode(true, false);
    replayAwareLogger.info("Info format {}", "arg");
    verify(mockLogger, never()).info("Info format {}", "arg");
  }

  @Test
  public void testInfoMessageWithOneArg_ReplayModeEnabled() {
    setReplayMode(true, true);
    replayAwareLogger.info("Info format {}", "arg");
    verify(mockLogger).info("Info format {}", "arg");
  }

  // ===========================
  // Tests for info(String, Object, Object)
  // ===========================
  @Test
  public void testInfoMessageWithTwoArgs_Normal() {
    setReplayMode(false, false);
    replayAwareLogger.info("Info format {} {}", "arg1", "arg2");
    verify(mockLogger).info("Info format {} {}", "arg1", "arg2");
  }

  @Test
  public void testInfoMessageWithTwoArgs_ReplayModeDisabled() {
    setReplayMode(true, false);
    replayAwareLogger.info("Info format {} {}", "arg1", "arg2");
    verify(mockLogger, never()).info("Info format {} {}", "arg1", "arg2");
  }

  @Test
  public void testInfoMessageWithTwoArgs_ReplayModeEnabled() {
    setReplayMode(true, true);
    replayAwareLogger.info("Info format {} {}", "arg1", "arg2");
    verify(mockLogger).info("Info format {} {}", "arg1", "arg2");
  }

  // ===========================
  // Tests for info(String, Object...)
  // ===========================
  @Test
  public void testInfoMessageWithMultipleArgs_Normal() {
    setReplayMode(false, false);
    replayAwareLogger.info("Info format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger).info("Info format {} {} {}", "arg1", "arg2", "arg3");
  }

  @Test
  public void testInfoMessageWithMultipleArgs_ReplayModeDisabled() {
    setReplayMode(true, false);
    replayAwareLogger.info("Info format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger, never()).info("Info format {} {} {}", "arg1", "arg2", "arg3");
  }

  @Test
  public void testInfoMessageWithMultipleArgs_ReplayModeEnabled() {
    setReplayMode(true, true);
    replayAwareLogger.info("Info format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger).info("Info format {} {} {}", "arg1", "arg2", "arg3");
  }

  // ===========================
  // Tests for info(String, Throwable)
  // ===========================
  @Test
  public void testInfoMessageWithThrowable_Normal() {
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(false, false);
    replayAwareLogger.info("Info message with throwable", exception);
    verify(mockLogger).info("Info message with throwable", exception);
  }

  @Test
  public void testInfoMessageWithThrowable_ReplayModeDisabled() {
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(true, false);
    replayAwareLogger.info("Info message with throwable", exception);
    verify(mockLogger, never()).info("Info message with throwable", exception);
  }

  @Test
  public void testInfoMessageWithThrowable_ReplayModeEnabled() {
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(true, true);
    replayAwareLogger.info("Info message with throwable", exception);
    verify(mockLogger).info("Info message with throwable", exception);
  }

  // ===========================
  // Tests for info(Marker, String)
  // ===========================
  @Test
  public void testInfoWithMarkerAndMessage_Normal() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false);
    replayAwareLogger.info(marker, "Marker info message");
    verify(mockLogger).info(marker, "Marker info message");
  }

  @Test
  public void testInfoWithMarkerAndMessage_ReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, false);
    replayAwareLogger.info(marker, "Marker info message");
    verify(mockLogger, never()).info(marker, "Marker info message");
  }

  @Test
  public void testInfoWithMarkerAndMessage_ReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true);
    replayAwareLogger.info(marker, "Marker info message");
    verify(mockLogger).info(marker, "Marker info message");
  }

  // ===========================
  // Tests for info(Marker, String, Object)
  // ===========================
  @Test
  public void testInfoWithMarkerAndOneArg_Normal() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false);
    replayAwareLogger.info(marker, "Marker info format {}", "arg");
    verify(mockLogger).info(marker, "Marker info format {}", "arg");
  }

  @Test
  public void testInfoWithMarkerAndOneArg_ReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, false);
    replayAwareLogger.info(marker, "Marker info format {}", "arg");
    verify(mockLogger, never()).info(marker, "Marker info format {}", "arg");
  }

  @Test
  public void testInfoWithMarkerAndOneArg_ReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true);
    replayAwareLogger.info(marker, "Marker info format {}", "arg");
    verify(mockLogger).info(marker, "Marker info format {}", "arg");
  }

  // ===========================
  // Tests for info(Marker, String, Object, Object)
  // ===========================
  @Test
  public void testInfoWithMarkerAndTwoArgs_Normal() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false);
    replayAwareLogger.info(marker, "Marker info format {} {}", "arg1", "arg2");
    verify(mockLogger).info(marker, "Marker info format {} {}", "arg1", "arg2");
  }

  @Test
  public void testInfoWithMarkerAndTwoArgs_ReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, false);
    replayAwareLogger.info(marker, "Marker info format {} {}", "arg1", "arg2");
    verify(mockLogger, never()).info(marker, "Marker info format {} {}", "arg1", "arg2");
  }

  @Test
  public void testInfoWithMarkerAndTwoArgs_ReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true);
    replayAwareLogger.info(marker, "Marker info format {} {}", "arg1", "arg2");
    verify(mockLogger).info(marker, "Marker info format {} {}", "arg1", "arg2");
  }

  // ===========================
  // Tests for info(Marker, String, Object...)
  // ===========================
  @Test
  public void testInfoWithMarkerAndMultipleArgs_Normal() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false);
    replayAwareLogger.info(marker, "Marker info format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger).info(marker, "Marker info format {} {} {}", "arg1", "arg2", "arg3");
  }

  @Test
  public void testInfoWithMarkerAndMultipleArgs_ReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, false);
    replayAwareLogger.info(marker, "Marker info format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger, never()).info(marker, "Marker info format {} {} {}", "arg1", "arg2", "arg3");
  }

  @Test
  public void testInfoWithMarkerAndMultipleArgs_ReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true);
    replayAwareLogger.info(marker, "Marker info format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger).info(marker, "Marker info format {} {} {}", "arg1", "arg2", "arg3");
  }

  // ===========================
  // Tests for info(Marker, String, Throwable)
  // ===========================
  @Test
  public void testInfoWithMarkerAndThrowable_Normal() {
    Marker marker = mock(Marker.class);
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(false, false);
    replayAwareLogger.info(marker, "Marker info with throwable", exception);
    verify(mockLogger).info(marker, "Marker info with throwable", exception);
  }

  @Test
  public void testInfoWithMarkerAndThrowable_ReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(true, false);
    replayAwareLogger.info(marker, "Marker info with throwable", exception);
    verify(mockLogger, never()).info(marker, "Marker info with throwable", exception);
  }

  @Test
  public void testInfoWithMarkerAndThrowable_ReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(true, true);
    replayAwareLogger.info(marker, "Marker info with throwable", exception);
    verify(mockLogger).info(marker, "Marker info with throwable", exception);
  }

  // ===========================
  // Tests for isInfoEnabled()
  // ===========================

  @Test
  public void testIsInfoEnabled_Normal() {
    setReplayMode(false, false); // Replay mode off
    when(mockLogger.isInfoEnabled()).thenReturn(true);
    assertTrue(replayAwareLogger.isInfoEnabled());
  }

  @Test
  public void testIsInfoEnabled_InReplayModeDisabled() {
    setReplayMode(true, false); // Replay mode on, logging disabled
    assertFalse(replayAwareLogger.isInfoEnabled());
  }

  @Test
  public void testIsInfoEnabled_InReplayModeEnabled() {
    setReplayMode(true, true); // Replay mode on, logging enabled
    when(mockLogger.isInfoEnabled()).thenReturn(true);
    assertTrue(replayAwareLogger.isInfoEnabled());
  }

  @Test
  public void testIsInfoEnabled_NormalWhenDisabled() {
    setReplayMode(false, false); // Replay mode off
    when(mockLogger.isInfoEnabled()).thenReturn(false);
    assertFalse(replayAwareLogger.isInfoEnabled());
  }

  @Test
  public void testIsInfoEnabled_InReplayModeEnabledWhenLoggerDisabled() {
    setReplayMode(true, true); // Replay mode on, logging enabled
    when(mockLogger.isInfoEnabled()).thenReturn(false);
    assertFalse(replayAwareLogger.isInfoEnabled());
  }

  // ===========================
  // Tests for isInfoEnabled(Marker)
  // ===========================

  @Test
  public void testIsInfoEnabledWithMarker_Normal() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false); // Replay mode off
    when(mockLogger.isInfoEnabled(marker)).thenReturn(true);
    assertTrue(replayAwareLogger.isInfoEnabled(marker));
  }

  @Test
  public void testIsInfoEnabledWithMarker_InReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, false); // Replay mode on, logging disabled
    assertFalse(replayAwareLogger.isInfoEnabled(marker));
  }

  @Test
  public void testIsInfoEnabledWithMarker_InReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true); // Replay mode on, logging enabled
    when(mockLogger.isInfoEnabled(marker)).thenReturn(true);
    assertTrue(replayAwareLogger.isInfoEnabled(marker));
  }

  @Test
  public void testIsInfoEnabledWithMarker_NormalWhenDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false); // Replay mode off
    when(mockLogger.isInfoEnabled(marker)).thenReturn(false);
    assertFalse(replayAwareLogger.isInfoEnabled(marker));
  }

  @Test
  public void testIsInfoEnabledWithMarker_InReplayModeEnabledWhenLoggerDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true); // Replay mode on, logging enabled
    when(mockLogger.isInfoEnabled(marker)).thenReturn(false);
    assertFalse(replayAwareLogger.isInfoEnabled(marker));
  }
  // ===========================
  // Tests for warn(String)
  // ===========================
  @Test
  public void testWarnMessage_Normal() {
    setReplayMode(false, false);
    replayAwareLogger.warn("Warn message");
    verify(mockLogger).warn("Warn message");
  }

  @Test
  public void testWarnMessage_ReplayModeDisabled() {
    setReplayMode(true, false);
    replayAwareLogger.warn("Warn message");
    verify(mockLogger, never()).warn("Warn message");
  }

  @Test
  public void testWarnMessage_ReplayModeEnabled() {
    setReplayMode(true, true);
    replayAwareLogger.warn("Warn message");
    verify(mockLogger).warn("Warn message");
  }

  // ===========================
  // Tests for warn(String, Object)
  // ===========================
  @Test
  public void testWarnMessageWithOneArg_Normal() {
    setReplayMode(false, false);
    replayAwareLogger.warn("Warn format {}", "arg");
    verify(mockLogger).warn("Warn format {}", "arg");
  }

  @Test
  public void testWarnMessageWithOneArg_ReplayModeDisabled() {
    setReplayMode(true, false);
    replayAwareLogger.warn("Warn format {}", "arg");
    verify(mockLogger, never()).warn("Warn format {}", "arg");
  }

  @Test
  public void testWarnMessageWithOneArg_ReplayModeEnabled() {
    setReplayMode(true, true);
    replayAwareLogger.warn("Warn format {}", "arg");
    verify(mockLogger).warn("Warn format {}", "arg");
  }

  // ===========================
  // Tests for warn(String, Object, Object)
  // ===========================
  @Test
  public void testWarnMessageWithTwoArgs_Normal() {
    setReplayMode(false, false);
    replayAwareLogger.warn("Warn format {} {}", "arg1", "arg2");
    verify(mockLogger).warn("Warn format {} {}", "arg1", "arg2");
  }

  @Test
  public void testWarnMessageWithTwoArgs_ReplayModeDisabled() {
    setReplayMode(true, false);
    replayAwareLogger.warn("Warn format {} {}", "arg1", "arg2");
    verify(mockLogger, never()).warn("Warn format {} {}", "arg1", "arg2");
  }

  @Test
  public void testWarnMessageWithTwoArgs_ReplayModeEnabled() {
    setReplayMode(true, true);
    replayAwareLogger.warn("Warn format {} {}", "arg1", "arg2");
    verify(mockLogger).warn("Warn format {} {}", "arg1", "arg2");
  }

  // ===========================
  // Tests for warn(String, Object...)
  // ===========================
  @Test
  public void testWarnMessageWithMultipleArgs_Normal() {
    setReplayMode(false, false);
    replayAwareLogger.warn("Warn format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger).warn("Warn format {} {} {}", "arg1", "arg2", "arg3");
  }

  @Test
  public void testWarnMessageWithMultipleArgs_ReplayModeDisabled() {
    setReplayMode(true, false);
    replayAwareLogger.warn("Warn format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger, never()).warn("Warn format {} {} {}", "arg1", "arg2", "arg3");
  }

  @Test
  public void testWarnMessageWithMultipleArgs_ReplayModeEnabled() {
    setReplayMode(true, true);
    replayAwareLogger.warn("Warn format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger).warn("Warn format {} {} {}", "arg1", "arg2", "arg3");
  }

  // ===========================
  // Tests for warn(String, Throwable)
  // ===========================
  @Test
  public void testWarnMessageWithThrowable_Normal() {
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(false, false);
    replayAwareLogger.warn("Warn message with throwable", exception);
    verify(mockLogger).warn("Warn message with throwable", exception);
  }

  @Test
  public void testWarnMessageWithThrowable_ReplayModeDisabled() {
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(true, false);
    replayAwareLogger.warn("Warn message with throwable", exception);
    verify(mockLogger, never()).warn("Warn message with throwable", exception);
  }

  @Test
  public void testWarnMessageWithThrowable_ReplayModeEnabled() {
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(true, true);
    replayAwareLogger.warn("Warn message with throwable", exception);
    verify(mockLogger).warn("Warn message with throwable", exception);
  }

  // ===========================
  // Tests for warn(Marker, String)
  // ===========================
  @Test
  public void testWarnWithMarkerAndMessage_Normal() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false);
    replayAwareLogger.warn(marker, "Marker warn message");
    verify(mockLogger).warn(marker, "Marker warn message");
  }

  @Test
  public void testWarnWithMarkerAndMessage_ReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, false);
    replayAwareLogger.warn(marker, "Marker warn message");
    verify(mockLogger, never()).warn(marker, "Marker warn message");
  }

  @Test
  public void testWarnWithMarkerAndMessage_ReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true);
    replayAwareLogger.warn(marker, "Marker warn message");
    verify(mockLogger).warn(marker, "Marker warn message");
  }

  // ===========================
  // Tests for warn(Marker, String, Object)
  // ===========================
  @Test
  public void testWarnWithMarkerAndOneArg_Normal() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false);
    replayAwareLogger.warn(marker, "Marker warn format {}", "arg");
    verify(mockLogger).warn(marker, "Marker warn format {}", "arg");
  }

  @Test
  public void testWarnWithMarkerAndOneArg_ReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, false);
    replayAwareLogger.warn(marker, "Marker warn format {}", "arg");
    verify(mockLogger, never()).warn(marker, "Marker warn format {}", "arg");
  }

  @Test
  public void testWarnWithMarkerAndOneArg_ReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true);
    replayAwareLogger.warn(marker, "Marker warn format {}", "arg");
    verify(mockLogger).warn(marker, "Marker warn format {}", "arg");
  }

  // ===========================
  // Tests for warn(Marker, String, Object, Object)
  // ===========================
  @Test
  public void testWarnWithMarkerAndTwoArgs_Normal() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false);
    replayAwareLogger.warn(marker, "Marker warn format {} {}", "arg1", "arg2");
    verify(mockLogger).warn(marker, "Marker warn format {} {}", "arg1", "arg2");
  }

  @Test
  public void testWarnWithMarkerAndTwoArgs_ReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, false);
    replayAwareLogger.warn(marker, "Marker warn format {} {}", "arg1", "arg2");
    verify(mockLogger, never()).warn(marker, "Marker warn format {} {}", "arg1", "arg2");
  }

  @Test
  public void testWarnWithMarkerAndTwoArgs_ReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true);
    replayAwareLogger.warn(marker, "Marker warn format {} {}", "arg1", "arg2");
    verify(mockLogger).warn(marker, "Marker warn format {} {}", "arg1", "arg2");
  }

  // ===========================
  // Tests for warn(Marker, String, Object...)
  // ===========================
  @Test
  public void testWarnWithMarkerAndMultipleArgs_Normal() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false);
    replayAwareLogger.warn(marker, "Marker warn format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger).warn(marker, "Marker warn format {} {} {}", "arg1", "arg2", "arg3");
  }

  @Test
  public void testWarnWithMarkerAndMultipleArgs_ReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, false);
    replayAwareLogger.warn(marker, "Marker warn format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger, never()).warn(marker, "Marker warn format {} {} {}", "arg1", "arg2", "arg3");
  }

  @Test
  public void testWarnWithMarkerAndMultipleArgs_ReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true);
    replayAwareLogger.warn(marker, "Marker warn format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger).warn(marker, "Marker warn format {} {} {}", "arg1", "arg2", "arg3");
  }

  // ===========================
  // Tests for warn(Marker, String, Throwable)
  // ===========================
  @Test
  public void testWarnWithMarkerAndThrowable_Normal() {
    Marker marker = mock(Marker.class);
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(false, false);
    replayAwareLogger.warn(marker, "Marker warn with throwable", exception);
    verify(mockLogger).warn(marker, "Marker warn with throwable", exception);
  }

  @Test
  public void testWarnWithMarkerAndThrowable_ReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(true, false);
    replayAwareLogger.warn(marker, "Marker warn with throwable", exception);
    verify(mockLogger, never()).warn(marker, "Marker warn with throwable", exception);
  }

  @Test
  public void testWarnWithMarkerAndThrowable_ReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(true, true);
    replayAwareLogger.warn(marker, "Marker warn with throwable", exception);
    verify(mockLogger).warn(marker, "Marker warn with throwable", exception);
  }

  // ===========================
  // Tests for isWarnEnabled()
  // ===========================

  @Test
  public void testIsWarnEnabled_Normal() {
    setReplayMode(false, false); // Replay mode off
    when(mockLogger.isWarnEnabled()).thenReturn(true);
    assertTrue(replayAwareLogger.isWarnEnabled());
  }

  @Test
  public void testIsWarnEnabled_InReplayModeDisabled() {
    setReplayMode(true, false); // Replay mode on, logging disabled
    assertFalse(replayAwareLogger.isWarnEnabled());
  }

  @Test
  public void testIsWarnEnabled_InReplayModeEnabled() {
    setReplayMode(true, true); // Replay mode on, logging enabled
    when(mockLogger.isWarnEnabled()).thenReturn(true);
    assertTrue(replayAwareLogger.isWarnEnabled());
  }

  @Test
  public void testIsWarnEnabled_NormalWhenDisabled() {
    setReplayMode(false, false); // Replay mode off
    when(mockLogger.isWarnEnabled()).thenReturn(false);
    assertFalse(replayAwareLogger.isWarnEnabled());
  }

  @Test
  public void testIsWarnEnabled_InReplayModeEnabledWhenLoggerDisabled() {
    setReplayMode(true, true); // Replay mode on, logging enabled
    when(mockLogger.isWarnEnabled()).thenReturn(false);
    assertFalse(replayAwareLogger.isWarnEnabled());
  }

  // ===========================
  // Tests for isWarnEnabled(Marker)
  // ===========================

  @Test
  public void testIsWarnEnabledWithMarker_Normal() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false); // Replay mode off
    when(mockLogger.isWarnEnabled(marker)).thenReturn(true);
    assertTrue(replayAwareLogger.isWarnEnabled(marker));
  }

  @Test
  public void testIsWarnEnabledWithMarker_InReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, false); // Replay mode on, logging disabled
    assertFalse(replayAwareLogger.isWarnEnabled(marker));
  }

  @Test
  public void testIsWarnEnabledWithMarker_InReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true); // Replay mode on, logging enabled
    when(mockLogger.isWarnEnabled(marker)).thenReturn(true);
    assertTrue(replayAwareLogger.isWarnEnabled(marker));
  }

  @Test
  public void testIsWarnEnabledWithMarker_NormalWhenDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false); // Replay mode off
    when(mockLogger.isWarnEnabled(marker)).thenReturn(false);
    assertFalse(replayAwareLogger.isWarnEnabled(marker));
  }

  @Test
  public void testIsWarnEnabledWithMarker_InReplayModeEnabledWhenLoggerDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true); // Replay mode on, logging enabled
    when(mockLogger.isWarnEnabled(marker)).thenReturn(false);
    assertFalse(replayAwareLogger.isWarnEnabled(marker));
  }
  // ===========================
  // Tests for error(String)
  // ===========================
  @Test
  public void testErrorMessage_Normal() {
    setReplayMode(false, false);
    replayAwareLogger.error("Error message");
    verify(mockLogger).error("Error message");
  }

  @Test
  public void testErrorMessage_ReplayModeDisabled() {
    setReplayMode(true, false);
    replayAwareLogger.error("Error message");
    verify(mockLogger, never()).error("Error message");
  }

  @Test
  public void testErrorMessage_ReplayModeEnabled() {
    setReplayMode(true, true);
    replayAwareLogger.error("Error message");
    verify(mockLogger).error("Error message");
  }

  // ===========================
  // Tests for error(String, Object)
  // ===========================
  @Test
  public void testErrorMessageWithOneArg_Normal() {
    setReplayMode(false, false);
    replayAwareLogger.error("Error format {}", "arg");
    verify(mockLogger).error("Error format {}", "arg");
  }

  @Test
  public void testErrorMessageWithOneArg_ReplayModeDisabled() {
    setReplayMode(true, false);
    replayAwareLogger.error("Error format {}", "arg");
    verify(mockLogger, never()).error("Error format {}", "arg");
  }

  @Test
  public void testErrorMessageWithOneArg_ReplayModeEnabled() {
    setReplayMode(true, true);
    replayAwareLogger.error("Error format {}", "arg");
    verify(mockLogger).error("Error format {}", "arg");
  }

  // ===========================
  // Tests for error(String, Object, Object)
  // ===========================
  @Test
  public void testErrorMessageWithTwoArgs_Normal() {
    setReplayMode(false, false);
    replayAwareLogger.error("Error format {} {}", "arg1", "arg2");
    verify(mockLogger).error("Error format {} {}", "arg1", "arg2");
  }

  @Test
  public void testErrorMessageWithTwoArgs_ReplayModeDisabled() {
    setReplayMode(true, false);
    replayAwareLogger.error("Error format {} {}", "arg1", "arg2");
    verify(mockLogger, never()).error("Error format {} {}", "arg1", "arg2");
  }

  @Test
  public void testErrorMessageWithTwoArgs_ReplayModeEnabled() {
    setReplayMode(true, true);
    replayAwareLogger.error("Error format {} {}", "arg1", "arg2");
    verify(mockLogger).error("Error format {} {}", "arg1", "arg2");
  }

  // ===========================
  // Tests for error(String, Object...)
  // ===========================
  @Test
  public void testErrorMessageWithMultipleArgs_Normal() {
    setReplayMode(false, false);
    replayAwareLogger.error("Error format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger).error("Error format {} {} {}", "arg1", "arg2", "arg3");
  }

  @Test
  public void testErrorMessageWithMultipleArgs_ReplayModeDisabled() {
    setReplayMode(true, false);
    replayAwareLogger.error("Error format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger, never()).error("Error format {} {} {}", "arg1", "arg2", "arg3");
  }

  @Test
  public void testErrorMessageWithMultipleArgs_ReplayModeEnabled() {
    setReplayMode(true, true);
    replayAwareLogger.error("Error format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger).error("Error format {} {} {}", "arg1", "arg2", "arg3");
  }

  // ===========================
  // Tests for error(String, Throwable)
  // ===========================
  @Test
  public void testErrorMessageWithThrowable_Normal() {
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(false, false);
    replayAwareLogger.error("Error message with throwable", exception);
    verify(mockLogger).error("Error message with throwable", exception);
  }

  @Test
  public void testErrorMessageWithThrowable_ReplayModeDisabled() {
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(true, false);
    replayAwareLogger.error("Error message with throwable", exception);
    verify(mockLogger, never()).error("Error message with throwable", exception);
  }

  @Test
  public void testErrorMessageWithThrowable_ReplayModeEnabled() {
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(true, true);
    replayAwareLogger.error("Error message with throwable", exception);
    verify(mockLogger).error("Error message with throwable", exception);
  }

  // ===========================
  // Tests for error(Marker, String)
  // ===========================
  @Test
  public void testErrorWithMarkerAndMessage_Normal() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false);
    replayAwareLogger.error(marker, "Marker error message");
    verify(mockLogger).error(marker, "Marker error message");
  }

  @Test
  public void testErrorWithMarkerAndMessage_ReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, false);
    replayAwareLogger.error(marker, "Marker error message");
    verify(mockLogger, never()).error(marker, "Marker error message");
  }

  @Test
  public void testErrorWithMarkerAndMessage_ReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true);
    replayAwareLogger.error(marker, "Marker error message");
    verify(mockLogger).error(marker, "Marker error message");
  }

  // ===========================
  // Tests for error(Marker, String, Object)
  // ===========================
  @Test
  public void testErrorWithMarkerAndOneArg_Normal() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false);
    replayAwareLogger.error(marker, "Marker error format {}", "arg");
    verify(mockLogger).error(marker, "Marker error format {}", "arg");
  }

  @Test
  public void testErrorWithMarkerAndOneArg_ReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, false);
    replayAwareLogger.error(marker, "Marker error format {}", "arg");
    verify(mockLogger, never()).error(marker, "Marker error format {}", "arg");
  }

  @Test
  public void testErrorWithMarkerAndOneArg_ReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true);
    replayAwareLogger.error(marker, "Marker error format {}", "arg");
    verify(mockLogger).error(marker, "Marker error format {}", "arg");
  }

  // ===========================
  // Tests for error(Marker, String, Object, Object)
  // ===========================
  @Test
  public void testErrorWithMarkerAndTwoArgs_Normal() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false);
    replayAwareLogger.error(marker, "Marker error format {} {}", "arg1", "arg2");
    verify(mockLogger).error(marker, "Marker error format {} {}", "arg1", "arg2");
  }

  @Test
  public void testErrorWithMarkerAndTwoArgs_ReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, false);
    replayAwareLogger.error(marker, "Marker error format {} {}", "arg1", "arg2");
    verify(mockLogger, never()).error(marker, "Marker error format {} {}", "arg1", "arg2");
  }

  @Test
  public void testErrorWithMarkerAndTwoArgs_ReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true);
    replayAwareLogger.error(marker, "Marker error format {} {}", "arg1", "arg2");
    verify(mockLogger).error(marker, "Marker error format {} {}", "arg1", "arg2");
  }

  // ===========================
  // Tests for error(Marker, String, Object...)
  // ===========================
  @Test
  public void testErrorWithMarkerAndMultipleArgs_Normal() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false);
    replayAwareLogger.error(marker, "Marker error format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger).error(marker, "Marker error format {} {} {}", "arg1", "arg2", "arg3");
  }

  @Test
  public void testErrorWithMarkerAndMultipleArgs_ReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, false);
    replayAwareLogger.error(marker, "Marker error format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger, never())
        .error(marker, "Marker error format {} {} {}", "arg1", "arg2", "arg3");
  }

  @Test
  public void testErrorWithMarkerAndMultipleArgs_ReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true);
    replayAwareLogger.error(marker, "Marker error format {} {} {}", "arg1", "arg2", "arg3");
    verify(mockLogger).error(marker, "Marker error format {} {} {}", "arg1", "arg2", "arg3");
  }

  // ===========================
  // Tests for error(Marker, String, Throwable)
  // ===========================
  @Test
  public void testErrorWithMarkerAndThrowable_Normal() {
    Marker marker = mock(Marker.class);
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(false, false);
    replayAwareLogger.error(marker, "Marker error with throwable", exception);
    verify(mockLogger).error(marker, "Marker error with throwable", exception);
  }

  @Test
  public void testErrorWithMarkerAndThrowable_ReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(true, false);
    replayAwareLogger.error(marker, "Marker error with throwable", exception);
    verify(mockLogger, never()).error(marker, "Marker error with throwable", exception);
  }

  @Test
  public void testErrorWithMarkerAndThrowable_ReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    Throwable exception = new RuntimeException("Test exception");
    setReplayMode(true, true);
    replayAwareLogger.error(marker, "Marker error with throwable", exception);
    verify(mockLogger).error(marker, "Marker error with throwable", exception);
  }

  // ===========================
  // Tests for isErrorEnabled()
  // ===========================

  @Test
  public void testIsErrorEnabled_Normal() {
    setReplayMode(false, false); // Replay mode off
    when(mockLogger.isErrorEnabled()).thenReturn(true);
    assertTrue(replayAwareLogger.isErrorEnabled());
  }

  @Test
  public void testIsErrorEnabled_InReplayModeDisabled() {
    setReplayMode(true, false); // Replay mode on, logging disabled
    assertFalse(replayAwareLogger.isErrorEnabled());
  }

  @Test
  public void testIsErrorEnabled_InReplayModeEnabled() {
    setReplayMode(true, true); // Replay mode on, logging enabled
    when(mockLogger.isErrorEnabled()).thenReturn(true);
    assertTrue(replayAwareLogger.isErrorEnabled());
  }

  @Test
  public void testIsErrorEnabled_NormalWhenDisabled() {
    setReplayMode(false, false); // Replay mode off
    when(mockLogger.isErrorEnabled()).thenReturn(false);
    assertFalse(replayAwareLogger.isErrorEnabled());
  }

  @Test
  public void testIsErrorEnabled_InReplayModeEnabledWhenLoggerDisabled() {
    setReplayMode(true, true); // Replay mode on, logging enabled
    when(mockLogger.isErrorEnabled()).thenReturn(false);
    assertFalse(replayAwareLogger.isErrorEnabled());
  }

  // ===========================
  // Tests for isErrorEnabled(Marker)
  // ===========================

  @Test
  public void testIsErrorEnabledWithMarker_Normal() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false); // Replay mode off
    when(mockLogger.isErrorEnabled(marker)).thenReturn(true);
    assertTrue(replayAwareLogger.isErrorEnabled(marker));
  }

  @Test
  public void testIsErrorEnabledWithMarker_InReplayModeDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, false); // Replay mode on, logging disabled
    assertFalse(replayAwareLogger.isErrorEnabled(marker));
  }

  @Test
  public void testIsErrorEnabledWithMarker_InReplayModeEnabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true); // Replay mode on, logging enabled
    when(mockLogger.isErrorEnabled(marker)).thenReturn(true);
    assertTrue(replayAwareLogger.isErrorEnabled(marker));
  }

  @Test
  public void testIsErrorEnabledWithMarker_NormalWhenDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(false, false); // Replay mode off
    when(mockLogger.isErrorEnabled(marker)).thenReturn(false);
    assertFalse(replayAwareLogger.isErrorEnabled(marker));
  }

  @Test
  public void testIsErrorEnabledWithMarker_InReplayModeEnabledWhenLoggerDisabled() {
    Marker marker = mock(Marker.class);
    setReplayMode(true, true); // Replay mode on, logging enabled
    when(mockLogger.isErrorEnabled(marker)).thenReturn(false);
    assertFalse(replayAwareLogger.isErrorEnabled(marker));
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
