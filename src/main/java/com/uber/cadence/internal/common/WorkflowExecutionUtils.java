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

package com.uber.cadence.internal.common;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.uber.cadence.ActivityType;
import com.uber.cadence.Decision;
import com.uber.cadence.DecisionType;
import com.uber.cadence.EntityNotExistsError;
import com.uber.cadence.EventType;
import com.uber.cadence.GetWorkflowExecutionHistoryRequest;
import com.uber.cadence.GetWorkflowExecutionHistoryResponse;
import com.uber.cadence.History;
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.HistoryEventFilterType;
import com.uber.cadence.TaskList;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.WorkflowExecutionCloseStatus;
import com.uber.cadence.WorkflowExecutionFailedEventAttributes;
import com.uber.cadence.WorkflowExecutionTerminatedEventAttributes;
import com.uber.cadence.WorkflowExecutionTimedOutEventAttributes;
import com.uber.cadence.WorkflowType;
import com.uber.cadence.client.WorkflowTerminatedException;
import com.uber.cadence.client.WorkflowTimedOutException;
import com.uber.cadence.common.RetryOptions;
import com.uber.cadence.common.WorkflowExecutionHistory;
import com.uber.cadence.serviceclient.IWorkflowService;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

/**
 * Convenience methods to be used by unit tests and during development.
 *
 * @author fateev
 */
public class WorkflowExecutionUtils {

  /**
   * Indentation for history and decisions pretty printing. Do not change it from 2 spaces. The gson
   * pretty printer has it hardcoded and changing it breaks the indentation of exception stack
   * traces.
   */
  private static final String INDENTATION = "  ";

  // Wait period for passive cluster to retry getting workflow result in case of replication delay.
  private static final long ENTITY_NOT_EXIST_RETRY_WAIT_MILLIS = 500;

  /**
   * Returns result of a workflow instance execution or throws an exception if workflow did not
   * complete successfully.
   *
   * @param workflowType is optional.
   * @throws TimeoutException if workflow didn't complete within specified timeout
   * @throws CancellationException if workflow was cancelled
   * @throws WorkflowExecutionFailedException if workflow execution failed
   * @throws WorkflowTimedOutException if workflow execution exceeded its execution timeout and was
   *     forcefully terminated by the Cadence server.
   * @throws WorkflowTerminatedException if workflow execution was terminated through an external
   *     terminate command.
   */
  public static byte[] getWorkflowExecutionResult(
      IWorkflowService service,
      String domain,
      WorkflowExecution workflowExecution,
      Optional<String> workflowType,
      long timeout,
      TimeUnit unit)
      throws TimeoutException, CancellationException, WorkflowExecutionFailedException,
          WorkflowTerminatedException, WorkflowTimedOutException, EntityNotExistsError {
    // getInstanceCloseEvent waits for workflow completion including new runs.
    HistoryEvent closeEvent =
        getInstanceCloseEvent(service, domain, workflowExecution, timeout, unit);
    return getResultFromCloseEvent(workflowExecution, workflowType, closeEvent);
  }

  public static CompletableFuture<byte[]> getWorkflowExecutionResultAsync(
      IWorkflowService service,
      String domain,
      WorkflowExecution workflowExecution,
      Optional<String> workflowType,
      long timeout,
      TimeUnit unit) {
    return getInstanceCloseEventAsync(service, domain, workflowExecution, timeout, unit)
        .thenApply(
            (closeEvent) -> getResultFromCloseEvent(workflowExecution, workflowType, closeEvent));
  }

