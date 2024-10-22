package com.uber.cadence.testUtils;

public final class TestEnvironment {
  public static final String DOMAIN = "UnitTest";
  public static final String DOMAIN2 = "UnitTest2";
  /**
   * When set to true increases test, activity and workflow timeouts to large values to support
   * stepping through code in a debugger without timing out.
   */
  private static final boolean DEBUGGER_TIMEOUTS = false;

  private static final boolean USE_DOCKER_SERVICE =
      Boolean.parseBoolean(System.getenv("USE_DOCKER_SERVICE"));

  private TestEnvironment() {}

  public static boolean isDebuggerTimeouts() {
    return DEBUGGER_TIMEOUTS;
  }

  public static boolean isUseDockerService() {
    return USE_DOCKER_SERVICE;
  }
}
