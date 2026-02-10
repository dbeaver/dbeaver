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
package org.jkiss.utils.time;

import org.junit.Assert;
import org.junit.Test;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

public class ExtendedDateFormatTest {

    @Test
    public void testFormatTimestampWithNanoseconds() {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.fffffffff");
        Timestamp ts = new Timestamp(1000000L);
        ts.setNanos(123456789);

        String result = format.format(ts);
        Assert.assertTrue(result.contains("123456789"));
    }

    @Test
    public void testFormatTimestampWithShortNanos() {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.fff");
        Timestamp ts = new Timestamp(1000000L);
        ts.setNanos(123456789);

        String result = format.format(ts);
        Assert.assertTrue(result.contains(".123"));
    }

    @Test
    public void testFormatTimestampWithOptionalNanos() {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss[.fff]");
        Timestamp ts = new Timestamp(1000000L);
        ts.setNanos(123456789);

        String result = format.format(ts);
        Assert.assertTrue(result.contains(".123"));
    }

    @Test
    public void testFormatTimestampWithOptionalNanosZero() {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss[.fff]");
        Timestamp ts = new Timestamp(1000000L);
        ts.setNanos(0);

        String result = format.format(ts);
        Assert.assertFalse(result.contains("."));
    }

    @Test
    public void testFormatRegularDateWithNanos() {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.fff");
        Date date = new Date(1000000L);

        String result = format.format(date);
        Assert.assertTrue(result.contains(".000"));
    }

    @Test
    public void testParseTimestampWithNanoseconds() throws ParseException {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.fffffffff");
        String dateStr = "1970-01-01 03:46:40.123456789";

        Date result = format.parse(dateStr);
        Assert.assertTrue(result instanceof Timestamp);
        Timestamp ts = (Timestamp) result;
        Assert.assertEquals(123456789, ts.getNanos());
    }

    @Test
    public void testParseTimestampWithShortNanos() throws ParseException {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.fff");
        String dateStr = "1970-01-01 03:46:40.123";

        Date result = format.parse(dateStr);
        Assert.assertTrue(result instanceof Timestamp);
        Timestamp ts = (Timestamp) result;
        Assert.assertEquals(123000000, ts.getNanos());
    }

    @Test
    public void testParseTimestampWithZeroNanos() throws ParseException {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.fff");
        String dateStr = "1970-01-01 03:46:40.000";

        Date result = format.parse(dateStr);
        Assert.assertNotNull(result);
    }

    @Test
    public void testParseWithoutNanos() throws ParseException {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr = "1970-01-01 03:46:40";

        Date result = format.parse(dateStr);
        Assert.assertNotNull(result);
        Assert.assertFalse(result instanceof Timestamp);
    }

    @Test
    public void testFormatWithSSS() {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Timestamp ts = new Timestamp(1000000L);
        ts.setNanos(500000000);

        String result = format.format(ts);
        Assert.assertTrue(result.contains(".500"));
    }

    @Test
    public void testParseWithSSS() throws ParseException {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String dateStr = "1970-01-01 03:46:40.500";

        Date result = format.parse(dateStr);
        Assert.assertTrue(result instanceof Timestamp);
        Timestamp ts = (Timestamp) result;
        Assert.assertEquals(500000000, ts.getNanos());
    }

    @Test
    public void testFormatWithPrefixAndPostfix() {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss[.fff000]");
        Timestamp ts = new Timestamp(1000000L);
        ts.setNanos(123456789);

        String result = format.format(ts);
        Assert.assertTrue(result.contains(".123000"));
    }

    @Test
    public void testFormatWithLeadingZeros() {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.fffffffff");
        Timestamp ts = new Timestamp(1000000L);
        ts.setNanos(123);

        String result = format.format(ts);
        Assert.assertTrue(result.contains(".000000123"));
    }

    @Test
    public void testFormatTruncatesNanos() {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.ff");
        Timestamp ts = new Timestamp(1000000L);
        ts.setNanos(987654321);

        String result = format.format(ts);
        Assert.assertTrue(result.contains(".98"));
    }

