/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl.data.transformers;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformer;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.data.ProxyValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Transforms numeric attribute value into epoch time
 */
public class EpochTimeAttributeTransformer implements DBDAttributeTransformer {
    private static final Log log = Log.getLog(EpochTimeAttributeTransformer.class);

    static final String PROP_UNIT = "unit";
    static final String ZONE_ID = "zoneId";

    private enum EpochUnit {
        seconds {
            @Override
            Instant toInstant(long rawValue) {
                return Instant.ofEpochSecond(rawValue);
            }

            @Override
            DateTimeFormatter getFormatter() {
                return SECONDS_FORMATTER;
            }

            @Override
            long toRawValue(Instant instant) {
                return instant.getEpochSecond();
            }
        },

        milliseconds {
            @Override
            Instant toInstant(long rawValue) {
                return Instant.ofEpochMilli(rawValue);
            }

            @Override
            DateTimeFormatter getFormatter() {
                return MILLIS_FORMATTER;
            }

            @Override
            long toRawValue(Instant instant) {
                return instant.toEpochMilli();
            }
        },

        nanoseconds {
            @Override
            Instant toInstant(long rawValue) {
                return Instant.ofEpochSecond(rawValue / 1_000_000_000, rawValue % 1_000_000_000);
            }

            @Override
            DateTimeFormatter getFormatter() {
                return NANOS_FORMATTER;
            }

            @Override
            long toRawValue(Instant instant) {
                return instant.getEpochSecond() * 1_000_000_000 + instant.getNano();
            }
        };

        private static final DateTimeFormatter SECONDS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        private static final DateTimeFormatter MILLIS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.ENGLISH);
        private static final DateTimeFormatter NANOS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.nnnnnnnnn",Locale.ENGLISH);

        abstract Instant toInstant(long rawValue);

        abstract DateTimeFormatter getFormatter();

        abstract long toRawValue(Instant instant);
    }

    @Override
    public void transformAttribute(@NotNull DBCSession session, @NotNull DBDAttributeBinding attribute, @NotNull List<Object[]> rows, @NotNull Map<String, Object> options) throws DBException {
        attribute.setPresentationAttribute(new TransformerPresentationAttribute(attribute, "EpochTime", -1, DBPDataKind.DATETIME));
        EpochUnit unit = EpochUnit.milliseconds;
        try {
            unit = EpochUnit.valueOf(CommonUtils.toString(options.get(PROP_UNIT)));
        } catch (IllegalArgumentException e) {
            log.error("Bad unit type");
        }
        attribute.setTransformHandler(new EpochValueHandler(attribute.getValueHandler(), unit, CommonUtils.toString(options.get(ZONE_ID))));
    }

    private static class EpochValueHandler extends ProxyValueHandler {
        private final EpochUnit unit;
        private final String zoneName;

        @Nullable
        private ZoneId zoneId;

        EpochValueHandler(DBDValueHandler target, EpochUnit unit, String zoneName) {
            super(target);
            this.unit = unit;
            this.zoneName = zoneName;
        }

        private ZoneId getZoneId() {
            if (zoneId != null) {
                return zoneId;
            }
            if (zoneName.isEmpty()) {
                return ZoneId.systemDefault();
            }
            zoneId = ZoneId.of(zoneName);
            return zoneId;
        }

        @NotNull
        @Override
        public String getValueDisplayString(@NotNull DBSTypedObject column, @Nullable Object value, @NotNull DBDDisplayFormat format) {
            if (!(value instanceof Number)) {
                return DBValueFormatting.getDefaultValueDisplayString(value, format);
            }
            long rawValue = ((Number) value).longValue();
            Instant instant = unit.toInstant(rawValue);
            ZonedDateTime dateTime = ZonedDateTime.ofInstant(instant, getZoneId());
            return unit.getFormatter().format(dateTime);
        }

        @Nullable
        @Override
        public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, @Nullable Object object, boolean copy, boolean validateValue) throws DBCException {
            if (!(object instanceof String)) {
                return super.getValueFromObject(session, type, object, copy, validateValue);
            }
            ZonedDateTime dateTime;
            try {
                dateTime = ZonedDateTime.of(LocalDateTime.parse((String) object, unit.getFormatter()), getZoneId());
            } catch (DateTimeException e) {
                return new DBCException("Incorrect zoneId");
            }
            return unit.toRawValue(Instant.from(dateTime));
        }
    }
}
