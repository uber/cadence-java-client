package com.uber.cadence.converter;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TJSONProtocol;

/**
 * Special handling of TBase message serialization and deserialization. This is to support for
 * inline Thrift fields in Java class.
 */
public class TBaseTypeAdapterFactory implements TypeAdapterFactory {

  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
    // this class only serializes 'TBase' and its subtypes
    if (!TBase.class.isAssignableFrom(typeToken.getRawType())) {
      return null;
    }
    TypeAdapter<T> result =
        new TypeAdapter<T>() {
          @Override
          public void write(JsonWriter jsonWriter, T value) throws IOException {
            try {
              String result =
                  newThriftSerializer().toString((TBase) value, StandardCharsets.UTF_8.name());
              jsonWriter.value(result);
            } catch (TException e) {
              throw new DataConverterException("Failed to serialize TBase", e);
            }
          }

          @Override
          public T read(JsonReader jsonReader) throws IOException {
            String value = jsonReader.nextString();
            try {
              @SuppressWarnings("unchecked")
              T instance = (T) typeToken.getRawType().getConstructor().newInstance();
              newThriftDeserializer()
                  .deserialize((TBase) instance, value, StandardCharsets.UTF_8.name());
              return instance;
            } catch (Exception e) {
              throw new DataConverterException("Failed to deserialize TBase", e);
            }
          }
        }.nullSafe();
    return result;
  }

  private static TSerializer newThriftSerializer() {
    return new TSerializer(new TJSONProtocol.Factory());
  }

  private static TDeserializer newThriftDeserializer() {
    return new TDeserializer(new TJSONProtocol.Factory());
  }
}