    @Test
    public void testConstructorWithLocale() {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.fff", Locale.US);
        Timestamp ts = new Timestamp(1000000L);
        ts.setNanos(123456789);

        String result = format.format(ts);
        Assert.assertTrue(result.contains(".123"));
    }

    @Test
    public void testParseInvalidNanoCharacter() throws ParseException {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.fff");
        String dateStr = "1970-01-01 03:46:40.ABC";

        Date result = format.parse(dateStr);
        Assert.assertNull(result);
    }

    @Test
    public void testFormatWithQuotedText() {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd 'T' HH:mm:ss.fff");
        Timestamp ts = new Timestamp(1000000L);
        ts.setNanos(123456789);

        String result = format.format(ts);
        Assert.assertTrue(result.contains("T"));
        Assert.assertTrue(result.contains(".123"));
    }

    @Test
    public void testParseWithQuotedText() throws ParseException {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd 'T' HH:mm:ss.fff");
        String dateStr = "1970-01-01 T 03:46:40.123";

        Date result = format.parse(dateStr);
        Assert.assertTrue(result instanceof Timestamp);
        Timestamp ts = (Timestamp) result;
        Assert.assertEquals(123000000, ts.getNanos());
    }

    @Test
    public void testFormatWithSixDigitNanos() {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.ffffff");
        Timestamp ts = new Timestamp(1000000L);
        ts.setNanos(123456789);

        String result = format.format(ts);
        Assert.assertTrue(result.contains(".123456"));
    }

    @Test
    public void testParseWithSixDigitNanos() throws ParseException {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.ffffff");
        String dateStr = "1970-01-01 03:46:40.123456";

        Date result = format.parse(dateStr);
        Assert.assertTrue(result instanceof Timestamp);
        Timestamp ts = (Timestamp) result;
        Assert.assertEquals(123456000, ts.getNanos());
    }

    @Test
    public void testRoundTripFormatAndParse() throws ParseException {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.fffffffff");
        Timestamp original = new Timestamp(1234567890L);
        original.setNanos(123456789);

        String formatted = format.format(original);
        Date parsed = format.parse(formatted);

        Assert.assertTrue(parsed instanceof Timestamp);
        Timestamp result = (Timestamp) parsed;
        Assert.assertEquals(original.getNanos(), result.getNanos());
    }

