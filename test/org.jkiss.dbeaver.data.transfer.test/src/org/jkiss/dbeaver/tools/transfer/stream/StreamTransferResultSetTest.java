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
package org.jkiss.dbeaver.tools.transfer.stream;

import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StreamTransferResultSetTest {

    private static final String DEFAULT_FORMAT = "yyyy-MM-dd[ HH:mm:ss[.SSS]]";

    @Test
    void localDateTimeIsConvertedToTimestamp() throws Exception {
        Object value = convert(DEFAULT_FORMAT, null, "2024-03-05 10:15:30.123");
        assertEquals(Timestamp.valueOf(LocalDateTime.of(2024, 3, 5, 10, 15, 30, 123_000_000)), value);
    }

    @Test
    void dateOnlyIsConvertedToStartOfDay() throws Exception {
        Object value = convert(DEFAULT_FORMAT, null, "2024-03-05");
        assertEquals(Timestamp.valueOf(LocalDate.of(2024, 3, 5).atStartOfDay()), value);
    }

    @Test
    void localDateTimeIsShiftedToTargetZone() throws Exception {
        ZoneId targetZone = ZoneId.of("UTC");
        Object value = convert(DEFAULT_FORMAT, targetZone, "2024-03-05 10:15:30");
        LocalDateTime expected = LocalDateTime.of(2024, 3, 5, 10, 15, 30)
            .atZone(ZoneId.systemDefault())
            .withZoneSameInstant(targetZone)
            .toLocalDateTime();
        assertEquals(Timestamp.valueOf(expected), value);
    }

    @Test
    void zonedDateTimeIsConvertedToInstant() throws Exception {
        Object value = convert("yyyy-MM-dd HH:mm:ss XXX", null, "2024-03-05 10:15:30 +09:00");
        assertEquals(Date.from(Instant.parse("2024-03-05T01:15:30Z")), value);
    }

    @Test
    void zonedDateWithoutTimeFallsBackToLocalDate() throws Exception {
        Object value = convert("yyyy-MM-dd XXX", null, "2024-03-05 +09:00");
        assertEquals(Timestamp.valueOf(LocalDate.of(2024, 3, 5).atStartOfDay()), value);
    }

    @Test
    void unparseableValueIsReturnedAsIs() throws Exception {
        Object value = convert(DEFAULT_FORMAT, null, "not a date");
        assertEquals("not a date", value);
    }

    private static Object convert(String pattern, ZoneId zoneId, String input) throws Exception {
        List<StreamDataImporterColumnInfo> columns = new ArrayList<>();
        StreamEntityMapping mapping = Mockito.mock(StreamEntityMapping.class);
        Mockito.when(mapping.getStreamColumns()).thenReturn(columns);
        columns.add(new StreamDataImporterColumnInfo(mapping, 0, "ts", "TIMESTAMP", 0, DBPDataKind.DATETIME));

        StreamTransferResultSet resultSet = new StreamTransferResultSet(
            Mockito.mock(DBCSession.class),
            Mockito.mock(DBCStatement.class),
            mapping
        );
        resultSet.setDateTimeFormat(DateTimeFormatter.ofPattern(pattern), zoneId);
        resultSet.setStreamRow(new Object[]{input});
        return resultSet.getAttributeValue(0);
    }
}
