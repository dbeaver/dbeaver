/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.utils.time;

import java.sql.Timestamp;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Formatter adapted to support nanoseconds from java.sql.Timestanp.
 */
public class ExtendedDateFormat extends SimpleDateFormat {

    private static final String NINE_ZEROES = "000000000";
    public static final int MAX_NANO_LENGTH = 8;

    int nanoStart = -1, nanoLength;
    boolean nanoOptional;
    String nanoPrefix, nanoPostfix;

    public ExtendedDateFormat(String pattern)
    {
        this(pattern, Locale.getDefault());
    }

    public ExtendedDateFormat(String pattern, Locale locale)
    {
        super(stripNanos(pattern), locale);

        int quoteCount = 0;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '\'') {
                quoteCount++;
                for (int k = i + 1; k < pattern.length(); k++) {
                    if (pattern.charAt(k) == '\'') {
                        if (k != i + 1) {
                            quoteCount++;
                        }
                        i = k;
                        break;
                    }
                }
            } else if (c == '[') {
                nanoStart = i;
                nanoOptional = true;
                for (int k = i + 1; k < pattern.length(); k++) {
                    if (pattern.charAt(k) == 'f') {
                        nanoLength++;
                        if (nanoPrefix == null) {
                            nanoPrefix = pattern.substring(i + 1, k);
                        }
                    }
                    if (pattern.charAt(k) == ']') {
                        nanoPostfix = pattern.substring(i + 1 + nanoPrefix.length() + nanoLength, k);
                        i = k + 1;
                        break;
                    }
                }
            } else if (c == 'f') {
                nanoStart = i - quoteCount;
                nanoOptional = false;
                for (int k = i + 1; k < pattern.length(); k++) {
                    if (pattern.charAt(k) != 'f') {
                        break;
                    }
                    nanoLength++;
                }
                nanoLength++;
                i = i + nanoLength;
            }
        }
    }

    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos)
    {
        StringBuffer result = super.format(date, toAppendTo, pos);
        if (nanoStart >= 0) {
            long nanos = 0;
            if (date instanceof Timestamp) {
                nanos = ((Timestamp) date).getNanos();
            }
            if (!nanoOptional || nanos > 0) {
                StringBuilder nanosRes = new StringBuilder(nanoLength);
                // Append nanos value in the end
                if (nanoPrefix != null) {
                    nanosRes.append(nanoPrefix);
                }
                String nanoStr = String.valueOf(nanos);

                // nanoStr must be a string of exactly 9 chars in length. Pad with leading "0" if not
                int nbZeroesToPad = 9 - nanoStr.length();
                if (nbZeroesToPad > 0) {
                    nanoStr = NINE_ZEROES.substring(0, nbZeroesToPad) + nanoStr;
                }

                if (nanoLength < nanoStr.length()) {
                    // Truncate nanos string to fit in the pattern
                    nanoStr = nanoStr.substring(0, nanoLength);
                } else {
                    // Pad with 0s
                    for (int i = 0; i < nanoLength - nanoStr.length(); i++) {
                        nanosRes.append("0");
                    }
                }
                nanosRes.append(nanoStr);
                if (nanoPostfix != null) {
                    nanosRes.append(nanoPostfix);
                }
                result.insert(nanoStart, nanosRes.toString());
            }
        }
        return result;
    }

    @Override
    public Date parse(String text, ParsePosition pos)
    {
        Date date = super.parse(text, pos);
        int index = pos.getIndex();
        if (index < text.length() && nanoStart > 0) {
            long nanos = 0;
            for (int i = 0; i < nanoLength; i++) {
                int digitPos = index + i;
                if (digitPos == text.length()) {
                    break;
                }
                char c = text.charAt(digitPos);
                if (!Character.isDigit(c)) {
                    pos.setErrorIndex(index);
                    pos.setIndex(index);
                    //throw new ParseException("Invalid nanosecond character at pos " + digitPos + ": " + c, index);
                    return null;
                }
                long digit = ((int)c - (int)'0');
                for (int k = MAX_NANO_LENGTH - i; k > 0; k--) {
                    digit *= 10;
                }
                nanos += digit;
            }
            if (nanos > 0) {
                Timestamp ts = new Timestamp(date.getTime());
                ts.setNanos((int)nanos);
                return ts;
            }
        }
        return date;
    }

    private static String stripNanos(String pattern)
    {
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '\'') {
                for (int k = i + 1; k < pattern.length(); k++) {
                    if (pattern.charAt(k) == '\'') {
                        i = k;
                        break;
                    }
                }
            } else if (c == '[') {
                for (int k = i + 1; k < pattern.length(); k++) {
                    if (pattern.charAt(k) == ']') {
                        return pattern.substring(0, i) + pattern.substring(k + 1);
                    }
                }
            } else if (c == 'f') {
                for (int k = i + 1; k < pattern.length(); k++) {
                    if (pattern.charAt(k) != 'f') {
                        return pattern.substring(0, i) + pattern.substring(k);
                    }
                }
                return pattern.substring(0, i);
            }
        }
        return pattern;
    }

    public static void main(String[] args)
    {
        test("'TIMESTAMP '''yyyy-MM-dd HH:mm:ss.ffffff''");
        test("yyyy-MM-dd Z hh:mm:ss[.fffffffff]");
        test("yyyy-MM-dd Z hh:mm:ss.fffffffff");
        test("yyyy-MM-dd Z hh:mm:ss");
        test("yyyy-MM-dd Z hh:mm:ss[.fffffffff nanos]");
        test("yyyy-MM-dd Z hh:mm:ss[.ffffff micros]");
        test("yyyy-MM-dd Z hh:mm:ss.ffffff");
        test("yyyy-MM-dd Z hh:mm:ss.f"); // 1/10 secs = 'S'
    }

    private static void test(String pattern)
    {
        ExtendedDateFormat edf = new ExtendedDateFormat(pattern);
        Timestamp date = new Timestamp(System.currentTimeMillis());
        System.out.println(edf.format(date));
        date.setNanos(0);
        System.out.println(edf.format(date));
    }

}
