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

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NumericFormatUtilsTest extends DBeaverUnitTest {

    @Test
    public void normalizeNumberValueTrimsAndConvertsSeparators() {
        String normalized = NumericFormatUtils.normalizeNumberValue(" 1.234,56 ", ',', '.');
        Assert.assertEquals("1234.56", normalized);
    }

    @Test
    public void normalizeNumberValueSupportsScientificNotation() {
        String normalized = NumericFormatUtils.normalizeNumberValue("1,25e+3", ',', Character.MIN_VALUE);
        Assert.assertEquals("1.25E+3", normalized);
    }

    @Test
    public void normalizeNumberValueRejectsInvalidFraction() {
        String normalized = NumericFormatUtils.normalizeNumberValue("1,2a", ',', Character.MIN_VALUE);
        Assert.assertNull(normalized);
    }

    @Test
    public void normalizeNumberValueRejectsInvalidSuffix() {
        String normalized = NumericFormatUtils.normalizeNumberValue("1,2E3foo", ',', Character.MIN_VALUE);
        Assert.assertNull(normalized);
    }

    @Test
    public void normalizeNumberValuePreservesPlainInteger() {
        String normalized = NumericFormatUtils.normalizeNumberValue("1234", '.', Character.MIN_VALUE);
        Assert.assertEquals("1234", normalized);
    }

    @Test
    public void normalizeNumberValueNormalizesGroupedInteger() {
        String normalized = NumericFormatUtils.normalizeNumberValue("1.234", ',', '.');
        Assert.assertEquals("1234", normalized);
    }

    @Test
    public void numericSeparatorsWhenLocaleIsEu() {
        Locale previousLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);

            Map<String, Object> properties = new HashMap<>();
            char decimalSeparator = NumericFormatUtils.getDecimalSeparator(properties);
            char groupingSeparator = NumericFormatUtils.getGroupingSeparator(properties, decimalSeparator);

            Assert.assertEquals(',', decimalSeparator);
            Assert.assertEquals('.', groupingSeparator);
        } finally {
            Locale.setDefault(previousLocale);
        }
    }

    @Test
    public void numericSeparatorsWhenLocaleIsUs() {
        Locale previousLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);

            Map<String, Object> properties = new HashMap<>();
            char decimalSeparator = NumericFormatUtils.getDecimalSeparator(properties);
            char groupingSeparator = NumericFormatUtils.getGroupingSeparator(properties, decimalSeparator);

            Assert.assertEquals('.', decimalSeparator);
            Assert.assertEquals(',', groupingSeparator);
        } finally {
            Locale.setDefault(previousLocale);
        }
    }

    @Test
    public void groupingSeparatorDisabledWhenEqualToDecimalSeparator() {
        char decimalSeparator = ',';
        char groupingSeparator = NumericFormatUtils.getGroupingSeparator(
            Map.of(NumericFormatUtils.PROP_GROUPING_SEPARATOR, ","),
            decimalSeparator
        );
        Assert.assertEquals(Character.MIN_VALUE, groupingSeparator);
    }

    @Test
    public void toPropertyValueReturnsEmptyStringForMinValue() {
        Assert.assertEquals("", NumericFormatUtils.toPropertyValue(Character.MIN_VALUE));
        Assert.assertEquals(",", NumericFormatUtils.toPropertyValue(','));
    }
}
