package com.uber.cadence.serviceclient;

public interface IAuthorizationProvider {
    // getAuthToken provides the OAuth authorization token
    // It's called before every request to Cadence server, and sets the token in the request header.
   byte[] getAuthToken();
}
