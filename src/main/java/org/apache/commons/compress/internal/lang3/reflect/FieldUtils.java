/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.internal.lang3.reflect;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Field;

public final class FieldUtils {

  public static Object readField(final Object target, final String fieldName, final boolean forceAccess) throws IllegalAccessException {
    requireNonNull(target, "target");
    requireNonNull(fieldName, "fieldName");
    final Class<?> cls = target.getClass();
    final Field field = getField(cls, fieldName, forceAccess);
    if (field == null) {
      throw new IllegalArgumentException(String.format("Cannot locate field %s on %s", fieldName, cls));
    }
    return field.get(target);
  }

  private static Field getField(final Class<?> cls, final String fieldName, final boolean forceAccess) {
    requireNonNull(cls, "cls");
    requireNonNull(fieldName, "fieldName");
    Class<?> currentClass = cls;
    while (currentClass != null) {
      try {
        final Field field = currentClass.getDeclaredField(fieldName);
        // getDeclaredField checks for non-public scopes as well
        // and it returns accurate results
        if (forceAccess && !field.isAccessible()) {
          field.setAccessible(true);
        }
        return field;
      } catch (final NoSuchFieldException ignored) {
        // ignore
      }
      currentClass = currentClass.getSuperclass();
    }
    return null;
  }

  private FieldUtils() {
  }
}
