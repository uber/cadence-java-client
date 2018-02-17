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
package com.uber.cadence.converter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;

/**
 * Implements conversion through GSON JSON processor.
 *
 * @author fateev
 */
public class JsonDataConverter implements DataConverter {

    private static final DataConverter INSTANCE = new JsonDataConverter();
    private static final byte[] EMPTY_BLOB = new byte[0];
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    private final Gson gson;
    private final JsonParser parser = new JsonParser();

    public static DataConverter getInstance() {
        return INSTANCE;
    }

    private JsonDataConverter() {
        gson = new GsonBuilder().serializeNulls().create();
    }

    @Override
    public byte[] toData(Object... values) throws DataConverterException {
        if (values == null || values.length == 0) {
            return EMPTY_BLOB;
        }
        try {
            if (values.length == 1) {
                return gson.toJson(values[0]).getBytes(StandardCharsets.UTF_8);
            }
            return gson.toJson(values).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new DataConverterException(e);
        }
    }

    @Override
    public <T> T fromData(byte[] content, Class<T> valueType) throws DataConverterException {
        try {
            return gson.fromJson(new String(content, StandardCharsets.UTF_8), valueType);
        } catch (Exception e) {
            throw new DataConverterException(content, e);
        }
    }

    @Override
    public Object[] fromDataArray(byte[] content, Class<?>... valueType) throws DataConverterException {
        try {
            if ((content == null || content.length == 0) && (valueType == null || valueType.length == 0)) {
                return EMPTY_OBJECT_ARRAY;
            }
            if (valueType.length == 1) {
                return new Object[]{fromData(content, valueType[0])};
            }
            JsonArray array = parser.parse(new String(content, StandardCharsets.UTF_8)).getAsJsonArray();
            Object[] result = new Object[valueType.length];
            for (int i = 0; i < valueType.length; i++) {
                result[i] = gson.fromJson(array.get(i), valueType[i]);
            }
            return result;
        } catch (Exception e) {
            throw new DataConverterException(content, valueType, e);
        }
    }
}
