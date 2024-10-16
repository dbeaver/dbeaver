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
package org.jkiss.dbeaver.ui.editors.sql.ai.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.jkiss.dbeaver.model.ai.AICompletionConstants;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.openai.GPTModel;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.PrefUtils;

public class AIPreferencesInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        PrefUtils.setDefaultPreferenceValue(store, AICompletionConstants.AI_COMPLETION_EXECUTE_IMMEDIATELY, false);
        PrefUtils.setDefaultPreferenceValue(store, AICompletionConstants.AI_SEND_DESCRIPTION, false);
        PrefUtils.setDefaultPreferenceValue(store, AICompletionConstants.AI_SEND_TYPE_INFO, false);
        PrefUtils.setDefaultPreferenceValue(store, AICompletionConstants.AI_COMPLETION_MAX_CHOICES, 1);
        PrefUtils.setDefaultPreferenceValue(store, AICompletionConstants.AI_INCLUDE_SOURCE_TEXT_IN_QUERY_COMMENT, true);

        PrefUtils.setDefaultPreferenceValue(store, AIConstants.GPT_MODEL, GPTModel.GPT_TURBO.getName());
        PrefUtils.setDefaultPreferenceValue(store, AIConstants.AI_TEMPERATURE, 0.0f);
        PrefUtils.setDefaultPreferenceValue(store, AIConstants.AI_LOG_QUERY, false);
    }
}
