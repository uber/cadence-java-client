package com.uber.cadence.workflow;

public interface MyWorkflow {

    @WorkflowMethod
    void main();

    @SignalMethod
    void signal1(Signal1Struct signal);

    @SignalMethod
    void signal2(Signal2Struct signal);

}
