/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Common utils
 */
public class CommonUtils {

    public static final char PARAGRAPH_CHAR = (char) 182;

    public static boolean isJavaIdentifier(@NotNull CharSequence str) {
        if (str.length() == 0 || !Character.isJavaIdentifierStart(str.charAt(0))) {
            return false;
        }
        for (int i = 1; i < str.length(); i++) {
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

    @NotNull
    public static String escapeFileName(@Nullable String str) {
        if (str == null) {
            return "";
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


    public static String toCamelCase(String str) {
        if (isEmpty(str)) {
            return str;
        }

        final StringBuilder ret = new StringBuilder(str.length());

        boolean isWordStart = true;
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                if (isWordStart) {
                    ret.append(Character.toUpperCase(ch));
                    isWordStart = false;
                } else {
                    ret.append(Character.toLowerCase(ch));
                }
            } else {
                ret.append(ch);
                isWordStart = true;
            }
        }

        return ret.toString();
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

    /**
     * Swaps the element with its neighbor to the left in the specified list.
     * If the element is not present in the list or it is the leftmost element in the list,
     * the list remains unchanged.
     *
     * @param list list
     * @param element element
     * @param <T> type of the list
     */
    public static <T> void shiftLeft(@NotNull List<? super T> list, @NotNull T element) {
        int idx = list.indexOf(element);
        if (idx > 0) {
            Collections.swap(list, idx - 1, idx);
        }
    }

    /**
     * Swaps the element with its neighbor to the right in the specified list.
     * If the element is not present in the list or it is the rightmost element in the list,
     * the list remains unchanged.
     *
     * @param list list
     * @param element element
     * @param <T> type of the list
     */
    public static <T> void shiftRight(@NotNull List<? super T> list, @NotNull T element) {
        int idx = list.indexOf(element);
        if (idx != -1 && idx != list.size() - 1) {
            Collections.swap(list, idx, idx + 1);
        }
    }

    @NotNull
    public static String notEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    @Nullable
    public static String nullIfEmpty(@Nullable String value) {
        return value == null || value.isEmpty() ? null : value;
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

    public static boolean equalOrEmptyStrings(@Nullable String s1, @Nullable String s2) {
        return equalObjects(s1, s2) || (isEmpty(s1) && isEmpty(s2));
    }

    public static boolean equalsContents(@Nullable Collection<?> c1, @Nullable Collection<?> c2) {
        if (CommonUtils.isEmpty(c1) && CommonUtils.isEmpty(c2)) {
            return true;
        }
        if (c1 == null || c2 == null || c1.size() != c2.size()) {
            return false;
        }
        for (Object o : c1) {
            if (!c2.contains(o)) {
                return false;
            }
        }
        return true;
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
                try {
                    return (int)Double.parseDouble(toString(object));
                } catch (NumberFormatException e1) {
                    return def;
                }
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
        return toLong(object, 0);
    }

    public static long toLong(@Nullable Object object, long defValue) {
        if (object == null) {
            return defValue;
        } else if (object instanceof Number) {
            return ((Number) object).longValue();
        } else {
            try {
                return Long.parseLong(toString(object));
            } catch (NumberFormatException e) {
                try {
                    return (int)Double.parseDouble(toString(object));
                } catch (NumberFormatException e1) {
                    return defValue;
                }
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

    public static boolean isNumber(@Nullable Object object) {
        if (object == null) {
            return false;
        } else if (object instanceof Number) {
            return true;
        } else {
            try {
                Double.parseDouble(toString(object));
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    public static double toDouble(@Nullable Object object) {
        if (object == null) {
            return 0.0;
        } else if (object instanceof Number) {
            return ((Number) object).doubleValue();
        } else {
            try {
                return Double.parseDouble(toString(object));
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }
    }

    public static double toDouble(@Nullable Object object, double def) {
        if (object == null) {
            return def;
        } else if (object instanceof Number) {
            return ((Number) object).doubleValue();
        } else {
            try {
                return Double.parseDouble(toString(object));
            } catch (NumberFormatException e) {
                return def;
            }
        }
    }

    public static float toFloat(@Nullable Object object) {
        if (object == null) {
            return 0.0f;
        } else if (object instanceof Number) {
            return ((Number) object).floatValue();
        } else {
            try {
                return Float.parseFloat(toString(object));
            } catch (NumberFormatException e) {
                return Float.NaN;
            }
        }
    }

    public static float toFloat(@Nullable Object object, float def) {
        if (object == null) {
            return def;
        } else if (object instanceof Number) {
            return ((Number) object).floatValue();
        } else {
            try {
                return Float.parseFloat(toString(object));
            } catch (NumberFormatException e) {
                return def;
            }
        }
    }

    public static boolean isNaN(@Nullable Object value) {
        return (value instanceof Float && ((Float) value).isNaN())
            || (value instanceof Double && ((Double) value).isNaN());
    }

    public static boolean isInfinite(@Nullable Object value) {
        return (value instanceof Float && ((Float) value).isInfinite())
            || (value instanceof Double && ((Double) value).isInfinite());
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

    public static byte[] parseHexString(String hex) {
        int strLength = hex.length();
        byte[] data = new byte[strLength / 2];
        for (int i = 0; i < strLength; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
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

    public static String[] splitWithDelimiter(String s, String delimiter) {
        if (s == null) {
            return null;
        }
        String delimiterReplacement = "DRDRDR"; //$NON-NLS-1$
        s = s.replace(delimiter, delimiterReplacement + delimiter);
        return s.split(delimiterReplacement);
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
    public static String[] split(@Nullable String str, String delimiter) {
        if (CommonUtils.isEmpty(str)) {
            return new String[0];
        } else {
            return str.split(delimiter);
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

    public static boolean isBitSet(long value, long mask) {
        return (value & mask) == mask;
    }

    @Nullable
    public static <T extends Enum<T>> T valueOf(@NotNull Class<T> type, @Nullable String name) {
        return valueOf(type, name, null, false);
    }

    @Nullable
    public static <T extends Enum<T>> T valueOf(@Nullable Class<T> type, @Nullable String name, T defValue, boolean underscoreSpaces) {
        if (name == null) {
            return defValue;
        }
        name = name.trim();
        if (name.length() == 0) {
            return defValue;
        }
        if (underscoreSpaces) {
            name = name.replace(' ', '_');
        }
        try {
            return Enum.valueOf(type, name);
        } catch (Exception e) {
            e.printStackTrace();
            return defValue;
        }
    }

    public static <T extends Enum<T>> T valueOf(Class<T> enumType, String str, T defValue) {
        if (isEmpty(str)) {
            return defValue;
        }
        try {
            return Enum.valueOf(enumType, str);
        } catch (Exception e) {
            e.printStackTrace();
            return defValue;
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
        if (options == null) {
            return false;
        }
        Object optionValue = options.get(name);
        return getBoolean(optionValue, defValue);
    }

    public static Map<String, Object> makeStringMap(Map<Object, Object> objMap) {
        Map<String, Object> strMap = new LinkedHashMap<>(objMap.size());
        for (Map.Entry<Object, Object> e : objMap.entrySet()) {
            strMap.put(toString(e.getKey(), null), e.getValue());
        }
        return strMap;
    }

    public static String fixedLengthString(String string, int length) {
        return String.format("%1$"+length+ "s", string);
    }

    public static boolean startsWithIgnoreCase(@Nullable String str, @Nullable String startPart) {
        if (isEmpty(str) || isEmpty(startPart)) {
            return false;
        }
        return str.regionMatches(true, 0, startPart, 0, startPart.length());
    }

    public static String niceFormatFloat(float val) {
        if (val == (int) val)
            return String.valueOf((int)val);
        else
            return String.valueOf(val);
    }

    public static String niceFormatDouble(double val) {
        if (val == (long) val)
            return String.valueOf((long)val);
        else
            return String.valueOf(val);
    }

    public static String trim(String str) {
        return str == null ? null : str.trim();
    }

    public static String compactWhiteSpaces(String str) {
        return str.replaceAll("\\s+", " ");
    }

    public static String getSingleLineString(String displayString) {
        return displayString
            .replace('\n', PARAGRAPH_CHAR)
            .replace("\r", "")
            .replace("\t", " ")
            .replace((char)0, ' ');
    }

    public static int compare(Object o1, Object o2) {
        if (o1 == o2) {
            return 0;
        }
        if (o1 == null) {
            return -1;
        } else if (o2 == null) {
            return 1;
        }
        if (o1.getClass() == o2.getClass() && o1 instanceof Comparable) {
            return ((Comparable) o1).compareTo(o2);
        }
        return toString(o1).compareTo(toString(o2));
    }

    public static int compareNumbers(Number value1, Number value2) {
        double numDiff = value1.doubleValue() - value2.doubleValue();
        return numDiff < 0 ? -1 : (numDiff > 0 ? 1 : 0);
    }

    public static String cutExtraLines(String message, int maxLines) {
        if (message == null || message.indexOf('\n') == -1) {
            return message;
        }
        int lfCount = 0;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == '\n') {
                lfCount++;
            }
            buf.append(c);
            if (lfCount == maxLines) {
                buf.append("...");
                break;
            }
        }
        return buf.toString();
    }

    public static boolean isSameDay(@NotNull Date date1, @NotNull Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return isSameDay(cal1, cal2);
    }

    public static boolean isSameDay(@NotNull Calendar cal1, @NotNull Calendar cal2) {
        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
            cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }

    public static String escapeBourneShellString(@NotNull String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }

    public static String unescapeBourneShellString(@NotNull String s) {
        if (!s.startsWith("'") || !s.endsWith("'") || s.length() < 2) { //not an escaped bourne shell string
            return s;
        }
        return s.substring(1, s.length() - 1).replace("'\\''", "'");
    }

    /**
     * Checks whether the supplied <code>ch</code> is within
     * the range of the specified <code>radix</code> value.
     *
     * @param ch    character codepoint to be checked
     * @param radix desired radix
     * @return <code>true</code> if the character fits
     * into the radix, <code>false</code> otherwise
     */
    public static boolean isDigit(int ch, int radix) {
        if (radix <= 0 || radix > 36)
            return false;
        if (ch >= '0' && ch <= '9')
            return radix > ch - '0';
        if (ch >= 'a' && ch <= 'z')
            return radix > ch - 'a' + 10;
        if (ch >= 'A' && ch <= 'Z')
            return radix > ch - 'A' + 10;
        return false;
    }

    @NotNull
    @SafeVarargs
    public static <T> Set<T> unmodifiableSet(@NotNull T... vararg) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(vararg)));
    }

    /**
     * Checks if the {@code index} is within the bounds of the range from
     * {@code 0} (inclusive) to {@code length} (exclusive).
     *
     * <p>The {@code index} is defined to be out of bounds if any of the
     * following inequalities is true:
     * <ul>
     *  <li>{@code index < 0}</li>
     *  <li>{@code index >= length}</li>
     *  <li>{@code length < 0}, which is implied from the former inequalities</li>
     * </ul>
     *
     * @param index the index
     * @param length the upper-bound (exclusive) of the range
     * @return {@code true} if it is within bounds of the range
     */
    public static boolean isValidIndex(int index, int length) {
        return 0 <= index && index < length;
    }

    @NotNull
    public static String escapeHtml(@Nullable String text) {
        if (text == null) {
            return "&nbsp;";
        }
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\r\n", "<br>")
            .replace("\r", "<br>")
            .replace("\n", "<br>");
    }

    /**
     * Finds the object with the best matching name. Here we consider a case sensitive match better then a case insensitive one.
     *
     * @param objects container with objects
     * @param name to match
     * @param nameExtractor function which extracts the name from object
     * @param <T> type of objects to search from
     * @return the best match or {@code null} if nothing found
     */
    @Nullable
    public static <T> T findBestCaseAwareMatch(@NotNull Iterable<? extends T> objects, @NotNull String name,
                                               @NotNull Function<? super T, String> nameExtractor) {
        T firstCaseInsensitiveMatch = null;
        for (T obj: objects) {
            String objectName = nameExtractor.apply(obj);
            if (name.equals(objectName)) { //case sensitive match
                return obj;
            }
            if (firstCaseInsensitiveMatch == null && name.equalsIgnoreCase(objectName)) {
                firstCaseInsensitiveMatch = obj;
            }
        }
        return firstCaseInsensitiveMatch;
    }

    /**
     * Groups values into a map of their shared key and a list of matching values using that key.
     * <p>
     * <h3>Group strings by their first character</h3>
     * <pre>{@code
     * final List<String> values = Arrays.asList("aaa", "abb", "bbb", "bab", "ccc");
     * final Map<Character, List<String>> groups = group(values, x -> x.charAt(0));
     *
     * Assert.assertEquals(Arrays.asList("aaa", "abb"), groups.get('a'));
     * Assert.assertEquals(Arrays.asList("bbb", "bab"), groups.get('b'));
     * Assert.assertEquals(Arrays.asList("ccc"), groups.get('c'));
     * }</pre>
     * @param values values to group
     * @param keyExtractor a function that extracts key from value that is used to group values
     * @return map of a shared key and a list of matching values
     */
    @NotNull
    public static <K, V> Map<K, List<V>> group(@NotNull Collection<V> values, @NotNull Function<? super V, ? extends K> keyExtractor) {
        final Map<K, List<V>> grouped = new HashMap<>();
        for (V value : values) {
            final K key = keyExtractor.apply(value);
            final List<V> group = grouped.computeIfAbsent(key, k -> new ArrayList<>());
            group.add(value);
        }
        return grouped;
    }

    /**
     * Clamps given value to range between lower and upper bounds.
     *
     * @param value the value to clamp
     * @param min   the lower boundary to clamp {@code value} to
     * @param max   the upper boundary to clamp {@code value} to
     * @return {@code min} if {@code value} is less than {@code min}, {@code max} if {@code value} is greater than {@code max}, otherwise {@code value}
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    /**
     * Replaces every subsequence of the input sequence that matches the
     * pattern with the result of applying the given replacer function to the
     * match result of this matcher corresponding to that subsequence.
     */
    @NotNull
    public static String replaceAll(@NotNull String input, @NotNull String regex, @NotNull Function<Matcher, String> replacer) {
        final Matcher matcher = Pattern.compile(regex).matcher(input);
        final StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, replacer.apply(matcher));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
