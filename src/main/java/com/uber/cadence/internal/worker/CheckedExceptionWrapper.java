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
package com.uber.cadence.internal.worker;

import java.lang.reflect.InvocationTargetException;

public final class CheckedExceptionWrapper extends RuntimeException {

    /**
     * Throws CheckedExceptionWrapper if e is checked exception.
     * If there is a need to return a checked exception from an activity or workflow implementation
     * wrap it using this method. The library code will unwrap it automatically when propagating exception
     * to the caller.
     *
     * Throws original exception if e is {@link RuntimeException} or {@link Error}.
     * Never returns. But return type is not empty to be able to use it as:
     * <pre>
     * try {
     *     return someCall();
     * } catch (Exception e) {
     *     throw CheckedExceptionWrapper.wrap(e);
     * }
     * </pre>
     * If wrap returned void it wouldn't be possible to write <code>throw CheckedExcptionWrapper.wrap</code>
     * and compiler would complain about missing return.
     *
     * @return never returns as always throws.
     */
    public static RuntimeException wrap(Throwable e) {
        if (e instanceof Error) {
            throw (Error) e;
        }
        if (e instanceof InvocationTargetException) {
            throw wrap(e.getCause());
        }
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        throw new CheckedExceptionWrapper((Exception) e);
    }

    /**
     * If argument is wrapped checked exception it is unwrapped and returned.
     * Otherwise the original exception is returned.
     */
    public static Throwable unwrap(Throwable e) {
        if (e instanceof CheckedExceptionWrapper) {
            return e.getCause();
        }
        return e;
    }

    private CheckedExceptionWrapper(Exception e) {
        super(e);
    }
}
