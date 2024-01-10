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
package org.jkiss.dbeaver.ext.oracle.ui.internal;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.utils.PrefUtils;

public class OraclePreferencesInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        // Init default preferences
        DBPPreferenceStore store = new BundlePreferenceStore(OracleUIActivator.getDefault().getBundle());

        // Common
        PrefUtils.setDefaultPreferenceValue(store, OracleConstants.PREF_EXPLAIN_TABLE_NAME, "");
        PrefUtils.setDefaultPreferenceValue(store, OracleConstants.PREF_SUPPORT_ROWID, true);
        PrefUtils.setDefaultPreferenceValue(store, OracleConstants.PREF_DBMS_OUTPUT, true);
        PrefUtils.setDefaultPreferenceValue(store, OracleConstants.PREF_DBMS_READ_ALL_SYNONYMS, true);
        PrefUtils.setDefaultPreferenceValue(store, OracleConstants.PREF_DISABLE_SCRIPT_ESCAPE_PROCESSING ,true);
        PrefUtils.setDefaultPreferenceValue(store, OracleConstants.PROP_USE_RULE_HINT, false);
        PrefUtils.setDefaultPreferenceValue(store, OracleConstants.PROP_USE_META_OPTIMIZER, true);
        PrefUtils.setDefaultPreferenceValue(store, OracleConstants.PROP_METADATA_USE_SIMPLE_CONSTRAINTS, false);
        PrefUtils.setDefaultPreferenceValue(store, OracleConstants.PROP_METADATA_USE_ALTERNATIVE_TABLE_QUERY, false);
        PrefUtils.setDefaultPreferenceValue(store, OracleConstants.PROP_SEARCH_METADATA_IN_SYNONYMS, false);
        PrefUtils.setDefaultPreferenceValue(store, OracleConstants.PROP_SHOW_DATE_AS_DATE, false);
    }
}
