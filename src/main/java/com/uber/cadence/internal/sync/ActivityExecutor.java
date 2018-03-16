package com.uber.cadence.internal.sync;

import com.uber.cadence.activity.ActivityOptions;
import com.uber.cadence.workflow.Promise;
import java.util.function.BiConsumer;

public interface ActivityExecutor {

  <T> void executeActivity(String name, ActivityOptions options, Object[] args, Class<T> returnType,
      BiConsumer<T, RuntimeException> resultCallback);

}
