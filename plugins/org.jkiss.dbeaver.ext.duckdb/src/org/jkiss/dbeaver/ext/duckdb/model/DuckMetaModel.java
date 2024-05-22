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
package org.jkiss.dbeaver.ext.duckdb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.format.SQLFormatUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Map;

public class DuckMetaModel extends GenericMetaModel {

    private static final Log log = Log.getLog(DuckMetaModel.class);

    @Override
    public JDBCBasicDataTypeCache<GenericStructContainer, ? extends JDBCDataType> createDataTypeCache(
        @NotNull GenericStructContainer container
    ) {
        return new DuckDataTypeCache(container);
    }

    @Override
    public String getTableDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericTableBase sourceObject,
        @NotNull Map<String, Object> options
    ) throws DBException {
        String objectDDL = getObjectDDL(monitor, sourceObject, "duckdb_tables()", "table_name");
        if (objectDDL == null) {
            return super.getTableDDL(monitor, sourceObject, options);
        }
        return objectDDL;
    }

    @Override
    public String getViewDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericView sourceObject,
        @NotNull Map<String, Object> options
    ) throws DBException {
        String objectDDL = getObjectDDL(monitor, sourceObject, "duckdb_views()", "view_name");
        if (objectDDL == null) {
            return "-- DuckDB view definition not found";
        }
        return objectDDL;
    }

    @Nullable
    private String getObjectDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericTableBase sourceObject,
        @NotNull String sysViewName,
        @NotNull String objectColumnName
    ) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read DuckDB object source")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT sql FROM " + sysViewName + " WHERE database_name = ? AND schema_name = ? AND " + objectColumnName + " = ?")) {
                dbStat.setString(1, sourceObject.getContainer().getCatalog().getName());
                dbStat.setString(2, sourceObject.getContainer().getName());
                dbStat.setString(3, sourceObject.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.nextRow()) {
                        String string = dbResult.getString(1);
                        if (CommonUtils.isNotEmpty(string)) {
                            return SQLFormatUtils.formatSQL(dataSource, string);
                        }
                    }
                    return null;
                }
            }
        } catch (SQLException e) {
            log.warn("Can't read object " + sourceObject.getName() + " definition from the database", e);
            return null;
        }
    }

    @Override
    public boolean isTableCommentEditable() {
        return true;
    }

    @Override
    public boolean supportsSequences(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public JDBCStatement prepareSequencesLoadStatement(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer container
    ) throws SQLException {
        JDBCPreparedStatement statement = session.prepareStatement(
            "SELECT * FROM duckdb_sequences() WHERE database_name = ? AND schema_name = ?");
        statement.setString(1, container.getCatalog().getName());
        statement.setString(2, container.getName());
        return statement;
    }

    @Override
    public GenericSequence createSequenceImpl(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer container,
        @NotNull JDBCResultSet dbResult
    ) {
        String name = JDBCUtils.safeGetString(dbResult, "sequence_name");
        if (CommonUtils.isEmpty(name)) {
            log.debug("Skip a sequence with an empty name.");
            return null;
        }
        String description = JDBCUtils.safeGetString(dbResult, "comment");
        return new DuckDBSequence(
            container,
            name,
            description,
            JDBCUtils.safeGetLong(dbResult, "last_value"),
            JDBCUtils.safeGetLong(dbResult, "min_value"),
            JDBCUtils.safeGetLong(dbResult, "max_value"),
            JDBCUtils.safeGetLong(dbResult, "increment_by"),
            dbResult);
    }

    @Override
    public GenericDataSource createDataSourceImpl(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new DuckDBDataSource(monitor, container, this);
    }
}
