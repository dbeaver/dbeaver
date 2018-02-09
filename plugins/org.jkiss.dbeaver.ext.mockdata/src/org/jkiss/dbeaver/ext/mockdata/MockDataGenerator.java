/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2010-2017 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.mockdata;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Random;

public class MockDataGenerator {
    private static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod " +
            "tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation " +
            "ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in " +
            "voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non " +
            "proident, sunt in culpa qui officia deserunt mollit anim id est laborum. ";
    private static int LOREM_IPSUM_LENGTH = LOREM_IPSUM.length();

    private  static int LONG_PRECISION    = String.valueOf(Long.MAX_VALUE).length();    // 19
    private  static int INTEGER_PRECISION = String.valueOf(Integer.MAX_VALUE).length(); // 10
    private  static int SHORT_PRECISION   = String.valueOf(Short.MAX_VALUE).length();   // 5
    private  static int BYTE_PRECISION    = String.valueOf(Byte.MAX_VALUE).length();    // 3

    private static Random random = new Random();

    public static String generateTextUpTo(int length) {
        return generateText(random.nextInt(length));
    }

    public static String generateText(int length) {
        int start = random.nextInt(LOREM_IPSUM_LENGTH);
        if (start + length < LOREM_IPSUM_LENGTH) {
            return LOREM_IPSUM.substring(start, start + length);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(LOREM_IPSUM.substring(start));
            int newlength = length - (LOREM_IPSUM_LENGTH - start);
            for (int i = 0; i < newlength / LOREM_IPSUM_LENGTH; i++) {
                sb.append(LOREM_IPSUM);
            }
            sb.append(LOREM_IPSUM.substring(0, newlength % LOREM_IPSUM_LENGTH));
            return sb.toString();
        }
    }

    private static int degree(int d) {
        int result = 10;
        for (int i = 0; i < d - 1; i++) {
            result *= 10;
        }
        return result;
    }

    private static int r(int i) {
        return random.nextInt(i);
    }

    public static Object generateNumeric(int length, Integer precision, Integer scale) {
        // Integers
        if ((scale == null || scale == 0) && (precision != null && precision != 0)) {
            if (precision < BYTE_PRECISION) {
                return new Byte((byte) random.nextInt(degree(r(precision))));
            }
            if (precision < SHORT_PRECISION) {
                return new Short((short) random.nextInt(degree(r(precision))));
            }
            if (precision < INTEGER_PRECISION) {
                return new Integer(random.nextInt(degree(r(precision))));
            }
            if (precision < LONG_PRECISION) {
                return new Long(random.nextLong());
            }

            // Default integer number
            return null; // TODO new BigInteger();
        }
        // Non-integers
        else {
            if (precision != null && precision > 0) {
                int scl = scale != null ? scale : 0;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < precision; i++) {
                    sb.append(random.nextInt(10));
                    if (i == scale) {
                        sb.append('.');
                    }
                }
                return new BigDecimal(sb.reverse().toString());
            } else {
                return new BigDecimal(random.nextLong()); // TODO
            }
        }
    }

    public static Date generateDate() {
        // Get an Epoch value roughly between 1940 and 2010
        // -946771200000L = January 1, 1940
        // Add up to 70 years to it (using modulus on the next long)
        long ms = -946771200000L + (Math.abs(random.nextLong()) % (70L * 365 * 24 * 60 * 60 * 1000));

        return new Date(ms);
    }

    public static void main(String[] args) {
        System.out.println(">> " + generateText(500) + " <<");
    }
}
