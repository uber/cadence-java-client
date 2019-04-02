package com.uber.cadence.converter;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import org.apache.thrift.TEnum;

/**
 * Special handling of TEnum serialization and deserialization. This is to support for inline TEnum
 * fields in Java class. The default gson serde serialize the TEnum with its String name
 * representation, this adapter serialize the TEnum class with its int representation.
 */
public class TEnumTypeAdapterFactory implements TypeAdapterFactory {

  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
    // this class only serializes 'TEnum' and its subtypes
    if (!TEnum.class.isAssignableFrom(typeToken.getRawType())) {
      return null;
    }
    TypeAdapter<T> result =
        new TypeAdapter<T>() {
          @Override
          public void write(JsonWriter jsonWriter, T value) throws IOException {
            jsonWriter.value(((TEnum) value).getValue());
          }

          @Override
          public T read(JsonReader jsonReader) throws IOException {
            int value = jsonReader.nextInt();
            try {
              Method m = (typeToken.getRawType().getDeclaredMethod("findByValue", Integer.TYPE));
              @SuppressWarnings("unchecked")
              T instance = (T) m.invoke(null, value);
              return instance;
            } catch (Exception e) {
              throw new DataConverterException("Failed to deserilize TEnum", e);
            }
          }
        }.nullSafe();
    return result;
  }
}
