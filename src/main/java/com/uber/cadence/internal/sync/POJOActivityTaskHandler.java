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
package com.uber.cadence.internal.sync;

import com.google.common.base.Joiner;
import com.google.common.reflect.TypeToken;
import com.uber.cadence.PollForActivityTaskResponse;
import com.uber.cadence.RespondActivityTaskCompletedRequest;
import com.uber.cadence.RespondActivityTaskFailedRequest;
import com.uber.cadence.WorkflowService;
import com.uber.cadence.activity.ActivityMethod;
import com.uber.cadence.activity.ActivityTask;
import com.uber.cadence.activity.DoNotCompleteOnReturn;
import com.uber.cadence.activity.MethodRetry;
import com.uber.cadence.client.ActivityCancelledException;
import com.uber.cadence.converter.DataConverter;
import com.uber.cadence.internal.common.InternalUtils;
import com.uber.cadence.internal.worker.ActivityTaskHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;

class POJOActivityTaskHandler implements ActivityTaskHandler {

    private DataConverter dataConverter;
    private final Map<String, POJOActivityImplementation> activities = Collections.synchronizedMap(new HashMap<>());

    POJOActivityTaskHandler(DataConverter dataConverter) {
        this.dataConverter = dataConverter;
    }

    public DataConverter getDataConverter() {
        return dataConverter;
    }

    public void setDataConverter(DataConverter dataConverter) {
        this.dataConverter = dataConverter;
    }

    public void addActivityImplementation(Object activity) {
        Class<?> cls = activity.getClass();
        for (Method method : cls.getMethods()) {
            if (method.getAnnotation(ActivityMethod.class) != null) {
                throw new IllegalArgumentException("Found @ActivityMethod annotation on \"" + method
                        + "\" This annotation can be used only on the interface method it implements.");
            }
            if (method.getAnnotation(MethodRetry.class) != null) {
                throw new IllegalArgumentException("Found @MethodRetry annotation on \"" + method
                        + "\" This annotation can be used only on the interface method it implements.");
            }
        }
        TypeToken<?>.TypeSet interfaces = TypeToken.of(cls).getTypes().interfaces();
        if (interfaces.isEmpty()) {
            throw new IllegalArgumentException("Activity must implement at least one interface");
        }
        for (TypeToken<?> i : interfaces) {
            for (Method method : i.getRawType().getMethods()) {
                POJOActivityImplementation implementation = new POJOActivityImplementation(method, activity);
                ActivityMethod annotation = method.getAnnotation(ActivityMethod.class);
                String activityType;
                if (annotation != null && !annotation.name().isEmpty()) {
                    activityType = annotation.name();
                } else {
                    activityType = InternalUtils.getSimpleName(method);
                }
                if (activities.containsKey(activityType)) {
                    throw new IllegalStateException(activityType + " activity type is already registered with the worker");
                }
                activities.put(activityType, implementation);
            }
        }
    }

    private ActivityTaskHandler.Result mapToActivityFailure(ActivityTask task, Throwable e) {
        if (e instanceof ActivityCancelledException) {
            throw new CancellationException(e.getMessage());
        }
        RespondActivityTaskFailedRequest result = new RespondActivityTaskFailedRequest();
        e = CheckedExceptionWrapper.unwrap(e);
        result.setReason(e.getClass().getName());
        result.setDetails(dataConverter.toData(e));
        return new ActivityTaskHandler.Result(null, result, null, null);
    }

    @Override
    public boolean isAnyTypeSupported() {
        return !activities.isEmpty();
    }

    public void setActivitiesImplementation(Object[] activitiesImplementation) {
        activities.clear();
        for (Object activity : activitiesImplementation) {
            addActivityImplementation(activity);
        }
    }

    @Override
    public Result handle(WorkflowService.Iface service, String domain, PollForActivityTaskResponse pollResponse) {
        String activityType = pollResponse.getActivityType().getName();
        ActivityTaskImpl activityTask = new ActivityTaskImpl(pollResponse);
        POJOActivityImplementation activity = activities.get(activityType);
        if (activity == null) {
            String knownTypes = Joiner.on(", ").join(activities.keySet());
            return mapToActivityFailure(activityTask, new IllegalArgumentException("Activity Type \""
                    + activityType + "\" is not registered with a worker. Known types are: " + knownTypes));
        }
        return activity.execute(service, domain, activityTask);
    }

    private class POJOActivityImplementation {
        private final Method method;
        private final Object activity;
        private boolean doNotCompleteOnReturn;

        POJOActivityImplementation(Method interfaceMethod, Object activity) {
            this.method = interfaceMethod;

            // @DoNotCompleteOnReturn is expected to be on implementation method, not the interface.
            // So lookup method starting from the implementation object class.
            DoNotCompleteOnReturn annotation;
            try {
                Method implementationMethod = activity.getClass().getMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
                annotation = implementationMethod.getAnnotation(DoNotCompleteOnReturn.class);
                if (interfaceMethod.getAnnotation(DoNotCompleteOnReturn.class) != null) {
                    throw new IllegalArgumentException("Found @" + DoNotCompleteOnReturn.class.getSimpleName() +
                            " annotation on activity interface method \"" + interfaceMethod +
                            "\". This annotation applies only to activity implementation methods. " +
                            "Try moving it to \"" + implementationMethod + "\"");
                }
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("No implementation method?", e);
            }
            if (annotation != null) {
                doNotCompleteOnReturn = true;
            }
            this.activity = activity;
        }

        public ActivityTaskHandler.Result execute(WorkflowService.Iface service, String domain, ActivityTask task) {
            ActivityExecutionContext context = new ActivityExecutionContextImpl(service, domain, task, dataConverter);
            byte[] input = task.getInput();
            Object[] args = dataConverter.fromDataArray(input, method.getParameterTypes());
            CurrentActivityExecutionContext.set(context);
            try {
                Object result = method.invoke(activity, args);
                RespondActivityTaskCompletedRequest request = new RespondActivityTaskCompletedRequest();
                if (doNotCompleteOnReturn) {
                    return new ActivityTaskHandler.Result(null, null, null, null);
                }
                if (method.getReturnType() != Void.TYPE) {
                    request.setResult(dataConverter.toData(result));
                }
                return new ActivityTaskHandler.Result(request, null, null, null);
            } catch (RuntimeException e) {
                return mapToActivityFailure(task, e);
            } catch (InvocationTargetException e) {
                return mapToActivityFailure(task, e.getTargetException());
            } catch (IllegalAccessException e) {
                return mapToActivityFailure(task, e);
            } finally {
                CurrentActivityExecutionContext.unset();
            }
        }
    }
}
