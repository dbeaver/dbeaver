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
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.TimezoneUtils;

import java.util.TimeZone;

public class TimezoneRegistry {

    private static DBPPreferenceStore preferenceStore;
    public static final String DEFAULT_VALUE = "N/A";
    private TimezoneRegistry() {
    }

    public static void setUsedTime(@NotNull String text) {
        initStore();
        if (!text.equals(DEFAULT_VALUE)) {
            preferenceStore.setValue(DBeaverPreferences.CLIENT_TIMEZONE, TimezoneUtils.extractTimezoneId(text));
            TimeZone.setDefault(TimeZone.getTimeZone(TimezoneUtils.extractTimezoneId(text)));
        } else {
            preferenceStore.setValue(DBeaverPreferences.CLIENT_TIMEZONE, text);
            TimeZone.setDefault(null);
        }
    }

    public static void overrideTimezone() {
        initStore();
        final String timezone = preferenceStore.getString(DBeaverPreferences.CLIENT_TIMEZONE);
        if (timezone != null && !timezone.equals(DEFAULT_VALUE)) {
            TimeZone.setDefault(TimeZone.getTimeZone(timezone));
        }
    }

    private static void initStore() {
        if (preferenceStore == null) {
            preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        }
    }

}
