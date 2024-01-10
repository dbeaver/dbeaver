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
package org.jkiss.dbeaver.tools.transfer.internal;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.tools.transfer.DTConstants;
import org.jkiss.dbeaver.utils.PrefUtils;

public class DataTransferPreferencesInitializer extends AbstractPreferenceInitializer {

    public DataTransferPreferencesInitializer() {
    }

    @Override
    public void initializeDefaultPreferences() {
        final DBPPreferenceStore store = new BundlePreferenceStore(DTActivator.getDefault().getBundle());
        PrefUtils.setDefaultPreferenceValue(store, DTConstants.PREF_NAME_CASE_MAPPING, 0);
        PrefUtils.setDefaultPreferenceValue(store, DTConstants.PREF_REPLACE_MAPPING, 0);
        PrefUtils.setDefaultPreferenceValue(store, DTConstants.PREF_MAX_TYPE_LENGTH, DTConstants.DEFAULT_MAX_TYPE_LENGTH);
        PrefUtils.setDefaultPreferenceValue(store, DTConstants.PREF_SAVE_LOCAL_SETTINGS, true);
    }
}
