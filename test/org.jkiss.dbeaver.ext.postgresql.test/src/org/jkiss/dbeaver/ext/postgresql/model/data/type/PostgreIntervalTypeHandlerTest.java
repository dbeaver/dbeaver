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
package org.jkiss.dbeaver.ext.postgresql.model.data.type;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataType;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class PostgreIntervalTypeHandlerTest extends DBeaverUnitTest {

    // Type modifiers as PostgreSQL stores them in pg_attribute.atttypmod
    private static final int TYPMOD_INTERVAL = -1;
    private static final int TYPMOD_INTERVAL_0 = 0x7fff_0000;
    private static final int TYPMOD_INTERVAL_3 = 0x7fff_0003;
    private static final int TYPMOD_INTERVAL_DAY = 0x0008_ffff;
    private static final int TYPMOD_INTERVAL_SECOND = 0x1000_ffff;
    private static final int TYPMOD_INTERVAL_SECOND_0 = 0x1000_0000;
    private static final int TYPMOD_INTERVAL_DAY_TO_SECOND_0 = 0x1c08_0000;
    private static final int TYPMOD_INTERVAL_DAY_TO_SECOND_6 = 0x1c08_0006;

    private final PostgreIntervalTypeHandler handler = PostgreIntervalTypeHandler.INSTANCE;
    private PostgreDataType intervalType;

    @BeforeEach
    public void setUp() {
        intervalType = Mockito.mock(PostgreDataType.class);
        Mockito.when(intervalType.getName()).thenReturn(PostgreConstants.TYPE_INTERVAL);
    }

    @Test
    public void zeroPrecisionIsPrinted() {
        Assertions.assertEquals("(0)", handler.getTypeModifiersString(intervalType, TYPMOD_INTERVAL_0));
        Assertions.assertEquals(" second(0)", handler.getTypeModifiersString(intervalType, TYPMOD_INTERVAL_SECOND_0));
        Assertions.assertEquals(" day to second(0)", handler.getTypeModifiersString(intervalType, TYPMOD_INTERVAL_DAY_TO_SECOND_0));
    }

    @Test
    public void positivePrecisionIsPrinted() {
        Assertions.assertEquals("(3)", handler.getTypeModifiersString(intervalType, TYPMOD_INTERVAL_3));
        Assertions.assertEquals(" day to second(6)", handler.getTypeModifiersString(intervalType, TYPMOD_INTERVAL_DAY_TO_SECOND_6));
    }

    @Test
    public void missingPrecisionIsNotPrinted() {
        Assertions.assertEquals("", handler.getTypeModifiersString(intervalType, TYPMOD_INTERVAL));
        Assertions.assertEquals(" day", handler.getTypeModifiersString(intervalType, TYPMOD_INTERVAL_DAY));
        Assertions.assertEquals(" second", handler.getTypeModifiersString(intervalType, TYPMOD_INTERVAL_SECOND));
    }

    @Test
    public void parsedModifiersMatchTheServerOnes() throws DBException {
        Assertions.assertEquals(TYPMOD_INTERVAL_0, parse("interval", "0"));
        Assertions.assertEquals(TYPMOD_INTERVAL_3, parse("interval", "3"));
        Assertions.assertEquals(TYPMOD_INTERVAL_DAY, parse("interval day"));
        Assertions.assertEquals(TYPMOD_INTERVAL_SECOND, parse("interval second"));
        Assertions.assertEquals(TYPMOD_INTERVAL_SECOND_0, parse("interval second", "0"));
        Assertions.assertEquals(TYPMOD_INTERVAL_DAY_TO_SECOND_6, parse("interval day to second", "6"));
    }

    @Test
    public void parsedTypeWithoutPrecisionIsPrintedWithoutPrecision() throws DBException {
        Assertions.assertEquals("", handler.getTypeModifiersString(intervalType, parse("interval")));
        Assertions.assertEquals(" second", handler.getTypeModifiersString(intervalType, parse("interval second")));
        Assertions.assertEquals(" year to month", handler.getTypeModifiersString(intervalType, parse("interval year to month")));
        Assertions.assertEquals(
            " hour to second",
            handler.getTypeModifiersString(intervalType, parse("interval hour to second"))
        );
    }

    @Test
    public void invalidPrecisionIsRejected() {
        Assertions.assertThrows(DBException.class, () -> parse("interval second", "7"));
        Assertions.assertThrows(DBException.class, () -> parse("interval second", "-1"));
        // Only intervals with seconds have precision
        Assertions.assertThrows(DBException.class, () -> parse("interval day", "3"));
    }

    private int parse(@NotNull String typeName, @NotNull String... modifiers) throws DBException {
        return handler.getTypeModifiers(intervalType, typeName, modifiers);
    }
}
