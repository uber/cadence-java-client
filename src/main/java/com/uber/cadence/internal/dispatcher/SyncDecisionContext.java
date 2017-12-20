package com.uber.cadence.internal.dispatcher;

import com.amazonaws.services.simpleworkflow.flow.AsyncDecisionContext;
import com.amazonaws.services.simpleworkflow.flow.generic.ExecuteActivityParameters;
import com.amazonaws.services.simpleworkflow.flow.generic.GenericAsyncActivityClient;
import com.uber.cadence.ActivityType;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SyncDecisionContext {
    private final AsyncDecisionContext context;
    private GenericAsyncActivityClient activityClient;

    public SyncDecisionContext(AsyncDecisionContext context) {
        this.context = context;
        activityClient = context.getActivityClient();
    }

    public byte[] executeActivity(String name, byte[] input) {
        ActivityFutureCancellationHandler cancellationHandler = new ActivityFutureCancellationHandler();
        WorkflowFuture<byte[]> result = new WorkflowFuture(cancellationHandler);
        ExecuteActivityParameters parameters = new ExecuteActivityParameters();
        //TODO: Real task list
        parameters.withActivityType(new ActivityType().setName(name)).
                withInput(input).
                withTaskList(context.getWorkflowContext().getTaskList()).
                withScheduleToStartTimeoutSeconds(10).
                withStartToCloseTimeoutSeconds(10).
                withScheduleToCloseTimeoutSeconds(30);
        Consumer<Throwable> cancellationCallback = activityClient.scheduleActivityTask(parameters,
                (output, failure) -> {
                    if (failure != null) {
                        // TODO: Make sure that only Exceptions are passed into the callback.
                        result.completeExceptionally((Exception) failure);
                    } else {
                        result.complete(output);
                    }
                });
        cancellationHandler.setCancellationCallback(cancellationCallback);
        // TODO: Exception mapping
        try {
            return result.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    private static class ActivityFutureCancellationHandler implements BiConsumer<WorkflowFuture, Boolean> {
        private Consumer<Throwable> cancellationCallback;

        public void setCancellationCallback(Consumer<Throwable> cancellationCallback) {
            this.cancellationCallback = cancellationCallback;
        }

        @Override
        public void accept(WorkflowFuture workflowFuture, Boolean aBoolean) {
            cancellationCallback.accept(new CancellationException("result future cancelled"));
        }
    }
}
