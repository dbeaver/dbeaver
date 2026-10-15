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
package org.jkiss.dbeaver.model.sync;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

/**
 * Tells whether a component or a project is allowed to be synchronized.
 */
public class DBPSyncSettings {

    private static final String PREF_UNIT_PREFIX = "datadam.sync.";
    private static final String PROP_PROJECT_SYNC = "datadam.sync";

    private DBPSyncSettings() {
    }

    public static boolean isEnabled(@NotNull DBPSyncUnit unit) {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        String name = PREF_UNIT_PREFIX + unit.getId();
        return store.contains(name) ? store.getBoolean(name) : unit.isEnabledByDefault();
    }

    public static boolean isEnabled(@NotNull DBPProject project) {
        return CommonUtils.toBoolean(project.getProjectProperty(PROP_PROJECT_SYNC), true);
    }

    public static void setEnabled(@NotNull DBPSyncUnit unit, boolean enabled) {
        DBWorkbench.getPlatform().getPreferenceStore().setValue(PREF_UNIT_PREFIX + unit.getId(), enabled);
    }

    public static void setEnabled(@NotNull DBPProject project, boolean enabled) {
        project.setProjectProperty(PROP_PROJECT_SYNC, enabled);
    }
}
