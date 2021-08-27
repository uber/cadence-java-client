/*
 *  Modifications Copyright (c) 2017-2021 Uber Technologies Inc.
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

import static org.junit.Assert.*;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

public class AdminJwtAuthorizationProviderTest {
  @Test
  public void testCreateAuthToken() throws NoSuchAlgorithmException, InvalidKeySpecException {

    Base64 b64 = new Base64();
    byte[] decodedPub = b64.decode(testPublicKey.getBytes(StandardCharsets.UTF_8));
    byte[] decodedPri = b64.decode(testPrivateKey.getBytes(StandardCharsets.UTF_8));

    KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");

    final RSAPublicKey rsaPublicKey =
        (RSAPublicKey) rsaKeyFactory.generatePublic(new X509EncodedKeySpec(decodedPub));

    final RSAPrivateKey rsaPrivateKey =
        (RSAPrivateKey) rsaKeyFactory.generatePrivate(new PKCS8EncodedKeySpec(decodedPri));

    final AdminJwtAuthorizationProvider authProvider =
        new AdminJwtAuthorizationProvider(rsaPublicKey, rsaPrivateKey);
    final String jwt = new String(authProvider.getAuthToken(), StandardCharsets.UTF_8);

    final DecodedJWT decodedJwt = JWT.decode(jwt);
    final Claim adminClaim = decodedJwt.getClaim("admin");
    assertTrue(adminClaim.asBoolean());
    final Claim ttlClaim = decodedJwt.getClaim("ttl");
    assertEquals((int) (60 * 10), (int) ttlClaim.asInt());
  }

  private static String testPublicKey =
      "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAscukltHilaq+o5gIVE4P\n"
          + "GwWl+esvJ2EaEpWw6ogr98Un11YJ4oKkwIkLw4iIo0tveCINA3cZmxaW1RejRWKE\n"
          + "qYFtQ1rYd6BsnFAHXWh2R3A1FtpG6ANUEGkE7OAJe2/L42E/ImJ+GQxRvartInDM\n"
          + "yfiRfB7+L2n3wG+Ni+hBNMtAaX4Wwbj2hup21Jjuo96TuhcGImBFBATGWaYR2wqe\n"
          + "/6by9wJexPHlY/1uDp3SnzF1dCLjp76SGCfyYqOGC/PxhQi7mDxeH9/tIC+lt/Sz\n"
          + "wc1n8gZLtlRlZHinvYa8lhWXqVYw6WD8h4LTgALq9iY+beD1PFQSY1GkQtt0RhRw\n"
          + "eQIDAQAB";

  private static String testPrivateKey =
      "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQCxy6SW0eKVqr6j\n"
          + "mAhUTg8bBaX56y8nYRoSlbDqiCv3xSfXVgnigqTAiQvDiIijS294Ig0DdxmbFpbV\n"
          + "F6NFYoSpgW1DWth3oGycUAddaHZHcDUW2kboA1QQaQTs4Al7b8vjYT8iYn4ZDFG9\n"
          + "qu0icMzJ+JF8Hv4vaffAb42L6EE0y0BpfhbBuPaG6nbUmO6j3pO6FwYiYEUEBMZZ\n"
          + "phHbCp7/pvL3Al7E8eVj/W4OndKfMXV0IuOnvpIYJ/Jio4YL8/GFCLuYPF4f3+0g\n"
          + "L6W39LPBzWfyBku2VGVkeKe9hryWFZepVjDpYPyHgtOAAur2Jj5t4PU8VBJjUaRC\n"
          + "23RGFHB5AgMBAAECggEABj1T9Orf0W9nskDQ2QQ7cuVdZEJjpMrbTK1Aw1L8/Qc9\n"
          + "TSkINDEayaV9mn1RXe61APcBSdP4ER7nXfTZiQ21LhLcWWg9T3cbh1b70oRqyI9z\n"
          + "Pi6HSBeWz4kfUBX9izMQFBZKzjYn6qaJp1b8bGXKRWkcvPRZqLhmsRPmeH3xrOHe\n"
          + "qsIDhYXMjRoOgEUxLbk8iPLP6nx0icPJl/tHK2l76R+1Ko6TBE69Md2krUIuh0u4\n"
          + "nm9n+Az+0GuvkFsLw5KMGhSBeqB+ez5qtFa8T8CUCn98IjiUDOwgZdFrNldFLcZf\n"
          + "putw7O2qCA9LT+mFBQ6CVsVu/9tKeXQ9sJ7p3lxhwQKBgQDjt7HNIabLncdXPMu0\n"
          + "ByRyNVme0+Y1vbj9Q7iodk77hvlzWpD1p5Oyvq7cN+Cb4c1iO/ZQXMyUw+9hLgmf\n"
          + "LNquH2d4hK1Jerzc/ciwu6dUBsCW8+0VJd4M2UNN15rJMPvbZGmqMq9Np1iCTCjE\n"
          + "dvHo7xjPcJhsbhMbHq+PaUU7OQKBgQDH4KuaHBFTGUPkRaQGAZNRB8dDvSExV6ID\n"
          + "Pblzr80g9kKHUnQCQfIDLjHVgDbTaSCdRw7+EXRyRmLy5mfPWEbUFfIemEpEcEcb\n"
          + "3geWeVDx4Z/FwprWFuVifRopRSQ/FAbMXLIui7OHXWLEtzBvLkR/uS2VIVPm10PV\n"
          + "pbh2EXifQQKBgQDbcOLbjelBYLt/euvGgfeCQ50orIS1Fy5UidVCKjh0tR5gJk95\n"
          + "G1L+tjilqQc+0LtuReBYkwTm+2YMXSQSi1P05fh9MEYZgDjOMZYbkcpu887V6Rx3\n"
          + "+7Te5uOv+OyFozmhs0MMK6m5iGGHtsK2iPUYBoj/Jj8MhorM4KZH6ic4KQKBgQCl\n"
          + "3zIpg09xSc9Iue5juZz6qtzXvzWzkAj4bZnggq1VxGfzix6Q3Q8tSoG6r1tQWLbj\n"
          + "Lpwnhm6/guAMud6+eIDW8ptqfnFrmE26t6hOXMEq6lXANT5vmrKj6DP0uddZrZHy\n"
          + "uJ55+B91n68elvPP4HKiGBfW4cCSGmTGAXAyM0+JwQKBgQCz2cNiFrr+oEnlHDLg\n"
          + "EqsiEufppT4FSZPy9/MtuWuMgEOBu34cckYaai+nahQLQvH62KskTK0EUjE1ywub\n"
          + "NPORuXcugxIBMHWyseOS7lrtrlSBxU9gntS7jHdM3IMrrUy9YZBvPvFGP0wLdpKM\n"
          + "nvt3vT46hs3n28XZpb18uRkSDw==";
}
