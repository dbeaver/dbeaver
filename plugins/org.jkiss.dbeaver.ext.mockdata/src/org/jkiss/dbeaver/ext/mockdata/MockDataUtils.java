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

import org.jkiss.code.NotNull;
import org.jkiss.utils.IntKeyMap;

import java.math.BigDecimal;
import java.util.Random;

public class MockDataUtils {

    public static int LONG_PRECISION    = String.valueOf(Long.MAX_VALUE).length();    // 19
    public static int INTEGER_PRECISION = String.valueOf(Integer.MAX_VALUE).length(); // 11
    public static int SHORT_PRECISION   = String.valueOf(Short.MAX_VALUE).length();   // 5
    public static int BYTE_PRECISION    = String.valueOf(Byte.MAX_VALUE).length();    // 3

    private static final Random random = new Random();
    private static IntKeyMap<Integer> degrees = new IntKeyMap<Integer>();

    public static Object generateNumeric(Integer precision, Integer scale, Double min, Double max) {
        // Integers
        if ((scale == null || scale == 0) && (precision != null && precision != 0)) {
            if (precision <= BYTE_PRECISION) {
                return (byte) randomInteger(degree(precision), min, max);
            }
            if (precision <= SHORT_PRECISION) {
                return (short) randomInteger(degree(precision), min, max);
            }
            if (precision <= INTEGER_PRECISION) {
                return randomInteger(degree(precision), min, max);
            }
            if (precision <= LONG_PRECISION) {
                return getRandomLong(min, max, random);
            }

            // Default integer number
            return null; // TODO new BigInteger();
        }
        // Non-integers
        else {
            if (precision != null && precision > 0) {
                int scl = scale != null ? scale : 0;
                StringBuilder sb = new StringBuilder();
                if (precision <= scl) {
                    sb.append('0');
                } else {
                    sb.append(randomInteger(degree(precision - scl), 0d, null));
                }
                if (scl > 0) {
                    sb.append('.');
                    sb.append(randomInteger(degree(scl), 0d, null));
                }
                return new BigDecimal(sb.toString());
            } else {
                return new BigDecimal(getRandomLong(min, max, random));
            }
        }
    }

    public static int getRandomInt(int min, int max, @NotNull Random random) {
        if (min == Integer.MIN_VALUE && max == Integer.MAX_VALUE) {
            return random.nextInt();
        }
        long dif = (long) max - (long) min;
        float number = random.nextFloat();      // 0 <= number < 1
        return ((int) ((long) min + number * dif));
    }

    public static double getRandomDouble(double min, double max, @NotNull Random random) {
        double dif = max - min;
        double number = random.nextDouble(); // 0 <= number < 1
        return min + number * dif;
    }

    private static long getRandomLong(Double min, Double max, Random random) {
        long minimum = Long.MIN_VALUE;
        if (min != null && min > minimum) {
            minimum = Math.round(min);
        }
        long maximum = Long.MAX_VALUE;
        if (max != null && max < maximum) {
            maximum = Math.round(max);
        }
        return getRandomLong(minimum, maximum, random);
    }

    public static long getRandomLong(long min, long max, @NotNull Random random) {
        if (min == Long.MIN_VALUE && max == Long.MAX_VALUE) {
            return random.nextLong();
        }
        double dif = (double)max - (double)min;
        double number = random.nextDouble();      // 0 <= number < 1
        return Math.round(min + number * dif);
    }

    public static int degree(int d) {
        Integer value = degrees.get(d);
        if (value == null) {
            int result = 10;
            for (int i = 0; i < d - 1; i++) {
                result *= 10;
            }
            degrees.put(d, value = result);
        }
        return value;
    }

    private static int randomInteger(int bound, Double min, Double max) {
        int minimum = Integer.MIN_VALUE;
        int maximum = Integer.MAX_VALUE;
        if (min != null && min > minimum && min < Integer.MAX_VALUE) {
            minimum = (int) Math.round(min);
        }
        if (max == null || max > bound) {
            max = (double)bound;
        }
        if (max < maximum) {
            maximum = (int) Math.round(max);
        }
        return getRandomInt(minimum, maximum, random);
    }
}
