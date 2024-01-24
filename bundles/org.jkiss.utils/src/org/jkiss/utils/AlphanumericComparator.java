/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import java.util.Comparator;

/**
 * A comparator for comparing two strings lexicographically, treating them as sequences of alphanumeric characters.
 * <p>
 * This comparator compares strings based on their alphanumeric content. It considers
 * the characters in the strings as a sequence of alphanumeric characters (letters and digits)
 * and compares them lexicographically. The comparison is case-insensitive.
 */
public class AlphanumericComparator implements Comparator<CharSequence> {
    private static final AlphanumericComparator INSTANCE = new AlphanumericComparator();

    private AlphanumericComparator() {
        // prevents instantiation
    }

    @NotNull
    public static AlphanumericComparator getInstance() {
        return INSTANCE;
    }

    @Override
    public int compare(CharSequence o1, CharSequence o2) {
        final int len1 = o1.length();
        final int len2 = o2.length();

        int i = 0;
        int j = 0;

        while (i < len1 && j < len2) {
            final char ch1 = Character.toUpperCase(o1.charAt(i));
            final char ch2 = Character.toUpperCase(o2.charAt(i));

            if (Character.isDigit(ch1) && Character.isDigit(ch2)) {
                int num1 = 0;
                int num2 = 0;

                while (i < len1 && Character.isDigit(o1.charAt(i))) {
                    num1 = num1 * 10 + Character.digit(o1.charAt(i), 10);
                    i += 1;
                }

                while (j < len2 && Character.isDigit(o2.charAt(j))) {
                    num2 = num2 * 10 + Character.digit(o2.charAt(j), 10);
                    j += 1;
                }

                if (num1 != num2) {
                    return num1 - num2;
                }
            } else {
                if (ch1 != ch2) {
                    return ch1 - ch2;
                }

                i += 1;
                j += 1;
            }
        }

        return len1 - len2;
    }
}
