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

package com.uber.cadence.internal.replay;

import com.uber.cadence.EventType;
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.MarkerRecordedEventAttributes;
import com.uber.cadence.internal.replay.DecisionContext.MutableSideEffectData;
import com.uber.cadence.workflow.Functions.Func1;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MutableSideEffectHandler {
  private static final class MutableSideEffectResult {

    private final byte[] data;

    /**
     * Count of how many times the mutableSideEffect was called since the last marker recorded. It
     * is used to ensure that an updated value is returned after the same exact number of times
     * during a replay.
     */
    private int accessCount;

    private MutableSideEffectResult(byte[] data) {
      this.data = data;
    }

    public byte[] getData() {
      accessCount++;
      return data;
    }

    int getAccessCount() {
      return accessCount;
    }
  }

  private final DecisionsHelper decisions;
  private final String markerName;
  private final ReplayAware replayContext;

  // Key is mutableSideEffect id
  private final Map<String, MutableSideEffectResult> mutableSideEffectResults = new HashMap<>();

  public MutableSideEffectHandler(
      DecisionsHelper decisions, String markerName, ReplayAware replayContext) {
    this.decisions = decisions;
    this.markerName = markerName;
    this.replayContext = replayContext;
  }

  /**
   * @param id mutable side effect id
   * @param func given the value from the last marker returns value to store. If result is empty
   *     nothing is recorded into the history.
   * @return the latest value returned by func
   */
  Optional<byte[]> handle(
      String id,
      Func1<MutableSideEffectData, byte[]> markerDataSerializer,
      Func1<byte[], MutableSideEffectData> markerDataDeserializer,
      Func1<Optional<byte[]>, Optional<byte[]>> func) {
    MutableSideEffectResult result = mutableSideEffectResults.get(id);
    Optional<byte[]> stored;
    if (result == null) {
      stored = Optional.empty();
    } else {
      stored = Optional.of(result.getData());
    }
    long eventId = decisions.getNextDecisionEventId();
    int accessCount = result == null ? 0 : result.getAccessCount();

    if (replayContext.isReplaying()) {
      Optional<byte[]> data =
          getSideEffectDataFromHistory(eventId, id, accessCount, markerDataDeserializer);
      if (data.isPresent()) {
        // Need to insert marker to ensure that eventId is incremented
        recordMutableSideEffectMarker(id, eventId, data.get(), accessCount, markerDataSerializer);
        return data;
      }
      return stored;
    }
    Optional<byte[]> toStore = func.apply(stored);
    if (toStore.isPresent()) {
      byte[] data = toStore.get();
      recordMutableSideEffectMarker(id, eventId, data, accessCount, markerDataSerializer);
      return toStore;
    }
    return stored;
  }

  private Optional<byte[]> getSideEffectDataFromHistory(
      long eventId,
      String mutableSideEffectId,
      int expectedAcccessCount,
      Func1<byte[], MutableSideEffectData> markerDataDeserializer) {
    HistoryEvent event = decisions.getDecisionEvent(eventId);
    if (event.getEventType() != EventType.MarkerRecorded) {
      return Optional.empty();
    }
    MarkerRecordedEventAttributes attributes = event.getMarkerRecordedEventAttributes();
    String name = attributes.getMarkerName();
    if (!markerName.equals(name)) {
      return Optional.empty();
    }
    MutableSideEffectData markerData = markerDataDeserializer.apply(attributes.getDetails());
    // access count is used to not return data from the marker before the recorded number of calls
    if (!mutableSideEffectId.equals(markerData.getId())
        || markerData.getAccessCount() > expectedAcccessCount) {
      return Optional.empty();
    }
    return Optional.of(markerData.getData());
  }

  private void recordMutableSideEffectMarker(
      String id,
      long eventId,
      byte[] data,
      int accessCount,
      Func1<MutableSideEffectData, byte[]> markerDataSerializer) {
    MutableSideEffectData dataObject = new MutableSideEffectData(id, eventId, data, accessCount);
    byte[] details = markerDataSerializer.apply(dataObject);
    mutableSideEffectResults.put(id, new MutableSideEffectResult(data));
    decisions.recordMarker(markerName, details);
  }
}
