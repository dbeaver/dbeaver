/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jkiss.utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Collection;

public class TypeUtils {

    public static final Short DEFAULT_SHORT = (short) 0;
    public static final Integer DEFAULT_INTEGER = 0;
    public static final Long DEFAULT_LONG = 0L;
    public static final Float DEFAULT_FLOAT = (float) 0.0;
    public static final Double DEFAULT_DOUBLE = 0.0;
    public static final Byte DEFAULT_BYTE = (byte) 0;
    public static final Character DEFAULT_CHAR = (char) 0;

    public static boolean isArrayType(Type type) {
        return (type instanceof Class && ((Class<?>) type).isArray());
    }

    public static boolean isCollectionType(Type type) {
        if (type instanceof Class && Collection.class.isAssignableFrom((Class<?>) type)) {
/*
            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType)type;
                if (pt.getActualTypeArguments().length == 1) {
                    return true;
                }
            }
*/
            return true;
        }
        return isArrayType(type);
    }

    public static Class<?> getCollectionType(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            if (pt.getActualTypeArguments().length == 1) {
                final Type argType = pt.getActualTypeArguments()[0];
                if (argType instanceof Class) {
                    return (Class<?>) argType;
                } else if (argType instanceof WildcardType) {
                    final Type[] upperBounds = ((WildcardType) argType).getUpperBounds();
                    if (upperBounds.length > 0 && upperBounds[0] instanceof Class) {
                        return (Class<?>) upperBounds[0];
                    }
                    final Type[] lowerBounds = ((WildcardType) argType).getLowerBounds();
                    if (lowerBounds.length > 0 && lowerBounds[0] instanceof Class) {
                        return (Class<?>) lowerBounds[0];
                    }
                }
            }
        }
        return null;
    }

    public static boolean isBooleanType(Type paramClass) {
        return paramClass == Boolean.class || paramClass == Boolean.TYPE;
    }

    public static Object getDefaultPrimitiveValue(Class<?> paramClass) {
        if (paramClass == Boolean.TYPE) {
            return Boolean.FALSE;
        } else if (paramClass == Short.TYPE) {
            return DEFAULT_SHORT;
        } else if (paramClass == Integer.TYPE) {
            return DEFAULT_INTEGER;
        } else if (paramClass == Long.TYPE) {
            return DEFAULT_LONG;
        } else if (paramClass == Float.TYPE) {
            return DEFAULT_FLOAT;
        } else if (paramClass == Double.TYPE) {
            return DEFAULT_DOUBLE;
        } else if (paramClass == Byte.TYPE) {
            return DEFAULT_BYTE;
        } else if (paramClass == Character.TYPE) {
            return DEFAULT_CHAR;
        } else {
            throw new IllegalArgumentException("Class " + paramClass.getName() + " is not primitive type");
        }
    }

    public static boolean isNumericType(Class<?> paramClass) {
        return
            Number.class.isAssignableFrom(paramClass) ||
                    paramClass == Short.TYPE ||
                    paramClass == Integer.TYPE ||
                    paramClass == Long.TYPE ||
                    paramClass == Double.TYPE ||
                    paramClass == Float.TYPE ||
                    paramClass == Byte.TYPE;
    }

    public static <T> Class<? extends T> findAssignableType(Class<?>[] types, Class<T> type) {
        for (Class<?> childType : types) {
            if (type.isAssignableFrom(childType)) {
                return (Class<? extends T>) childType;
            }
        }
        return null;
    }
}
