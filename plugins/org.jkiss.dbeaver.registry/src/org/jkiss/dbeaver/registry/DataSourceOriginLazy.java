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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceOrigin;
import org.jkiss.dbeaver.model.DBPDataSourceOriginProvider;
import org.jkiss.dbeaver.model.DBPImage;

import java.util.Map;

/**
 * DataSourceOriginProviderLocal
 */
class DataSourceOriginLazy implements DBPDataSourceOrigin
{
    private String originId;
    private Map<String, Object> originProperties;

    public DataSourceOriginLazy(String originId, Map<String, Object> originProperties) {
        this.originId = originId;
        this.originProperties = originProperties;
    }

    @NotNull
    @Override
    public String getId() {
        return originId;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return originId;
    }

    @Nullable
    @Override
    public DBPImage getIcon() {
        return DBIcon.TYPE_UNKNOWN;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @NotNull
    @Override
    public Map<String, Object> getConfiguration() {
        return originProperties;
    }

    @Override
    public String toString() {
        return getId();
    }

    @NotNull
    DBPDataSourceOrigin resolveRealOrigin() {
        // Loaded from configuration
        // Instantiate in lazy mode
        DBPDataSourceOrigin origin = null;
        DBPDataSourceOriginProvider originProvider = DataSourceProviderRegistry.getInstance().getDataSourceOriginProvider(originId);
        if (originProvider != null) {
            origin = originProvider.getOrigin(originProperties);
        }
        if (origin == null) {
            origin = DataSourceOriginLocal.INSTANCE;
        }
        return origin;
    }

}
