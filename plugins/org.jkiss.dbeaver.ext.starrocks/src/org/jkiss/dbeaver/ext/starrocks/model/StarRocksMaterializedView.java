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
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.format.SQLFormatUtils;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

/**
 * StarRocks Materialized View - represents a materialized view within a StarRocks database.
 */
public class StarRocksMaterializedView extends StarRocksTableBase implements DBPScriptObject {

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

        public String getDefinition() {
            return definition;
        }

        public void setDefinition(String definition) {
            this.definition = definition;
        }

        @Property(viewable = true, order = 4)
        public String getRefreshType() {
            return refreshType;
        }

        public void setRefreshType(String refreshType) {
            this.refreshType = refreshType;
        }

        @Property(viewable = true, order = 5)
        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
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
        GenericStructContainer container,
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

    @Override
    public String getDDL() {
        return additionalInfo.getDefinition();
    }

    public AdditionalInfo getAdditionalInfo() {
        return additionalInfo;
    }

    @PropertyGroup()
    @LazyProperty(cacheValidator = AdditionalInfoValidator.class)
    public AdditionalInfo getAdditionalInfo(DBRProgressMonitor monitor) throws DBCException {
        synchronized (additionalInfo) {
            if (!additionalInfo.loaded) {
                loadAdditionalInfo(monitor);
            }
            return additionalInfo;
        }
    }

    private void loadAdditionalInfo(DBRProgressMonitor monitor) throws DBCException {
        if (!isPersisted()) {
            additionalInfo.loaded = true;
            return;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load materialized view definition")) { //$NON-NLS-1$
            // Switch to the correct catalog context
            StarRocksCatalog catalog = getStarRocksCatalog();
            if (catalog != null) {
                try (Statement stmt = session.getOriginal().createStatement()) {
                    stmt.execute("SET CATALOG " + DBUtils.getQuotedIdentifier(getDataSource(), catalog.getName())); //$NON-NLS-1$
                }
            }

            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SHOW CREATE MATERIALIZED VIEW " + getFullyQualifiedName(DBPEvaluationContext.DDL))) { //$NON-NLS-1$
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        String definition = JDBCUtils.safeGetString(dbResult, COL_CREATE_MV);
                        if (definition != null) {
                            additionalInfo.setDefinition(
                                SQLFormatUtils.formatSQL(getDataSource(), definition));
                        }
                    }
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
            additionalInfo.loaded = true;
        } catch (SQLException e) {
            throw new DBCException("Error loading materialized view definition", e); //$NON-NLS-1$
        }
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

    public void setObjectDefinitionText(String sourceText) throws DBException {
        getAdditionalInfo().setDefinition(sourceText);
    }
}
