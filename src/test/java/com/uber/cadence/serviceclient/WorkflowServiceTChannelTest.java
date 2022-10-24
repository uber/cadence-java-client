package com.uber.cadence.serviceclient;

import com.uber.cadence.StartWorkflowExecutionRequest;
import com.uber.cadence.serviceclient.ClientOptions;
import com.uber.cadence.serviceclient.WorkflowServiceTChannel;
import com.uber.tchannel.messages.ThriftRequest;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WorkflowServiceTChannelTest {

    @Test
    public void testThriftRequestIncludesTracingHeaders() {
        Tracer tracer = new JaegerTracer.Builder("test")
                .withSampler(new ConstSampler(true))
                .build();

        GlobalTracer.registerIfAbsent(tracer);
        Span span = tracer.buildSpan("testspan").start();
        tracer.activateSpan(span);

        WorkflowServiceTChannel workflowServiceTChannel = new WorkflowServiceTChannel(ClientOptions.defaultInstance());

        ThriftRequest<StartWorkflowExecutionRequest> thriftRequest = workflowServiceTChannel.buildThriftRequest("testapi", new StartWorkflowExecutionRequest(), 10L);

        Assert.assertNotNull(thriftRequest.getHeaders().get("$tracing$uber-trace-id"));
    }

    @Test
    public void testThriftRequestDoesNotIncludesTracingHeadersIfNoSpanPresent() {
        WorkflowServiceTChannel workflowServiceTChannel = new WorkflowServiceTChannel(ClientOptions.defaultInstance());

        ThriftRequest<StartWorkflowExecutionRequest> thriftRequest = workflowServiceTChannel.buildThriftRequest("testapi", new StartWorkflowExecutionRequest(), 10L);

        Assert.assertNull(thriftRequest.getHeaders().get("$tracing$uber-trace-id"));
    }
}
