/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 * Bytes formatter
 */
public class ByteNumberFormat extends NumberFormat {
    private static final long serialVersionUID = 1;

    private static final String B = "B";
    private static final String KB = "Kb";
    private static final String MB = "Mb";
    private static final String GB = "Gb";
    private static final String TB = "Tb";
    private static final String PB = "Pb";

    public static final String[] BYTES = {
        B, KB, MB, GB, TB, PB
    };

    private static final DecimalFormat fpFormat = new DecimalFormat("#.#");

    /**
     * Creates a new formatter.
     */
    public ByteNumberFormat() {
        super();
    }

    public static int computeIndex(double bytes) {
        int index = 0;

        for (int i = 0; i < BYTES.length; i++) {
            int result = (int)(bytes / 1024);
            if (result == 0) {
                break;
            } else {
                bytes /= 1024;
                if (bytes < 1) {
                    break;
                }
                index++;
            }
        }
        
        return index;
    }

    /**
     * Returns a string representing the bytes.
     *
     * @param bytes  the number of bytes.
     *
     * @return A string.
     */
    public String getBytes(double bytes) {

        int index = computeIndex(bytes);

        double intBytes = bytes;
        if (intBytes == 0) {
            return String.valueOf(0);
        }

        for (int i = 0; i < index; i++) {
            intBytes /= 1024;
        }


        String str;
        if ((long)intBytes >= 10) {
            str = String.valueOf((long)intBytes);
        } else {
            str = fpFormat.format(intBytes);
        }
        if (index == 0) {
            return str;
        } else {
            return str + BYTES[index];
        }
    }

    /**
     * Formats a number into the specified string buffer.
     *
     * @param number  the number to format.
     * @param toAppendTo  the string buffer.
     * @param pos  the field position (ignored here).
     *
     * @return The string buffer.
     */
    @Override
    public StringBuffer format(double number, StringBuffer toAppendTo,
                               FieldPosition pos) {
        return toAppendTo.append(getBytes(number));
    }

    /**
     * Formats a number into the specified string buffer.
     *
     * @param number  the number to format.
     * @param toAppendTo  the string buffer.
     * @param pos  the field position (ignored here).
     *
     * @return The string buffer.
     */
    @Override
    public StringBuffer format(long number, StringBuffer toAppendTo,
                               FieldPosition pos) {
        return toAppendTo.append(getBytes(number));
    }

    /**
     * This method returns <code>null</code> for all inputs.  This class cannot
     * be used for parsing.
     *
     * @param source  the source string.
     * @param parsePosition  the parse position.
     *
     * @return <code>null</code>.
     */
    @Override
    public Number parse(String source, ParsePosition parsePosition) {
        return null;
    }

    public static void main(String[] args) {
        System.out.println(new ByteNumberFormat().format(100));
        System.out.println(new ByteNumberFormat().format(1000));
        System.out.println(new ByteNumberFormat().format(10000));
        System.out.println(new ByteNumberFormat().format(11000));
        System.out.println(new ByteNumberFormat().format(100000));
        System.out.println(new ByteNumberFormat().format(1000000));
        System.out.println(new ByteNumberFormat().format(10000000));
    }

}