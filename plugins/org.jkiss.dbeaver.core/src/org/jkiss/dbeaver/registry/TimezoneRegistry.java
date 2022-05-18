/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class TimezoneRegistry {

    public static final String DEFAULT_VALUE = "N/A";
    private TimezoneRegistry() {
    }

    public static void setDefaultZone(@Nullable ZoneId id) {
        DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        if (id != null) {
            preferenceStore.setValue(DBeaverPreferences.CLIENT_TIMEZONE, id.getId());
            TimeZone.setDefault(TimeZone.getTimeZone(id));
        } else {
            preferenceStore.setToDefault(DBeaverPreferences.CLIENT_TIMEZONE);
            TimeZone.setDefault(null);
        }
        UIUtils.updateTimezoneBarIfExists();
    }

    public static void overrideTimezone() {
        DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        final String timezone = preferenceStore.getString(DBeaverPreferences.CLIENT_TIMEZONE);
        if (timezone != null && !timezone.equals(DEFAULT_VALUE)) {
            TimeZone.setDefault(TimeZone.getTimeZone(timezone));
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


    public static String extractTimezoneId(@NotNull String timezone) {
        return timezone.split(" ")[0];
    }


}
