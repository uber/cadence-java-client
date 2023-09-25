package com.uber.cadence.migration;

import com.uber.cadence.StartWorkflowExecutionResponse;

public class StartWorkflowInNewResponse {
    StartWorkflowExecutionResponse startWorkflowExecutionResponse;
    String message;

    StartWorkflowInNewResponse(StartWorkflowExecutionResponse startWorkflowResponse, String msg){
        startWorkflowExecutionResponse = startWorkflowResponse;
        message = msg;
    }

}
