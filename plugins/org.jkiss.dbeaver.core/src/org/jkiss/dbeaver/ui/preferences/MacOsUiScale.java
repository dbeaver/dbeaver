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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;

public final class MacOsUiScale {
    public static final String ENABLE_PROPERTY = "dbeaver.ui.liveScale";
    public static final String PREFERENCE = "ui.experimental.liveScale";
    public static final int DEFAULT_SCALE = 100;
    public static final int MIN_SCALE = 75;
    public static final int MAX_SCALE = 200;

    private static final Log log = Log.getLog(MacOsUiScale.class);

    private MacOsUiScale() {
    }

    public static boolean isEnabled() {
        return "cocoa".equals(SWT.getPlatform()) && Boolean.getBoolean(ENABLE_PROPERTY) && MacOsNativeScale.isAvailable();
    }

    public static int getSavedScale(@NotNull DBPPreferenceStore store) {
        int saved = store.getInt(PREFERENCE);
        return saved >= MIN_SCALE && saved <= MAX_SCALE ? saved : DEFAULT_SCALE;
    }

    public static void initialize(@NotNull Display display, @NotNull DBPPreferenceStore store) {
        String error = System.getProperty("dbeaver.ui.nativeScale.error");
        if (error != null) {
            log.error("Native UI scale is unavailable: " + error);
        }
        if (isEnabled()) {
            apply(display, getSavedScale(store));
        }
    }

    public static void apply(@NotNull Display display, int percentage) {
        if (isEnabled()) {
            MacOsNativeScale.apply(display, Math.max(MIN_SCALE, Math.min(MAX_SCALE, percentage)));
        }
    }
}
