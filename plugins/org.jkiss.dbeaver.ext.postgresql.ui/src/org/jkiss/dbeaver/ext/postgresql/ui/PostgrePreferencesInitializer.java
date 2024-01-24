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
package org.jkiss.dbeaver.ext.postgresql.ui;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUIActivator;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.utils.PrefUtils;

public class PostgrePreferencesInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        // Init default preferences
        DBPPreferenceStore store = new BundlePreferenceStore(PostgreUIActivator.getDefault().getBundle());

        // Common
        PrefUtils.setDefaultPreferenceValue(store, PostgreConstants.PROP_SHOW_NON_DEFAULT_DB, false);
        PrefUtils.setDefaultPreferenceValue(store, PostgreConstants.PROP_SHOW_TEMPLATES_DB, false);
        PrefUtils.setDefaultPreferenceValue(store, PostgreConstants.PROP_SHOW_UNAVAILABLE_DB, false);
        PrefUtils.setDefaultPreferenceValue(store, PostgreConstants.PROP_SHOW_DATABASE_STATISTICS, false);
        PrefUtils.setDefaultPreferenceValue(store, PostgreConstants.PROP_READ_ALL_DATA_TYPES, false);
        PrefUtils.setDefaultPreferenceValue(store, PostgreConstants.PROP_READ_KEYS_WITH_COLUMNS, false);

        PrefUtils.setDefaultPreferenceValue(store, PostgreConstants.PROP_DD_PLAIN_STRING, 1);
        PrefUtils.setDefaultPreferenceValue(store, PostgreConstants.PROP_DD_TAG_STRING, 1);
    }
}
