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

import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;

public class PostgreOffsetEndOfDay extends PostgreEndOfDay {

    private final ZoneOffset offset;

    public PostgreOffsetEndOfDay(@NotNull ZoneOffset offset) {
        this.offset = offset;
    }

    @Override
    public boolean isSupported(@NotNull TemporalField field) {
        return super.isSupported(field) || ChronoField.OFFSET_SECONDS.equals(field);
    }

    @Override
    public long getLong(@NotNull TemporalField field) {
        if (ChronoField.OFFSET_SECONDS.equals(field)) {
            return this.offset.getTotalSeconds();
        } else {
            return super.getLong(field);
        }
    }
}
