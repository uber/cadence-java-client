/*
 *  Modifications Copyright (c) 2017-2021 Uber Technologies Inc.
 *  Portions of the Software are attributed to Copyright (c) 2020 Temporal Technologies Inc.
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.uber.cadence.serviceclient.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.sql.Date;
import java.time.Clock;

public class AdminJwtAuthorizationProvider implements IAuthorizationProvider {

  private final RSAPrivateKey rsaPrivateKey;
  private final RSAPublicKey rsaPublicKey;

  public AdminJwtAuthorizationProvider(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
    this.rsaPrivateKey = privateKey;
    this.rsaPublicKey = publicKey;
  }

  @Override
  public byte[] getAuthToken() {
    final JWTCreator.Builder jwtBuilder = JWT.create();
    jwtBuilder.withClaim("admin", true);
    jwtBuilder.withClaim("ttl", 60 * 10);
    jwtBuilder.withIssuedAt(Date.from(Clock.systemUTC().instant()));
    return jwtBuilder
        .sign(Algorithm.RSA256(this.rsaPublicKey, this.rsaPrivateKey))
        .getBytes(StandardCharsets.UTF_8);
  }
}
