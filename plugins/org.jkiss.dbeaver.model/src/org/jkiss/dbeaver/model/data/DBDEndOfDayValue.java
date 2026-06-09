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
package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;

import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Set;

public class DBDEndOfDayValue implements TemporalAccessor {

    private static final DBDEndOfDayValue INSTANCE = new DBDEndOfDayValue();

    public static final Timestamp TIMESTAMP = Timestamp.valueOf(
        LocalDateTime.ofInstant(Instant.ofEpochMilli(Duration.ofHours(24).toMillis()), ZoneId.of(ZoneOffset.UTC.getId()))
    );

    @NotNull
    public static DBDEndOfDayValue withoutOffset() {
        return INSTANCE;
    }

    @NotNull
    public static DBDEndOfDayValue withOffset(@NotNull ZoneOffset offset) {
        return new DBDOffsetEndOfDayValue(offset);
    }

    private static final Set<TemporalField> fields = Set.of(
        ChronoField.HOUR_OF_DAY,
        ChronoField.MINUTE_OF_HOUR,
        ChronoField.SECOND_OF_MINUTE,
        ChronoField.MILLI_OF_SECOND,
        ChronoField.NANO_OF_SECOND
    );

    protected DBDEndOfDayValue() {
    }

    @Override
    public boolean isSupported(@NotNull TemporalField field) {
        return fields.contains(field);
    }

    @Override
    public long getLong(@NotNull TemporalField field) {
        if (field.isSupportedBy(this)) {
            return ChronoField.HOUR_OF_DAY.equals(field) ? 24 : 0;
        } else {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
    }

    @NotNull
    @Override
    public String toString() {
        return "24:00:00";
    }
}
