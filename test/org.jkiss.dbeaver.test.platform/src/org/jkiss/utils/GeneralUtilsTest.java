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
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import static org.junit.Assert.assertEquals;

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
            assertEquals(expectedResult, actualResult);
        }
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
