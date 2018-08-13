package com.uber.cadence.internal.worker;

import com.uber.cadence.PollForDecisionTaskResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class PollDecisionTaskDispatcher implements Consumer<PollForDecisionTaskResponse> {

    private final Map<String, Consumer<PollForDecisionTaskResponse>> subscribers = new HashMap<>();

    @Override
    public void accept(PollForDecisionTaskResponse t) {
        //String taskListName = t.get.getWorkflowExecution()..getWorkflowExecutionTaskList().name;
        synchronized (this) {
            if (subscribers.containsKey(t)) {
                subscribers.get(t).accept(t);
            }
            // what to do if we don't find the tasklist?
        }
    }

    public void Subscribe(String tasklist, Consumer<PollForDecisionTaskResponse> consumer) {
        synchronized (this) {
            subscribers.put(tasklist, consumer);
        }
    }
}
