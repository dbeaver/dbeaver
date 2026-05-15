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
package org.jkiss.dbeaver.ext.postgresql.model.data.value;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.postgresql.model.data.PostgreDateTimeValueHandler;

import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.util.Set;

public class PostgreEndOfDay implements TemporalAccessor {

    private static final PostgreEndOfDay INSTANCE = new PostgreEndOfDay();

    public static PostgreEndOfDay withoutOffset() {
        return INSTANCE;
    }

    public static PostgreEndOfDay withOffset(@NotNull ZoneOffset offset) {
        return new PostgreOffsetEndOfDay(offset);
    }

    private static final Set<TemporalField> fields = Set.of(
        ChronoField.HOUR_OF_DAY,
        ChronoField.MINUTE_OF_DAY,
        ChronoField.SECOND_OF_MINUTE,
        ChronoField.MILLI_OF_SECOND
    );

    protected PostgreEndOfDay() {
    }

    @Override
    public boolean isSupported(@NotNull TemporalField field) {
        return fields.contains(field);
    }

    @Override
    public long getLong(@NotNull TemporalField field) {
        return ChronoField.HOUR_OF_DAY.equals(field) ? 24 : 0;
    }
}
