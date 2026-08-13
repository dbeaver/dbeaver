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
package org.jkiss.dbeaver.model.impl.data.formatters;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NumberDataFormatterTest extends DBeaverUnitTest {

    private static final BigDecimal SMALL_VALUE = new BigDecimal("0.0000000000015");

    private static Map<String, Object> properties(Locale locale, boolean scientific) {
        Map<String, Object> props = new HashMap<>(new NumberFormatSample().getDefaultProperties(locale));
        props.put(NumberFormatSample.PROP_SCIENTIFIC_SMALL_VALUES, scientific);
        return props;
    }

    private static NumberDataFormatter formatter(Locale locale, Map<String, Object> props) {
        NumberDataFormatter formatter = new NumberDataFormatter();
        formatter.init(null, locale, props);
        return formatter;
    }

    @Test
    public void defaultPatternIsScientific() {
        // A plain number pattern would silently turn the option into a no-op
        Assertions.assertEquals(
            "0.###E0",
            new NumberFormatSample().getDefaultProperties(Locale.US).get(NumberFormatSample.PROP_SCIENTIFIC_PATTERN));
    }

    @Test
    public void smallValueUsesScientificNotation() {
        NumberDataFormatter formatter = formatter(Locale.US, properties(Locale.US, true));
        Assertions.assertEquals("1.5E-12", formatter.formatValue(SMALL_VALUE));
        Assertions.assertEquals("-1.5E-12", formatter.formatValue(SMALL_VALUE.negate()));
    }

    @Test
    public void disabledOptionKeepsPlainFormatting() {
        NumberDataFormatter formatter = formatter(Locale.US, properties(Locale.US, false));
        Assertions.assertEquals("0", formatter.formatValue(SMALL_VALUE));
    }

    @Test
    public void regularValuesAreNotAffected() {
        NumberDataFormatter formatter = formatter(Locale.US, properties(Locale.US, true));
        Assertions.assertEquals("0", formatter.formatValue(BigDecimal.ZERO));
        Assertions.assertEquals("0.5", formatter.formatValue(new BigDecimal("0.5")));
        Assertions.assertEquals("1,234,567,890.012", formatter.formatValue(new BigDecimal("1234567890.012345")));
    }

    @Test
    public void separatorsComeFromLocale() {
        NumberDataFormatter formatter = formatter(Locale.FRENCH, properties(Locale.FRENCH, true));
        Assertions.assertEquals("1,5E-12", formatter.formatValue(SMALL_VALUE));
    }

    @Test
    public void missingPropertiesFallBackToDefaults() {
        // Profiles saved before these properties existed
        Map<String, Object> props = properties(Locale.US, true);
        props.remove(NumberFormatSample.PROP_SCIENTIFIC_PATTERN);
        props.remove(NumberFormatSample.PROP_SCIENTIFIC_EXP_SEP);
        Assertions.assertEquals("1.5E-12", formatter(Locale.US, props).formatValue(SMALL_VALUE));
    }

    @Test
    public void emptyPropertiesFallBackToDefaults() {
        Map<String, Object> props = properties(Locale.US, true);
        props.put(NumberFormatSample.PROP_SCIENTIFIC_PATTERN, "");
        props.put(NumberFormatSample.PROP_SCIENTIFIC_EXP_SEP, "");
        Assertions.assertEquals("1.5E-12", formatter(Locale.US, props).formatValue(SMALL_VALUE));
    }

    @Test
    public void invalidPatternIsRejected() {
        // Preferences page relies on this to roll the entered value back
        Map<String, Object> props = properties(Locale.US, true);
        props.put(NumberFormatSample.PROP_SCIENTIFIC_PATTERN, "0.###E");
        Assertions.assertThrows(IllegalArgumentException.class, () -> formatter(Locale.US, props));
    }

    @Test
    public void patternWithoutExponentIsRejected() {
        // Such a pattern is valid for DecimalFormat but renders every small number as 'x0'
        Map<String, Object> props = properties(Locale.US, true);
        props.put(NumberFormatSample.PROP_SCIENTIFIC_PATTERN, "x");
        Assertions.assertThrows(IllegalArgumentException.class, () -> formatter(Locale.US, props));
    }

    @Test
    public void customExponentSeparatorSurvivesRoundTrip() throws Exception {
        Map<String, Object> props = properties(Locale.US, true);
        props.put(NumberFormatSample.PROP_SCIENTIFIC_EXP_SEP, "e");
        NumberDataFormatter formatter = formatter(Locale.US, props);

        String formatted = formatter.formatValue(SMALL_VALUE);
        Assertions.assertEquals("1.5e-12", formatted);
        Assertions.assertEquals(0, SMALL_VALUE.compareTo((BigDecimal) formatter.parseValue(formatted, BigDecimal.class)));
    }
}
