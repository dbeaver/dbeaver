/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.ToNumberPolicy;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;

import java.util.Map;
import java.util.Objects;

/**
 * AI context settings
 */
public abstract class AIContextSettings {

    private static final Gson GSON = new GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
        .create();

    // Persistent settings. Can be saved/loaded from/to some persistent state (e.g. json)
    @NotNull
    protected PersistentSettings settings = new PersistentSettings();

    protected static class PersistentSettings {
        public boolean confirmed;
        public AIDatabaseScope scope;
        public String[] objects;
    }

    public abstract DBPDataSourceContainer getDataSourceContainer();

    public abstract void saveSettings() throws DBException;

    public boolean isMetaTransferConfirmed() {
        return settings.confirmed;
    }

    public void setMetaTransferConfirmed(boolean metaTransferConfirmed) {
        this.settings.confirmed = metaTransferConfirmed;
    }

    public AIDatabaseScope getScope() {
        return settings.scope;
    }

    public void setScope(AIDatabaseScope scope) {
        this.settings.scope = scope;
    }

    public String[] getCustomObjectIds() {
        return settings.objects;
    }

    public void setCustomObjectIds(String[] customObjectIds) {
        this.settings.objects = customObjectIds;
    }

    public void loadSettingsFromMap(Map<String, Object> dsConfig) {
        settings = GSON.fromJson(GSON.toJsonTree(dsConfig), PersistentSettings.class);
    }

    public void loadSettingsFromString(String dsConfig) {
        loadSettingsFromMap(GSON.fromJson(dsConfig, Map.class));
    }

    public Map<String, Object> saveSettingsToMap() {
        return GSON.fromJson(GSON.toJson(settings), Map.class);
    }

    public String saveSettingsToString() {
        return GSON.toJson(saveSettingsToMap());
    }

    public boolean equalsSettings(AIContextSettings that) {
        return settings.confirmed == that.settings.confirmed &&
            settings.scope == that.settings.scope &&
            Objects.deepEquals(settings.objects, that.settings.objects);
    }

}