  private static byte[] getResultFromCloseEvent(
      WorkflowExecution workflowExecution, Optional<String> workflowType, HistoryEvent closeEvent) {
    if (closeEvent == null) {
      throw new IllegalStateException("Workflow is still running");
    }
    switch (closeEvent.getEventType()) {
      case WorkflowExecutionCompleted:
        return closeEvent.getWorkflowExecutionCompletedEventAttributes().getResult();
      case WorkflowExecutionCanceled:
        byte[] details = closeEvent.getWorkflowExecutionCanceledEventAttributes().getDetails();
        String message = details != null ? new String(details, UTF_8) : null;
        throw new CancellationException(message);
      case WorkflowExecutionFailed:
        WorkflowExecutionFailedEventAttributes failed =
            closeEvent.getWorkflowExecutionFailedEventAttributes();
        throw new WorkflowExecutionFailedException(
            failed.getReason(), failed.getDetails(), failed.getDecisionTaskCompletedEventId());
      case WorkflowExecutionTerminated:
        WorkflowExecutionTerminatedEventAttributes terminated =
            closeEvent.getWorkflowExecutionTerminatedEventAttributes();
        throw new WorkflowTerminatedException(
            workflowExecution,
            workflowType,
            terminated.getReason(),
            terminated.getIdentity(),
            terminated.getDetails());
      case WorkflowExecutionTimedOut:
        WorkflowExecutionTimedOutEventAttributes timedOut =
            closeEvent.getWorkflowExecutionTimedOutEventAttributes();
        throw new WorkflowTimedOutException(
            workflowExecution, workflowType, timedOut.getTimeoutType());
      default:
        throw new RuntimeException(
            "Workflow end state is not completed: " + prettyPrintHistoryEvent(closeEvent));
    }
  }

  /** Returns an instance closing event, potentially waiting for workflow to complete. */
  private static HistoryEvent getInstanceCloseEvent(
      IWorkflowService service,
      String domain,
      WorkflowExecution workflowExecution,
      long timeout,
      TimeUnit unit)
      throws TimeoutException, EntityNotExistsError {
    byte[] pageToken = null;
    GetWorkflowExecutionHistoryResponse response;
    // TODO: Interrupt service long poll call on timeout and on interrupt
    long start = System.currentTimeMillis();
    HistoryEvent event;
    do {
      if (timeout != 0 && System.currentTimeMillis() - start > unit.toMillis(timeout)) {
        throw new TimeoutException(
            "WorkflowId="
                + workflowExecution.getWorkflowId()
                + ", runId="
                + workflowExecution.getRunId()
                + ", timeout="
                + timeout
                + ", unit="
                + unit);
      }

      GetWorkflowExecutionHistoryRequest r = new GetWorkflowExecutionHistoryRequest();
      r.setDomain(domain);
      r.setExecution(workflowExecution);
      r.setHistoryEventFilterType(HistoryEventFilterType.CLOSE_EVENT);
      r.setNextPageToken(pageToken);
      r.setWaitForNewEvent(true);
      r.setSkipArchival(true);
      RetryOptions retryOptions = getRetryOptionWithTimeout(timeout, unit);
      try {
        response =
            RpcRetryer.retryWithResult(
                retryOptions,
                () -> service.GetWorkflowExecutionHistoryWithTimeout(r, unit.toMillis(timeout)));
      } catch (EntityNotExistsError e) {
        if (e.activeCluster != null
            && e.currentCluster != null
            && !e.activeCluster.equals(e.currentCluster)) {
          // Current cluster is passive cluster. Execution might not exist because of replication
          // lag. If we are still within timeout, wait for a little bit and retry.
          if (timeout != 0
              && System.currentTimeMillis() + ENTITY_NOT_EXIST_RETRY_WAIT_MILLIS - start
                  > unit.toMillis(timeout)) {
            throw e;
          }

          try {
            Thread.sleep(ENTITY_NOT_EXIST_RETRY_WAIT_MILLIS);
          } catch (InterruptedException ie) {
            // Throw entity not exist here.
            throw e;
          }
          continue;
        }
        throw e;
      } catch (TException e) {
        throw CheckedExceptionWrapper.wrap(e);
      }

      pageToken = response.getNextPageToken();
      History history = response.getHistory();
      if (history != null && history.getEvents().size() > 0) {
        event = history.getEvents().get(0);
        if (!isWorkflowExecutionCompletedEvent(event)) {
          throw new RuntimeException("Last history event is not completion event: " + event);
        }
        // Workflow called continueAsNew. Start polling the new generation with new runId.
        if (event.getEventType() == EventType.WorkflowExecutionContinuedAsNew) {
          pageToken = null;
          workflowExecution =
              new WorkflowExecution()
                  .setWorkflowId(workflowExecution.getWorkflowId())
                  .setRunId(
                      event
                          .getWorkflowExecutionContinuedAsNewEventAttributes()
                          .getNewExecutionRunId());
          continue;
        }
        break;
      }
    } while (true);
    return event;
  }

