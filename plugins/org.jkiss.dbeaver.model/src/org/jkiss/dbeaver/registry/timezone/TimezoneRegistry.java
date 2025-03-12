/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry.timezone;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class TimezoneRegistry {

    private static final Log log = Log.getLog(TimezoneRegistry.class);

    public static final String PROP_USER_TIMEZONE = "user.timezone";
    public static final String GMT_TIMEZONE = "GMT";
    private static String userDefaultTimezone = "";

    private TimezoneRegistry() {
    }

    public static void setDefaultZone(@Nullable ZoneId id, boolean updatePreferences) {
        DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        if (id != null) {
            TimeZone timeZone = TimeZone.getTimeZone(id);
            TimeZone.setDefault(timeZone);
            System.setProperty(PROP_USER_TIMEZONE, id.getId());
            if (updatePreferences) {
                preferenceStore.setValue(ModelPreferences.CLIENT_TIMEZONE, id.getId());
            }
        } else {
            if (!TimeZone.getDefault().getID().equals(userDefaultTimezone)) {
                TimeZone.setDefault(TimeZone.getTimeZone(userDefaultTimezone));
                System.setProperty(PROP_USER_TIMEZONE, userDefaultTimezone);
                if (updatePreferences) {
                    preferenceStore.setToDefault(ModelPreferences.CLIENT_TIMEZONE);
                }
            }
        }
    }

    public static void overrideTimezone() {
        userDefaultTimezone = System.getProperty(PROP_USER_TIMEZONE);
        System.setProperty("user.old.timezone", userDefaultTimezone);
        DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        final String customTimeZone = preferenceStore.getString(ModelPreferences.CLIENT_TIMEZONE);
        if (customTimeZone != null && !customTimeZone.equals(DBConstants.DEFAULT_TIMEZONE)) {
            log.debug("Overriding system time zone to '" + customTimeZone + "'");
            TimeZone currentTimeZone = TimeZone.getTimeZone(customTimeZone);
            if (!GMT_TIMEZONE.equals(customTimeZone) && GMT_TIMEZONE.equals(currentTimeZone.getID())) {
                log.debug("Time zone '" + customTimeZone + "' no recognized, falling back to GMT");
            } else if (Objects.equals(currentTimeZone.getID(), customTimeZone)) {
                log.debug("Time zone '" + customTimeZone + "' differs from current '" + currentTimeZone.getID() + "'");
            }
            TimeZone.setDefault(currentTimeZone);
            System.setProperty(PROP_USER_TIMEZONE, customTimeZone);
        }
    }

    @NotNull
    public static Collection<String> getTimezoneNames() {
        return ZoneId.getAvailableZoneIds().stream()
            .map(TimezoneRegistry::getGMTString).sorted(String::compareTo).collect(Collectors.toList());
    }

    @NotNull
    public static String getGMTString(@NotNull String id) {
        Instant instant = Instant.now();
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.of(id));
        return  String.format("%s (UTC%s)", id, zonedDateTime.getOffset());
    }

    @NotNull
    public static String getUserDefaultTimezone() {
        return "".equals(userDefaultTimezone) ? TimeZone.getDefault().getID() : userDefaultTimezone;
    }

    @NotNull
    public static String extractTimezoneId(@NotNull String timezone) {
        return timezone.split(" ")[0];
    }


}
