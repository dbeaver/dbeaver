/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry;

import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.navigator.DBNBrowseSettings;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.utils.CommonUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Browse settings
 */
public class DataSourceNavigatorSettings implements DBNBrowseSettings {

    public static final Map<String, Preset> PRESETS = new LinkedHashMap<>();

    public static final Preset PRESET_SIMPLE = new Preset("simple", "Simple", "Shows only tables");
    public static final Preset PRESET_FULL = new Preset("advanced", "Advanced", "Shows all database objects");
    public static final Preset PRESET_CUSTOM = new Preset("custom", "Custom", "User configuration");

    public static class Preset {
        private final String id;
        private final String name;
        private final String description;
        private final DataSourceNavigatorSettings settings = new DataSourceNavigatorSettings();

        public Preset(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public DataSourceNavigatorSettings getSettings() {
            return settings;
        }
    }

    static {
        PRESET_SIMPLE.settings.setShowOnlyEntities(true);
        PRESET_SIMPLE.settings.setHideFolders(true);
        PRESET_SIMPLE.settings.setHideVirtualModel(true);

        PRESET_FULL.settings.setShowSystemObjects(true);

        PRESETS.put(PRESET_SIMPLE.name, PRESET_SIMPLE);
        PRESETS.put(PRESET_FULL.name, PRESET_FULL);
        PRESETS.put(PRESET_CUSTOM.name, PRESET_CUSTOM);
    }

    private boolean showSystemObjects;
    private boolean showUtilityObjects;
    private boolean showOnlyEntities;
    private boolean mergeEntities;
    private boolean hideFolders;
    private boolean hideSchemas;
    private boolean hideVirtualModel;

    public DataSourceNavigatorSettings() {
    }

    public DataSourceNavigatorSettings(DBNBrowseSettings copyFrom) {
        this.showSystemObjects = copyFrom.isShowSystemObjects();
        this.showUtilityObjects = copyFrom.isShowUtilityObjects();
        this.showOnlyEntities = copyFrom.isShowOnlyEntities();
        this.mergeEntities = copyFrom.isMergeEntities();
        this.hideFolders = copyFrom.isHideFolders();
        this.hideSchemas = copyFrom.isHideSchemas();
        this.hideVirtualModel = copyFrom.isHideVirtualModel();
    }

    @Override
    public boolean isShowSystemObjects() {
        return showSystemObjects;
    }

    public void setShowSystemObjects(boolean showSystemObjects) {
        this.showSystemObjects = showSystemObjects;
    }

    @Override
    public boolean isShowUtilityObjects() {
        return showUtilityObjects;
    }

    public void setShowUtilityObjects(boolean showUtilityObjects) {
        this.showUtilityObjects = showUtilityObjects;
    }

    @Override
    public boolean isShowOnlyEntities() {
        return showOnlyEntities;
    }

    public void setShowOnlyEntities(boolean showOnlyEntities) {
        this.showOnlyEntities = showOnlyEntities;
    }

    @Override
    public boolean isMergeEntities() {
        return mergeEntities;
    }

    public void setMergeEntities(boolean mergeEntities) {
        this.mergeEntities = mergeEntities;
    }

    @Override
    public boolean isHideFolders() {
        return hideFolders;
    }

    public void setHideFolders(boolean hideFolders) {
        this.hideFolders = hideFolders;
    }

    @Override
    public boolean isHideSchemas() {
        return hideSchemas;
    }

    public void setHideSchemas(boolean hideSchemas) {
        this.hideSchemas = hideSchemas;
    }

    @Override
    public boolean isHideVirtualModel() {
        return hideVirtualModel;
    }