  /** Returns an instance closing event, potentially waiting for workflow to complete. */
  private static CompletableFuture<HistoryEvent> getInstanceCloseEventAsync(
      IWorkflowService service,
      String domain,
      final WorkflowExecution workflowExecution,
      long timeout,
      TimeUnit unit) {
    return getInstanceCloseEventAsync(service, domain, workflowExecution, null, timeout, unit);
  }

  private static CompletableFuture<HistoryEvent> getInstanceCloseEventAsync(
      IWorkflowService service,
      String domain,
      final WorkflowExecution workflowExecution,
      byte[] pageToken,
      long timeout,
      TimeUnit unit) {
    // TODO: Interrupt service long poll call on timeout and on interrupt
    long start = System.currentTimeMillis();
    GetWorkflowExecutionHistoryRequest request = new GetWorkflowExecutionHistoryRequest();
    request.setDomain(domain);
    request.setExecution(workflowExecution);
    request.setHistoryEventFilterType(HistoryEventFilterType.CLOSE_EVENT);
    request.setWaitForNewEvent(true);
    request.setNextPageToken(pageToken);
    CompletableFuture<GetWorkflowExecutionHistoryResponse> response =
        getWorkflowExecutionHistoryAsync(service, request, timeout, unit);
    return response.thenComposeAsync(
        (r) -> {
          long elapsedTime = System.currentTimeMillis() - start;
          if (timeout != 0 && elapsedTime > unit.toMillis(timeout)) {
            throw CheckedExceptionWrapper.wrap(
                new TimeoutException(
                    "WorkflowId="
                        + workflowExecution.getWorkflowId()
                        + ", runId="
                        + workflowExecution.getRunId()
                        + ", timeout="
                        + timeout
                        + ", unit="
                        + unit));
          }
          History history = r.getHistory();
          if (history == null || history.getEvents().size() == 0) {
            // Empty poll returned
            return getInstanceCloseEventAsync(
                service, domain, workflowExecution, pageToken, timeout - elapsedTime, unit);
          }
          HistoryEvent event = history.getEvents().get(0);
          if (!isWorkflowExecutionCompletedEvent(event)) {
            throw new RuntimeException("Last history event is not completion event: " + event);
          }
          // Workflow called continueAsNew. Start polling the new generation with new runId.
          if (event.getEventType() == EventType.WorkflowExecutionContinuedAsNew) {
            WorkflowExecution nextWorkflowExecution =
                new WorkflowExecution()
                    .setWorkflowId(workflowExecution.getWorkflowId())
                    .setRunId(
                        event
                            .getWorkflowExecutionContinuedAsNewEventAttributes()
                            .getNewExecutionRunId());
            return getInstanceCloseEventAsync(
                service,
                domain,
                nextWorkflowExecution,
                r.getNextPageToken(),
                timeout - elapsedTime,
                unit);
          }
          return CompletableFuture.completedFuture(event);
        });
  }

  private static RetryOptions getRetryOptionWithTimeout(long timeout, TimeUnit unit) {
    return new RetryOptions.Builder(RpcRetryer.DEFAULT_RPC_RETRY_OPTIONS)
        .setExpiration(Duration.ofSeconds(unit.toSeconds(timeout)))
        .build();
  }

