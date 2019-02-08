package com.uber.cadence.client;

import com.uber.cadence.workflow.Functions;
import java.util.concurrent.CompletableFuture;

public interface BatchRequest {

  /** Sends the batch to the service. Blocks until it is processed. */
  void invoke();

  /**
   * Executes zero argument request with void return type
   *
   * @param request The only supported value is method reference to a proxy created through {@link
   *     WorkflowClient#newWorkflowStub(Class, WorkflowOptions)}.
   * @return Future that contains the result of the operation after BatchRequest#invoke is called.
   */
  CompletableFuture<Void> add(Functions.Proc request);

  /**
   * Executes one argument request with void return type
   *
   * @param request The only supported value is method reference to a proxy created through {@link
   *     WorkflowClient#newWorkflowStub(Class, WorkflowOptions)}.
   * @param arg1 first request function parameter
   * @return Future that contains the result of the operation after BatchRequest#invoke is called.
   */
  <A1> CompletableFuture<Void> add(Functions.Proc1<A1> request, A1 arg1);

  /**
   * Executes two argument request with void return type
   *
   * @param request The only supported value is method reference to a proxy created through {@link
   *     WorkflowClient#newWorkflowStub(Class, WorkflowOptions)}.
   * @param arg1 first request function parameter
   * @param arg2 second request function parameter
   * @return Future that contains the result of the operation after BatchRequest#invoke is called.
   */
  <A1, A2> CompletableFuture<Void> add(Functions.Proc2<A1, A2> request, A1 arg1, A2 arg2);

  /**
   * Executes three argument request with void return type
   *
   * @param request The only supported value is method reference to a proxy created through {@link
   *     WorkflowClient#newWorkflowStub(Class, WorkflowOptions)}.
   * @param arg1 first request function parameter
   * @param arg2 second request function parameter
   * @param arg3 third request function parameter
   * @return Future that contains the result of the operation after BatchRequest#invoke is called.
   */
  <A1, A2, A3> CompletableFuture<Void> add(
      Functions.Proc3<A1, A2, A3> request, A1 arg1, A2 arg2, A3 arg3);

  /**
   * Executes four argument request with void return type
   *
   * @param request The only supported value is method reference to a proxy created through {@link
   *     WorkflowClient#newWorkflowStub(Class, WorkflowOptions)}.
   * @param arg1 first request function parameter
   * @param arg2 second request function parameter
   * @param arg3 third request function parameter
   * @param arg4 fourth request function parameter
   * @return Future that contains the result of the operation after BatchRequest#invoke is called.
   */
  <A1, A2, A3, A4> CompletableFuture<Void> add(
      Functions.Proc4<A1, A2, A3, A4> request, A1 arg1, A2 arg2, A3 arg3, A4 arg4);

  /**
   * Executes five argument request with void return type
   *
   * @param request The only supported value is method reference to a proxy created through {@link
   *     WorkflowClient#newWorkflowStub(Class, WorkflowOptions)}.
   * @param arg1 first request function parameter
   * @param arg2 second request function parameter
   * @param arg3 third request function parameter
   * @param arg4 fourth request function parameter
   * @param arg5 fifth request function parameter
   * @return Future that contains the result of the operation after BatchRequest#invoke is called.
   */
  <A1, A2, A3, A4, A5> CompletableFuture<Void> add(
      Functions.Proc5<A1, A2, A3, A4, A5> request, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5);

  /**
   * Executes six argument request with void return type
   *
   * @param request The only supported value is method reference to a proxy created through {@link
   *     WorkflowClient#newWorkflowStub(Class, WorkflowOptions)}.
   * @param arg1 first request function parameter
   * @param arg2 second request function parameter
   * @param arg3 third request function parameter
   * @param arg4 fourth request function parameter
   * @param arg5 sixth request function parameter
   * @param arg6 sixth request function parameter
   * @return Future that contains the result of the operation after BatchRequest#invoke is called.
   */
  <A1, A2, A3, A4, A5, A6> CompletableFuture<Void> add(
      Functions.Proc6<A1, A2, A3, A4, A5, A6> request,
      A1 arg1,
      A2 arg2,
      A3 arg3,
      A4 arg4,
      A5 arg5,
      A6 arg6);

