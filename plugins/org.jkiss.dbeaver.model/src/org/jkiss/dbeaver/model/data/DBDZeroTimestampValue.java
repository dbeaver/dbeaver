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

import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Set;

public class DBDZeroTimestampValue implements TemporalAccessor {

    public static final DBDZeroTimestampValue INSTANCE = new DBDZeroTimestampValue();

    private static final Set<TemporalField> fields = Set.of(
        ChronoField.YEAR_OF_ERA,
        ChronoField.YEAR,
        ChronoField.MONTH_OF_YEAR,
        ChronoField.DAY_OF_MONTH,
        ChronoField.HOUR_OF_DAY,
        ChronoField.MINUTE_OF_HOUR,
        ChronoField.SECOND_OF_MINUTE,
        ChronoField.MILLI_OF_SECOND,
        ChronoField.NANO_OF_SECOND
    );

    private DBDZeroTimestampValue() {
    }

    @Override
    public boolean isSupported(@NotNull TemporalField field) {
        return fields.contains(field);
    }

    @Override
    public long getLong(@NotNull TemporalField field) {
        if (field.isSupportedBy(this)) {
            return 0;
        } else {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
    }
}
