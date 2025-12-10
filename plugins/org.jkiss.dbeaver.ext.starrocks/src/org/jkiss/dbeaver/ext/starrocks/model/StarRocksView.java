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
import org.jkiss.dbeaver.model.struct.rdb.DBSView;

import java.sql.SQLException;
import java.util.Map;

/**
 * StarRocks View - represents a view within a StarRocks database.
 */
public class StarRocksView extends StarRocksTableBase implements DBSView, DBPScriptObject {

    public static class AdditionalInfo {
        private volatile boolean loaded = false;
        private String definition;
        private String definer;

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
        public String getDefiner() {
            return definer;
        }

        public void setDefiner(String definer) {
            this.definer = definer;
        }
    }

    public static class AdditionalInfoValidator implements IPropertyCacheValidator<StarRocksView> {
        @Override
        public boolean isPropertyCached(@NotNull StarRocksView object, @NotNull Object propertyId) {
            return object.additionalInfo.loaded;
        }
    }

    private final AdditionalInfo additionalInfo = new AdditionalInfo();

    public StarRocksView(StarRocksDatabase database, String viewName) {
        super(database, viewName, true);
    }

    @Override
    public boolean isView() {
        return true;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
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
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load view definition")) {
            // Switch to the correct catalog context
            getContainer().switchToCatalogContext(session);

            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SHOW CREATE VIEW " + getFullyQualifiedName(DBPEvaluationContext.DDL))) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        String definition = JDBCUtils.safeGetString(dbResult, "Create View");
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
            throw new DBCException("Error loading view definition", e);
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
