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
package org.jkiss.dbeaver.ext.altibase.internal;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.PrefUtils;

public class AltibasePreferencesInitializer extends AbstractPreferenceInitializer {

    public AltibasePreferencesInitializer() {
    }

    @Override
    public void initializeDefaultPreferences() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        PrefUtils.setDefaultPreferenceValue(store, AltibaseConstants.PREF_EXPLAIN_PLAN_TYPE, 0);
        PrefUtils.setDefaultPreferenceValue(store, AltibaseConstants.PREF_DBMS_OUTPUT, true);
        PrefUtils.setDefaultPreferenceValue(store, AltibaseConstants.PREF_PLAN_PREFIX, true);
    }

} 