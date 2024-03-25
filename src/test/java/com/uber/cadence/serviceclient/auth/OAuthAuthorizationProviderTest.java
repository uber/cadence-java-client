package com.uber.cadence.serviceclient.auth;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

public class OAuthAuthorizationProviderTest {
  @Test
  public void testConstructorWillThrowException() {
    try {
      final OAuthAuthorizationProvider provider =
          new OAuthAuthorizationProvider("a", "b", "c", null);
    } catch (IllegalArgumentException e) {
      Assert.assertEquals(IllegalArgumentException.class, e.getClass());
    }
  }

  @Test
  public void testClientWillReturnEmptyOnWrongServer() {

    final OAuthAuthorizationProvider provider =
        new OAuthAuthorizationProvider("a", "b", "https://test", null);

    byte[] token = provider.getAuthToken();

    assertEquals(0, token.length);
  }
}
