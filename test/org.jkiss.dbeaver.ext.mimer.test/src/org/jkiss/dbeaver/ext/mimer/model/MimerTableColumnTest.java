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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link MimerTableColumn#buildIntervalTypeName} - rebuilding a fully-precisioned {@code
 * INTERVAL} type declaration from {@code INFORMATION_SCHEMA.COLUMNS}'s {@code INTERVAL_TYPE}/
 * {@code INTERVAL_PRECISION}/{@code DATETIME_PRECISION}, since the driver's own {@code TYPE_NAME}
 * never carries the real precision. Expected values confirmed against a real DbVisualizer DDL
 * export for a table covering all 13 qualifier shapes.
 *
 * @author Mimer Information Technology
 */
public class MimerTableColumnTest extends DBeaverUnitTest {

    @Test
    public void singleFieldNonSecond() {
        Assertions.assertEquals("INTERVAL DAY(5)", MimerTableColumn.buildIntervalTypeName("DAY", 5, 0));
        Assertions.assertEquals("INTERVAL HOUR(2)", MimerTableColumn.buildIntervalTypeName("HOUR", 2, 0));
        Assertions.assertEquals("INTERVAL MINUTE(5)", MimerTableColumn.buildIntervalTypeName("MINUTE", 5, 0));
    }

    @Test
    public void singleFieldSecondUsesOneCommaSeparatedPrecisionPair() {
        Assertions.assertEquals("INTERVAL SECOND(2, 1)", MimerTableColumn.buildIntervalTypeName("SECOND", 2, 1));
    }

    @Test
    public void rangeEndingInNonSecondOnlyPrecisionsTheLeadingField() {
        Assertions.assertEquals("INTERVAL DAY(2) TO HOUR", MimerTableColumn.buildIntervalTypeName("DAY TO HOUR", 2, 0));
        Assertions.assertEquals("INTERVAL DAY(3) TO MINUTE", MimerTableColumn.buildIntervalTypeName("DAY TO MINUTE", 3, 0));
        Assertions.assertEquals("INTERVAL HOUR(1) TO MINUTE", MimerTableColumn.buildIntervalTypeName("HOUR TO MINUTE", 1, 0));
    }

    @Test
    public void rangeEndingInSecondGetsItsOwnSeparateFractionalPrecision() {
        Assertions.assertEquals("INTERVAL DAY(7) TO SECOND(9)", MimerTableColumn.buildIntervalTypeName("DAY TO SECOND", 7, 9));
        Assertions.assertEquals("INTERVAL HOUR(6) TO SECOND(6)", MimerTableColumn.buildIntervalTypeName("HOUR TO SECOND", 6, 6));
        Assertions.assertEquals("INTERVAL MINUTE(2) TO SECOND(2)", MimerTableColumn.buildIntervalTypeName("MINUTE TO SECOND", 2, 2));
    }

    @Test
    public void nullFractionalPrecisionIsTreatedAsZero() {
        Assertions.assertEquals("INTERVAL SECOND(2, 0)", MimerTableColumn.buildIntervalTypeName("SECOND", 2, null));
        Assertions.assertEquals("INTERVAL DAY(7) TO SECOND(0)", MimerTableColumn.buildIntervalTypeName("DAY TO SECOND", 7, null));
    }

    @Test
    public void missingIntervalTypeOrLeadingPrecisionYieldsNull() {
        Assertions.assertNull(MimerTableColumn.buildIntervalTypeName(null, 5, 0));
        Assertions.assertNull(MimerTableColumn.buildIntervalTypeName("", 5, 0));
        Assertions.assertNull(MimerTableColumn.buildIntervalTypeName("DAY", null, 0));
    }

    @Test
    public void unfilledPlaceholdersAreStrippedFromAPendingColumnsTypeName() {
        Assertions.assertEquals("INTERVAL DAY", MimerTableColumn.stripUnfilledIntervalPlaceholders("INTERVAL DAY(p)"));
        Assertions.assertEquals("INTERVAL DAY TO HOUR", MimerTableColumn.stripUnfilledIntervalPlaceholders("INTERVAL DAY(p) TO HOUR"));
        Assertions.assertEquals("INTERVAL DAY TO SECOND", MimerTableColumn.stripUnfilledIntervalPlaceholders("INTERVAL DAY(p) TO SECOND(s)"));
        Assertions.assertEquals("INTERVAL SECOND", MimerTableColumn.stripUnfilledIntervalPlaceholders("INTERVAL SECOND(p,s)"));
    }

    @Test
    public void aRealTypedInPrecisionIsLeftUntouched() {
        Assertions.assertEquals("INTERVAL DAY(5)", MimerTableColumn.stripUnfilledIntervalPlaceholders("INTERVAL DAY(5)"));
        Assertions.assertEquals("INTERVAL DAY(7) TO SECOND(9)", MimerTableColumn.stripUnfilledIntervalPlaceholders("INTERVAL DAY(7) TO SECOND(9)"));
        Assertions.assertEquals("INTERVAL SECOND(2, 1)", MimerTableColumn.stripUnfilledIntervalPlaceholders("INTERVAL SECOND(2, 1)"));
    }

    @Test
    public void onlyTheUnfilledGroupIsStrippedWhenOneFieldWasEdited() {
        Assertions.assertEquals(
            "INTERVAL DAY TO SECOND(9)",
            MimerTableColumn.stripUnfilledIntervalPlaceholders("INTERVAL DAY(p) TO SECOND(9)"));
        Assertions.assertEquals(
            "INTERVAL DAY(7) TO SECOND",
            MimerTableColumn.stripUnfilledIntervalPlaceholders("INTERVAL DAY(7) TO SECOND(s)"));
    }
}
