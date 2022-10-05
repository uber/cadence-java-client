package com.uber.cadence.context;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;

import java.util.Arrays;
import java.util.HashMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ContextPropagationHandlerTest {

  @Mock private ContextPropagator contextPropagator;

  @Test
  public void refreshContextUpdatesContext() {
    ContextPropagationHandler contextPropagationHandler =
        new ContextPropagationHandler(Arrays.asList(contextPropagator), new HashMap<>());

    contextPropagationHandler.refreshContext();

    Mockito.verify(contextPropagator).deserializeContext(any());
  }
}
