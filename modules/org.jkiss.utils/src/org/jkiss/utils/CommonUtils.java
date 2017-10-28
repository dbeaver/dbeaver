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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Common utils
 */
public class CommonUtils {

    public static boolean isJavaIdentifier(@NotNull CharSequence str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isJavaIdentifierPart(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    public static String escapeJavaString(@NotNull String str) {
        StringBuilder res = new StringBuilder(str.length() + 5);
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"':
                    res.append("\\\"");
                    break;
                case '\n':
                    res.append("\\n");
                    break;
                case '\r':
                    res.append("\\r");
                    break;
                case '\t':
                    res.append("\\t");
                    break;
                default:
                    res.append(c);
                    break;
            }
        }
        return res.toString();
    }

    @Nullable
    public static String escapeIdentifier(@Nullable String str) {
        if (str == null) {
            return null;
        }
        StringBuilder res = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isJavaIdentifierPart(c)) {
                res.append(c);
            } else {
                if (res.length() == 0 || res.charAt(res.length() - 1) != '_') {
                    res.append('_');
                }
            }
        }
        return res.toString();
    }

    @Nullable
    public static String escapeFileName(@Nullable String str) {
        if (str == null) {
            return null;
        }
        StringBuilder res = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isISOControl(c) || c == '\\' || c == '/' || c == '<' || c == '>' || c == '|' || c == '"' || c == ':'
                || c == '*' || c == '?') {
                res.append('_');
            } else {
                res.append(c);
            }
        }
        return res.toString();
    }

    public static String makeDirectoryName(@NotNull String str) {
        if (!str.endsWith("/")) {
            str += "/";
        }
        return str;
    }

    @NotNull
    public static String removeTrailingSlash(@NotNull String str) {
        while (str.endsWith("/") || str.endsWith("\\")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    public static String capitalizeWord(String str) {
        if (isEmpty(str) || Character.isUpperCase(str.charAt(0))) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    @NotNull
    public static <T> T notNull(@Nullable T value, @NotNull T defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static boolean isEmpty(@Nullable CharSequence value) {
        return value == null || value.length() == 0;
    }

    public static boolean isEmpty(@Nullable String value) {
        return value == null || value.length() == 0;
    }

    public static boolean isNotEmpty(@Nullable String value) {
        return !isEmpty(value);
    }

    public static boolean isEmpty(@Nullable Collection<?> value) {
        return value == null || value.isEmpty();
    }

    public static boolean isEmpty(@Nullable Map<?, ?> value) {
        return value == null || value.isEmpty();
    }

    @NotNull
    public static <T> Collection<T> safeCollection(@Nullable Collection<T> theList) {
        if (theList == null) {
            theList = Collections.emptyList();
        }
        return theList;
    }

    @NotNull
    public static <T> List<T> safeList(@Nullable List<T> theList) {
        if (theList == null) {
            theList = Collections.emptyList();
        }
        return theList;
    }

    @NotNull
    public static <T> List<T> copyList(@Nullable Collection<T> theList) {
        if (theList == null) {
            return new ArrayList<>();
        } else {
            return new ArrayList<>(theList);
        }
    }

    @NotNull
    public static String notEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    public static boolean isTrue(Boolean value) {
        return value != null && value;
    }

    public static boolean getBoolean(String value) {
        return Boolean.parseBoolean(value);
    }

    public static boolean getBoolean(@Nullable String value, boolean defaultValue) {
        return isEmpty(value) ? defaultValue : Boolean.parseBoolean(value);
    }

    public static boolean getBoolean(@Nullable Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        } else if (value instanceof Boolean) {
            return (Boolean) value;
        } else {
            return getBoolean(value.toString(), defaultValue);
        }
    }

    @NotNull
    public static String getLineSeparator() {
        String lineSeparator = System.getProperty(StandardConstants.ENV_LINE_SEPARATOR);
        return lineSeparator == null ? "\n" : lineSeparator;
    }

    @NotNull
    public static Throwable getRootCause(@NotNull Throwable ex) {
        Throwable rootCause = ex;
        for (; ; ) {
            if (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            } else if (rootCause instanceof InvocationTargetException
                && ((InvocationTargetException) rootCause).getTargetException() != null) {
                rootCause = ((InvocationTargetException) rootCause).getTargetException();
            } else {
                break;
            }
        }
        return rootCause;
    }

    public static boolean equalObjects(@Nullable Object o1, @Nullable Object o2) {
        if (o1 == o2) {
            return true;
        }
        if (o1 == null || o2 == null) {
            return false;
        }
//        if (o1.getClass() != o2.getClass()) {
//            return false;
//        }
        return o1.equals(o2);
    }

    @NotNull
    public static String toString(@Nullable Object object) {
        if (object == null) {
            return "";
        } else if (object instanceof String) {
            return (String) object;
        } else {
            return object.toString();
        }
    }

    public static String toString(@Nullable Object object, String def) {
        if (object == null) {
            return def;
        } else if (object instanceof String) {
            return (String) object;
        } else {
            return object.toString();
        }
    }

    public static boolean toBoolean(@Nullable Object object) {
        return object != null && getBoolean(object.toString());
    }

    public static int toInt(@Nullable Object object, int def) {
        if (object == null) {
            return def;
        } else if (object instanceof Number) {
            return ((Number) object).intValue();
        } else {
            try {
                return Integer.parseInt(toString(object));
            } catch (NumberFormatException e) {
                return def;
            }
        }
    }

    public static int toInt(@Nullable Object object) {
        return toInt(object, 0);
    }

    public static boolean isInt(@Nullable Object object) {
        if (object == null) {
            return false;
        } else if (object instanceof Number) {
            return true;
        } else {
            try {
                Integer.parseInt(toString(object));
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    public static long toLong(@Nullable Object object) {
        if (object == null) {
            return 0;
        } else if (object instanceof Number) {
            return ((Number) object).longValue();
        } else {
            try {
                return Long.parseLong(toString(object));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
    }

    public static boolean isLong(@Nullable Object object) {
        if (object == null) {
            return false;
        } else if (object instanceof Number) {
            return true;
        } else {
            try {
                Long.parseLong(toString(object));
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    @NotNull
    public static String toHexString(@Nullable byte[] bytes) {
        return bytes == null ? "" : toHexString(bytes, 0, bytes.length);
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    @NotNull
    public static String toHexString(@Nullable byte[] bytes, int offset, int length) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        char[] hexChars = new char[length * 2];
        for (int i = 0; i < length; i++) {
            int v = bytes[offset + i] & 0xFF;
            hexChars[i * 2] = hexArray[v >>> 4];
            hexChars[i * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String toBinaryString(long longValue, int bitCount) {
        String strValue = Long.toString(longValue, 2);
        if (strValue.length() < bitCount) {
            char[] headZeroes = new char[bitCount - strValue.length()];
            Arrays.fill(headZeroes, '0');
            strValue = String.valueOf(headZeroes) + strValue;
        }
        return strValue;
    }

    @NotNull
    public static List<String> splitString(@Nullable String str, char delimiter) {
        if (CommonUtils.isEmpty(str)) {
            return Collections.emptyList();
        } else {
            List<String> result = new ArrayList<>();
            StringTokenizer st = new StringTokenizer(str, String.valueOf(delimiter));
            while (st.hasMoreTokens()) {
                result.add(st.nextToken());
            }
            return result;
        }
    }

    @NotNull
    public static String makeString(@Nullable List<String> tokens, char delimiter) {
        if (tokens == null) {
            return "";
        } else if (tokens.size() == 1) {
            return tokens.get(0);
        } else {
            StringBuilder buf = new StringBuilder();
            for (String token : tokens) {
                if (buf.length() > 0) {
                    buf.append(delimiter);
                }
                buf.append(token);
            }
            return buf.toString();
        }
    }

    @Nullable
    public static String truncateString(@Nullable String str, int maxLength) {
        if (str != null && str.length() > maxLength) {
            return str.substring(0, maxLength);
        }
        return str;
    }

    public static String joinStrings(String divider, String ... array) {
        if (array == null) return "";
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) str.append(divider);
            str.append(array[i]);
        }
        return str.toString();
    }

    public static String joinStrings(String divider, Collection<String> col) {
        if (col == null) return "";
        StringBuilder str = new StringBuilder();
        for (String item : col) {
            if (str.length() > 0) str.append(divider);
            str.append(item);
        }
        return str.toString();
    }

    public static boolean isEmptyTrimmed(@Nullable String str) {
        return str == null || str.length() == 0 || str.trim().length() == 0;
    }

    public static boolean isBitSet(int value, int mask) {
        return (value & mask) == mask;
    }

    @Nullable
    public static <T extends Enum<T>> T valueOf(@NotNull Class<T> type, @Nullable String name) {
        return valueOf(type, name, false);
    }

    @Nullable
    public static <T extends Enum<T>> T valueOf(@Nullable Class<T> type, @Nullable String name, boolean underscoreSpaces) {
        if (name == null) {
            return null;
        }
        name = name.trim();
        if (name.length() == 0) {
            return null;
        }
        if (underscoreSpaces) {
            name = name.replace(' ', '_');
        }
        try {
            return Enum.valueOf(type, name);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @NotNull
    public static <T> T getItem(@NotNull Collection<T> collection, int index) {
        if (collection instanceof List) {
            return ((List<T>) collection).get(index);
        } else {
            Iterator<T> iter = collection.iterator();
            for (int i = 0; i < index; i++) {
                iter.next();
            }
            return iter.next();
        }
    }

    @NotNull
    public static <T extends Enum<T>> T fromOrdinal(Class<T> enumClass, int ordinal) {
        T[] enumConstants = enumClass.getEnumConstants();
        for (T value : enumConstants) {
            if (value.ordinal() == ordinal) {
                return value;
            }
        }
        IllegalArgumentException error = new IllegalArgumentException("Invalid ordinal " + ordinal + " for type " + enumClass.getName());
        if (enumConstants.length == 0) {
            throw error;
        } else {
            error.printStackTrace(System.err);
            return enumConstants[0];
        }
    }

    @NotNull
    public static <T> List<T> filterCollection(@NotNull Collection<?> collection, @NotNull Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Object item : collection) {
            if (type.isInstance(item)) {
                result.add(type.cast(item));
            }
        }
        return result;
    }

    @NotNull
    public static String escapeDisplayString(@NotNull final String delim) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < delim.length(); i++) {
            char c = delim.charAt(i);
            if (c == '\n') {
                str.append("\\n");
            } else if (c == '\r') {
                str.append("\\r");
            } else if (c == '\t') {
                str.append("\\t");
            } else {
                str.append(c);
            }
        }
        return str.toString();
    }

    @NotNull
    public static String unescapeDisplayString(@NotNull final String delim) {
        return delim.replace("\\t", "\t").replace("\\n", "\n").replace("\\r", "\r");
    }

    public static int hashCode(@Nullable  Object obj) {
        return obj == null ? 0 : obj.hashCode();
    }

    public static <T> T getOption(Map<String, ?> options, String name, T defValue) {
        Object optionValue = options.get(name);
        if (optionValue == null) {
            return defValue;
        }
        return (T)optionValue;
    }

    public static boolean getOption(Map<String, ?> options, String name) {
        return getOption(options, name, false);
    }

    public static boolean getOption(Map<String, ?> options, String name, boolean defValue) {
        Object optionValue = options.get(name);
        if (optionValue == null) {
            return defValue;
        }
        return Boolean.TRUE.equals(optionValue);
    }

}