  private static CompletableFuture<GetWorkflowExecutionHistoryResponse>
      getWorkflowExecutionHistoryAsync(
          IWorkflowService service,
          GetWorkflowExecutionHistoryRequest r,
          long timeout,
          TimeUnit unit) {
    RetryOptions retryOptions = getRetryOptionWithTimeout(timeout, unit);
    return RpcRetryer.retryWithResultAsync(
        retryOptions,
        () -> {
          CompletableFuture<GetWorkflowExecutionHistoryResponse> result = new CompletableFuture<>();
          try {
            service.GetWorkflowExecutionHistoryWithTimeout(
                r,
                new AsyncMethodCallback<GetWorkflowExecutionHistoryResponse>() {
                  @Override
                  public void onComplete(GetWorkflowExecutionHistoryResponse response) {
                    result.complete(response);
                  }

                  @Override
                  public void onError(Exception exception) {
                    result.completeExceptionally(exception);
                  }
                },
                unit.toMillis(timeout));
          } catch (TException e) {
            result.completeExceptionally(e);
          }
          return result;
        });
  }

  public static boolean isWorkflowExecutionCompletedEvent(HistoryEvent event) {
    return ((event != null)
        && (event.getEventType() == EventType.WorkflowExecutionCompleted
            || event.getEventType() == EventType.WorkflowExecutionCanceled
            || event.getEventType() == EventType.WorkflowExecutionFailed
            || event.getEventType() == EventType.WorkflowExecutionTimedOut
            || event.getEventType() == EventType.WorkflowExecutionContinuedAsNew
            || event.getEventType() == EventType.WorkflowExecutionTerminated));
  }

  public static boolean isWorkflowExecutionCompleteDecision(Decision decision) {
    return ((decision != null)
        && (decision.getDecisionType() == DecisionType.CompleteWorkflowExecution
            || decision.getDecisionType() == DecisionType.CancelWorkflowExecution
            || decision.getDecisionType() == DecisionType.FailWorkflowExecution
            || decision.getDecisionType() == DecisionType.ContinueAsNewWorkflowExecution));
  }

  public static String getId(HistoryEvent historyEvent) {
    String id = null;
    if (historyEvent != null) {
      if (historyEvent.getEventType() == EventType.StartChildWorkflowExecutionFailed) {
        id = historyEvent.getStartChildWorkflowExecutionFailedEventAttributes().getWorkflowId();
      }
    }

    return id;
  }

  public static WorkflowExecutionCloseStatus getCloseStatus(HistoryEvent event) {
    switch (event.getEventType()) {
      case WorkflowExecutionCanceled:
        return WorkflowExecutionCloseStatus.CANCELED;
      case WorkflowExecutionFailed:
        return WorkflowExecutionCloseStatus.FAILED;
      case WorkflowExecutionTimedOut:
        return WorkflowExecutionCloseStatus.TIMED_OUT;
      case WorkflowExecutionContinuedAsNew:
        return WorkflowExecutionCloseStatus.CONTINUED_AS_NEW;
      case WorkflowExecutionCompleted:
        return WorkflowExecutionCloseStatus.COMPLETED;
      case WorkflowExecutionTerminated:
        return WorkflowExecutionCloseStatus.TERMINATED;
      default:
        throw new IllegalArgumentException("Not close event: " + event);
    }
  }

  public static GetWorkflowExecutionHistoryResponse getHistoryPage(
      byte[] nextPageToken,
      IWorkflowService service,
      String domain,
      WorkflowExecution workflowExecution) {

    GetWorkflowExecutionHistoryRequest getHistoryRequest = new GetWorkflowExecutionHistoryRequest();
    getHistoryRequest.setDomain(domain);
    getHistoryRequest.setExecution(workflowExecution);
    getHistoryRequest.setNextPageToken(nextPageToken);

    GetWorkflowExecutionHistoryResponse history;
    try {
      history = service.GetWorkflowExecutionHistory(getHistoryRequest);
    } catch (TException e) {
      throw new Error(e);
    }
    if (history == null) {
      throw new IllegalArgumentException("unknown workflow execution: " + workflowExecution);
    }
    return history;
  }

