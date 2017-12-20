package com.amazonaws.services.simpleworkflow.flow;

import com.uber.cadence.WorkflowService;
import com.uber.cadence.serviceclient.WorkflowServiceTChannel;

public class WorkflowServiceBuilder {

    private final String host;
    private final int port;
    private final String serviceName;
    private final WorkflowServiceTChannel.ClientOptions options;

    public WorkflowServiceBuilder(String host, int port, String serviceName) {
        this(host, port, serviceName, new WorkflowServiceTChannel.ClientOptions.Builder().build());
    }

    public WorkflowServiceBuilder(String host, int port, String serviceName, WorkflowServiceTChannel.ClientOptions options) {
        this.host = host;
        this.port = port;
        this.serviceName = serviceName;
        this.options = options;
    }
    
    public WorkflowService.Iface build() {
        return new WorkflowServiceTChannel(host, port, serviceName, options);
    }
}
