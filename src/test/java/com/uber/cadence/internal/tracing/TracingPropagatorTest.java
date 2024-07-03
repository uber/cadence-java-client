package com.uber.cadence.internal.tracing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.common.collect.ImmutableMap;
import com.uber.cadence.Header;
import com.uber.cadence.PollForActivityTaskResponse;
import io.opentracing.Span;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import java.nio.ByteBuffer;
import java.util.List;
import org.junit.Test;

public class TracingPropagatorTest {

  private final MockTracer mockTracer = new MockTracer();
  private final TracingPropagator propagator = new TracingPropagator(mockTracer);

  @Test
  public void testSpanForExecuteActivity_allowReusingHeaders() {
    Header header =
        new Header()
            .setFields(
                ImmutableMap.of(
                    "traceid",
                    ByteBuffer.wrap("100".getBytes()),
                    "spanid",
                    ByteBuffer.wrap("200".getBytes())));

    Span span =
        propagator.spanForExecuteActivity(
            new PollForActivityTaskResponse().setHeader(header).setActivityId("id"));
    span.finish();
    Span span2 =
        propagator.spanForExecuteActivity(
            new PollForActivityTaskResponse().setHeader(header).setActivityId("id2"));
    span2.finish();

    for (MockSpan mockSpan : mockTracer.finishedSpans()) {
      assertEquals("100", mockSpan.context().toTraceId());
      List<MockSpan.Reference> references = mockSpan.references();
      assertFalse(references.isEmpty());
      MockSpan.Reference from = references.get(0);
      assertEquals("200", from.getContext().toSpanId());
      assertEquals("follows_from", from.getReferenceType());
    }
  }
}
