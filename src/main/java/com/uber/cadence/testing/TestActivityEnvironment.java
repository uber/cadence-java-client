package com.uber.cadence.testing;

import com.uber.cadence.workflow.Functions;
import com.uber.cadence.workflow.Promise;

public interface TestActivityEnvironment {

    /**
     * Executes zero argument activity synchronously in the calling thread.
     *
     * @param function Function to execute asynchronously
     * @return promise that contains function result or failure
     */
    <R> R executeActivity(Functions.Func<R> function);

    /**
     * Executes one argument activity synchronously in the calling thread.
     *
     * @param function Function to execute asynchronously
     * @param arg1     first function argument
     * @return promise that contains function result or failure
     */
    <A1, R> R executeActivity(Functions.Func1<A1, R> function, A1 arg1);

    /**
     * Executes two argument activity synchronously in the calling thread.
     *
     * @param function Function to execute asynchronously
     * @param arg1     first function argument
     * @param arg2     second function argument
     * @return Promise that contains function result or failure
     */
    <A1, A2, R> R executeActivity(Functions.Func2<A1, A2, R> function, A1 arg1, A2 arg2);

    /**
     * Executes three argument activity synchronously in the calling thread.
     *
     * @param function Function to execute asynchronously
     * @param arg1     first function argument
     * @param arg2     second function argument
     * @param arg3     third function argument
     * @return Promise that contains function result or failure
     */
    <A1, A2, A3, R> R executeActivity(Functions.Func3<A1, A2, A3, R> function, A1 arg1, A2 arg2, A3 arg3);

    /**
     * Executes four argument activity synchronously in the calling thread.
     *
     * @param function Function to execute asynchronously
     * @param arg1     first function argument
     * @param arg2     second function argument
     * @param arg3     third function argument
     * @param arg4     forth function argument
     * @return Promise that contains function result or failure
     */
    <A1, A2, A3, A4, R> R executeActivity(Functions.Func4<A1, A2, A3, A4, R> function, A1 arg1, A2 arg2, A3 arg3, A4 arg4);

    /**
     * Executes five argument activity synchronously in the calling thread.
     *
     * @param function Function to execute asynchronously
     * @param arg1     first function argument
     * @param arg2     second function argument
     * @param arg3     third function argument
     * @param arg4     forth function argument
     * @param arg5     fifth function argument
     * @return Promise that contains function result or failure
     */
    <A1, A2, A3, A4, A5, R> R executeActivity(Functions.Func5<A1, A2, A3, A4, A5, R> function, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5);

    /**
     * Executes six argument activity synchronously in the calling thread.
     *
     * @param function Function to execute asynchronously
     * @param arg1     first function argument
     * @param arg2     second function argument
     * @param arg3     third function argument
     * @param arg4     forth function argument
     * @param arg5     fifth function argument
     * @param arg6     sixth function argument
     * @return Promise that contains function result or failure
     */
    <A1, A2, A3, A4, A5, A6, R> R executeActivity(Functions.Func6<A1, A2, A3, A4, A5, A6, R> function, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5, A6 arg6);

    /**
     * Executes zero argument activity synchronously in the calling thread.
     *
     * @param activity Activity method to execute asynchronously
     * @return Promise that contains activity result or failure
     */
    Promise<Void> executeActivity(Functions.Proc activity);

    /**
     * Executes one argument activity synchronously in the calling thread.
     *
     * @param activity Activity method to execute asynchronously
     * @param arg1      first activity argument
     * @return Promise that contains activity result or failure
     */
    <A1> Promise<Void> executeActivity(Functions.Proc1<A1> activity, A1 arg1);

    /**
     * Executes two argument activity synchronously in the calling thread.
     *
     * @param activity Activity method to execute asynchronously
     * @param arg1      first activity argument
     * @param arg2      second activity argument
     * @return Promise that contains activity result or failure
     */
    <A1, A2> Promise<Void> executeActivity(Functions.Proc2<A1, A2> activity, A1 arg1, A2 arg2);

    /**
     * Executes three argument activity synchronously in the calling thread.
     *
     * @param activity Activity method to execute asynchronously
     * @param arg1      first activity argument
     * @param arg2      second activity argument
     * @param arg3      third activity argument
     * @return Promise that contains activity result or failure
     */
    <A1, A2, A3> Promise<Void> executeActivity(Functions.Proc3<A1, A2, A3> activity, A1 arg1, A2 arg2, A3 arg3);

    /**
     * Executes four argument activity synchronously in the calling thread.
     *
     * @param activity Activity method to execute asynchronously
     * @param arg1      first activity argument
     * @param arg2      second activity argument
     * @param arg3      third activity argument
     * @param arg4      forth activity argument
     * @return Promise that contains activity result or failure
     */
    <A1, A2, A3, A4> Promise<Void> executeActivity(Functions.Proc4<A1, A2, A3, A4> activity, A1 arg1, A2 arg2, A3 arg3, A4 arg4);

    /**
     * Executes five argument activity synchronously in the calling thread.
     *
     * @param activity Activity method to execute asynchronously
     * @param arg1      first activity argument
     * @param arg2      second activity argument
     * @param arg3      third activity argument
     * @param arg4      forth activity argument
     * @param arg5      fifth activity argument
     * @return Promise that contains activity result or failure
     */
    <A1, A2, A3, A4, A5> Promise<Void> executeActivity(Functions.Proc5<A1, A2, A3, A4, A5> activity, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5);

    /**
     * Executes six argument activity synchronously in the calling thread.
     *
     * @param activity Activity method to execute asynchronously
     * @param arg1      first activity argument
     * @param arg2      second activity argument
     * @param arg3      third activity argument
     * @param arg4      forth activity argument
     * @param arg5      fifth activity argument
     * @param arg6      sixth activity argument
     * @return Promise that contains activity result or failure
     */
    <A1, A2, A3, A4, A5, A6> Promise<Void> executeActivity(Functions.Proc6<A1, A2, A3, A4, A5, A6> activity, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5, A6 arg6);

}
