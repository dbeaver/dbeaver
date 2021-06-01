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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.auth.DBASessionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

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
    public String getType() {
        return originId;
    }

    @Nullable
    @Override
    public String getSubType() {
        return null;
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

    @Nullable
    @Override
    public DBPObject getObjectDetails(@NotNull DBRProgressMonitor monitor, @NotNull DBASessionContext sessionContext, @NotNull DBPDataSourceContainer dataSource) throws DBException {
        DBPDataSourceOrigin realOrigin = resolveRealOrigin();
        return realOrigin == null ? null : realOrigin.getObjectDetails(monitor, sessionContext, dataSource);
    }

    @Override
    public String toString() {
        return getType();
    }

    @Nullable
    DBPDataSourceOrigin resolveRealOrigin() {
        // Loaded from configuration
        // Instantiate in lazy mode
        DBPDataSourceOriginProvider originProvider = DataSourceProviderRegistry.getInstance().getDataSourceOriginProvider(originId);
        if (originProvider != null) {
            return originProvider.getOrigin(originProperties);
        }
        return null;
    }

}
