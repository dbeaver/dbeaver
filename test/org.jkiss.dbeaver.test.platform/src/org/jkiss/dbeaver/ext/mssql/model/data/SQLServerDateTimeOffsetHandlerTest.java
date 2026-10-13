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
package org.jkiss.dbeaver.ext.mssql.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.impl.data.formatters.DateTimeDataFormatter;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;

import static org.mockito.Mockito.*;

public class SQLServerDateTimeOffsetHandlerTest extends DBeaverUnitTest {
    private final DBSTypedObject column = mock(DBSTypedObject.class);
    private final DBDFormatSettings settings = mock(DBDFormatSettings.class);
    private final DateTimeDataFormatter formatter = spy(new DateTimeDataFormatter());
    private final SQLServerDateTimeOffsetHandler handler = new SQLServerDateTimeOffsetHandler(settings);

    @BeforeEach
    public void setUp() throws ReflectiveOperationException {
        when(column.getTypeID()).thenReturn(Types.VARCHAR);
        // Deliberately use a display pattern that is unsuitable for SQL literals.
        formatter.init(column, Locale.ROOT, Map.of(DateTimeDataFormatter.PROP_PATTERN, "dd/MM/yyyy HH:mm:ss.SSS Z"));
        clearInvocations(formatter);
        DBDDataFormatterProfile profile = mock(DBDDataFormatterProfile.class);
        when(settings.getDataFormatterProfile()).thenReturn(profile);
        when(profile.createFormatter(DBDDataFormatter.TYPE_NAME_TIMESTAMP_TZ, column)).thenReturn(formatter);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "2007-05-08T12:35:29.1234567+12:15",
        "2007-05-08T12:35:29.1234567-03:30",
        "2007-05-08T12:35:29.1234567Z",
        "2007-05-08T12:35:29.0000001+12:15",
        "2007-05-08T12:35:29.1+12:15",
        "2007-05-08T12:35:29+12:15",
        "2007-05-08T00:00:00Z"
    })
    public void formatsNativeValueWithoutLosingPrecisionOrOffset(@NotNull String text) {
        OffsetDateTime value = OffsetDateTime.parse(text);
        String literal = handler.getValueDisplayString(column, value, DBDDisplayFormat.NATIVE);

        Assertions.assertEquals("'" + text + "'", literal);
        Assertions.assertEquals(value, OffsetDateTime.parse(literal.substring(1, literal.length() - 1)));
        verifyNoInteractions(formatter);
        verify(settings, never()).getDataFormatterProfile();
    }

    @ParameterizedTest
    @EnumSource(value = DBDDisplayFormat.class, names = {"UI", "EDIT"})
    public void preservesConfiguredDisplayFormat(@NotNull DBDDisplayFormat format) {
        OffsetDateTime value = OffsetDateTime.parse("2007-05-08T12:35:29.1234567+12:15");

        Assertions.assertEquals("08/05/2007 12:35:29.123 +1215", handler.getValueDisplayString(column, value, format));
        verify(formatter).formatValue(value);
    }

    @Test
    public void preservesNativeStrings() {
        String value = "2007-05-08 12:35:29.1234567 +12:15";
        Assertions.assertEquals("'" + value + "'", handler.getValueDisplayString(column, value, DBDDisplayFormat.NATIVE));
        Assertions.assertEquals("'" + value + "'", handler.getValueDisplayString(column, "'" + value + "'", DBDDisplayFormat.NATIVE));
        verifyNoInteractions(formatter);
    }

    @Test
    public void preservesNativeNull() {
        Assertions.assertEquals("", handler.getValueDisplayString(column, null, DBDDisplayFormat.NATIVE));
        verifyNoInteractions(formatter);
    }
}
