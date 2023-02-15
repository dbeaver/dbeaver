/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class TimezoneRegistry {

    private static String userDefaultTimezone = "";

    private TimezoneRegistry() {
    }

    public static void setDefaultZone(@Nullable ZoneId id) {
        DBPPreferenceStore preferenceStore = ModelPreferences.getPreferences();
        if (id != null) {
            if (!TimeZone.getDefault().getID().equals(id.getId())) {
                TimeZone.setDefault(TimeZone.getTimeZone(id));
                System.setProperty("user.timezone", id.getId());
                preferenceStore.setValue(ModelPreferences.CLIENT_TIMEZONE, id.getId());
            }
        } else {
            if (!TimeZone.getDefault().getID().equals(userDefaultTimezone)) {
                TimeZone.setDefault(TimeZone.getTimeZone(userDefaultTimezone));
                System.setProperty("user.timezone", userDefaultTimezone);
                preferenceStore.setToDefault(ModelPreferences.CLIENT_TIMEZONE);
            }
        }
    }

    public static void overrideTimezone() {
        userDefaultTimezone = System.getProperty("user.timezone");
        System.setProperty("user.old.timezone", userDefaultTimezone);
        DBPPreferenceStore preferenceStore = ModelPreferences.getPreferences();
        final String timezone = preferenceStore.getString(ModelPreferences.CLIENT_TIMEZONE);
        if (timezone != null && !timezone.equals(DBConstants.DEFAULT_TIMEZONE)) {
            TimeZone.setDefault(TimeZone.getTimeZone(timezone));
            System.setProperty("user.timezone", timezone);
        }
    }

    @NotNull
    public static List<String> getTimezoneNames() {
        return ZoneId.getAvailableZoneIds().stream().map(TimezoneRegistry::getGMTString).sorted(String::compareTo).collect(Collectors.toList());
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
