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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NumericFormatUtils {
    public static final String PROP_DECIMAL_SEPARATOR = "decimalSeparator";
    public static final String PROP_GROUPING_SEPARATOR = "groupingSeparator";

    private NumericFormatUtils() {
    }

    public static char getDecimalSeparator(@NotNull Map<String, Object> processorProperties) {
        if (processorProperties.containsKey(PROP_DECIMAL_SEPARATOR)) {
            String decimalSeparator = CommonUtils.toString(processorProperties.get(PROP_DECIMAL_SEPARATOR)).trim();
            if (!CommonUtils.isEmpty(decimalSeparator)) {
                return decimalSeparator.charAt(0);
            }
        }
        return getLocaleDecimalSeparator();
    }

    /**
     * Resolves the grouping separator from processor settings.
     * Returns the configured separator, falls back to the locale default when the setting is absent,
     * or {@code null} when the setting is blank or matches the decimal separator.
     */
    @Nullable
    public static Character getGroupingSeparator(@NotNull Map<String, Object> processorProperties, char decimalSeparator) {
        if (!processorProperties.containsKey(PROP_GROUPING_SEPARATOR)) {
            return getLocaleGroupingSeparator(decimalSeparator);
        }
        String groupingSeparator = CommonUtils.toString(processorProperties.get(PROP_GROUPING_SEPARATOR)).trim();
        if (CommonUtils.isEmpty(groupingSeparator)) {
            return null;
        }
        char grouping = groupingSeparator.charAt(0);
        return grouping == decimalSeparator ? null : grouping;
    }

    public static char getLocaleDecimalSeparator() {
        return DecimalFormatSymbols.getInstance(Locale.getDefault()).getDecimalSeparator();
    }

    @Nullable
    public static Character getLocaleGroupingSeparator(char decimalSeparator) {
        char groupingSeparator = DecimalFormatSymbols.getInstance(Locale.getDefault()).getGroupingSeparator();
        return groupingSeparator == decimalSeparator ? null : groupingSeparator;
    }

    public static String toPropertyValue(@Nullable Character separator) {
        return separator == null ? "" : String.valueOf(separator);
    }

    public static void normalizeNumericValues(
        @NotNull String[] line,
        @NotNull List<StreamDataImporterColumnInfo> streamColumns,
        char decimalSeparator,
        @Nullable Character groupingSeparator
    ) {
        DecimalFormat format = createFormat(decimalSeparator, groupingSeparator);
        ParsePosition position = new ParsePosition(0);
        for (int i = 0; i < Math.min(line.length, streamColumns.size()); i++) {
            if (streamColumns.get(i).getDataKind() == DBPDataKind.NUMERIC && line[i] != null) {
                String normalizedValue = normalizeNumberValue(
                    line[i],
                    decimalSeparator,
                    groupingSeparator,
                    format,
                    position
                );
                line[i] = normalizedValue == null ? line[i] : normalizedValue;
            }
        }
    }

    /**
     * Validates and normalizes a localized numeric string.
     * Trims surrounding whitespace, parses the value using the provided decimal
     * and grouping separators, and returns a normalized representation that uses
     * Java numeric syntax.
     *
     * @param value input string expected to represent a number
     * @param decimalSeparator decimal separator accepted in the input
     * @param groupingSeparator grouping separator accepted in the input, or {@code null} if grouping is disabled
     * @return normalized numeric string, or {@code null} if the input is blank or cannot be parsed fully
     */
    @Nullable
    public static String normalizeNumberValue(@NotNull String value, char decimalSeparator, @Nullable Character groupingSeparator) {
        DecimalFormat format = createFormat(decimalSeparator, groupingSeparator);
        ParsePosition position = new ParsePosition(0);
        return normalizeNumberValue(value, decimalSeparator, groupingSeparator, format, position);
    }

    @Nullable
    private static String normalizeNumberValue(
        @NotNull String value,
        char decimalSeparator,
        @Nullable Character groupingSeparator,
        @NotNull DecimalFormat format,
        @NotNull ParsePosition position
    ) {
        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            return null;
        }

        boolean scientific = containsExponent(trimmedValue, decimalSeparator, groupingSeparator);
        String normalizedValue = scientific ? trimmedValue.replace('e', 'E') : trimmedValue;
        position.setIndex(0);
        position.setErrorIndex(-1);
        Number parsedNumber = format.parse(normalizedValue, position);
        if (parsedNumber == null || position.getIndex() != normalizedValue.length()) {
            return null;
        }

        int exponentIndex = scientific ? normalizedValue.indexOf('E') : -1;
        String numberPart = exponentIndex >= 0 ? normalizedValue.substring(0, exponentIndex) : normalizedValue;
        String exponentPart = exponentIndex >= 0 ? normalizedValue.substring(exponentIndex) : "";

        if (groupingSeparator != null) {
            numberPart = numberPart.replace(String.valueOf(groupingSeparator), "");
        }
        if (decimalSeparator != '.') {
            numberPart = numberPart.replace(decimalSeparator, '.');
        }
        return numberPart + exponentPart;
    }

    private static boolean containsExponent(@NotNull String value, char decimalSeparator, @Nullable Character groupingSeparator) {
        if (decimalSeparator == 'e' || decimalSeparator == 'E') {
            return false;
        }
        if (groupingSeparator != null && (groupingSeparator == 'e' || groupingSeparator == 'E')) {
            return false;
        }
        return value.indexOf('e') >= 0 || value.indexOf('E') >= 0;
    }

    @NotNull
    private static DecimalFormat createFormat(char decimalSeparator, @Nullable Character groupingSeparator) {
        boolean groupingEnabled = groupingSeparator != null;
        DecimalFormat format = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ROOT);
        format.setDecimalFormatSymbols(createSymbols(decimalSeparator, groupingSeparator));
        format.setGroupingUsed(groupingEnabled);
        return format;
    }

    @NotNull
    private static DecimalFormatSymbols createSymbols(char decimalSeparator, @Nullable Character groupingSeparator) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.ROOT);
        symbols.setDecimalSeparator(decimalSeparator);
        if (groupingSeparator != null) {
            symbols.setGroupingSeparator(groupingSeparator);
        }

        return symbols;
    }
}
