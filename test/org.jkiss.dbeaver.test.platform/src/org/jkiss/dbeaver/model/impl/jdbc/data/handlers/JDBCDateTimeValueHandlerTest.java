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
package org.jkiss.dbeaver.model.impl.jdbc.data.handlers;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.sql.Types;
import java.text.FieldPosition;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.Date;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class JDBCDateTimeValueHandlerTest extends DBeaverUnitTest {
    private static final String DISPLAY_VALUE = "2007-05-08 12:35:29.123 +1215";
    private static final OffsetDateTime OFFSET_VALUE = OffsetDateTime.parse("2007-05-08T12:35:29.1234567+12:15");

    private final DBSTypedObject column = mock(DBSTypedObject.class);
    private final DBDFormatSettings settings = mock(DBDFormatSettings.class);
    private final DBDDataFormatter formatter = mock(DBDDataFormatter.class);
    private final JDBCDateTimeValueHandler handler = new JDBCDateTimeValueHandler(settings);

    @BeforeEach
    public void setUp() throws ReflectiveOperationException {
        DBDDataFormatterProfile profile = mock(DBDDataFormatterProfile.class);
        when(settings.getDataFormatterProfile()).thenReturn(profile);
        when(profile.createFormatter(anyString(), same(column))).thenReturn(formatter);
        when(formatter.formatValue(any())).thenReturn(DISPLAY_VALUE);
        when(column.getTypeID()).thenReturn(Types.VARCHAR);
    }

    @Test
    public void quotesNativeOffsetDateTimeFallback() {
        Assertions.assertEquals("'" + DISPLAY_VALUE + "'", handler.getValueDisplayString(column, OFFSET_VALUE, DBDDisplayFormat.NATIVE));
        verify(formatter).formatValue(OFFSET_VALUE);
    }

    @Test
    public void quotesDateWithoutNativeFormatter() {
        Timestamp value = Timestamp.valueOf("2007-05-08 12:35:29.123");
        Assertions.assertEquals("'" + DISPLAY_VALUE + "'", handler.getValueDisplayString(column, value, DBDDisplayFormat.NATIVE));
    }

    @Test
    public void quotesFallbackAfterNativeFormattingFailure() {
        Format nativeFormat = new SimpleDateFormat() {
            @Override
            public StringBuffer format(@NotNull Date date, @NotNull StringBuffer buffer, @NotNull FieldPosition position) {
                throw new IllegalArgumentException("Unsupported date");
            }
        };
        JDBCDateTimeValueHandler failingHandler = new JDBCDateTimeValueHandler(settings) {
            @NotNull
            @Override
            protected Format getNativeValueFormat(@NotNull DBSTypedObject type) {
                return nativeFormat;
            }
        };
        Assertions.assertEquals(
            "'" + DISPLAY_VALUE + "'",
            failingHandler.getValueDisplayString(column, Timestamp.valueOf("2007-05-08 12:35:29.123"), DBDDisplayFormat.NATIVE)
        );
    }

    @Test
    public void quotesNativeStringsOnce() {
        String value = "2007-05-08 12:35:29.1234567 +12:15";
        Assertions.assertEquals("'" + value + "'", handler.getValueDisplayString(column, value, DBDDisplayFormat.NATIVE));
        Assertions.assertEquals("'" + value + "'", handler.getValueDisplayString(column, "'" + value + "'", DBDDisplayFormat.NATIVE));
    }

    @Test
    public void preservesAlreadyQuotedFallback() {
        when(formatter.formatValue(OFFSET_VALUE)).thenReturn("'" + DISPLAY_VALUE + "'");
        Assertions.assertEquals("'" + DISPLAY_VALUE + "'", handler.getValueDisplayString(column, OFFSET_VALUE, DBDDisplayFormat.NATIVE));
    }

    @Test
    public void preservesNativeFormatterQuotes() {
        when(column.getTypeID()).thenReturn(Types.TIMESTAMP);
        Assertions.assertEquals(
            "'2007-05-08 12:35:29.123'",
            handler.getValueDisplayString(column, Timestamp.valueOf("2007-05-08 12:35:29.123"), DBDDisplayFormat.NATIVE)
        );
    }

    @Test
    public void preservesNativeSQLExpressions() {
        JDBCDateTimeValueHandler expressionHandler = new JDBCDateTimeValueHandler(settings) {
            @NotNull
            @Override
            protected Format getNativeValueFormat(@NotNull DBSTypedObject type) {
                return new SimpleDateFormat("'TIMESTAMP '''yyyy-MM-dd HH:mm:ss.SSS''");
            }
        };
        Assertions.assertEquals(
            "TIMESTAMP '2007-05-08 12:35:29.123'",
            expressionHandler.getValueDisplayString(column, Timestamp.valueOf("2007-05-08 12:35:29.123"), DBDDisplayFormat.NATIVE)
        );
    }

    @Test
    public void preservesNullDisplay() {
        Assertions.assertEquals("", handler.getValueDisplayString(column, null, DBDDisplayFormat.NATIVE));
        Assertions.assertEquals("", handler.getValueDisplayString(column, null, DBDDisplayFormat.EDIT));
        Assertions.assertEquals(DBConstants.NULL_VALUE_LABEL, handler.getValueDisplayString(column, null, DBDDisplayFormat.UI));
    }

    @Test
    public void preservesNonNativeDisplay() {
        Assertions.assertEquals(DISPLAY_VALUE, handler.getValueDisplayString(column, OFFSET_VALUE, DBDDisplayFormat.UI));
        Assertions.assertEquals(DISPLAY_VALUE, handler.getValueDisplayString(column, OFFSET_VALUE, DBDDisplayFormat.EDIT));
        Assertions.assertEquals(DISPLAY_VALUE, handler.getValueDisplayString(column, DISPLAY_VALUE, DBDDisplayFormat.UI));
        Assertions.assertEquals(DISPLAY_VALUE, handler.getValueDisplayString(column, DISPLAY_VALUE, DBDDisplayFormat.EDIT));
    }
}
