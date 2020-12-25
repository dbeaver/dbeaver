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
package org.jkiss.dbeaver.registry;

import org.jkiss.dbeaver.model.navigator.DBNBrowseSettings;

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

        public DBNBrowseSettings getSettings() {
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

}
