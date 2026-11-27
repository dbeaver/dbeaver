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
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;

import static org.junit.jupiter.api.Assertions.*;

class DBDEndOfDayValueTest extends DBeaverUnitTest {

    @Test
    void serializationPreservesSingleton() {
        Object restored = roundTrip(DBDEndOfDayValue.withoutOffset());

        assertSame(DBDEndOfDayValue.withoutOffset(), restored);
        assertEquals("24:00:00", restored.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Z", "+02:00", "-05:30"})
    void serializationPreservesOffset(@NotNull String offsetText) {
        ZoneOffset offset = ZoneOffset.of(offsetText);
        Object restored = roundTrip(DBDEndOfDayValue.withOffset(offset));

        DBDOffsetEndOfDayValue value = assertInstanceOf(DBDOffsetEndOfDayValue.class, restored);
        assertEquals(offset, value.getOffset());
        assertEquals(24, value.getLong(ChronoField.HOUR_OF_DAY));
        assertEquals(offset.getTotalSeconds(), value.getLong(ChronoField.OFFSET_SECONDS));
        assertEquals("24:00:00" + offset, value.toString());
    }

    @Test
    void serializationPreservesMultiValueFilter() {
        ZoneOffset offset = ZoneOffset.ofHours(2);
        LocalTime time = LocalTime.of(12, 34, 56, 123456789);
        OffsetTime offsetTime = OffsetTime.of(time, offset);
        Object[] values = {DBDEndOfDayValue.withoutOffset(), DBDEndOfDayValue.withOffset(offset), time, offsetTime};

        Object[] restored = assertInstanceOf(Object[].class, roundTrip(values));

        assertEquals(values.length, restored.length);
        assertSame(DBDEndOfDayValue.withoutOffset(), restored[0]);
        DBDOffsetEndOfDayValue endOfDay = assertInstanceOf(DBDOffsetEndOfDayValue.class, restored[1]);
        assertEquals(offset, endOfDay.getOffset());
        assertEquals("24:00:00+02:00", endOfDay.toString());
        assertEquals(time, restored[2]);
        assertEquals(offsetTime, restored[3]);
    }

    @NotNull
    private static Object roundTrip(@NotNull Object value) {
        String serialized = GeneralUtils.serializeObject(value);
        assertFalse(serialized.isEmpty());
        Object restored = GeneralUtils.deserializeObject(serialized);
        assertNotNull(restored);
        return restored;
    }
}
