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

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParsePosition;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NumericFormatUtils {
    public static final String PROP_DECIMAL_SEPARATOR = "decimalSeparator";
    public static final String PROP_GROUPING_SEPARATOR = "groupingSeparator";

    // DecimalFormat patterns require an explicit upper bound for optional fraction digits.
    private static final int MAX_FRACTION_DIGITS = 31;
    private static final String OPTIONAL_FRACTION = "#".repeat(MAX_FRACTION_DIGITS);
    private static final String DECIMAL_PATTERN_NO_GROUPING = "0." + OPTIONAL_FRACTION;
    private static final String DECIMAL_PATTERN_GROUPING = "#,##0." + OPTIONAL_FRACTION;
    private static final String SCIENTIFIC_PATTERN_NO_GROUPING = "0." + OPTIONAL_FRACTION + "E0";
    private static final String SCIENTIFIC_PATTERN_GROUPING = "#,##0." + OPTIONAL_FRACTION + "E0";

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
        for (int i = 0; i < Math.min(line.length, streamColumns.size()); i++) {
            if (streamColumns.get(i).getDataKind() == DBPDataKind.NUMERIC && line[i] != null) {
                String normalizedValue = normalizeNumberValue(line[i], decimalSeparator, groupingSeparator);
                line[i] = normalizedValue == null ? line[i] : normalizedValue;
            }
        }
    }

    @Nullable
    public static String normalizeNumberValue(@NotNull String value, char decimalSeparator, char groupingSeparator) {
        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            return null;
        }

        boolean scientific = containsExponent(trimmedValue, decimalSeparator, groupingSeparator);
        BigDecimal parsedValue = parseBigDecimal(trimmedValue, decimalSeparator, groupingSeparator, scientific);
        if (parsedValue == null) {
            return null;
        }

        return scientific ? parsedValue.toString() : parsedValue.toPlainString();
    }

    private static boolean containsExponent(String value, char decimalSeparator, char groupingSeparator) {
        if (decimalSeparator == 'e' || decimalSeparator == 'E' || groupingSeparator == 'e' || groupingSeparator == 'E') {
            return false;
        }
        return value.indexOf('e') >= 0 || value.indexOf('E') >= 0;
    }

    @Nullable
    private static BigDecimal parseBigDecimal(
        @NotNull String value,
        char decimalSeparator,
        char groupingSeparator,
        boolean scientific
    ) {
        String normalizedValue = scientific ? value.replace('e', 'E') : value;
        DecimalFormat format = createFormat(scientific, decimalSeparator, groupingSeparator);
        ParsePosition position = new ParsePosition(0);
        Number parsedNumber = format.parse(normalizedValue, position);

        if (position.getIndex() != normalizedValue.length() || !(parsedNumber instanceof BigDecimal bigDecimal)) {
            return null;
        }

        return bigDecimal;
    }

    @NotNull
    private static DecimalFormat createFormat(boolean scientific, char decimalSeparator, char groupingSeparator) {
        boolean groupingEnabled = groupingSeparator != Character.MIN_VALUE;
        String pattern = scientific
            ? (groupingEnabled ? SCIENTIFIC_PATTERN_GROUPING : SCIENTIFIC_PATTERN_NO_GROUPING)
            : (groupingEnabled ? DECIMAL_PATTERN_GROUPING : DECIMAL_PATTERN_NO_GROUPING);

        DecimalFormat format = new DecimalFormat(pattern, createSymbols(decimalSeparator, groupingSeparator));
        format.setParseBigDecimal(true);
        format.setGroupingUsed(groupingEnabled);
        return format;
    }

    @NotNull
    private static DecimalFormatSymbols createSymbols(char decimalSeparator, char groupingSeparator) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.ROOT);

        if (decimalSeparator != Character.MIN_VALUE) {
            symbols.setDecimalSeparator(decimalSeparator);
        }

        if (groupingSeparator != Character.MIN_VALUE) {
            symbols.setGroupingSeparator(groupingSeparator);
        }

        return symbols;
    }
}
