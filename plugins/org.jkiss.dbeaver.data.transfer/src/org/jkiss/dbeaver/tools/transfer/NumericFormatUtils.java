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
package org.jkiss.dbeaver.tools.transfer;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.tools.transfer.stream.StreamDataImporterColumnInfo;
import org.jkiss.utils.CommonUtils;

import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NumericFormatUtils {
    public static final String PROP_DECIMAL_SEPARATOR = "decimalSeparator";
    public static final String PROP_GROUPING_SEPARATOR = "groupingSeparator";
    private static final int EXPONENT_NOT_FOUND = -1;
    private static final int EXPONENT_INVALID = -2;

    private NumericFormatUtils() {
    }

    public static char getDecimalSeparator(Map<String, Object> processorProperties) {
        if (processorProperties.containsKey(PROP_DECIMAL_SEPARATOR)) {
            String decimalSeparator = CommonUtils.toString(processorProperties.get(PROP_DECIMAL_SEPARATOR)).trim();
            return CommonUtils.isEmpty(decimalSeparator) ? Character.MIN_VALUE : decimalSeparator.charAt(0);
        }
        return getLocaleDecimalSeparator();
    }

    public static char getGroupingSeparator(Map<String, Object> processorProperties, char decimalSeparator) {
        if (processorProperties.containsKey(PROP_GROUPING_SEPARATOR)) {
            String groupingSeparator = CommonUtils.toString(processorProperties.get(PROP_GROUPING_SEPARATOR)).trim();
            if (CommonUtils.isEmpty(groupingSeparator)) {
                return Character.MIN_VALUE;
            }
            char grouping = groupingSeparator.charAt(0);
            return grouping == decimalSeparator ? Character.MIN_VALUE : grouping;
        }
        return getLocaleGroupingSeparator(decimalSeparator);
    }

    public static char getLocaleDecimalSeparator() {
        try {
            return DecimalFormatSymbols.getInstance(Locale.getDefault()).getDecimalSeparator();
        } catch (Exception ignored) {
            return '.';
        }
    }

    public static char getLocaleGroupingSeparator(char decimalSeparator) {
        final char groupingSeparator;
        try {
            groupingSeparator = DecimalFormatSymbols.getInstance(Locale.getDefault()).getGroupingSeparator();
        } catch (Exception ignored) {
            return Character.MIN_VALUE;
        }
        return groupingSeparator == decimalSeparator ? Character.MIN_VALUE : groupingSeparator;
    }

    public static String toPropertyValue(char separator) {
        return separator == Character.MIN_VALUE ? "" : String.valueOf(separator);
    }

    public static void normalizeNumericValues(
        String[] line,
        List<StreamDataImporterColumnInfo> streamColumns,
        char decimalSeparator,
        char groupingSeparator
    ) {
        if (decimalSeparator == Character.MIN_VALUE && groupingSeparator == Character.MIN_VALUE) {
            return;
        }
        for (int i = 0; i < Math.min(line.length, streamColumns.size()); i++) {
            if (streamColumns.get(i).getDataKind() == DBPDataKind.NUMERIC && line[i] != null) {
                String normalizedValue = normalizeNumberValue(line[i], decimalSeparator, groupingSeparator);
                line[i] = normalizedValue == null ? line[i] : normalizedValue;
            }
        }
    }

    // Normalizes locale-formatted numeric text to Java-parsable form; returns null for invalid input.
    @Nullable
    public static String normalizeNumberValue(@NotNull String value, char decimalSeparator, char groupingSeparator) {
        String normalizedInput = value.trim();
        if (normalizedInput.isEmpty()) {
            return null;
        }
        int exponentPos = findExponentPos(normalizedInput, decimalSeparator, groupingSeparator);
        if (exponentPos == EXPONENT_INVALID) {
            return null;
        }
        if (exponentPos >= 0) {
            return normalizeExponentNumber(
                normalizedInput,
                exponentPos,
                decimalSeparator,
                groupingSeparator
            );
        }
        return normalizeMantissa(normalizedInput, decimalSeparator, groupingSeparator);
    }

    @Nullable
    private static String normalizeExponentNumber(
        @NotNull String value,
        int exponentPos,
        char decimalSeparator,
        char groupingSeparator
    ) {
        String mantissa = value.substring(0, exponentPos);
        String exponent = value.substring(exponentPos + 1);
        if (!isValidExponent(exponent)) {
            return null;
        }
        String normalizedMantissa = normalizeMantissa(mantissa, decimalSeparator, groupingSeparator);
        if (normalizedMantissa == null) {
            return null;
        }
        return normalizedMantissa + value.charAt(exponentPos) + exponent;
    }

    @Nullable
    private static String normalizeMantissa(@NotNull String value, char decimalSeparator, char groupingSeparator) {
        String normalizedValue = value;
        // Reject mixed style: dot/comma must be configured as decimal or grouping separators
        // if present in value.
        if (decimalSeparator != '.' && groupingSeparator != '.' && normalizedValue.indexOf('.') >= 0) {
            return null;
        }
        if (decimalSeparator != ',' && groupingSeparator != ',' && normalizedValue.indexOf(',') >= 0) {
            return null;
        }
        if (groupingSeparator != Character.MIN_VALUE && groupingSeparator != decimalSeparator) {
            if (normalizedValue.indexOf(groupingSeparator) >= 0) {
                normalizedValue = stripGroupingSeparator(normalizedValue, decimalSeparator, groupingSeparator);
                if (normalizedValue == null) {
                    return null;
                }
            }
        }
        if (decimalSeparator != Character.MIN_VALUE) {
            int decimalIndex = normalizedValue.indexOf(decimalSeparator);
            if (decimalIndex >= 0) {
                if (decimalIndex + 1 >= normalizedValue.length()) {
                    return null;
                }
                for (int i = decimalIndex + 1; i < normalizedValue.length(); i++) {
                    if (!Character.isDigit(normalizedValue.charAt(i))) {
                        return null;
                    }
                }
            }
        }
        if (decimalSeparator == '.' || normalizedValue.indexOf(decimalSeparator) < 0) {
            return normalizedValue;
        }
        if (normalizedValue.indexOf('.') >= 0) {
            return null;
        }
        return normalizedValue.replace(decimalSeparator, '.');
    }

    /*
     * Return value semantics:
     * - [0..n]: position of the single exponent marker (e/E)
     * - EXPONENT_NOT_FOUND: no exponent marker, or e/E is used as configured separator
     * - EXPONENT_INVALID: multiple exponent markers were found
     */
    private static int findExponentPos(@NotNull String value, char decimalSeparator, char groupingSeparator) {
        if (isExponentMarker(decimalSeparator) || isExponentMarker(groupingSeparator)) {
            return EXPONENT_NOT_FOUND;
        }
        int exponentPos = EXPONENT_NOT_FOUND;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (isExponentMarker(ch)) {
                if (exponentPos != EXPONENT_NOT_FOUND) {
                    return EXPONENT_INVALID;
                }
                exponentPos = i;
            }
        }
        return exponentPos;
    }

    private static boolean isExponentMarker(char ch) {
        return ch == 'e' || ch == 'E';
    }

    private static boolean isValidExponent(@NotNull String exponent) {
        if (exponent.isEmpty()) {
            return false;
        }
        int i = 0;
        char first = exponent.charAt(0);
        if (first == '+' || first == '-') {
            i = 1;
        }
        if (i == exponent.length()) {
            return false;
        }
        for (; i < exponent.length(); i++) {
            if (!Character.isDigit(exponent.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    // Strips grouping separators only when they appear in integer part with valid grouping layout.
    @Nullable
    private static String stripGroupingSeparator(@NotNull String value, char decimalSeparator, char groupingSeparator) {
        int decimalIndex = value.indexOf(decimalSeparator);
        if (decimalIndex >= 0 && value.lastIndexOf(groupingSeparator) > decimalIndex) {
            return null;
        }
        if (!hasValidGroupingLayout(value, decimalSeparator, groupingSeparator)) {
            return null;
        }
        return value.replace(String.valueOf(groupingSeparator), "");
    }

    // Returns true if integer-part grouping is valid:
    // groups are 3 digits from right to left, with a leftmost group of 1..3 digits.
    private static boolean hasValidGroupingLayout(@NotNull String value, char decimalSeparator, char groupingSeparator) {
        int start = value.startsWith("+") || value.startsWith("-") ? 1 : 0;
        int decimalIndex = value.indexOf(decimalSeparator);
        int integerEnd = decimalIndex >= 0 ? decimalIndex : value.length();
        int digitsInGroup = 0;
        boolean sawGrouping = false;
        for (int i = integerEnd - 1; i >= start; i--) {
            char ch = value.charAt(i);
            if (Character.isDigit(ch)) {
                digitsInGroup++;
            } else if (ch == groupingSeparator) {
                sawGrouping = true;
                if (digitsInGroup != 3) {
                    return false;
                }
                digitsInGroup = 0;
            } else {
                return false;
            }
        }
        if (!sawGrouping) {
            return true;
        }
        return digitsInGroup >= 1 && digitsInGroup <= 3;
    }
}
