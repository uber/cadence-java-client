/**
 * Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * <p>Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. A copy of the License is located at
 *
 * <p>http://aws.amazon.com/apache2.0
 *
 * <p>or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.uber.cadence.serviceclient.auth;

import com.google.api.client.auth.oauth2.ClientCredentialsTokenRequest;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

public class OAuthAuthorizationProvider implements IAuthorizationProvider {
  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

  private Instant nextRefresh;

  private final ClientCredentialsTokenRequest tokenRequest;

  private TokenResponse token;

  static final JsonFactory JSON_FACTORY = new GsonFactory();

  public OAuthAuthorizationProvider(
      String clientId, String clientSecret, String url, List<String> scopes) {
    this.tokenRequest =
        new ClientCredentialsTokenRequest(HTTP_TRANSPORT, JSON_FACTORY, new GenericUrl(url))
            .setClientAuthentication(new ClientParametersAuthentication(clientId, clientSecret))
            .setScopes(scopes);
  }

  @Override
  public byte[] getAuthToken() {
    return fetchToken().getBytes(StandardCharsets.UTF_8);
  }

  private synchronized String fetchToken() {
    if (this.nextRefresh == null || Instant.now().isAfter(this.nextRefresh)) {
      try {
        this.token = this.tokenRequest.execute();
        this.nextRefresh = Instant.now().plusSeconds(this.token.getExpiresInSeconds());
      } catch (IOException e) {
        return "";
      }
    }
    return this.token.getAccessToken();
  }
}
