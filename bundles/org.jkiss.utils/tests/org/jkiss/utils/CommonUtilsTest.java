package org.jkiss.utils;

import java.util.ArrayList;
import java.util.List;
import org.jkiss.utils.CommonUtils;
import org.junit.runner.RunWith;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.AdditionalMatchers.or;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class CommonUtilsTest {

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
    Assert.assertNull(CommonUtils.escapeFileName(null));
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
    Assert.assertNull(CommonUtils.toCamelCase(""));
    Assert.assertEquals("Abcd", CommonUtils.toCamelCase("abcd"));
    Assert.assertEquals("Ab|Cd", CommonUtils.toCamelCase("ab|cd"));
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

  @PrepareForTest({ CommonUtils.class, System.class })
  @Test
  public void testGetLineSeparator() {
    PowerMockito.mockStatic(System.class);
    PowerMockito.when(System.getProperty(or(isA(String.class), isNull(String.class)))).thenReturn("\r\n");
    Assert.assertEquals("\r\n", CommonUtils.getLineSeparator());

    PowerMockito.when(System.getProperty(or(isA(String.class), isNull(String.class)))).thenReturn(null);
    Assert.assertEquals("\n", CommonUtils.getLineSeparator());
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
  public void testToHexString() {
    Assert.assertEquals("", CommonUtils.toHexString(null));
    Assert.assertEquals("", CommonUtils.toHexString(new byte[] {}));
    Assert.assertEquals("000102", CommonUtils.toHexString(new byte[] { 0, 1, 2 }));
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
    Assert.assertArrayEquals(new String[] { "abc", ":def" }, CommonUtils.splitWithDelimiter("abc:def", ":"));
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
    Assert.assertArrayEquals(new String[] {}, CommonUtils.split("", ":"));
    Assert.assertArrayEquals(new String[] { "abc", "def" }, CommonUtils.split("abc:def", ":"));
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
    Assert.assertEquals("abc:def", CommonUtils.joinStrings(":", new String[] { "abc", "def" }));
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
  public void testEscapeDisplayString() {
    Assert.assertEquals("\\n\\r\\t:", CommonUtils.escapeDisplayString("\n\r\t:"));
  }

  @Test
  public void testUnescapeDisplayString() {
    Assert.assertEquals("\t\r\n", CommonUtils.unescapeDisplayString("\\t\\r\\n"));
  }

  @Test
  public void testFixedLengthString() {
    Assert.assertEquals("abc", CommonUtils.fixedLengthString("abc", 3));
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
}
