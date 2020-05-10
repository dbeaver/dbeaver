/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.core;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.qm.QMConstants;
import org.jkiss.dbeaver.model.qm.QMObjectType;
import org.jkiss.dbeaver.registry.formatter.DataFormatterProfile;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;

public class DBeaverPreferencesInitializer extends AbstractPreferenceInitializer {

    public DBeaverPreferencesInitializer() {
    }

    @Override
    public void initializeDefaultPreferences() {
        boolean isWindows = RuntimeUtils.isPlatformWindows();

        // Init default preferences
        DBPPreferenceStore store = new BundlePreferenceStore(DBeaverActivator.getInstance().getBundle());

        // Resources
        //PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.DEFAULT_RESOURCE_ENCODING, GeneralUtils.UTF8_ENCODING);

        // Agent
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.AGENT_ENABLED, true);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.AGENT_LONG_OPERATION_NOTIFY, isWindows);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.AGENT_LONG_OPERATION_TIMEOUT, 30);

        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.SECURITY_USE_BOUNCY_CASTLE, true);

        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.NAVIGATOR_EDITOR_FULL_NAME, false);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.TOOLBARS_SHOW_GENERAL_ALWAYS, false);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.TOOLBARS_SHOW_EDIT, false);

        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.TEXT_EDIT_UNDO_LEVEL, 200);

        // General UI
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_AUTO_UPDATE_CHECK, true);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_KEEP_DATABASE_EDITORS, true);

        // QM
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_HISTORY_DAYS, 90);
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_ENTRIES_PER_PAGE, 200);
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_OBJECT_TYPES,
            QMObjectType.toString(Arrays.asList(QMObjectType.txn, QMObjectType.query)));
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_QUERY_TYPES,
            DBCExecutionPurpose.USER + "," + DBCExecutionPurpose.USER_FILTERED + "," + DBCExecutionPurpose.USER_SCRIPT);
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_STORE_LOG_FILE, false);
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_LOG_DIRECTORY, GeneralUtils.getMetadataFolder().getAbsolutePath());

        // Logs
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.LOGS_DEBUG_ENABLED, true);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.LOGS_DEBUG_LOCATION,
            "${" + SystemVariablesResolver.VAR_WORKSPACE + "}" + File.separator + ".metadata" + File.separator + "dbeaver-debug.log");
    }

}
