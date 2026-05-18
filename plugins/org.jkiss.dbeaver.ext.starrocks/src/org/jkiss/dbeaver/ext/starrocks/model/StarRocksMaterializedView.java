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
package org.jkiss.dbeaver.ext.starrocks.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.starrocks.StarRocksUtils;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Map;

/**
 * StarRocks Materialized View - represents a materialized view within a StarRocks database.
 */
public class StarRocksMaterializedView extends StarRocksViewBase implements DBPScriptObject {

    private static final String COL_CREATE_MV = "Create Materialized View"; //$NON-NLS-1$

    private final AdditionalInfo additionalInfo = new AdditionalInfo();

    public static class AdditionalInfo {
        private volatile boolean loaded = false;
        private String definition;
        private String refreshType;
        private String status;

        public boolean isLoaded() {
            return loaded;
        }

        @Nullable
        public String getDefinition() {
            return definition;
        }

        public void setDefinition(@Nullable String definition) {
            this.definition = definition;
        }

        @Nullable
        @Property(viewable = true, order = 4)
        public String getRefreshType() {
            return refreshType;
        }

        public void setRefreshType(@Nullable String refreshType) {
            this.refreshType = refreshType;
        }

        @Nullable
        @Property(viewable = true, order = 5)
        public String getStatus() {
            return status;
        }

        public void setStatus(@Nullable String status) {
            this.status = status;
        }
    }

    public static class AdditionalInfoValidator implements IPropertyCacheValidator<StarRocksMaterializedView> {
        @Override
        public boolean isPropertyCached(@NotNull StarRocksMaterializedView object, @NotNull Object propertyId) {
            return object.additionalInfo.loaded;
        }
    }

    public StarRocksMaterializedView(
        @NotNull GenericStructContainer container,
        @Nullable String viewName,
        @Nullable String tableType,
        @Nullable JDBCResultSet dbResult
    ) {
        super(container, viewName, tableType, dbResult);
    }

    @Override
    public boolean isView() {
        return true;
    }

    @Nullable
    @Override
    public String getDDL() {
        return additionalInfo.getDefinition();
    }

    @NotNull
    public AdditionalInfo getAdditionalInfo() {
        return additionalInfo;
    }

    @NotNull
    @PropertyGroup()
    @LazyProperty(cacheValidator = AdditionalInfoValidator.class)
    public AdditionalInfo getAdditionalInfo(@NotNull DBRProgressMonitor monitor) throws DBCException {
        synchronized (additionalInfo) {
            if (!additionalInfo.loaded) {
                loadAdditionalInfo(monitor);
            }
            return additionalInfo;
        }
    }

    private void loadAdditionalInfo(@NotNull DBRProgressMonitor monitor) throws DBCException {
        if (!isPersisted()) {
            additionalInfo.loaded = true;
            return;
        }
        String definition = StarRocksUtils.loadShowCreateDDL(
            monitor, this, "Load materialized view definition", "SHOW CREATE MATERIALIZED VIEW", COL_CREATE_MV); //$NON-NLS-1$ //$NON-NLS-2$
        additionalInfo.setDefinition(definition);
        additionalInfo.loaded = true;
    }

    @NotNull
    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options) throws DBException {
        String definition = getAdditionalInfo(monitor).getDefinition();
        if (definition == null && !isPersisted()) {
            return "";
        }
        return definition != null ? definition : "";
    }

    public void setObjectDefinitionText(@Nullable String sourceText) {
        getAdditionalInfo().setDefinition(sourceText);
    }
}
