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
package org.jkiss.utils;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class GeneralUtilsTest extends DBeaverUnitTest {

    public static final String VARIABLE_YEAR = "year";
    public static final String VARIABLE_MONTH = "month";
    public static final String VARIABLE_DAY = "day";
    public static final String VARIABLE_HOUR = "hour";
    public static final String VARIABLE_MINUTE = "minute";
    
    @Test
    public void testVariablesSubstitution() {
        @SuppressWarnings("deprecation")
        Date ts = new Date(90, 3, 21, 3, 20, 54);
        var patternsWithResults = Map.of(
            "${missingVariable}", "missingVariable",
            "${minute}", getVariableValue(VARIABLE_MINUTE, ts),
            "abracadabra${hour}", "abracadabra" + getVariableValue(VARIABLE_HOUR, ts),
            "hour${month}day", "hour" + getVariableValue(VARIABLE_MONTH, ts) + "day",
            "${year}${year}-${month}${year}-${month}${day}-${day}${month}", 
                getVariableValue(VARIABLE_YEAR, ts) + getVariableValue(VARIABLE_YEAR, ts) + "-"
                + getVariableValue(VARIABLE_MONTH, ts) + getVariableValue(VARIABLE_YEAR, ts) + "-"
                + getVariableValue(VARIABLE_MONTH, ts) + getVariableValue(VARIABLE_DAY, ts) + "-"
                + getVariableValue(VARIABLE_DAY, ts) + getVariableValue(VARIABLE_MONTH, ts)
        );
        for (Map.Entry<String, String> entry : patternsWithResults.entrySet()) {
            String pattern = entry.getKey();
            String expectedResult = entry.getValue();
            String actualResult = GeneralUtils.replaceVariables(pattern, (name) -> getVariableValue(name, ts));
            Assertions.assertEquals(expectedResult, actualResult);
        }
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "user@example.com",
        "first.last+tag@example.co.uk",
        "user_name@example-domain.com",
        "USER@EXAMPLE.COM",
        "user@sub.example.com",
        "user@xn--bcher-kva.de",
        "o'connor@example.com",
        "!#$%&'*+/=?^_`{|}~-@example.com"
    })
    public void acceptsValidEmail(@NotNull String email) {
        Assertions.assertTrue(GeneralUtils.isValidEmail(email));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "user",
        "@example.com",
        "user@",
        "user@localhost",
        "user@@example.com",
        ".user@example.com",
        "user.@example.com",
        "first..last@example.com",
        "user@.example.com",
        "user@example..com",
        "user@example.com.",
        "user@-example.com",
        "user@example-.com",
        "user@exa_mple.com",
        "first last@example.com",
        "user@exa mple.com",
        "user@example.com\nother@example.com",
        "user@example.com\u0000",
        " user@example.com",
        "user@example.com ",
        "name[1]@example.com",
        "../../../@../../../../qqqqqwwwwwwwwwwwwwwwwwqqqqqqqqqq"
    })
    public void rejectsInvalidEmail(@NotNull String email) {
        Assertions.assertFalse(GeneralUtils.isValidEmail(email));
    }

    @Test
    public void enforcesEmailLengthLimits() {
        String longestEmail = "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(61);
        Assertions.assertEquals(254, longestEmail.length());
        Assertions.assertTrue(GeneralUtils.isValidEmail(longestEmail));
        Assertions.assertFalse(GeneralUtils.isValidEmail(longestEmail + "d"));
        Assertions.assertFalse(GeneralUtils.isValidEmail("a".repeat(65) + "@example.com"));
        Assertions.assertFalse(GeneralUtils.isValidEmail("user@" + "a".repeat(64) + ".com"));
    }

    @NotNull
    private static String getVariableValue(@NotNull String name, @NotNull Date ts) {
        switch (name) {
            case VARIABLE_YEAR:
                return new SimpleDateFormat("yyyy").format(ts);
            case VARIABLE_MONTH:
                return new SimpleDateFormat("MM").format(ts);
            case VARIABLE_DAY:
                return new SimpleDateFormat("dd").format(ts);
            case VARIABLE_HOUR:
                return new SimpleDateFormat("HH").format(ts);
            case VARIABLE_MINUTE:
                return new SimpleDateFormat("mm").format(ts);
            default:
                return name;
        }
    }   
}
