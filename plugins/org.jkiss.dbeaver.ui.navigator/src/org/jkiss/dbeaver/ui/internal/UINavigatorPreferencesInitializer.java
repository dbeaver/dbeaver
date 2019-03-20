/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.internal;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.dbeaver.ui.navigator.database.NavigatorViewBase;
import org.jkiss.dbeaver.utils.PrefUtils;

/**
 * Preference initializer.
 * Note: in order to active this class we have to initiate preferences in activator (see UINavigatorActivator class)
 */
public class UINavigatorPreferencesInitializer extends AbstractPreferenceInitializer {

    public UINavigatorPreferencesInitializer() {
    }

    @Override
    public void initializeDefaultPreferences() {
        // Init default preferences
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        // Navigator
        PrefUtils.setDefaultPreferenceValue(store, NavigatorPreferences.NAVIGATOR_EXPAND_ON_CONNECT, false);
        PrefUtils.setDefaultPreferenceValue(store, NavigatorPreferences.NAVIGATOR_SORT_ALPHABETICALLY, false);
        PrefUtils.setDefaultPreferenceValue(store, NavigatorPreferences.NAVIGATOR_SORT_FOLDERS_FIRST, true);
        PrefUtils.setDefaultPreferenceValue(store, NavigatorPreferences.NAVIGATOR_SYNC_EDITOR_DATASOURCE, false);
        PrefUtils.setDefaultPreferenceValue(store, NavigatorPreferences.NAVIGATOR_REFRESH_EDITORS_ON_OPEN, false);

        PrefUtils.setDefaultPreferenceValue(store, NavigatorPreferences.NAVIGATOR_GROUP_BY_DRIVER, false);
        PrefUtils.setDefaultPreferenceValue(store, NavigatorPreferences.NAVIGATOR_EDITOR_SHOW_TABLE_GRID, true);
        PrefUtils.setDefaultPreferenceValue(store, NavigatorPreferences.NAVIGATOR_OBJECT_DOUBLE_CLICK, NavigatorViewBase.DoubleClickBehavior.EDIT.name());
        PrefUtils.setDefaultPreferenceValue(store, NavigatorPreferences.NAVIGATOR_CONNECTION_DOUBLE_CLICK, NavigatorViewBase.DoubleClickBehavior.EXPAND.name());
        PrefUtils.setDefaultPreferenceValue(store, NavigatorPreferences.NAVIGATOR_SHOW_SQL_PREVIEW, true);
        PrefUtils.setDefaultPreferenceValue(store, NavigatorPreferences.NAVIGATOR_SHOW_OBJECT_TIPS, true);
        PrefUtils.setDefaultPreferenceValue(store, NavigatorPreferences.NAVIGATOR_LONG_LIST_FETCH_SIZE, 5000);
        PrefUtils.setDefaultPreferenceValue(store, NavigatorPreferences.ENTITY_EDITOR_DETACH_INFO, true);

    }


}
