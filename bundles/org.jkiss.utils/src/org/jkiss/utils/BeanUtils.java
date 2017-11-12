/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import java.lang.reflect.*;
import java.util.Collection;
import java.util.StringTokenizer;

/**
 * BeanUtils
 */
public class BeanUtils {

    public static boolean isGetterName(String name) {
        return name.startsWith("get") || name.startsWith("is") || name.startsWith("has");
    }

    public static String getPropertyNameFromGetter(String getterName) {
        if (getterName.startsWith("get")) {
            return
                Character.toLowerCase(getterName.charAt(3)) +
                    getterName.substring(4);
        } else if (getterName.startsWith("is")) {
            return
                Character.toLowerCase(getterName.charAt(2)) +
                    getterName.substring(3);
        } else if (getterName.startsWith("has")) {
            return
                Character.toLowerCase(getterName.charAt(3)) +
                    getterName.substring(4);
        } else {
            // Unrecognized getter name
            return null;
        }
    }

    public static String getSetterName(String getterName) {
        if (getterName.startsWith("get")) {
            return "set" + getterName.substring(3);
        } else if (getterName.startsWith("is")) {
            return "set" + getterName.substring(2);
        } else if (getterName.startsWith("has")) {
            return "set" + getterName.substring(3);
        } else {
            // Unrecognized getter name
            return null;
        }
    }

    /**
     * Returns a set method matching the property name.
     */
    public static Method getSetMethod(Class<?> cl, String propertyName) {
        Method method = getSetMethod(cl, propertyName, false);

        if (method != null) {
            return method;
        }

        return getSetMethod(cl, propertyName, true);
    }

    /**
     * Returns a set method matching the property name.
     */
    public static Method getSetMethod(
        Class<?> cl,
        String propertyName,
        boolean ignoreCase) {
        String setName = "set" + propertyNameToMethodName(propertyName);

        return getSetMethod(
            cl.getMethods(),
            setName,
            ignoreCase);
    }

    /**
     * Returns a get method matching the property name.
     */
    public static Method getGetMethod(Class<?> cl, String propertyName) {
        Method method = getGetMethod(cl, propertyName, false);

        return method != null ?
            method :
            getGetMethod(cl, propertyName, true);
    }

    /**
     * Returns a get method matching the property name.
     */
    public static Method getGetMethod(
        Class<?> cl,
        String propertyName,
        boolean ignoreCase) {
        String methodName = propertyNameToMethodName(propertyName);
        return getGetMethod(
            cl.getMethods(),
            "get" + methodName,
            "is" + methodName,
            ignoreCase);
    }

    /**
     * Converts a user's property name to a bean method name.
     *
     * @param propertyName the user property name
     * @return the equivalent bean method name
     */
    public static String propertyNameToMethodName(String propertyName) {
        char ch = propertyName.charAt(0);
        if (Character.isLowerCase(ch))
            propertyName = Character.toUpperCase(ch) + propertyName.substring(1);

        return propertyName;
    }

    /**
     * Converts a user's property name to a bean method name.
     *
     * @param methodName the method name
     * @return the equivalent property name
     */
    public static String methodNameToPropertyName(String methodName) {
        if (methodName.startsWith("get"))
            methodName = methodName.substring(3);
        else if (methodName.startsWith("set"))
            methodName = methodName.substring(3);
        else if (methodName.startsWith("is"))
            methodName = methodName.substring(2);

        if (methodName.length() == 0)
            return null;

        char ch = methodName.charAt(0);
        if (Character.isUpperCase(ch) && (methodName.length() == 1 || !Character.isUpperCase(methodName.charAt(1)))) {
            methodName = Character.toLowerCase(ch) + methodName.substring(1);
        }

        return methodName;
    }

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

    public static Object readObjectProperty(Object object, String propName)
        throws IllegalAccessException, InvocationTargetException {
        if (propName.indexOf('.') == -1) {
            Method getter = getGetMethod(object.getClass(), propName);
            return getter == null ? null : getter.invoke(object);
        }
        // Parse property path
        StringTokenizer st = new StringTokenizer(propName, ".");
        Object value = object;
        while (value != null && st.hasMoreTokens()) {
            String pathItem = st.nextToken();
            Method getter = getGetMethod(value.getClass(), pathItem);
            if (getter == null) {
                return null;
            }
            value = getter.invoke(value);
        }
        return value;
    }

    /**
     * Finds the matching set method
     */
    private static Method getGetMethod(
        Method[] methods,
        String getName,
        String isName,
        boolean ignoreCase) {
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];

            // The method must be public
            if (
                (!Modifier.isPublic(method.getModifiers())) ||
                    (!Modifier.isPublic(method.getDeclaringClass().getModifiers())) ||
                    (method.getParameterTypes().length != 0) ||
                    (method.getReturnType().equals(void.class))) {
                continue;
            } else if (!ignoreCase && method.getName().equals(getName)) {
                // If it matches the get name, it's the right method
                return method;
            } else if (ignoreCase && method.getName().equalsIgnoreCase(getName)) {
                // If it matches the get name, it's the right method
                return method;
            } else if (!method.getReturnType().equals(boolean.class)) {
                // The is methods must return boolean
                continue;
            } else if (!ignoreCase && method.getName().equals(isName)) {
                // If it matches the is name, it must return boolean
                return method;
            } else if (ignoreCase && method.getName().equalsIgnoreCase(isName)) {
                // If it matches the is name, it must return boolean
                return method;
            }
        }

        return null;
    }

    /**
     * Finds the matching set method
     *
     * @param setName the method name
     */
    private static Method getSetMethod(
        Method[] methods,
        String setName,
        boolean ignoreCase) {
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];

            // The method name must match
            if (
                !(ignoreCase ? method.getName().equalsIgnoreCase(setName) : method.getName().equals(setName)) ||
                    !Modifier.isPublic(method.getModifiers()) ||
                    !Modifier.isPublic(method.getDeclaringClass().getModifiers()) ||
                    method.getParameterTypes().length != 1
                )
                continue;

            return method;
        }

        return null;
    }

    public static final Short DEFAULT_SHORT = (short) 0;
    public static final Integer DEFAULT_INTEGER = 0;
    public static final Long DEFAULT_LONG = 0l;
    public static final Float DEFAULT_FLOAT = new Float(0.0);
    public static final Double DEFAULT_DOUBLE = 0.0;
    public static final Byte DEFAULT_BYTE = (byte) 0;
    public static final Character DEFAULT_CHAR = (char) 0;

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

    public static Object invokeObjectMethod(Object object, String name, Class<?> paramTypes[], Object args[])
        throws Throwable {
        Method method = object.getClass().getMethod(name, paramTypes);
        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        try {
            return method.invoke(object, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    public static Object invokeObjectMethod(Object object, String name)
        throws Throwable {
        Method method = object.getClass().getMethod(name);
        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        try {
            return method.invoke(object);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    public static Object invokeStaticMethod(Class<?> objectType, String name, Class<?> paramTypes[], Object args[])
        throws Throwable {
        Method method = objectType.getMethod(name, paramTypes);
        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        try {
            return method.invoke(null, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

}
