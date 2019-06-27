package com.uber.cadence.workflow;

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

import com.uber.cadence.activity.ActivityMethod;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.workflow.*;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Demonstrates query capability. Requires a local instance of Cadence server to be running. */
public class HelloQuery {

    static final String TASK_LIST = "HelloQuery";
    static final String DOMAIN = "sample";

    public interface GreetingWorkflow {

        @WorkflowMethod
        void createGreeting(String name);

        /** Returns greeting as a query value. */
        @QueryMethod
        String queryGreeting();
    }

    public interface GreetingActivities {
        @ActivityMethod(scheduleToCloseTimeoutSeconds = 60)
        String composeGreeting(String string);
    }

    static class GreetingActivitiesImpl implements HelloQuery.GreetingActivities {
        @Override
        public String composeGreeting(String string) {
            try {
                Thread.sleep(10000);
            } catch (Exception e) {
                System.out.println("Exception");
            }
            return "greetings: " + string;
        }
    }

    /** GreetingWorkflow implementation that updates greeting after sleeping for 5 seconds. */
    public static class GreetingWorkflowImpl implements GreetingWorkflow {

        private String greeting;
        private final GreetingActivities activities =
                Workflow.newActivityStub(GreetingActivities.class);

        @Override
        public void createGreeting(String name) {
            greeting = "Hello " + name + "!";
            // Workflow code always uses WorkflowThread.sleep
            // and Workflow.currentTimeMillis instead of standard Java ones.
            Workflow.sleep(Duration.ofSeconds(2));
            greeting = "Bye " + name + "!";
            Promise<String> promiseString1 =
                    Async.function(() -> activities.composeGreeting("1"));
            Promise<String> promiseString2 =
                    Async.function(
                            () -> {
                                return "aString2";
                            });

            Set<Promise<String>> promiseSet = new HashSet<>();
            promiseSet.add(promiseString1);
            promiseSet.add(promiseString2);
            System.out.println("HERE");
            Workflow.await(Duration.ofSeconds(30),() -> promiseSet.stream().anyMatch(Promise::isCompleted));

//      If you use below while loop instead of await(), workflow execution completes and behavior is as expected
//      while (true) {
//        if (promiseSet.stream().anyMatch(Promise::isCompleted)) {
//          break;
//        }
//        Workflow.sleep(500);
//        System.out.println("tick");
//      }

            System.out.println("HERE2");
            greeting = promiseString1.get();
            System.out.println("HERE3 " + greeting);

            // Change below line to 21 s, then workflow execution completes
            Workflow.sleep(Duration.ofSeconds(20));
            greeting = promiseString2.get();
            System.out.println("HERE4 " + greeting);
        }

        @Override
        public String queryGreeting() {
            return greeting;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // Start a worker that hosts the workflow implementation.
        Worker.Factory factory = new Worker.Factory(DOMAIN);
        Worker worker = factory.newWorker(TASK_LIST);
        worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
        worker.registerActivitiesImplementations(new HelloQuery.GreetingActivitiesImpl());
        factory.start();

        // Start a workflow execution. Usually this is done from another program.
        WorkflowClient workflowClient = WorkflowClient.newInstance(DOMAIN);
        // Get a workflow stub using the same task list the worker uses.
        WorkflowOptions workflowOptions =
                new WorkflowOptions.Builder()
                        .setTaskList(TASK_LIST)
                        .setExecutionStartToCloseTimeout(Duration.ofSeconds(3600))
                        .build();
        GreetingWorkflow workflow =
                workflowClient.newWorkflowStub(GreetingWorkflow.class, workflowOptions);

        // Start workflow asynchronously to not use another thread to query.
        WorkflowClient.start(workflow::createGreeting, "World");
        // After start for getGreeting returns, the workflow is guaranteed to be started.
        // So we can send a signal to it using workflow stub.

//    System.out.println(workflow.queryGreeting()); // Should print Hello...
        // Note that inside a workflow only WorkflowThread.sleep is allowed. Outside
        // WorkflowThread.sleep is not allowed.
        Thread.sleep(2500);
//    System.out.println(workflow.queryGreeting()); // Should print Bye ...
        Thread.sleep(35000);
//    System.out.println(workflow.queryGreeting());
        Thread.sleep(600000);
        System.exit(0);
    }
}