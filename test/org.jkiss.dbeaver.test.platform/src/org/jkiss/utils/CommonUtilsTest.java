/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class CommonUtilsTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testIsJavaIdentifier() {
        Assert.assertEquals(false, CommonUtils.isJavaIdentifier(""));
        Assert.assertEquals(false, CommonUtils.isJavaIdentifier("|"));
        Assert.assertEquals(false, CommonUtils.isJavaIdentifier("a-b"));
        Assert.assertEquals(true, CommonUtils.isJavaIdentifier("aa"));
    }

    @Test
    public void testEscapeJavaString() throws Exception {
        Assert.assertEquals("", CommonUtils.escapeJavaString(""));
        Assert.assertEquals("\\\"", CommonUtils.escapeJavaString("\""));
        Assert.assertEquals("\\n", CommonUtils.escapeJavaString("\n"));
        Assert.assertEquals("\\r", CommonUtils.escapeJavaString("\r"));
        Assert.assertEquals("\\t", CommonUtils.escapeJavaString("\t"));
        Assert.assertEquals("a", CommonUtils.escapeJavaString("a"));
    }

    @Test
    public void testEscapeIdentifier() {
        Assert.assertNull(CommonUtils.escapeIdentifier(null));
        Assert.assertEquals("", CommonUtils.escapeIdentifier(""));
        Assert.assertEquals("_", CommonUtils.escapeIdentifier("|"));
        Assert.assertEquals("_", CommonUtils.escapeIdentifier("||"));
        Assert.assertEquals("a_", CommonUtils.escapeIdentifier("a|"));
    }

    @Test
    public void testEscapeFileName() {
        Assert.assertEquals("", CommonUtils.escapeFileName(null));
        Assert.assertEquals("_", CommonUtils.escapeFileName("\\"));
        Assert.assertEquals("_", CommonUtils.escapeFileName("\u0013"));
        Assert.assertEquals("_", CommonUtils.escapeFileName("/"));
        Assert.assertEquals("_", CommonUtils.escapeFileName("<"));
        Assert.assertEquals("_", CommonUtils.escapeFileName(">"));
        Assert.assertEquals("_", CommonUtils.escapeFileName("|"));
        Assert.assertEquals("_", CommonUtils.escapeFileName("\""));
        Assert.assertEquals("_", CommonUtils.escapeFileName(":"));
        Assert.assertEquals("_", CommonUtils.escapeFileName("*"));
        Assert.assertEquals("_", CommonUtils.escapeFileName("?"));
        Assert.assertEquals("a", CommonUtils.escapeFileName("a"));
    }

    @Test
    public void testMakeDirectoryName() {
        Assert.assertEquals("a/", CommonUtils.makeDirectoryName("a"));
        Assert.assertEquals("a/", CommonUtils.makeDirectoryName("a/"));
    }

    @Test
    public void testRemoveTrailingSlash() {
        Assert.assertEquals("a", CommonUtils.removeTrailingSlash("a/"));
        Assert.assertEquals("a", CommonUtils.removeTrailingSlash("a\\"));
        Assert.assertEquals("a", CommonUtils.removeTrailingSlash("a"));
    }

    @Test
    public void testCapitalizeWord() {
        Assert.assertEquals("", CommonUtils.capitalizeWord(""));
        Assert.assertEquals("Abc", CommonUtils.capitalizeWord("Abc"));
        Assert.assertEquals("Abc", CommonUtils.capitalizeWord("abc"));
    }

    @Test
    public void testToCamelCase() {
        Assert.assertNull(CommonUtils.toCamelCase(null));
        Assert.assertEquals(CommonUtils.toCamelCase(""), "");
        Assert.assertEquals("Abcd", CommonUtils.toCamelCase("abcd"));
        Assert.assertEquals("Ab|Cd", CommonUtils.toCamelCase("ab|cd"));
    }

    @Test
    public void testNotNull() {
        final Object value = "value";
        final Object defaultValue = "defaultValue";
        Assert.assertEquals(value, CommonUtils.notNull(value, defaultValue));
        Assert.assertEquals(defaultValue, CommonUtils.notNull(null, defaultValue));
    }

    @Test
    public void testIsEmptyCharSequence() {
        final CharSequence nullValue = null;
        final CharSequence emptyValue = "";
        final CharSequence value = "abc";
        Assert.assertTrue(CommonUtils.isEmpty(nullValue));
        Assert.assertTrue(CommonUtils.isEmpty(emptyValue));
        Assert.assertFalse(CommonUtils.isEmpty(value));
    }

    @Test
    public void testIsEmptyString() {
        final String nullValue = null;
        final String emptyValue = "";
        final String value = "abc";
        Assert.assertTrue(CommonUtils.isEmpty(nullValue));
        Assert.assertTrue(CommonUtils.isEmpty(emptyValue));
        Assert.assertFalse(CommonUtils.isEmpty(value));
    }

    @Test
    public void testIsNotEmpty() {
        final String nullValue = null;
        final String emptyValue = "";
        final String value = "abc";
        Assert.assertFalse(CommonUtils.isNotEmpty(nullValue));
        Assert.assertFalse(CommonUtils.isNotEmpty(emptyValue));
        Assert.assertTrue(CommonUtils.isNotEmpty(value));
    }

    @Test
    public void testIsEmptyCollection() {
        final ArrayList<Character> nullValue = null;
        final ArrayList<Character> emptyValue = new ArrayList<>();
        final ArrayList<Character> value = new ArrayList<>();
        value.add('a');
        Assert.assertTrue(CommonUtils.isEmpty(nullValue));
        Assert.assertTrue(CommonUtils.isEmpty(emptyValue));
        Assert.assertFalse(CommonUtils.isEmpty(value));
    }

    @Test
    public void testIsEmptyMap() {
        final HashMap<Integer, Character> nullValue = null;
        final HashMap<Integer, Character> emptyValue = new HashMap<>();
        final HashMap<Integer, Character> value = new HashMap<>();
        value.put(0, 'a');
        Assert.assertTrue(CommonUtils.isEmpty(nullValue));
        Assert.assertTrue(CommonUtils.isEmpty(emptyValue));
        Assert.assertFalse(CommonUtils.isEmpty(value));
    }

    @Test
    public void testSafeCollection() {
        final ArrayList<Character> theList = new ArrayList<>();
        Assert.assertEquals(theList, CommonUtils.safeCollection(null));
        Assert.assertEquals(theList, CommonUtils.safeCollection(theList));
    }

    @Test
    public void testSafeList() {
        final ArrayList<Character> theList = new ArrayList<>();
        Assert.assertEquals(theList, CommonUtils.safeList(null));
        Assert.assertEquals(theList, CommonUtils.safeList(theList));
    }

    @Test
    public void testCopyList() {
        final ArrayList<Integer> theList = new ArrayList<>();
        Assert.assertEquals(theList, CommonUtils.copyList(null));

        theList.add(0);
        Assert.assertEquals(theList, CommonUtils.copyList(theList));
    }

    @Test
    public void testNotEmpty() {
        Assert.assertEquals("", CommonUtils.notEmpty(null));
        Assert.assertEquals("abc", CommonUtils.notEmpty("abc"));
    }

    @Test
    public void testNullIfEmpty() {
        Assert.assertNull(CommonUtils.nullIfEmpty(null));
        Assert.assertNull(CommonUtils.nullIfEmpty(""));
        Assert.assertEquals("abc", CommonUtils.nullIfEmpty("abc"));
    }

    @Test
    public void testIsTrue() {
        Assert.assertTrue(CommonUtils.isTrue(true));
        Assert.assertFalse(CommonUtils.isTrue(false));
        Assert.assertFalse(CommonUtils.isTrue(null));
    }

    @Test
    public void testGetBooleanString() {
        Assert.assertTrue(CommonUtils.getBoolean("true"));
        Assert.assertFalse(CommonUtils.getBoolean("false"));
        Assert.assertFalse(CommonUtils.getBoolean("null"));
    }

    @Test
    public void testGetBooleanStringDefault() {
        Assert.assertTrue(CommonUtils.getBoolean("", true));
        Assert.assertFalse(CommonUtils.getBoolean("false", true));
    }

    @Test
    public void testGetBooleanObjectDefault() {
        final Object nullValue = null;
        final Object value = 0;
        Assert.assertTrue(CommonUtils.getBoolean(nullValue, true));
        Assert.assertTrue(CommonUtils.getBoolean(true, false));
        Assert.assertFalse(CommonUtils.getBoolean(value, true));
    }

