# Java framework for Cadence [![Build Status](https://travis-ci.org/uber-java/cadence-client.svg?branch=master)](https://travis-ci.org/uber-java/cadence-client) [![Coverage Status](https://coveralls.io/repos/uber-java/cadence-client/badge.svg?branch=master&service=github)](https://coveralls.io/github/uber-java/cadence-client?branch=master)
[Cadence](https://github.com/uber/cadence) is a distributed, scalable, durable, and highly available orchestration engine we developed at Uber Engineering to execute asynchronous long-running business logic in a scalable and resilient way.

`cadence-client` is the framework for authoring workflows and activities.


## Service Installation

For development install a local copy of Cadence service.
The simplest is to use a [docker version](https://github.com/uber/cadence/blob/master/docker/README.md).
If for whatever reason docker is not an option follow instructions
from the [Cadence Getting Started](https://github.com/uber/cadence#getting-started)

If you work for Uber install the Cadence CLI. Search 'Cadence CLI' on EngDocs for installation and usage instructions.
For not so fortunate it will be open sourced very soon.

# Build Configuration

Add *cadence-client* dependency to your *pom.xml*:

    <dependency>
      <groupId>com.uber</groupId>
      <artifactId>cadence-client</artifactId>
      <version>0.1.0</version>
    </dependency>

# Overview

Cadence is a task orchestrator for your application’s tasks. Applications using Cadence can execute a logical flow of tasks,
especially long-running business logic, synchronously or asynchronously.

A Cadence client application consists from two main types of components:
activities and workflows.

Activities are business level tasks that implement your application logic like calling services or transcoding media files.
Activities can be both short and long running. Usually it is expected that each activity implements a single well defined action.

Workflows are orchestrators of activities. They fully control which activities and in what order are executed.
Workflows do not affect external world directly, only through activities. What makes workflow code "a workflow" is that its state
is preserved by Cadence. So any failure of a worker process that hosts a workflow code does not affect workflow execution.
It continues as if these failures do not happen. At the same time activities can fail any moment for any reason.
But as workflow code is fully fault tolerant it is guaranteed to get notification about activity failure or timeout and
act accordingly.

# Activities

Activity is the implementation of a particular task in the business logic.

## Activity Interface

Activities are defined as methods of a plain Java interface. Each method defines a single activity type. A single
workflow can use more than one activity interface and call more that one activity method from the same interface.
The only requirement is that activity method arguments and return values are serializable to byte array using provided
[DataConverter](src/main/java/com/uber/cadence/converter/DataConverter.java) interface. The default implementation uses
JSON serializer, but any alternative serialization mechanism is pluggable.

```Java
/**
 * Contract of the hello world activities
 */
public interface HelloWorldActivities {

    String printHello(String name);

    String getName();
}

```

## Activity Implementation

Activity implementation is just an implementation of an activity interface.

The values passed to activities through invocation parameters or returned through the result value is recorded in the execution history. 
The entire execution history is transferred from the Cadence service to workflow workers with every event that the workflow logic needs to process. 
A large execution history can thus adversely impact the performance of your workflow. 
Therefore be mindful of the amount of data you transfer via activity invocation parameters or return values. 
Other than that no additional limitations exist on activity implementations.

```java
/**
 * Implementation of the hello world activities
 */
public class HelloWorldActivitiesImpl implements HelloWorldActivities {

    @Override
    public String printHello(String name) {
        String result = "Hello " + name + "!";
        System.out.println(result);
        return result;
    }

    @Override
    public String getName() {
        return "World";
    }
}
```
### Accessing Activity Info

Class [Activity](src/main/java/com/uber/cadence/activity/Activity.java) provides static getters to access information about workflow that invoked it.
Note that this information is stored in a thread local variable. So calls to Activity accessors succeed only in the thread that invoked the activity function.
```java
 /**
  * Implementation of the hello world activities that uses static Activity accessors.
  */
 public class HelloWorldActivitiesImpl implements HelloWorldActivities {

     @Override
     public String printHello(String name) {
        WorkflowExecution execution = Activity.getWorkflowExecution();
        String domain = Activity.getDomain();
        ActivityTask activityTask = Activity.getTask();
        log.info("workflowId=" + execution.getWorkflowId());
        log.info("runId=" + execution.getRunId());
        log.info("domain=" + domain);
        log.info("activityId=" + activityTask.getActivityId());
        log.info("activityTimeout=" + activityTask.getStartToCloseTimeoutSeconds());
        return "Hello " + name + "!";;
     }
 
     @Override
     public String getName() {
         return "World";
     }
 }
```
### Asynchronous Activity Completion

Sometimes activity lifecycle goes beyond a synchronous method invocation. For example a request can be put in a queue
and later reply comes and picked up by a different worker process. The whole such request reply interaction can be modeled
as a single Cadence activity. 

To indicate that an activity should not be completed upon its method return annotate it with @DoNotCompleteOnReturn.
Then later when replies come complete it using [ActivityCompletionClient](src/main/java/com/uber/cadence/client/ActivityCompletionClient.java).
To correlate activity invocation with completion use either `TaskToken` or workflow and activity ids.
```java
public class HelloWorldActivitiesImpl implements HelloWorldActivities {
    
    @DoNotCompleteOnReturn
    @Override
    public String getName() {
        byte[] taskToken = Activity.getTaskToken();
        makeAsyncRequest("GetName", taskToken); // contrived example
        return "ignored"; // This value is ignored when annotated with @DoNotCompleteOnReturn
    }
}
```
The activity completion code:
```java
    public static void completeActivity(byte[] taskToken, String result) {
        completionClient.complete(taskToken, result);
    }

    public static void failActivity(byte[] taskToken, Exception failure) {
        completionClient.completeExceptionally(taskToken, failure);
    }
```
### Activity heartbeating