  public static Iterator<HistoryEvent> getHistory(
      IWorkflowService service, String domain, WorkflowExecution workflowExecution) {
    return new Iterator<HistoryEvent>() {
      byte[] nextPageToken;
      Iterator<HistoryEvent> current;

      {
        getNextPage();
      }

      @Override
      public boolean hasNext() {
        return current.hasNext() || nextPageToken != null;
      }

      @Override
      public HistoryEvent next() {
        if (current.hasNext()) {
          return current.next();
        }
        getNextPage();
        return current.next();
      }

      private void getNextPage() {
        GetWorkflowExecutionHistoryResponse history =
            getHistoryPage(nextPageToken, service, domain, workflowExecution);
        current = history.getHistory().getEvents().iterator();
        nextPageToken = history.getNextPageToken();
      }
    };
  }

  /**
   * Returns workflow instance history in a human readable format.
   *
   * @param showWorkflowTasks when set to false workflow task events (decider events) are not
   *     included
   * @param history Workflow instance history
   */
  public static String prettyPrintHistory(History history, boolean showWorkflowTasks) {
    return prettyPrintHistory(history.getEvents().iterator(), showWorkflowTasks);
  }

  public static String prettyPrintHistory(
      Iterator<HistoryEvent> events, boolean showWorkflowTasks) {
    StringBuilder result = new StringBuilder();
    result.append("{");
    boolean first = true;
    long firstTimestamp = 0;
    while (events.hasNext()) {
      HistoryEvent event = events.next();
      if (!showWorkflowTasks && event.getEventType().toString().startsWith("WorkflowTask")) {
        continue;
      }
      if (first) {
        first = false;
        firstTimestamp = event.getTimestamp();
      } else {
        result.append(",");
      }
      result.append("\n");
      result.append(INDENTATION);
      result.append(prettyPrintHistoryEvent(event, firstTimestamp));
    }
    result.append("\n}");
    return result.toString();
  }

  /**
   * Returns single event in a human readable format
   *
   * @param event event to pretty print
   */
  public static String prettyPrintHistoryEvent(HistoryEvent event) {
    return prettyPrintHistoryEvent(event, -1);
  }

  private static String prettyPrintHistoryEvent(HistoryEvent event, long firstTimestamp) {
    String eventType = event.getEventType().toString();
    StringBuilder result = new StringBuilder();
    result.append(event.getEventId());
    result.append(": ");
    result.append(eventType);
    if (firstTimestamp > 0) {
      // timestamp is in nanos
      long timestamp = (event.getTimestamp() - firstTimestamp) / 1_000_000;
      result.append(String.format(" [%s ms]", timestamp));
    }
    result.append(" ");
    result.append(
        prettyPrintObject(
            getEventAttributes(event), "getFieldValue", true, INDENTATION, false, false));

    return result.toString();
  }

  private static Object getEventAttributes(HistoryEvent event) {
    try {
      Method m = HistoryEvent.class.getMethod("get" + event.getEventType() + "EventAttributes");
      return m.invoke(event);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      return event;
    }
  }

  /**
   * Returns decisions in a human readable format
   *
   * @param decisions decisions to pretty print
   */
  public static String prettyPrintDecisions(Iterable<Decision> decisions) {
    StringBuilder result = new StringBuilder();
    result.append("{");
    boolean first = true;
    for (Decision decision : decisions) {
      if (first) {
        first = false;
      } else {
        result.append(",");
      }
      result.append("\n");
      result.append(INDENTATION);
      result.append(prettyPrintDecision(decision));
    }
    result.append("\n}");
    return result.toString();
  }

  /**
   * Returns single decision in a human readable format
   *
   * @param decision decision to pretty print
   */
  public static String prettyPrintDecision(Decision decision) {
    return prettyPrintObject(decision, "getFieldValue", true, INDENTATION, true, true);
  }

