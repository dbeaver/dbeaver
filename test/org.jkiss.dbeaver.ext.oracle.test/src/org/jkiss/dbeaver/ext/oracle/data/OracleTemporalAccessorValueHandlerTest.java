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
package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Types;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

class OracleTemporalAccessorValueHandlerTest extends DBeaverUnitTest {

    private static final int ORACLE_TIMESTAMP_TZ = OracleConstants.DATA_TYPE_TIMESTAMP_WITH_TIMEZONE;

    private final OracleTemporalAccessorValueHandler handler =
        new OracleTemporalAccessorValueHandler(Mockito.mock(DBDFormatSettings.class));

    @Test
    void timestampWithTimeZoneLiteralUsesHoursAndMinutesOffset() {
        Assertions.assertEquals(
            "TIMESTAMP '2026-10-26 14:30:45.123456000 +03:00'",
            toNativeString(ORACLE_TIMESTAMP_TZ, OffsetDateTime.parse("2026-10-26T14:30:45.123456+03:00"))
        );
        Assertions.assertEquals(
            "TIMESTAMP '2026-10-26 14:30:45.123456000 +05:30'",
            toNativeString(ORACLE_TIMESTAMP_TZ, OffsetDateTime.parse("2026-10-26T14:30:45.123456+05:30"))
        );
        Assertions.assertEquals(
            "TIMESTAMP '2026-10-26 14:30:45.123456000 +00:00'",
            toNativeString(Types.TIMESTAMP_WITH_TIMEZONE, OffsetDateTime.parse("2026-10-26T14:30:45.123456Z"))
        );
    }

    @Test
    void fractionalSecondsBelowOneTenthKeepTheirValue() {
        Assertions.assertEquals(
            "TIMESTAMP '2026-10-26 14:30:45.050000000 +03:00'",
            toNativeString(ORACLE_TIMESTAMP_TZ, OffsetDateTime.parse("2026-10-26T14:30:45.05+03:00"))
        );
        Assertions.assertEquals(
            "TIMESTAMP '2026-10-26 14:30:45.000005000'",
            toNativeString(Types.TIMESTAMP, LocalDateTime.parse("2026-10-26T14:30:45.000005"))
        );
    }

    @NotNull
    private String toNativeString(int typeId, @NotNull Object value) {
        DBSTypedObject column = Mockito.mock(DBSTypedObject.class);
        Mockito.when(column.getTypeID()).thenReturn(typeId);
        return handler.getValueDisplayString(column, value, DBDDisplayFormat.NATIVE);
    }
}
