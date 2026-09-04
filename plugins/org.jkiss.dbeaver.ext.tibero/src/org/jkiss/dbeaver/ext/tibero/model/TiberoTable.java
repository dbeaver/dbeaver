/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.tibero.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.OracleDDLFormat;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleTable;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableBase;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableColumn;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableConstraint;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableForeignKey;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableIndex;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableTrigger;
import org.jkiss.dbeaver.ext.oracle.model.OracleUtils;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBStructUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class TiberoTable extends OracleTable {

    private volatile List<OracleTableColumn> columnsCache;

    public TiberoTable(@NotNull OracleSchema schema, @NotNull String name) {
        super(schema, name);
    }

    public TiberoTable(@NotNull DBRProgressMonitor monitor, @NotNull OracleSchema schema, @NotNull ResultSet dbResult) {
        super(monitor, schema, dbResult);
    }

    /**
     * Routed through the schema so the Tibero-compatible index loading kicks in
     * instead of the per-table Oracle index query (Tibero has no ALL_INDEXES.SAMPLE_SIZE).
     */
    @NotNull
    @Association
    @Override
    public Collection<OracleTableIndex> getIndexes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return ((TiberoSchema) getContainer()).getTableIndexes(monitor, this);
    }

    @Nullable
    @Association
    @Override
    public List<OracleTableTrigger> getTriggers(@NotNull DBRProgressMonitor monitor) throws DBException {
        return ((TiberoSchema) getContainer()).getTableTriggersForTable(monitor, this);
    }

    @Nullable
    @Override
    public Object getTablespace(@NotNull DBRProgressMonitor monitor) throws DBException {
        // Keep the raw tablespace name instead of resolving OracleTablespace.
        return getLazyReference(null);
    }

    @NotNull
    @Override
    public String getDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull OracleDDLFormat ddlFormat,
        @NotNull java.util.Map<String, Object> options
    ) throws DBException {
        java.util.Map<String, Object> ddlOptions = new java.util.HashMap<>(options);
        ddlOptions.put(DBPScriptObject.OPTION_SKIP_INDEXES, true);
        ddlOptions.put(DBPScriptObject.OPTION_DDL_SKIP_FOREIGN_KEYS, true);
        ddlOptions.put(DBPScriptObject.OPTION_DDL_SEPARATE_FOREIGN_KEYS_STATEMENTS, false);
        return DBStructUtils.generateTableDDL(monitor, this, ddlOptions, true);
    }

    @NotNull
    @Override
    public String getObjectDefinitionText(
        @NotNull DBRProgressMonitor monitor,
        @NotNull java.util.Map<String, Object> options
    ) throws DBException {
        return getDDL(monitor, OracleDDLFormat.getCurrentFormat(getDataSource()), options);
    }

    @NotNull
    @Override
    public List<OracleTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<OracleTableColumn> columns = columnsCache;
        if (columns != null) {
            return columns;
        }
        synchronized (this) {
            if (columnsCache == null) {
                columnsCache = loadAttributes(monitor);
            }
            return columnsCache;
        }
    }

    @Nullable
    @Override
    public OracleTableColumn getAttribute(
        @NotNull DBRProgressMonitor monitor,
        @NotNull String attributeName
    ) throws DBException {
        for (OracleTableColumn column : getAttributes(monitor)) {
            if (attributeName.equals(column.getName())) {
                return column;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public org.jkiss.dbeaver.model.struct.DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        columnsCache = null;
        return super.refreshObject(monitor);
    }

    @PropertyGroup
    @LazyProperty(cacheValidator = OracleTableBase.AdditionalInfoValidator.class)
    @NotNull
    @Override
    public OracleTable.AdditionalInfo getAdditionalInfo(@NotNull DBRProgressMonitor monitor) {
        return (OracleTable.AdditionalInfo) super.getAdditionalInfo();
    }

    @Nullable
    @Override
    public Collection<OracleTableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
        return ((TiberoSchema) getContainer()).getTableConstraints(monitor, this);
    }

    @NotNull
    @Override
    public Collection<OracleTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException {
        return ((TiberoSchema) getContainer()).getTableForeignKeys(monitor, this);
    }

    @NotNull
    @Override
    public Collection<OracleTableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
        return ((TiberoSchema) getContainer()).getTableForeignKeyReferences(monitor, this);
    }

    @NotNull
    private List<OracleTableColumn> loadAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<OracleTableColumn> columns = new ArrayList<>();
        final String colsView = getDataSource().isViewAvailable(monitor, OracleConstants.SCHEMA_SYS, "ALL_TAB_COLS")
            ? "ALL_TAB_COLS"
            : "TAB_COLS";
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Tibero table columns")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT c.*\n" +
                     ", c.TABLE_NAME AS OBJECT_NAME\n" +
                     ", NULL AS COMMENTS, 0 AS COMMENTS_LOADED\n" +
                     ", NULL AS DATA_TYPE_MOD, NULL AS HIDDEN_COLUMN\n" +
                "FROM " + OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), colsView) + " c\n" +
                "WHERE c.OWNER=? AND c.TABLE_NAME=?\n" +
                "ORDER BY c.COLUMN_ID"
            )) {
                dbStat.setString(1, getSchema().getName());
                dbStat.setString(2, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        columns.add(new OracleTableColumn(monitor, this, dbResult));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, getDataSource());
        }
        columns.sort(DBUtils.orderComparator());
        return Collections.unmodifiableList(columns);
    }
}