  /**
   * Not really a generic method for printing random object graphs. But it works for events and
   * decisions.
   */
  private static String prettyPrintObject(
      Object object,
      String methodToSkip,
      boolean skipNullsAndEmptyCollections,
      String indentation,
      boolean skipLevel,
      boolean printTypeName) {
    StringBuilder result = new StringBuilder();
    if (object == null) {
      return "null";
    }
    Class<? extends Object> clz = object.getClass();
    if (Number.class.isAssignableFrom(clz)) {
      return String.valueOf(object);
    }
    if (Boolean.class.isAssignableFrom(clz)) {
      return String.valueOf(object);
    }
    if (clz.equals(String.class)) {
      return (String) object;
    }
    if (clz.equals(byte[].class)) {
      return new String((byte[]) object, UTF_8);
    }
    if (ByteBuffer.class.isAssignableFrom(clz)) {
      byte[] bytes = org.apache.thrift.TBaseHelper.byteBufferToByteArray((ByteBuffer) object);
      return new String(bytes, UTF_8);
    }
    if (clz.equals(Date.class)) {
      return String.valueOf(object);
    }
    if (clz.equals(TaskList.class)) {
      return String.valueOf(((TaskList) object).getName());
    }
    if (clz.equals(ActivityType.class)) {
      return String.valueOf(((ActivityType) object).getName());
    }
    if (clz.equals(WorkflowType.class)) {
      return String.valueOf(((WorkflowType) object).getName());
    }
    if (Map.Entry.class.isAssignableFrom(clz)) {
      result.append(
          prettyPrintObject(
              ((Map.Entry) object).getKey(),
              methodToSkip,
              skipNullsAndEmptyCollections,
              "",
              skipLevel,
              printTypeName));
      result.append("=");
      result.append(
          prettyPrintObject(
              ((Map.Entry) object).getValue(),
              methodToSkip,
              skipNullsAndEmptyCollections,
              "",
              skipLevel,
              printTypeName));
      return result.toString();
    }
    if (Map.class.isAssignableFrom(clz)) {
      result.append("{ ");

      String prefix = "";
      Map<?, ?> sortedMap = new TreeMap<>((Map<?, ?>) object); // Automatically sorts by keys
      for (Map.Entry<?, ?> entry : sortedMap.entrySet()) {
        result.append(prefix);
        prefix = ", ";
        result.append(
            prettyPrintObject(
                entry, methodToSkip, skipNullsAndEmptyCollections, "", skipLevel, printTypeName));
      }

      result.append(" }");
      return result.toString();
    }
    if (Collection.class.isAssignableFrom(clz)) {
      return String.valueOf(object);
    }

    if (!skipLevel) {
      if (printTypeName) {
        result.append(object.getClass().getSimpleName());
        result.append(" ");
      }
      result.append("{");
    }

    // walk through getter methods without params and dump them as
    // key (method-name) = value (getter-result)

    Method[] eventMethods = object.getClass().getDeclaredMethods();
    Arrays.sort(eventMethods, Comparator.comparing(Method::getName));

    boolean first = true;
    for (Method method : eventMethods) {
      String name = method.getName();
      if (!name.startsWith("get")
          || name.equals("getDecisionType")
          || method.getParameterCount() != 0
          || !Modifier.isPublic(method.getModifiers())) {
        continue;
      }
      if (name.equals(methodToSkip) || name.equals("getClass")) {
        continue;
      }
      if (Modifier.isStatic(method.getModifiers())) {
        continue;
      }
      Object value;
      try {
        value = method.invoke(object, (Object[]) null);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(e.getTargetException());
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      if (skipNullsAndEmptyCollections) {
        if (value == null) {
          continue;
        }
        if (value instanceof Map && ((Map<?, ?>) value).isEmpty()) {
          continue;
        }
        if (value instanceof Collection && ((Collection<?>) value).isEmpty()) {
          continue;
        }
      }
      if (!skipLevel) {
        if (first) {
          first = false;
        } else {
          result.append(";");
        }
        result.append("\n");
        result.append(indentation);
        result.append(INDENTATION);
        result.append(name.substring(3));
        result.append(" = ");
        // Pretty print JSON serialized exceptions.
        if (name.equals("getDetails") && value instanceof byte[]) {
          String details = new String((byte[]) value, UTF_8);
          details = prettyPrintJson(details, INDENTATION + INDENTATION);
          // GSON pretty prints, but doesn't let to set an initial indentation.
          // Thus indenting the pretty printed JSON through regexp :(.
          String replacement = "\n" + indentation + INDENTATION;
          details = details.replaceAll("\\n|\\\\n", replacement);
          result.append(details);
          continue;
        }
        result.append(
            prettyPrintObject(
                value,
                methodToSkip,
                skipNullsAndEmptyCollections,
                indentation + INDENTATION,
                false,
                false));
      } else {
        result.append(
            prettyPrintObject(
                value,
                methodToSkip,
                skipNullsAndEmptyCollections,
                indentation,
                false,
                printTypeName));
      }
    }
    if (!skipLevel) {
      result.append("\n");
      result.append(indentation);
      result.append("}");
    }
    return result.toString();
  }

  public static boolean containsEvent(List<HistoryEvent> history, EventType eventType) {
    for (HistoryEvent event : history) {
      if (event.getEventType() == eventType) {
        return true;
      }
    }
    return false;
  }

  /**
   * Pretty prints JSON. Not a generic utility. Used to prettify Details fields that contain
   * serialized exceptions.
   */
  private static String prettyPrintJson(String jsonValue, String stackIndentation) {
    try {
      JsonObject json = JsonParser.parseString(jsonValue).getAsJsonObject();
      fixStackTrace(json, stackIndentation);
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      return gson.toJson(json);
    } catch (Exception e) {
      System.err.println("Error parsing JSON: " + jsonValue);
      return jsonValue;
    }
  }

  private static void fixStackTrace(JsonElement json, String stackIndentation) {
    if (!json.isJsonObject()) {
      return;
    }
    for (Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
      if ("stackTrace".equals(entry.getKey())) {
        String value = entry.getValue().getAsString();
        String replacement = "\n" + stackIndentation;
        String fixed = value.replaceAll("\\n", replacement);
        entry.setValue(new JsonPrimitive(fixed));
        continue;
      }
      fixStackTrace(entry.getValue(), stackIndentation + INDENTATION);
    }
  }

  /** Is this an event that was created to mirror a decision? */
  public static boolean isDecisionEvent(HistoryEvent event) {
    EventType eventType = event.getEventType();
    boolean result =
        ((event != null)
            && (eventType == EventType.ActivityTaskScheduled
                || eventType == EventType.StartChildWorkflowExecutionInitiated
                || eventType == EventType.TimerStarted
                || eventType == EventType.WorkflowExecutionCompleted
                || eventType == EventType.WorkflowExecutionFailed
                || eventType == EventType.WorkflowExecutionCanceled
                || eventType == EventType.WorkflowExecutionContinuedAsNew
                || eventType == EventType.ActivityTaskCancelRequested
                || eventType == EventType.RequestCancelActivityTaskFailed
                || eventType == EventType.TimerCanceled
                || eventType == EventType.CancelTimerFailed
                || eventType == EventType.RequestCancelExternalWorkflowExecutionInitiated
                || eventType == EventType.MarkerRecorded
                || eventType == EventType.SignalExternalWorkflowExecutionInitiated
                || eventType == EventType.UpsertWorkflowSearchAttributes));
    return result;
  }

  public static WorkflowExecutionHistory readHistoryFromResource(String resourceFileName)
      throws IOException {
    ClassLoader classLoader = WorkflowExecutionUtils.class.getClassLoader();
    String historyUrl = classLoader.getResource(resourceFileName).getFile();
    File historyFile = new File(historyUrl);
    return readHistory(historyFile);
  }

  public static WorkflowExecutionHistory readHistory(File historyFile) throws IOException {
    try (Reader reader = Files.newBufferedReader(historyFile.toPath(), UTF_8)) {
      String jsonHistory = CharStreams.toString(reader);
      return WorkflowExecutionHistory.fromJson(jsonHistory);
    }
  }
}