  /**
   * Executes zero argument request.
   *
   * @param request The only supported value is method reference to a proxy created through {@link
   *     WorkflowClient#newWorkflowStub(Class, WorkflowOptions)}.
   * @return Future that contains the result of the operation after BatchRequest#invoke is called.
   */
  <R> CompletableFuture<R> add(Functions.Func<R> request);

  /**
   * Executes one argument request asynchronously.
   *
   * @param request The only supported value is method reference to a proxy created through {@link
   *     WorkflowClient#newWorkflowStub(Class, WorkflowOptions)}.
   * @param arg1 first request argument
   * @return Future that contains the result of the operation after BatchRequest#invoke is called.
   */
  <A1, R> CompletableFuture<R> add(Functions.Func1<A1, R> request, A1 arg1);

  /**
   * Executes two argument request asynchronously.
   *
   * @param request The only supported value is method reference to a proxy created through {@link
   *     WorkflowClient#newWorkflowStub(Class, WorkflowOptions)}.
   * @param arg1 first request function parameter
   * @param arg2 second request function parameter
   * @return Future that contains the result of the operation after BatchRequest#invoke is called.
   */
  <A1, A2, R> CompletableFuture<R> add(Functions.Func2<A1, A2, R> request, A1 arg1, A2 arg2);

  /**
   * Executes three argument request asynchronously.
   *
   * @param request The only supported value is method reference to a proxy created through {@link
   *     WorkflowClient#newWorkflowStub(Class, WorkflowOptions)}.
   * @param arg1 first request function parameter
   * @param arg2 second request function parameter
   * @param arg3 third request function parameter
   * @return Future that contains the result of the operation after BatchRequest#invoke is called.
   */
  <A1, A2, A3, R> CompletableFuture<R> add(
      Functions.Func3<A1, A2, A3, R> request, A1 arg1, A2 arg2, A3 arg3);

  /**
   * Executes four argument request asynchronously.
   *
   * @param request The only supported value is method reference to a proxy created through {@link
   *     WorkflowClient#newWorkflowStub(Class, WorkflowOptions)}.
   * @param arg1 first request function parameter
   * @param arg2 second request function parameter
   * @param arg3 third request function parameter
   * @param arg4 fourth request function parameter
   * @return Future that contains the result of the operation after BatchRequest#invoke is called.
   */
  <A1, A2, A3, A4, R> CompletableFuture<R> add(
      Functions.Func4<A1, A2, A3, A4, R> request, A1 arg1, A2 arg2, A3 arg3, A4 arg4);

  /**
   * Executes five argument request asynchronously.
   *
   * @param request The only supported value is method reference to a proxy created through {@link
   *     WorkflowClient#newWorkflowStub(Class, WorkflowOptions)}.
   * @param arg1 first request function parameter
   * @param arg2 second request function parameter
   * @param arg3 third request function parameter
   * @param arg4 fourth request function parameter
   * @param arg5 sixth request function parameter
   * @return Future that contains the result of the operation after BatchRequest#invoke is called.
   */
  <A1, A2, A3, A4, A5, R> CompletableFuture<R> add(
      Functions.Func5<A1, A2, A3, A4, A5, R> request, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5);

  /**
   * Executes six argument request asynchronously.
   *
   * @param request The only supported value is method reference to a proxy created through {@link
   *     WorkflowClient#newWorkflowStub(Class, WorkflowOptions)}.
   * @param arg1 first request argument
   * @param arg2 second request function parameter
   * @param arg3 third request function parameter
   * @param arg4 fourth request function parameter
   * @param arg5 sixth request function parameter
   * @param arg6 sixth request function parameter
   * @return Future that contains the result of the operation after BatchRequest#invoke is called.
   */
  <A1, A2, A3, A4, A5, A6, R> CompletableFuture<R> add(
      Functions.Func6<A1, A2, A3, A4, A5, A6, R> request,
      A1 arg1,
      A2 arg2,
      A3 arg3,
      A4 arg4,
      A5 arg5,
      A6 arg6);
}
