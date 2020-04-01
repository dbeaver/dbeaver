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

/**
 * Browse settings
 */
public class DataSourceNavigatorSettings implements DBNBrowseSettings {

    private boolean showSystemObjects;
    private boolean showUtilityObjects;
    private boolean showOnlyEntities;
    private boolean mergeEntities;
    private boolean hideFolders;
    private boolean hideSchemas;

    DataSourceNavigatorSettings() {
    }

    DataSourceNavigatorSettings(DataSourceNavigatorSettings copyFrom) {
        this.showSystemObjects = copyFrom.showSystemObjects;
        this.showUtilityObjects = copyFrom.showUtilityObjects;
        this.showOnlyEntities = copyFrom.showOnlyEntities;
        this.mergeEntities = copyFrom.mergeEntities;
        this.hideFolders = copyFrom.hideFolders;
        this.hideSchemas = copyFrom.hideSchemas;
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

    public void copyFrom(DataSourceNavigatorSettings copyFrom) {
        this.showSystemObjects = copyFrom.showSystemObjects;
        this.showUtilityObjects = copyFrom.showUtilityObjects;
        this.showOnlyEntities = copyFrom.showOnlyEntities;
        this.mergeEntities = copyFrom.mergeEntities;
        this.hideFolders = copyFrom.hideFolders;
        this.hideSchemas = copyFrom.hideSchemas;
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
            this.hideSchemas == source.hideSchemas;
    }

}
