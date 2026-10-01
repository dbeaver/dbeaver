/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class CommonUtilsTest {

    @Test
    public void testIsJavaIdentifier() {
        Assertions.assertEquals(false, CommonUtils.isJavaIdentifier(""));
        Assertions.assertEquals(false, CommonUtils.isJavaIdentifier("|"));
        Assertions.assertEquals(false, CommonUtils.isJavaIdentifier("a-b"));
        Assertions.assertEquals(true, CommonUtils.isJavaIdentifier("aa"));
    }

    @Test
    public void testEscapeJavaString() throws Exception {
        Assertions.assertEquals("", CommonUtils.escapeJavaString(""));
        Assertions.assertEquals("\\\"", CommonUtils.escapeJavaString("\""));
        Assertions.assertEquals("\\n", CommonUtils.escapeJavaString("\n"));
        Assertions.assertEquals("\\r", CommonUtils.escapeJavaString("\r"));
        Assertions.assertEquals("\\t", CommonUtils.escapeJavaString("\t"));
        Assertions.assertEquals("a", CommonUtils.escapeJavaString("a"));
    }

    @Test
    public void testEscapeIdentifier() {
        Assertions.assertNull(CommonUtils.escapeIdentifier(null));
        Assertions.assertEquals("", CommonUtils.escapeIdentifier(""));
        Assertions.assertEquals("_", CommonUtils.escapeIdentifier("|"));
        Assertions.assertEquals("_", CommonUtils.escapeIdentifier("||"));
        Assertions.assertEquals("a_", CommonUtils.escapeIdentifier("a|"));
    }

    @Test
    public void testEscapeFileName() {
        Assertions.assertEquals("", CommonUtils.escapeFileName(null));
        Assertions.assertEquals("_", CommonUtils.escapeFileName("\\"));
        Assertions.assertEquals("_", CommonUtils.escapeFileName("\u0013"));
        Assertions.assertEquals("_", CommonUtils.escapeFileName("/"));
        Assertions.assertEquals("_", CommonUtils.escapeFileName("<"));
        Assertions.assertEquals("_", CommonUtils.escapeFileName(">"));
        Assertions.assertEquals("_", CommonUtils.escapeFileName("|"));
        Assertions.assertEquals("_", CommonUtils.escapeFileName("\""));
        Assertions.assertEquals("_", CommonUtils.escapeFileName(":"));
        Assertions.assertEquals("_", CommonUtils.escapeFileName("*"));
        Assertions.assertEquals("_", CommonUtils.escapeFileName("?"));
        Assertions.assertEquals("a", CommonUtils.escapeFileName("a"));
    }

    @Test
    public void testMakeDirectoryName() {
        Assertions.assertEquals("a/", CommonUtils.makeDirectoryName("a"));
        Assertions.assertEquals("a/", CommonUtils.makeDirectoryName("a/"));
    }

    @Test
    public void testRemoveTrailingSlash() {
        Assertions.assertEquals("a", CommonUtils.removeTrailingSlash("a/"));
        Assertions.assertEquals("a", CommonUtils.removeTrailingSlash("a\\"));
        Assertions.assertEquals("a", CommonUtils.removeTrailingSlash("a"));
    }

    @Test
    public void testCapitalizeWord() {
        Assertions.assertEquals("", CommonUtils.capitalizeWord(""));
        Assertions.assertEquals("Abc", CommonUtils.capitalizeWord("Abc"));
        Assertions.assertEquals("Abc", CommonUtils.capitalizeWord("abc"));
    }

    @Test
    public void testToCamelCase() {
        Assertions.assertNull(CommonUtils.toCamelCase(null));
        Assertions.assertEquals(CommonUtils.toCamelCase(""), "");
        Assertions.assertEquals("Abcd", CommonUtils.toCamelCase("abcd"));
        Assertions.assertEquals("Ab|Cd", CommonUtils.toCamelCase("ab|cd"));
    }

    @Test
    public void testNotNull() {
        final Object value = "value";
        final Object defaultValue = "defaultValue";
        Assertions.assertEquals(value, CommonUtils.notNull(value, defaultValue));
        Assertions.assertEquals(defaultValue, CommonUtils.notNull(null, defaultValue));
    }

    @Test
    public void testIsEmptyCharSequence() {
        final CharSequence nullValue = null;
        final CharSequence emptyValue = "";
        final CharSequence value = "abc";
        Assertions.assertTrue(CommonUtils.isEmpty(nullValue));
        Assertions.assertTrue(CommonUtils.isEmpty(emptyValue));
        Assertions.assertFalse(CommonUtils.isEmpty(value));
    }

    @Test
    public void testIsEmptyString() {
        final String nullValue = null;
        final String emptyValue = "";
        final String value = "abc";
        Assertions.assertTrue(CommonUtils.isEmpty(nullValue));
        Assertions.assertTrue(CommonUtils.isEmpty(emptyValue));
        Assertions.assertFalse(CommonUtils.isEmpty(value));
    }

    @Test
    public void testIsNotEmpty() {
        final String nullValue = null;
        final String emptyValue = "";
        final String value = "abc";
        Assertions.assertFalse(CommonUtils.isNotEmpty(nullValue));
        Assertions.assertFalse(CommonUtils.isNotEmpty(emptyValue));
        Assertions.assertTrue(CommonUtils.isNotEmpty(value));
    }

    @Test
    public void testIsEmptyCollection() {
        final ArrayList<Character> nullValue = null;
        final ArrayList<Character> emptyValue = new ArrayList<>();
        final ArrayList<Character> value = new ArrayList<>();
        value.add('a');
        Assertions.assertTrue(CommonUtils.isEmpty(nullValue));
        Assertions.assertTrue(CommonUtils.isEmpty(emptyValue));
        Assertions.assertFalse(CommonUtils.isEmpty(value));
    }

    @Test
    public void testIsEmptyMap() {
        final HashMap<Integer, Character> nullValue = null;
        final HashMap<Integer, Character> emptyValue = new HashMap<>();
        final HashMap<Integer, Character> value = new HashMap<>();
        value.put(0, 'a');
        Assertions.assertTrue(CommonUtils.isEmpty(nullValue));
        Assertions.assertTrue(CommonUtils.isEmpty(emptyValue));
        Assertions.assertFalse(CommonUtils.isEmpty(value));
    }

    @Test
    public void testSafeCollection() {
        final ArrayList<Character> theList = new ArrayList<>();
        Assertions.assertEquals(theList, CommonUtils.safeCollection(null));
        Assertions.assertEquals(theList, CommonUtils.safeCollection(theList));
    }

    @Test
    public void testSafeList() {
        final ArrayList<Character> theList = new ArrayList<>();
        Assertions.assertEquals(theList, CommonUtils.safeList(null));
        Assertions.assertEquals(theList, CommonUtils.safeList(theList));
    }

    @Test
    public void testCopyList() {
        final ArrayList<Integer> theList = new ArrayList<>();
        Assertions.assertEquals(theList, CommonUtils.copyList(null));

        theList.add(0);
        Assertions.assertEquals(theList, CommonUtils.copyList(theList));
    }

    @Test
    public void testNotEmpty() {
        Assertions.assertEquals("", CommonUtils.notEmpty(null));
        Assertions.assertEquals("abc", CommonUtils.notEmpty("abc"));
    }

    @Test
    public void testNullIfEmpty() {
        Assertions.assertNull(CommonUtils.nullIfEmpty(null));
        Assertions.assertNull(CommonUtils.nullIfEmpty(""));
        Assertions.assertEquals("abc", CommonUtils.nullIfEmpty("abc"));
    }

    @Test
    public void testIsTrue() {
        Assertions.assertTrue(CommonUtils.isTrue(true));
        Assertions.assertFalse(CommonUtils.isTrue(false));
        Assertions.assertFalse(CommonUtils.isTrue(null));
    }

    @Test
    public void testGetBooleanString() {
        Assertions.assertTrue(CommonUtils.getBoolean("true"));
        Assertions.assertFalse(CommonUtils.getBoolean("false"));
        Assertions.assertFalse(CommonUtils.getBoolean("null"));
    }

    @Test
    public void testGetBooleanStringDefault() {
        Assertions.assertTrue(CommonUtils.getBoolean("", true));
        Assertions.assertFalse(CommonUtils.getBoolean("false", true));
    }

    @Test
    public void testGetBooleanObjectDefault() {
        final Object nullValue = null;
        final Object value = 0;
        Assertions.assertTrue(CommonUtils.getBoolean(nullValue, true));
        Assertions.assertTrue(CommonUtils.getBoolean(true, false));
        Assertions.assertFalse(CommonUtils.getBoolean(value, true));
    }

/*
  @PrepareForTest({ CommonUtils.class, System.class })
  @Test
  public void testGetLineSeparator() {
    PowerMockito.mockStatic(System.class);
    PowerMockito.when(System.getProperty(or(isA(String.class), isNull(String.class)))).thenReturn("\r\n");
    Assertions.assertEquals("\r\n", CommonUtils.getLineSeparator());

    PowerMockito.when(System.getProperty(or(isA(String.class), isNull(String.class)))).thenReturn(null);
    Assertions.assertEquals("\n", CommonUtils.getLineSeparator());
  }
*/

    @Test
    public void testGetRootCause() {
        Assertions.assertEquals("def", CommonUtils.getRootCause(new Throwable("abc", new Throwable("def"))).getMessage());
        Assertions.assertEquals("abc", CommonUtils.getRootCause(new Throwable("abc")).getMessage());
        Assertions.assertNull(CommonUtils.getRootCause(new InvocationTargetException(null)).getMessage());
    }

    @Test
    public void testEqualOrEmptyStrings() {
        Assertions.assertTrue(CommonUtils.equalOrEmptyStrings("abc", "abc"));
        Assertions.assertFalse(CommonUtils.equalOrEmptyStrings("abc", null));
        Assertions.assertFalse(CommonUtils.equalOrEmptyStrings(null, "def"));
        Assertions.assertFalse(CommonUtils.equalOrEmptyStrings("abc", "def"));
        Assertions.assertTrue(CommonUtils.equalOrEmptyStrings("", ""));
    }

    @Test
    public void testToStringObject() {
        Assertions.assertEquals("", CommonUtils.toString(null));
        Assertions.assertEquals("a", CommonUtils.toString(new String("a")));
        Assertions.assertEquals("1", CommonUtils.toString(new Integer(1)));
    }

    @Test
    public void testToStringDef() {
        Assertions.assertEquals("", CommonUtils.toString(null, ""));
        Assertions.assertEquals("a", CommonUtils.toString(new String("a"), ""));
        Assertions.assertEquals("1", CommonUtils.toString(new Integer(1), ""));
    }

    @Test
    public void testToBoolean() {
        Assertions.assertFalse(CommonUtils.toBoolean(null));
        Assertions.assertFalse(CommonUtils.toBoolean("false"));
        Assertions.assertTrue(CommonUtils.toBoolean("true"));
    }

    @Test
    public void testToIntDef() {
        Assertions.assertEquals(1, CommonUtils.toInt(null, 1));
        Assertions.assertEquals(2, CommonUtils.toInt(2, 1));
        Assertions.assertEquals(2, CommonUtils.toInt("2", 1));
        Assertions.assertEquals(1, CommonUtils.toInt("a", 1));
    }

    @Test
    public void testToInt() {
        Assertions.assertEquals(1, CommonUtils.toInt(1));
    }

    @Test
    public void testIsInt() {
        Assertions.assertFalse(CommonUtils.isInt(null));
        Assertions.assertTrue(CommonUtils.isInt(1));
        Assertions.assertTrue(CommonUtils.isInt("2"));
        Assertions.assertFalse(CommonUtils.isInt("a"));
    }

    @Test
    public void testToLong() {
        Assertions.assertEquals(1, CommonUtils.toLong(1L));
    }

    @Test
    public void testToLongDef() {
        Assertions.assertEquals(1, CommonUtils.toLong(null, 1L));
        Assertions.assertEquals(2, CommonUtils.toLong(2L, 1L));
        Assertions.assertEquals(2, CommonUtils.toLong("2", 1L));
        Assertions.assertEquals(1, CommonUtils.toLong("a", 1L));
    }

    @Test
    public void testIsLong() {
        Assertions.assertFalse(CommonUtils.isLong(null));
        Assertions.assertTrue(CommonUtils.isLong(1L));
        Assertions.assertTrue(CommonUtils.isLong("2"));
        Assertions.assertFalse(CommonUtils.isLong("a"));
    }

    @Test
    public void testToDouble() {
        Assertions.assertEquals(0.0, CommonUtils.toDouble(null), 0);
        Assertions.assertEquals(0.1, CommonUtils.toDouble(0.1), 0);
        Assertions.assertEquals(0.1, CommonUtils.toDouble("0.1"), 0);
        Assertions.assertEquals(Double.NaN, CommonUtils.toDouble("a"), 0);
    }

    @Test
    public void testToDoubleDef() {
        Assertions.assertEquals(0.1, CommonUtils.toDouble(null, 0.1), 0);
        Assertions.assertEquals(0.2, CommonUtils.toDouble(0.2, 0.1), 0);
        Assertions.assertEquals(0.2, CommonUtils.toDouble("0.2", 0.1), 0);
        Assertions.assertEquals(0.1, CommonUtils.toDouble("a", 0.1), 0);
    }

    @Test
    public void testToHexString() {
        Assertions.assertEquals("", CommonUtils.toHexString(null));
        Assertions.assertEquals("", CommonUtils.toHexString(new byte[]{}));
        Assertions.assertEquals("000102", CommonUtils.toHexString(new byte[]{0, 1, 2}));
        Assertions.assertEquals("", CommonUtils.toHexString(null, 0, 0));
    }

    @Test
    public void testToBinaryString() {
        Assertions.assertEquals("1100100", CommonUtils.toBinaryString(100L, 6));
        Assertions.assertEquals("01010", CommonUtils.toBinaryString(10L, 5));
    }

    @Test
    public void testSplitWithDelimiter() {
        Assertions.assertNull(CommonUtils.splitWithDelimiter(null, ":"));
        Assertions.assertArrayEquals(new String[]{"abc", ":def"}, CommonUtils.splitWithDelimiter("abc:def", ":"));
    }

    @Test
    public void testSplitString() {
        Assertions.assertNotNull(CommonUtils.splitString("", ':'));
        List<String> result = new ArrayList<>();
        result.add("abc");
        result.add("def");
        Assertions.assertArrayEquals(result.toArray(), CommonUtils.splitString("abc:def", ':').toArray());
    }

    @Test
    public void testSplit() {
        Assertions.assertArrayEquals(new String[]{}, CommonUtils.split("", ":"));
        Assertions.assertArrayEquals(new String[]{"abc", "def"}, CommonUtils.split("abc:def", ":"));
    }

    @Test
    public void testMakeString() {
        Assertions.assertEquals("", CommonUtils.makeString(null, ':'));

        List<String> tokens = new ArrayList<>();
        tokens.add("abc");
        Assertions.assertEquals("abc", CommonUtils.makeString(tokens, ':'));

        tokens.add("def");
        Assertions.assertEquals("abc:def", CommonUtils.makeString(tokens, ':'));
    }

    @Test
    public void testTruncteString() {
        Assertions.assertEquals(null, CommonUtils.truncateString(null, 3));
        Assertions.assertEquals("abc", CommonUtils.truncateString("abc", 3));
        Assertions.assertEquals("abc", CommonUtils.truncateString("abcdef", 3));
    }

    @Test
    public void testJoinStringsArray() {
        final String[] nullArray = null;
        Assertions.assertEquals("", CommonUtils.joinStrings(":", nullArray));
        Assertions.assertEquals("abc:def", CommonUtils.joinStrings(":", new String[]{"abc", "def"}));
    }

    @Test
    public void testJoinStringsCollection() {
        final ArrayList<String> nullCol = null;
        final ArrayList<String> col = new ArrayList<String>();
        col.add("abc");
        col.add("def");
        Assertions.assertEquals("", CommonUtils.joinStrings(":", nullCol));
        Assertions.assertEquals("abc:def", CommonUtils.joinStrings(":", col));
    }

    @Test
    public void testIsEmptyTrimmed() {
        Assertions.assertTrue(CommonUtils.isEmptyTrimmed(null));
        Assertions.assertTrue(CommonUtils.isEmptyTrimmed(""));
        Assertions.assertTrue(CommonUtils.isEmptyTrimmed(" "));
        Assertions.assertFalse(CommonUtils.isEmptyTrimmed(":"));
    }

    @Test
    public void testIsBitSet() {
        Assertions.assertTrue(CommonUtils.isBitSet(1, 1));
        Assertions.assertFalse(CommonUtils.isBitSet(1, 2));
    }

    enum enumClass {
        A_B,
    }

    enum enumClassEmpty {
    }

    @Test
    public void testValueOf() {
        Assertions.assertNull(CommonUtils.valueOf(enumClass.class, null));

        Assertions.assertEquals(enumClass.A_B, CommonUtils.valueOf(enumClass.class, null, enumClass.A_B, false));
        Assertions.assertEquals(enumClass.A_B, CommonUtils.valueOf(enumClass.class, " ", enumClass.A_B, false));
        Assertions.assertEquals(enumClass.A_B, CommonUtils.valueOf(enumClass.class, "A B", enumClass.A_B, true));

        Assertions.assertEquals(enumClass.A_B, CommonUtils.valueOf(enumClass.class, "", enumClass.A_B));
        Assertions.assertEquals(enumClass.A_B, CommonUtils.valueOf(enumClass.class, "A_B", enumClass.A_B));
    }

    @Test
    public void testGetItem() {
        final ArrayList<String> collectionList = new ArrayList<>();
        collectionList.add("a");
        Assertions.assertEquals("a", CommonUtils.getItem(collectionList, 0));

        final HashSet<String> collectionSet = new LinkedHashSet<>();
        collectionSet.add("a");
        collectionSet.add("b");
        Assertions.assertEquals("b", CommonUtils.getItem(collectionSet, 1));
    }

    @Test
    public void testFromOrdinal() {
        Assertions.assertEquals(enumClass.A_B, CommonUtils.fromOrdinal(enumClass.class, 0));
        //Assertions.assertNotEquals(enumClass.A_B, CommonUtils.fromOrdinal(enumClass.class, 3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtils.fromOrdinal(enumClassEmpty.class, 3));
    }

    @Test
    public void testFilterCollection() {
        final ArrayList<Object> collection = new ArrayList<>();
        collection.add("a");
        collection.add(1);
        Assertions.assertArrayEquals(new Object[]{"a"}, CommonUtils.filterCollection(collection, String.class).toArray());
    }

    @Test
    public void testEscapeDisplayString() {
        Assertions.assertEquals("\\n\\r\\t:", CommonUtils.escapeDisplayString("\n\r\t:"));
    }

    @Test
    public void testUnescapeDisplayString() {
        Assertions.assertEquals("\t\r\n", CommonUtils.unescapeDisplayString("\\t\\r\\n"));
    }

    @Test
    public void testHashCode() {
        Assertions.assertEquals(0, CommonUtils.hashCode(null));
        Assertions.assertEquals(96354, CommonUtils.hashCode("abc"));
    }

    @Test
    public void testGetOption() {
        final HashMap<String, Boolean> options = new HashMap<>();
        options.put("A", false);
        options.put("B", false);
        options.put("C", true);

        Assertions.assertEquals("default", CommonUtils.getOption(options, "D", "default"));
        Assertions.assertEquals(true, CommonUtils.getOption(options, "C", "default"));

        Assertions.assertFalse(CommonUtils.getOption(null, "A"));

        Assertions.assertFalse(CommonUtils.getOption(options, "D", false));
        Assertions.assertTrue(CommonUtils.getOption(options, "C", false));
    }

    @Test
    public void testFixedLengthString() {
        Assertions.assertEquals("abc", CommonUtils.fixedLengthString("abc", 3));
    }

    @Test
    public void testStartsWithIgnoreCase() {
        Assertions.assertFalse(CommonUtils.startsWithIgnoreCase("", "a"));
        Assertions.assertFalse(CommonUtils.startsWithIgnoreCase("abc", ""));
        Assertions.assertTrue(CommonUtils.startsWithIgnoreCase("abc", "a"));
        Assertions.assertTrue(CommonUtils.startsWithIgnoreCase("Abc", "aB"));
    }

    @Test
    public void testNiceFormatFloat() {
        Assertions.assertEquals("1", CommonUtils.niceFormatFloat(1));
        Assertions.assertEquals("1.1", CommonUtils.niceFormatFloat(1.1f));
    }

    @Test
    public void testNiceFormatDouble() {
        Assertions.assertEquals("1", CommonUtils.niceFormatDouble(1.0));
        Assertions.assertEquals("1.1", CommonUtils.niceFormatDouble(1.1));
    }

    @Test
    public void testTrim() {
        Assertions.assertNull(CommonUtils.trim(null));
        Assertions.assertEquals("abcdef", CommonUtils.trim("abcdef "));
    }

    @Test
    public void testCompactWhiteSpaces() {
        Assertions.assertEquals("abc def", CommonUtils.compactWhiteSpaces("abc  def"));
    }

    @Test
    public void testgetSingleLineString() {
        Assertions.assertEquals("a¶bc d ", CommonUtils.getSingleLineString("a\nb\rc\td\0"));
    }

    @Test
    public void testEscapeStringForBourneShell() {
        Assertions.assertEquals("''", CommonUtils.escapeBourneShellString(""));
        Assertions.assertEquals("'string'", CommonUtils.escapeBourneShellString("string"));
        Assertions.assertEquals("'string with '\\''one single quote symbol'", CommonUtils.escapeBourneShellString("string with 'one single quote symbol"));
        Assertions.assertEquals("'string with '\\''two '\\''single quote symbols'", CommonUtils.escapeBourneShellString("string with 'two 'single quote symbols"));
        Assertions.assertEquals("'string with '\\''three '\\''single '\\''quote symbols'", CommonUtils.escapeBourneShellString("string with 'three 'single 'quote symbols"));
        Assertions.assertEquals("''\\'''", CommonUtils.escapeBourneShellString("'"));
        Assertions.assertEquals("'unit'\\'''\\''test'", CommonUtils.escapeBourneShellString("unit''test"));
        Assertions.assertEquals("'unit'\\'''\\'''\\''test'", CommonUtils.escapeBourneShellString("unit'''test"));
    }

    @Test
    public void testUnescapeStringForBourneShell() {
        Assertions.assertEquals("", CommonUtils.unescapeBourneShellString("''"));
        Assertions.assertEquals("string", CommonUtils.unescapeBourneShellString("'string'"));
        Assertions.assertEquals("string with 'one single quote symbol", CommonUtils.unescapeBourneShellString("'string with '\\''one single quote symbol'"));
        Assertions.assertEquals("string with 'two 'single quote symbols", CommonUtils.unescapeBourneShellString("'string with '\\''two '\\''single quote symbols'"));
        Assertions.assertEquals("string with 'three 'single 'quote symbols", CommonUtils.unescapeBourneShellString("'string with '\\''three '\\''single '\\''quote symbols'"));
        Assertions.assertEquals("'", CommonUtils.unescapeBourneShellString("''\\'''"));
        Assertions.assertEquals("unit''test", CommonUtils.unescapeBourneShellString("'unit'\\'''\\''test'"));
        Assertions.assertEquals("unit'''test", CommonUtils.unescapeBourneShellString("'unit'\\'''\\'''\\''test'"));
        Assertions.assertEquals("'''unit'''test'''", CommonUtils.unescapeBourneShellString("''\\'''\\'''\\''unit'\\'''\\'''\\''test'\\'''\\'''\\'''"));
    }

    @Test
    public void testGroup() {
        final List<String> values = Arrays.asList("aaa", "abb", "bbb", "bab", "ccc");
        final Map<Character, List<String>> groups = CommonUtils.group(values, x -> x.charAt(0));
        Assertions.assertEquals(Arrays.asList("aaa", "abb"), groups.get('a'));
        Assertions.assertEquals(Arrays.asList("bbb", "bab"), groups.get('b'));
        Assertions.assertEquals(Arrays.asList("ccc"), groups.get('c'));
    }

    @Test
    public void testNormalizeResourcePath() {
        var emptyString = "";
        var normalizedPath = "place";
        var normalizedTwoLevelPath = "some/place";
        var pathWithBackslashSuffix = "/some/place";
        var pathWithMultipleBackslashSuffix = "//some/place";
        var pathWithWrongBackslash = "some\\place";
        var mixedCasesPath = "//some\\place";

        Assertions.assertEquals(emptyString, CommonUtils.normalizeResourcePath(emptyString));
        Assertions.assertEquals(normalizedPath, CommonUtils.normalizeResourcePath(normalizedPath));
        Assertions.assertEquals(normalizedTwoLevelPath, CommonUtils.normalizeResourcePath(normalizedTwoLevelPath));
        Assertions.assertEquals(normalizedTwoLevelPath, CommonUtils.normalizeResourcePath(pathWithBackslashSuffix));
        Assertions.assertEquals(normalizedTwoLevelPath, CommonUtils.normalizeResourcePath(pathWithMultipleBackslashSuffix));
        Assertions.assertEquals(normalizedTwoLevelPath, CommonUtils.normalizeResourcePath(pathWithWrongBackslash));
        Assertions.assertEquals(normalizedTwoLevelPath, CommonUtils.normalizeResourcePath(mixedCasesPath));
    }

    @Test
    public void testReplaceLast() {
        Assertions.assertEquals(CommonUtils.replaceLast("foobarfoobar", "foo", "bar"), "foobarbarbar");
        Assertions.assertEquals(CommonUtils.replaceLast("foobarbarbar", "foo", "bar"), "barbarbarbar");
        Assertions.assertEquals(CommonUtils.replaceLast("foo", "bar", "foo"), "foo");
        Assertions.assertEquals(CommonUtils.replaceLast("", "bar", "foo"), "");
    }
}
