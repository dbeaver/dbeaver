/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
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
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBStructUtils;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

public class SQLServerExternalTable extends SQLServerTableBase {
    private final AdditionalInfo additionalInfo = new AdditionalInfo();

    public SQLServerExternalTable(@NotNull SQLServerSchema catalog, @NotNull ResultSet dbResult, @NotNull String name) {
        super(catalog, dbResult, name);
    }

    public SQLServerExternalTable(@NotNull SQLServerSchema schema) {
        super(schema);
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
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table status")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT et.location,eds.name AS data_source_name, eff.name AS file_format_name\n" +
                    "FROM " + SQLServerUtils.getSystemTableName(getDatabase(), "external_tables") + " et\n" +
                    "LEFT JOIN " + SQLServerUtils.getSystemTableName(getDatabase(), "external_data_sources") + " eds ON eds.data_source_id=et.data_source_id\n" +
                    "LEFT JOIN " + SQLServerUtils.getSystemTableName(getDatabase(), "external_file_formats") + " eff ON eff.file_format_id=et.file_format_id\n" +
                    "WHERE et.object_id=?"
            )) {
                dbStat.setLong(1, getObjectId());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        additionalInfo.externalDataSource = JDBCUtils.safeGetString(dbResult, "data_source_name");
                        additionalInfo.externalFileFormat = JDBCUtils.safeGetString(dbResult, "file_format_name");
                        additionalInfo.externalLocation = JDBCUtils.safeGetString(dbResult, "location");
                    }
                    additionalInfo.loaded = true;
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }
    }

    @Override
    boolean supportsTriggers() {
        return false;
    }

    @Override
    public boolean isView() {
        return false;
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityAssociation> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityAssociation> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Nullable
    @Override
    public Collection<? extends DBSTableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Override
    public boolean supportsObjectDefinitionOption(String option) {
        return false;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return DBStructUtils.generateTableDDL(monitor, this, options, false);
    }

    @Override
    public void setObjectDefinitionText(String source) {
        // not implemented
    }

    public static class AdditionalInfoValidator implements IPropertyCacheValidator<SQLServerExternalTable> {
        @Override
        public boolean isPropertyCached(SQLServerExternalTable object, Object propertyId) {
            return object.additionalInfo.loaded;
        }
    }

    public static class AdditionalInfo {
        private boolean loaded;
        private String externalDataSource;
        private String externalFileFormat;
        private String externalLocation;

        @NotNull
        @Property(viewable = true, order = 7)
        public String getExternalDataSource() {
            return externalDataSource;
        }

        @Nullable
        @Property(viewable = true, order = 8)
        public String getExternalFileFormat() {
            return externalFileFormat;
        }

        @NotNull
        @Property(viewable = true, order = 9)
        public String getExternalLocation() {
            return externalLocation;
        }
    }
}