    public void setHideVirtualModel(boolean hideVirtualModel) {
        this.hideVirtualModel = hideVirtualModel;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DataSourceNavigatorSettings)) {
            return false;
        }
        DataSourceNavigatorSettings source = (DataSourceNavigatorSettings) obj;
        return this.showSystemObjects == source.showSystemObjects &&
            this.showUtilityObjects == source.showUtilityObjects &&
            this.showOnlyEntities == source.showOnlyEntities &&
            this.mergeEntities == source.mergeEntities &&
            this.hideFolders == source.hideFolders &&
            this.hideSchemas == source.hideSchemas &&
            this.hideVirtualModel == source.hideVirtualModel;
    }

    public static final String DEFAULT_NAVIGATOR_SETTINGS_PRESET = "navigator.settings.default.preset";
    private static final String DEFAULT_SHOW_SYSTEM_OBJECTS = "navigator.settings.default.showSystemObjects";
    private static final String DEFAULT_SHOW_UTILITY_OBJECTS = "navigator.settings.default.showUtilityObjects";
    private static final String DEFAULT_SHOW_ONLY_ENTITIES = "navigator.settings.default.showOnlyEntities";
    private static final String DEFAULT_MERGE_ENTITIES = "navigator.settings.default.mergeEntities";
    private static final String DEFAULT_HIDE_FOLDERS = "navigator.settings.default.hideFolders";
    private static final String DEFAULT_MERGE_SCHEMAS = "navigator.settings.default.hideSchemas";
    private static final String DEFAULT_HIDE_VIRTUAL_MODEL = "navigator.settings.default.hideVirtualModel";

    public static DBNBrowseSettings getDefaultSettings() {
        DBPPreferenceStore preferences = ModelPreferences.getPreferences();

        String defPreset = preferences.getString(DEFAULT_NAVIGATOR_SETTINGS_PRESET);
        if (!CommonUtils.isEmpty(defPreset)) {
            for (DataSourceNavigatorSettings.Preset p : DataSourceNavigatorSettings.PRESETS.values()) {
                if (p.getId().equals(defPreset)) {
                    return p.getSettings();
                }
            }
        }
        // Custom settings
        DataSourceNavigatorSettings settings = new DataSourceNavigatorSettings();
        settings.setShowSystemObjects(preferences.getBoolean(DEFAULT_SHOW_SYSTEM_OBJECTS));
        settings.setShowUtilityObjects(preferences.getBoolean(DEFAULT_SHOW_UTILITY_OBJECTS));
        settings.setShowOnlyEntities(preferences.getBoolean(DEFAULT_SHOW_ONLY_ENTITIES));
        settings.setMergeEntities(preferences.getBoolean(DEFAULT_MERGE_ENTITIES));
        settings.setHideFolders(preferences.getBoolean(DEFAULT_HIDE_FOLDERS));
        settings.setHideSchemas(preferences.getBoolean(DEFAULT_MERGE_SCHEMAS));
        settings.setHideVirtualModel(preferences.getBoolean(DEFAULT_HIDE_VIRTUAL_MODEL));
        return settings;
    }

    public static void setDefaultSettings(DBNBrowseSettings settings) {
        // Save preset
        DBPPreferenceStore preferences = ModelPreferences.getPreferences();

        String presetId = null;
        for (DataSourceNavigatorSettings.Preset p : DataSourceNavigatorSettings.PRESETS.values()) {
            if (p.getSettings().equals(settings)) {
                presetId = p.getId();
                break;
            }
        }

        if (CommonUtils.isEmptyTrimmed(presetId)) {
            preferences.setValue(DEFAULT_NAVIGATOR_SETTINGS_PRESET, "");
        } else {
            preferences.setValue(DEFAULT_NAVIGATOR_SETTINGS_PRESET, presetId);
        }

        // Save custom settings
        preferences.setValue(DEFAULT_SHOW_SYSTEM_OBJECTS, settings.isShowSystemObjects());
        preferences.setValue(DEFAULT_SHOW_UTILITY_OBJECTS, settings.isShowUtilityObjects());
        preferences.setValue(DEFAULT_SHOW_ONLY_ENTITIES, settings.isShowOnlyEntities());
        preferences.setValue(DEFAULT_MERGE_ENTITIES, settings.isMergeEntities());
        preferences.setValue(DEFAULT_HIDE_FOLDERS, settings.isHideFolders());
        preferences.setValue(DEFAULT_MERGE_SCHEMAS, settings.isHideSchemas());
        preferences.setValue(DEFAULT_HIDE_VIRTUAL_MODEL, settings.isHideVirtualModel());
    }

}
