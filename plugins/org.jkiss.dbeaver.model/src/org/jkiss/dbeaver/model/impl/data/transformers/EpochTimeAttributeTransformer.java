/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

    private static final int GIGA = 1_000_000_000;
    private static final int TEN_MEGA = 10_000_000;
    private static final int MEGA = 1_000_000;
    private static final int TICKS_TO_NANOS = 100;
    private static final int NANOS_TO_MICROS = 1000;
    private static final long DOTNET_TICKS_OFFSET = 621_355_968_000_000_000L;  // DateTime.UnixEpoch.Ticks
    private static final long W32_FILETIME_OFFSET = 116_444_736_000_000_000L;  // DateTime.UnixEpoch.ToFileTimeUtc()
    private static final double OADATE_OFFSET = 25569.0;  // DateTime.UnixEpoch.ToOADate()
    private static final double SQLITE_JULIAN_OFFSET = 2440587.5;  // select julianday(0, "unixepoch")
    private static final int SECONDS_IN_DAY = 24 * 3600;

    private static final DateTimeFormatter SECONDS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
    private static final DateTimeFormatter MILLIS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.ENGLISH);
    private static final DateTimeFormatter MICROS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.nnnnnn", Locale.ENGLISH);
    private static final DateTimeFormatter NANOS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.nnnnnnnnn", Locale.ENGLISH);
    // 10 us precision
    private static final DateTimeFormatter SQLITE_JULIAN_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.nnnnn", Locale.ENGLISH);
    // 100 ns precision
    private static final DateTimeFormatter DOTNET_TICKS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.nnnnnnn", Locale.ENGLISH);

    private enum EpochUnit {
        seconds {
            @Override
            Instant toInstant(Number value) {
                long longValue = value.longValue();
                return Instant.ofEpochSecond(longValue);
            }

            @Override
            DateTimeFormatter getFormatter() {
                return SECONDS_FORMATTER;
            }

            @Override
            Long toRawValue(Instant instant) {
                return instant.getEpochSecond();
            }
        },

        milliseconds {
            @Override
            Instant toInstant(Number value) {
                long longValue = value.longValue();
                return Instant.ofEpochMilli(longValue);
            }

            @Override
            DateTimeFormatter getFormatter() {
                return MILLIS_FORMATTER;
            }

            @Override
            Long toRawValue(Instant instant) {
                return instant.toEpochMilli();
            }
        },

        microseconds {
            @Override
            Instant toInstant(Number value) {
                long longValue = value.longValue();
                return Instant.ofEpochSecond(longValue / MEGA, longValue % MEGA);
            }

            @Override
            DateTimeFormatter getFormatter() {
                return MICROS_FORMATTER;
            }

            @Override
            Long toRawValue(Instant instant) {
                return instant.getEpochSecond() * MEGA + instant.getNano() / NANOS_TO_MICROS;
            }
        },

        nanoseconds {
            @Override
            Instant toInstant(Number value) {
                long longValue = value.longValue();
                return Instant.ofEpochSecond(longValue / GIGA, longValue % GIGA);
            }

            @Override
            DateTimeFormatter getFormatter() {
                return NANOS_FORMATTER;
            }

            @Override
            Long toRawValue(Instant instant) {
                return instant.getEpochSecond() * GIGA + instant.getNano();
            }
        },

        // https://docs.microsoft.com/en-us/dotnet/api/system.datetime.ticks?view=net-6.0#remarks
        dotnet {
            @Override
            Instant toInstant(Number value) {
                return ticksToInstant(value.longValue(), DOTNET_TICKS_OFFSET);
            }

            @Override
            DateTimeFormatter getFormatter() {
                return DOTNET_TICKS_FORMATTER;
            }

            @Override
            Long toRawValue(Instant instant) {
                return instantToTicks(instant, DOTNET_TICKS_OFFSET);
            }
        },

        // https://docs.microsoft.com/en-us/dotnet/api/system.datetime.fromfiletimeutc?view=net-6.0#system-datetime-fromfiletimeutc(system-int64)
        w32filetime {
            @Override
            Instant toInstant(Number value) {
                return ticksToInstant(value.longValue(), W32_FILETIME_OFFSET);
            }

            @Override
            DateTimeFormatter getFormatter() {
                return DOTNET_TICKS_FORMATTER;
            }

            @Override
            Long toRawValue(Instant instant) {
                return instantToTicks(instant, W32_FILETIME_OFFSET);
            }
        },

        // https://docs.microsoft.com/en-us/dotnet/api/system.datetime.fromoadate?view=net-6.0#system-datetime-fromoadate(system-double)
        oadate {
            @Override
            Instant toInstant(Number value) {
                return daysToInstant(value.doubleValue(), OADATE_OFFSET);
            }

            @Override
            DateTimeFormatter getFormatter() {
                return NANOS_FORMATTER;
            }

            @Override
            Double toRawValue(Instant instant) {
                return instantToDays(instant, OADATE_OFFSET);
            }
        },

        // https://www.sqlite.org/lang_datefunc.html
        sqliteJulian {
            @Override
            Instant toInstant(Number value) {
                return daysToInstant(value.doubleValue(), SQLITE_JULIAN_OFFSET);
            }

            @Override
            DateTimeFormatter getFormatter() {
                return SQLITE_JULIAN_FORMATTER;
            }

            @Override
            Double toRawValue(Instant instant) {
                return instantToDays(instant, SQLITE_JULIAN_OFFSET);
            }
        };

        private static Instant ticksToInstant(long rawValue, long offset) {
            long sinceUnixEpoch = rawValue - offset;
            return Instant.ofEpochSecond(sinceUnixEpoch / TEN_MEGA, sinceUnixEpoch % TEN_MEGA * TICKS_TO_NANOS);
        }

        private static long instantToTicks(Instant instant, long offset) {
            return instant.getEpochSecond() * TEN_MEGA + instant.getNano() / TICKS_TO_NANOS + offset;
        }

        private static Instant daysToInstant(double rawValue, double offset) {
            double daysSinceUnixEpoch = rawValue - offset;
            long wholeDaysSinceUnixEpoch = (long)daysSinceUnixEpoch;
            double fractionalDay = daysSinceUnixEpoch - wholeDaysSinceUnixEpoch;
            long fractionalDayNanos = (long)(fractionalDay * SECONDS_IN_DAY * GIGA);
            return Instant.ofEpochSecond(wholeDaysSinceUnixEpoch * SECONDS_IN_DAY, fractionalDayNanos);
        }

        private static double instantToDays(Instant instant, double offset) {
            double daysSinceUnixEpoch = (instant.getEpochSecond() + 1e-9 * instant.getNano()) / SECONDS_IN_DAY;
            return daysSinceUnixEpoch + offset;
        }

        abstract Instant toInstant(Number value);

        abstract DateTimeFormatter getFormatter();

        abstract Number toRawValue(Instant instant);
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
            try {
                zoneId = ZoneId.of(zoneName);
            } catch (Exception e) {
                log.debug(e);
                zoneId = ZoneId.systemDefault();
            }
            return zoneId;
        }

        @NotNull
        @Override
        public String getValueDisplayString(@NotNull DBSTypedObject column, @Nullable Object value, @NotNull DBDDisplayFormat format) {
            if (!(value instanceof Number)) {
                return DBValueFormatting.getDefaultValueDisplayString(value, format);
            }
            Instant instant = unit.toInstant((Number)value);
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
