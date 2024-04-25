package com.uber.cadence.internal.compatibility;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.thrift.TBase;
import org.apache.thrift.TFieldIdEnum;
import org.junit.Assert;

/**
 * Utility that asserts all fields on a Thrift object are present other than a specified list of
 * fields. This ensures that any changes to the IDL will result in the test failing unless either
 * the test or mapper is updated.
 */
public class MapperTestUtil {

  public static <E extends Enum<E> & TFieldIdEnum, M extends TBase<M, E>>
      void assertNoMissingFields(M message, Class<E> fields) {
    Assert.assertEquals(
        "All fields expected to be set", Collections.emptySet(), getUnsetFields(message, fields));
  }

  public static <E extends Enum<E> & TFieldIdEnum, M extends TBase<M, E>> void assertMissingFields(
      M message, Class<E> fields, String... values) {
    assertMissingFields(message, fields, ImmutableSet.copyOf(values));
  }

  public static <E extends Enum<E> & TFieldIdEnum, M extends TBase<M, E>> void assertMissingFields(
      M message, Class<E> fields, Set<String> expected) {
    Assert.assertEquals(
        "Additional fields are unexpectedly not set", expected, getUnsetFields(message, fields));
  }

  private static <E extends Enum<E> & TFieldIdEnum, M extends TBase<M, E>>
      Set<String> getUnsetFields(M message, Class<E> fields) {
    return Arrays.stream(fields.getEnumConstants())
        .filter(field -> !message.isSet(field))
        .map(TFieldIdEnum::getFieldName)
        .collect(Collectors.toSet());
  }
}
