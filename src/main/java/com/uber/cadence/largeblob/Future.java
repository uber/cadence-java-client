/*
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

package com.uber.cadence.largeblob;

import java.io.IOException;

/**
 * Future is used for passing around potentially large parameters between activities. Future can never be used in Workflow code because it uses an external storage and that will make
 * workflow code non deterministic. Small amounts of data are stored in the future instance itself, while larger amounts of data are stored in an external storage.
 */
public class Future {

  private byte[] encoded;
  private String url;
  private final Storage storage;
  private final long maxBytesInMemory;

  public Future(Storage storage, long maxBytesInMemory, byte[] encoded) {
    this.storage = storage;
    this.maxBytesInMemory = maxBytesInMemory;
    this.encoded = encoded;
  }

  public Future(Storage storage, long maxBytesInMemory, String url) {
    this.url = url;
    this.storage = storage;
    this.maxBytesInMemory = maxBytesInMemory;
  }

  public Future(Storage storage, long maxBytesInMemory) {
    if (storage == null) {
      throw new IllegalArgumentException("storage can't be null");
    }

    this.storage = storage;
    this.maxBytesInMemory = maxBytesInMemory;
  }

  public byte[] get() throws IOException {
    if (encoded != null) {
      return encoded;
    }

    if (url != null) {
      return storage.get(url);
    }

    return null;
  }

  public void put(String url, byte[] bytes) throws IOException {
    this.url = url;
    if (bytes.length < maxBytesInMemory) {
      this.encoded = bytes;
    } else {
      storage.put(url, bytes);
    }
  }

  public void delete() throws IOException {
    if (this.encoded == null) {
      storage.delete(url);
    } else {
      this.encoded = null;
    }
  }

  public byte[] getEncoded() {
    return encoded;
  }

  public String getUrl() {
    return url;
  }
}
