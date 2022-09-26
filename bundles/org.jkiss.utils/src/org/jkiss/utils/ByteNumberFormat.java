/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 * Bytes formatter
 */
public class ByteNumberFormat extends NumberFormat {
    private static final long serialVersionUID = 1;

    public static final Unit[] UNITS = Unit.values();

    private static final DecimalFormat fpFormat = new DecimalFormat("#.#");

    private final BinaryPrefix binaryPrefix;

    /**
     * Creates a new formatter.
     */
    public ByteNumberFormat() {
        binaryPrefix = BinaryPrefix.JEDEC;
    }

    public ByteNumberFormat(@NotNull BinaryPrefix binaryPrefix) {
        this.binaryPrefix = binaryPrefix;
    }

    public static int computeIndex(double bytes) {
        int index = 0;

        for (int i = 0; i < UNITS.length; i++) {
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
        if (index >= UNITS.length) {
            index = UNITS.length - 1;
        }

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
        final Unit unit = UNITS[index];
        if (unit == Unit.BYTE) {
            return str;
        }
        return str + (binaryPrefix == BinaryPrefix.ISO ? unit.isoPrefix : unit.jedecPrefix);
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

    private enum Unit {
        BYTE("B", "B"),
        KILOBYTE("K", "KiB"),
        MEGABYTE("M", "MiB"),
        GIGABYTE("G", "GiB"),
        TERABYTE("T", "TiB"),
        PETABYTE("P", "PiB");

        private final String jedecPrefix;
        private final String isoPrefix;

        Unit(String jedecPrefix, String isoPrefix) {
            this.jedecPrefix = jedecPrefix;
            this.isoPrefix = isoPrefix;
        }
    }

    /**
     * <a href="https://en.wikipedia.org/wiki/Binary_prefix">A unit prefix for multiples of byte.</a>
     */
    public enum BinaryPrefix {
        /**
         * JEDEC-compliant prefixes (K, M, G, T, etc.). It's easy to confuse such prefixes with SI prefixes, leading to ambiguity.
         */
        JEDEC,

        /**
         * ISO 80000-13 format. This format explicitly states the binary nature of prefixes right in their names (kibi, mibi, gibi, etc.)
         */
        ISO,
    }
}