/*
  @PrepareForTest({ CommonUtils.class, System.class })
  @Test
  public void testGetLineSeparator() {
    PowerMockito.mockStatic(System.class);
    PowerMockito.when(System.getProperty(or(isA(String.class), isNull(String.class)))).thenReturn("\r\n");
    Assert.assertEquals("\r\n", CommonUtils.getLineSeparator());

    PowerMockito.when(System.getProperty(or(isA(String.class), isNull(String.class)))).thenReturn(null);
    Assert.assertEquals("\n", CommonUtils.getLineSeparator());
  }
*/

    @Test
    public void testGetRootCause() {
        Assert.assertEquals("def", CommonUtils.getRootCause(new Throwable("abc", new Throwable("def"))).getMessage());
        Assert.assertEquals("abc", CommonUtils.getRootCause(new Throwable("abc")).getMessage());
        Assert.assertNull(CommonUtils.getRootCause(new InvocationTargetException(null)).getMessage());
    }

    @Test
    public void testEqualOrEmptyStrings() {
        Assert.assertTrue(CommonUtils.equalOrEmptyStrings("abc", "abc"));
        Assert.assertFalse(CommonUtils.equalOrEmptyStrings("abc", null));
        Assert.assertFalse(CommonUtils.equalOrEmptyStrings(null, "def"));
        Assert.assertFalse(CommonUtils.equalOrEmptyStrings("abc", "def"));
        Assert.assertTrue(CommonUtils.equalOrEmptyStrings("", ""));
    }

    @Test
    public void testToStringObject() {
        Assert.assertEquals("", CommonUtils.toString(null));
        Assert.assertEquals("a", CommonUtils.toString(new String("a")));
        Assert.assertEquals("1", CommonUtils.toString(new Integer(1)));
    }

    @Test
    public void testToStringDef() {
        Assert.assertEquals("", CommonUtils.toString(null, ""));
        Assert.assertEquals("a", CommonUtils.toString(new String("a"), ""));
        Assert.assertEquals("1", CommonUtils.toString(new Integer(1), ""));
    }

    @Test
    public void testToBoolean() {
        Assert.assertFalse(CommonUtils.toBoolean(null));
        Assert.assertFalse(CommonUtils.toBoolean("false"));
        Assert.assertTrue(CommonUtils.toBoolean("true"));
    }

    @Test
    public void testToIntDef() {
        Assert.assertEquals(1, CommonUtils.toInt(null, 1));
        Assert.assertEquals(2, CommonUtils.toInt(2, 1));
        Assert.assertEquals(2, CommonUtils.toInt("2", 1));
        Assert.assertEquals(1, CommonUtils.toInt("a", 1));
    }

    @Test
    public void testToInt() {
        Assert.assertEquals(1, CommonUtils.toInt(1));
    }

    @Test
    public void testIsInt() {
        Assert.assertFalse(CommonUtils.isInt(null));
        Assert.assertTrue(CommonUtils.isInt(1));
        Assert.assertTrue(CommonUtils.isInt("2"));
        Assert.assertFalse(CommonUtils.isInt("a"));
    }

    @Test
    public void testToLong() {
        Assert.assertEquals(1, CommonUtils.toLong(1L));
    }

    @Test
    public void testToLongDef() {
        Assert.assertEquals(1, CommonUtils.toLong(null, 1L));
        Assert.assertEquals(2, CommonUtils.toLong(2L, 1L));
        Assert.assertEquals(2, CommonUtils.toLong("2", 1L));
        Assert.assertEquals(1, CommonUtils.toLong("a", 1L));
    }

    @Test
    public void testIsLong() {
        Assert.assertFalse(CommonUtils.isLong(null));
        Assert.assertTrue(CommonUtils.isLong(1L));
        Assert.assertTrue(CommonUtils.isLong("2"));
        Assert.assertFalse(CommonUtils.isLong("a"));
    }

    @Test
    public void testToDouble() {
        Assert.assertEquals(0.0, CommonUtils.toDouble(null), 0);
        Assert.assertEquals(0.1, CommonUtils.toDouble(0.1), 0);
        Assert.assertEquals(0.1, CommonUtils.toDouble("0.1"), 0);
        Assert.assertEquals(Double.NaN, CommonUtils.toDouble("a"), 0);
    }

    @Test
    public void testToDoubleDef() {
        Assert.assertEquals(0.1, CommonUtils.toDouble(null, 0.1), 0);
        Assert.assertEquals(0.2, CommonUtils.toDouble(0.2, 0.1), 0);
        Assert.assertEquals(0.2, CommonUtils.toDouble("0.2", 0.1), 0);
        Assert.assertEquals(0.1, CommonUtils.toDouble("a", 0.1), 0);
    }

    @Test
    public void testToHexString() {
        Assert.assertEquals("", CommonUtils.toHexString(null));
        Assert.assertEquals("", CommonUtils.toHexString(new byte[]{}));
        Assert.assertEquals("000102", CommonUtils.toHexString(new byte[]{0, 1, 2}));
        Assert.assertEquals("", CommonUtils.toHexString(null, 0, 0));
    }

    @Test
    public void testToBinaryString() {
        Assert.assertEquals("1100100", CommonUtils.toBinaryString(100L, 6));
        Assert.assertEquals("01010", CommonUtils.toBinaryString(10L, 5));
    }

    @Test
    public void testSplitWithDelimiter() {
        Assert.assertNull(CommonUtils.splitWithDelimiter(null, ":"));
        Assert.assertArrayEquals(new String[]{"abc", ":def"}, CommonUtils.splitWithDelimiter("abc:def", ":"));
    }

    @Test
    public void testSplitString() {
        Assert.assertNotNull(CommonUtils.splitString("", ':'));
        List<String> result = new ArrayList<>();
        result.add("abc");
        result.add("def");
        Assert.assertArrayEquals(result.toArray(), CommonUtils.splitString("abc:def", ':').toArray());
    }

    @Test
    public void testSplit() {
        Assert.assertArrayEquals(new String[]{}, CommonUtils.split("", ":"));
        Assert.assertArrayEquals(new String[]{"abc", "def"}, CommonUtils.split("abc:def", ":"));
    }

    @Test
    public void testMakeString() {
        Assert.assertEquals("", CommonUtils.makeString(null, ':'));

        List<String> tokens = new ArrayList<>();
        tokens.add("abc");
        Assert.assertEquals("abc", CommonUtils.makeString(tokens, ':'));

        tokens.add("def");
        Assert.assertEquals("abc:def", CommonUtils.makeString(tokens, ':'));
    }

    @Test
    public void testTruncteString() {
        Assert.assertEquals(null, CommonUtils.truncateString(null, 3));
        Assert.assertEquals("abc", CommonUtils.truncateString("abc", 3));
        Assert.assertEquals("abc", CommonUtils.truncateString("abcdef", 3));
    }

    @Test
    public void testJoinStringsArray() {
        final String[] nullArray = null;
        Assert.assertEquals("", CommonUtils.joinStrings(":", nullArray));
        Assert.assertEquals("abc:def", CommonUtils.joinStrings(":", new String[]{"abc", "def"}));
    }

    @Test
    public void testJoinStringsCollection() {
        final ArrayList<String> nullCol = null;
        final ArrayList<String> col = new ArrayList<String>();
        col.add("abc");
        col.add("def");
        Assert.assertEquals("", CommonUtils.joinStrings(":", nullCol));
        Assert.assertEquals("abc:def", CommonUtils.joinStrings(":", col));
    }

    @Test
    public void testIsEmptyTrimmed() {
        Assert.assertTrue(CommonUtils.isEmptyTrimmed(null));
        Assert.assertTrue(CommonUtils.isEmptyTrimmed(""));
        Assert.assertTrue(CommonUtils.isEmptyTrimmed(" "));
        Assert.assertFalse(CommonUtils.isEmptyTrimmed(":"));
    }

    @Test
    public void testIsBitSet() {
        Assert.assertTrue(CommonUtils.isBitSet(1, 1));
        Assert.assertFalse(CommonUtils.isBitSet(1, 2));
    }

    enum enumClass {
        A_B,
    }

    enum enumClassEmpty {
    }

    @Test
    public void testValueOf() {
        Assert.assertNull(CommonUtils.valueOf(enumClass.class, null));

        Assert.assertEquals(enumClass.A_B, CommonUtils.valueOf(enumClass.class, null, enumClass.A_B, false));
        Assert.assertEquals(enumClass.A_B, CommonUtils.valueOf(enumClass.class, " ", enumClass.A_B, false));
        Assert.assertEquals(enumClass.A_B, CommonUtils.valueOf(enumClass.class, "A B", enumClass.A_B, true));

        Assert.assertEquals(enumClass.A_B, CommonUtils.valueOf(enumClass.class, "", enumClass.A_B));
        Assert.assertEquals(enumClass.A_B, CommonUtils.valueOf(enumClass.class, "A_B", enumClass.A_B));
    }

    @Test
    public void testGetItem() {
        final ArrayList<String> collectionList = new ArrayList<>();
        collectionList.add("a");
        Assert.assertEquals("a", CommonUtils.getItem(collectionList, 0));

        final HashSet<String> collectionSet = new LinkedHashSet<>();
        collectionSet.add("a");
        collectionSet.add("b");
        Assert.assertEquals("b", CommonUtils.getItem(collectionSet, 1));
    }

    @Test
    public void testFromOrdinal() {
        Assert.assertEquals(enumClass.A_B, CommonUtils.fromOrdinal(enumClass.class, 0));
        //Assert.assertNotEquals(enumClass.A_B, CommonUtils.fromOrdinal(enumClass.class, 3));
        thrown.expect(IllegalArgumentException.class);
        CommonUtils.fromOrdinal(enumClassEmpty.class, 3);
    }

    @Test
    public void testFilterCollection() {
        final ArrayList<Object> collection = new ArrayList<>();
        collection.add("a");
        collection.add(1);
        Assert.assertEquals(new String[]{"a"}, CommonUtils.filterCollection(collection, String.class).toArray());
    }

    @Test
    public void testEscapeDisplayString() {
        Assert.assertEquals("\\n\\r\\t:", CommonUtils.escapeDisplayString("\n\r\t:"));
    }

    @Test
    public void testUnescapeDisplayString() {
        Assert.assertEquals("\t\r\n", CommonUtils.unescapeDisplayString("\\t\\r\\n"));
    }

    @Test
    public void testHashCode() {
        Assert.assertEquals(0, CommonUtils.hashCode(null));
        Assert.assertEquals(96354, CommonUtils.hashCode("abc"));
    }

    @Test
    public void testGetOption() {
        final HashMap<String, Boolean> options = new HashMap<>();
        options.put("A", false);
        options.put("B", false);
        options.put("C", true);

        Assert.assertEquals("default", CommonUtils.getOption(options, "D", "default"));
        Assert.assertEquals(true, CommonUtils.getOption(options, "C", "default"));

        Assert.assertFalse(CommonUtils.getOption(null, "A"));

        Assert.assertFalse(CommonUtils.getOption(options, "D", false));
        Assert.assertTrue(CommonUtils.getOption(options, "C", false));
    }

    @Test
    public void testFixedLengthString() {
        Assert.assertEquals("abc", CommonUtils.fixedLengthString("abc", 3));
    }

    @Test
    public void testStartsWithIgnoreCase() {
        Assert.assertFalse(CommonUtils.startsWithIgnoreCase("", "a"));
        Assert.assertFalse(CommonUtils.startsWithIgnoreCase("abc", ""));
        Assert.assertTrue(CommonUtils.startsWithIgnoreCase("abc", "a"));
        Assert.assertTrue(CommonUtils.startsWithIgnoreCase("Abc", "aB"));
    }

    @Test
    public void testNiceFormatFloat() {
        Assert.assertEquals("1", CommonUtils.niceFormatFloat(1));
        Assert.assertEquals("1.1", CommonUtils.niceFormatFloat(1.1f));
    }

    @Test
    public void testNiceFormatDouble() {
        Assert.assertEquals("1", CommonUtils.niceFormatDouble(1.0));
        Assert.assertEquals("1.1", CommonUtils.niceFormatDouble(1.1));
    }

    @Test
    public void testTrim() {
        Assert.assertNull(CommonUtils.trim(null));
        Assert.assertEquals("abcdef", CommonUtils.trim("abcdef "));
    }

    @Test
    public void testCompactWhiteSpaces() {
        Assert.assertEquals("abc def", CommonUtils.compactWhiteSpaces("abc  def"));
    }

    @Test
    public void testgetSingleLineString() {
        Assert.assertEquals("a¶bc d ", CommonUtils.getSingleLineString("a\nb\rc\td\0"));
    }

    @Test
    public void testEscapeStringForBourneShell() {
        Assert.assertEquals("''", CommonUtils.escapeBourneShellString(""));
        Assert.assertEquals("'string'", CommonUtils.escapeBourneShellString("string"));
        Assert.assertEquals("'string with '\\''one single quote symbol'", CommonUtils.escapeBourneShellString("string with 'one single quote symbol"));
        Assert.assertEquals("'string with '\\''two '\\''single quote symbols'", CommonUtils.escapeBourneShellString("string with 'two 'single quote symbols"));
        Assert.assertEquals("'string with '\\''three '\\''single '\\''quote symbols'", CommonUtils.escapeBourneShellString("string with 'three 'single 'quote symbols"));
        Assert.assertEquals("''\\'''", CommonUtils.escapeBourneShellString("'"));
        Assert.assertEquals("'unit'\\'''\\''test'", CommonUtils.escapeBourneShellString("unit''test"));
        Assert.assertEquals("'unit'\\'''\\'''\\''test'", CommonUtils.escapeBourneShellString("unit'''test"));
    }

    @Test
    public void testUnescapeStringForBourneShell() {
        Assert.assertEquals("", CommonUtils.unescapeBourneShellString("''"));
        Assert.assertEquals("string", CommonUtils.unescapeBourneShellString("'string'"));
        Assert.assertEquals("string with 'one single quote symbol", CommonUtils.unescapeBourneShellString("'string with '\\''one single quote symbol'"));
        Assert.assertEquals("string with 'two 'single quote symbols", CommonUtils.unescapeBourneShellString("'string with '\\''two '\\''single quote symbols'"));
        Assert.assertEquals("string with 'three 'single 'quote symbols", CommonUtils.unescapeBourneShellString("'string with '\\''three '\\''single '\\''quote symbols'"));
        Assert.assertEquals("'", CommonUtils.unescapeBourneShellString("''\\'''"));
        Assert.assertEquals("unit''test", CommonUtils.unescapeBourneShellString("'unit'\\'''\\''test'"));
        Assert.assertEquals("unit'''test", CommonUtils.unescapeBourneShellString("'unit'\\'''\\'''\\''test'"));
        Assert.assertEquals("'''unit'''test'''", CommonUtils.unescapeBourneShellString("''\\'''\\'''\\''unit'\\'''\\'''\\''test'\\'''\\'''\\'''"));
    }

    @Test
    public void testGroup() {
        final List<String> values = Arrays.asList("aaa", "abb", "bbb", "bab", "ccc");
        final Map<Character, List<String>> groups = CommonUtils.group(values, x -> x.charAt(0));
        Assert.assertEquals(Arrays.asList("aaa", "abb"), groups.get('a'));
        Assert.assertEquals(Arrays.asList("bbb", "bab"), groups.get('b'));
        Assert.assertEquals(Arrays.asList("ccc"), groups.get('c'));
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

        Assert.assertEquals(emptyString, CommonUtils.normalizeResourcePath(emptyString));
        Assert.assertEquals(normalizedPath, CommonUtils.normalizeResourcePath(normalizedPath));
        Assert.assertEquals(normalizedTwoLevelPath, CommonUtils.normalizeResourcePath(normalizedTwoLevelPath));
        Assert.assertEquals(normalizedTwoLevelPath, CommonUtils.normalizeResourcePath(pathWithBackslashSuffix));
        Assert.assertEquals(normalizedTwoLevelPath, CommonUtils.normalizeResourcePath(pathWithMultipleBackslashSuffix));
        Assert.assertEquals(normalizedTwoLevelPath, CommonUtils.normalizeResourcePath(pathWithWrongBackslash));
        Assert.assertEquals(normalizedTwoLevelPath, CommonUtils.normalizeResourcePath(mixedCasesPath));
    }

    @Test
    public void testReplaceLast() {
        Assert.assertEquals(CommonUtils.replaceLast("foobarfoobar", "foo", "bar"), "foobarbarbar");
        Assert.assertEquals(CommonUtils.replaceLast("foobarbarbar", "foo", "bar"), "barbarbarbar");
        Assert.assertEquals(CommonUtils.replaceLast("foo", "bar", "foo"), "foo");
        Assert.assertEquals(CommonUtils.replaceLast("", "bar", "foo"), "");
    }
}
