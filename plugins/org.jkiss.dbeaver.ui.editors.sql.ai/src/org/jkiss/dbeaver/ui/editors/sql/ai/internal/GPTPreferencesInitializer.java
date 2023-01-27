/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.ai.internal;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.jkiss.dbeaver.model.ai.GPTPreferences;
import org.jkiss.dbeaver.model.ai.internal.GPTModel;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.PrefUtils;

public class GPTPreferencesInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        PrefUtils.setDefaultPreferenceValue(store, GPTPreferences.GPT_MODEL, GPTModel.CODE_DAVINCI.getName());
        PrefUtils.setDefaultPreferenceValue(store, GPTPreferences.GPT_MODEL_MAX_TOKENS, 1000);
        PrefUtils.setDefaultPreferenceValue(store, GPTPreferences.GPT_MODEL_TEMPERATURE, 0.0f);
        PrefUtils.setDefaultPreferenceValue(store, GPTPreferences.GPT_EXECUTE_IMMEDIATELY, false);
        PrefUtils.setDefaultPreferenceValue(store, GPTPreferences.GPT_LOG_QUERY, false);
        PrefUtils.setDefaultPreferenceValue(store, GPTPreferences.GPT_MAX_TABLES, 200);
    }
}
