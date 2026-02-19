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

package org.jkiss.dbeaver.model.ai;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * AI completion settings.
 * Datasource-specific settings. Used in prompt generators to generate context info message.
 */
public class AICompletionSettings extends AIContextSettings {

    // Meta parameters
    public static final String AI_DS_EXTENSION = "ai.assistant";
    public static final String AI_META_TRANSFER_CONFIRMED = "ai.meta.transferConfirmed";
    public static final String AI_META_SCOPE = "ai.meta.scope";
    public static final String AI_META_CUSTOM = "ai.meta.customObjects";

    private static final Log log = Log.getLog(AICompletionSettings.class);

    private final DBPDataSourceContainer dataSourceContainer;
    protected final DBPPreferenceStore preferenceStore;

    public AICompletionSettings(@NotNull DBPDataSourceContainer dataSourceContainer) {
        this(getPreferenceStore(), dataSourceContainer);
    }

    private AICompletionSettings(@NotNull DBPPreferenceStore preferenceStore, @NotNull DBPDataSourceContainer dataSourceContainer) {
        this.dataSourceContainer = dataSourceContainer;
        this.preferenceStore = preferenceStore;
        loadSettings();
    }

    @Override
    @NotNull
    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceContainer;
    }

    protected void loadSettings() {
        Object dsConfig = dataSourceContainer.getExtension(AI_DS_EXTENSION);
        if (dsConfig == null) {
            loadLegacySettings();
        } else if (dsConfig instanceof Map map){
            // Load settings from map
            loadSettingsFromMap(map);
        } else {
            log.error("Unknown AI settings format: " + dsConfig);
        }
    }

    public void saveSettings() {
        // Save settings as map
        dataSourceContainer.setExtension(AI_DS_EXTENSION, saveSettingsToMap());
        dataSourceContainer.persistConfiguration();
    }

    // Deprecated methods - kept for backward compatibility

    private void loadLegacySettings() {
        // Legacy configuration from preferences
        settings.confirmed = preferenceStore.getBoolean(getParameterName(AI_META_TRANSFER_CONFIRMED));
        settings.scope = CommonUtils.valueOf(
            AIDatabaseScope.class,
            preferenceStore.getString(getParameterName(AI_META_SCOPE)),
            null);
        String csString = preferenceStore.getString(getParameterName(AI_META_CUSTOM));
        settings.objects = CommonUtils.isEmpty(csString) ? new String[0] : csString.split(",");
    }

    @NotNull
    private static BundlePreferenceStore getPreferenceStore() {
        return new BundlePreferenceStore(AIConstants.AI_MODEL_PLUGIN_ID);
    }

    @NotNull
    protected String getParameterName(@NotNull String postfix) {
        return "ai-" + dataSourceContainer.getId() + "." + postfix;
    }

}