    @Test
    public void testParsePartialNanos() throws ParseException {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.fffffffff");
        String dateStr = "1970-01-01 03:46:40.12";

        Date result = format.parse(dateStr);
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof Timestamp);
        Timestamp ts = (Timestamp) result;
        Assert.assertEquals(120000000, ts.getNanos());
    }

    @Test
    public void testFormatWithNonOptionalNanosAndZeroValue() {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.fff");
        Timestamp ts = new Timestamp(1000000L);
        ts.setNanos(0);

        String result = format.format(ts);
        Assert.assertTrue(result.contains(".000"));
    }

    @Test
    public void testFormatRegularDateWithNanosPattern() {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.fffffffff");
        Date date = new Date(1234567890L);

        String result = format.format(date);
        Assert.assertTrue(result.contains(".890000000"));
    }

    @Test
    public void testFormatRegularDateWithOptionalNanos() {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss[.fff]");
        Date date = new Date(1234567890L);

        String result = format.format(date);
        Assert.assertTrue(result.contains(".890"));
    }

    @Test
    public void testFormatRegularDateWithOptionalNanosZeroMillis() {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss[.fff]");
        Date date = new Date(1000000L);

        String result = format.format(date);
        Assert.assertFalse(result.contains("."));
    }


    @Test
    public void testFormatTimestampWithMilliseconds() {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Timestamp ts = new Timestamp(1000123L);
        ts.setNanos(123456789);

        String result = format.format(ts);
        Assert.assertTrue(result.contains(".123"));
    }

    @Test
    public void testParseTimestampWithMilliseconds() throws ParseException {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String dateStr = "1970-01-01 03:46:40.999";

        Date result = format.parse(dateStr);
        Assert.assertTrue(result instanceof Timestamp);
        Timestamp ts = (Timestamp) result;
        Assert.assertEquals(999000000, ts.getNanos());
    }

    @Test
    public void testFormatTimestampWithZeroMilliseconds() {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Timestamp ts = new Timestamp(1000000L);
        ts.setNanos(0);

        String result = format.format(ts);
        Assert.assertTrue(result.contains(".000"));
    }

    @Test
    public void testFormatRegularDateWithMilliseconds() {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date date = new Date(1000500L);

        String result = format.format(date);
        Assert.assertTrue(result.contains(".500"));
    }

    @Test
    public void testParseMillisecondsWithLeadingZero() throws ParseException {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String dateStr = "1970-01-01 03:46:40.001";

        Date result = format.parse(dateStr);
        Assert.assertTrue(result instanceof Timestamp);
        Timestamp ts = (Timestamp) result;
        Assert.assertEquals(1000000, ts.getNanos());
    }

    @Test
    public void testFormatMillisecondsWithOptionalPattern() {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss[.SSS]");
        Timestamp ts = new Timestamp(1000000L);
        ts.setNanos(456000000);

        String result = format.format(ts);
        Assert.assertTrue(result.contains(".456"));
    }

    @Test
    public void testFormatMillisecondsOptionalWithZero() {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss[.SSS]");
        Timestamp ts = new Timestamp(1000000L);
        ts.setNanos(0);

        String result = format.format(ts);
        Assert.assertFalse(result.contains("."));
    }

    @Test
    public void testRoundTripMilliseconds() throws ParseException {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Timestamp original = new Timestamp(1234567890L);
        original.setNanos(789000000);

        String formatted = format.format(original);
        Date parsed = format.parse(formatted);

        Assert.assertTrue(parsed instanceof Timestamp);
        Timestamp result = (Timestamp) parsed;
        Assert.assertEquals(789000000, result.getNanos());
    }

    @Test
    public void testFormatMillisecondsWithTruncation() {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Timestamp ts = new Timestamp(1000000L);
        ts.setNanos(123456789);

        String result = format.format(ts);
        Assert.assertTrue(result.contains(".123"));
        Assert.assertFalse(result.contains("456"));
    }

    @Test
    public void testParseMillisecondsToNanoseconds() throws ParseException {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String dateStr = "1970-01-01 03:46:40.250";

        Date result = format.parse(dateStr);
        Assert.assertTrue(result instanceof Timestamp);
        Timestamp ts = (Timestamp) result;
        Assert.assertEquals(250000000, ts.getNanos());
    }

    @Test
    public void testFormatMillisecondsWithQuotedText() {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        Timestamp ts = new Timestamp(1000000L);
        ts.setNanos(567000000);

        String result = format.format(ts);
        Assert.assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}"));
        Assert.assertTrue(result.contains(".567"));
    }

    @Test
    public void testParseMillisecondsWithQuotedText() throws ParseException {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        Timestamp ts = new Timestamp(1000000L);
        ts.setNanos(123000000);
        String formattedDate = format.format(ts);

        Assert.assertNotNull(formattedDate);
        Assert.assertTrue(formattedDate.contains("T"));

        Date result = format.parse(formattedDate);
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof Timestamp);
        Timestamp resultTs = (Timestamp) result;
        Assert.assertEquals(123000000, resultTs.getNanos());
    }

    @Test
    public void testFormatMillisecondsWithLocale() {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        Timestamp ts = new Timestamp(1000000L);
        ts.setNanos(999000000);

        String result = format.format(ts);
        Assert.assertTrue(result.contains(".999"));
    }

    @Test
    public void testParseInvalidMilliseconds() throws ParseException {
        ExtendedDateFormat format = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String dateStr = "1970-01-01 03:46:40.XYZ";

        Date result = format.parse(dateStr);
        Assert.assertNull(result);
    }
}
