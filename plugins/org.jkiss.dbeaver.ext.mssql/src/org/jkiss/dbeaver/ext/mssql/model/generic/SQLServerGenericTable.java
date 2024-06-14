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
package org.jkiss.dbeaver.ext.mssql.model.generic;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPObjectStatistics;
import org.jkiss.dbeaver.model.DBPObjectWithLazyDescription;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.ByteNumberFormat;

import java.sql.SQLException;
import java.util.List;

/**
* SQL Server table
*/
public class SQLServerGenericTable extends GenericTable implements DBPObjectWithLazyDescription, DBPObjectStatistics {

    private static final Log log = Log.getLog(SQLServerGenericTable.class);

    private long tableSize = -1;
    private long rowCount;
    private long pages;

    public SQLServerGenericTable(GenericStructContainer container, String tableName, String tableType, JDBCResultSet dbResult) {
        super(container, tableName, tableType, dbResult);
    }

    @Property(category = DBConstants.CAT_STATISTICS, order = 3, formatter = ByteNumberFormat.class)
    public long getTableSize(DBRProgressMonitor monitor) {
        readTableStats(monitor);
        return tableSize;
    }

    @Property(category = DBConstants.CAT_STATISTICS, order = 4)
    public Long getRowCount(DBRProgressMonitor monitor) {
        readTableStats(monitor);
        return rowCount;
    }

    @Property(category = DBConstants.CAT_STATISTICS, order = 5)
    public long getPages() {
        return pages;
    }

    @Nullable
    @Override
    public synchronized List<SQLServerGenericTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return (List<SQLServerGenericTableColumn>) super.getAttributes(monitor);
    }

    @Override
    public SQLServerGenericTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException {
        return (SQLServerGenericTableColumn)super.getAttribute(monitor, attributeName);
    }

/*
    @Override
    public String getOverloadedName() {
        //return getSchema().getName() + "." + getName();
        return getName();
    }
*/

    @Override
    public String getDescription() {
        return super.getDescription();
    }

    @Override
    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 100)
    public String getDescription(DBRProgressMonitor monitor) throws DBException {
        String description = getDescription();
        if (description != null || !isSqlServer()) {
            return description;
        }
        // Query row count
        try (JDBCSession session = DBUtils.openUtilSession(monitor, this, "Read table description")) {
            DBSObject defaultDatabase = DBUtils.getDefaultContext(getDataSource(), true).getContextDefaults().getDefaultCatalog();
            boolean switchSchema = getCatalog() != null && defaultDatabase != null && defaultDatabase != getCatalog();
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                (switchSchema ? "USE " + DBUtils.getQuotedIdentifier(getCatalog()) + ";\n" : "") +
                "SELECT cast(value as varchar(8000)) as value " +
                    "FROM fn_listextendedproperty ('MS_DESCRIPTION', 'schema', ?, 'table', ?, default, default);\n" +
                    (switchSchema ? "USE "+ DBUtils.getQuotedIdentifier(defaultDatabase) + ";\n" : "")))
            {
                dbStat.setString(1, getSchema().getName());
                dbStat.setString(2, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        description = dbResult.getString(1);
                    } else {
                        description = "";
                    }
                }

            } catch (SQLException e) {
                throw new DBDatabaseException(e, getDataSource());
            }
        }

        setDescription(description);
        return description;
    }

    @NotNull
    @Override
    public SQLServerGenericDataSource getDataSource() {
        return (SQLServerGenericDataSource)super.getDataSource();
    }

    private boolean isSqlServer() {
        return ((SQLServerMetaModel)getDataSource().getMetaModel()).isSqlServer();
    }

    @Override
    protected boolean isTruncateSupported() {
        return true;
    }

    void fetchTableStats(JDBCResultSet dbResult) {
        tableSize = JDBCUtils.safeGetLong(dbResult,"totalSize");
        if (isSqlServer()) {
            tableSize = tableSize * 1024;
        } else {
            pages = JDBCUtils.safeGetLong(dbResult,"pages");
        }
        rowCount = JDBCUtils.safeGetLong(dbResult,"rows");
    }

    @Override
    public boolean hasStatistics() {
        return tableSize != -1;
    }

    @Override
    public long getStatObjectSize() {
        return tableSize;
    }

    @Nullable
    @Override
    public DBPPropertySource getStatProperties() {
        return null;
    }

    private void readTableStats(DBRProgressMonitor monitor) {
        if (hasStatistics()) {
            return;
        }
        if (!isSqlServer() && !getDataSource().isServerVersionAtLeast(15, 0)) {
            tableSize = 0;
            return;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table statistics")) {
            try (JDBCPreparedStatement dbStat = SQLServerUtils.prepareTableStatisticLoadStatement(
                session,
                getDataSource(),
                getCatalog(),
                ((SQLServerGenericSchema) getSchema()).getSchemaId(),
                this,
                isSqlServer())) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        fetchTableStats(dbResult);
                    }
                }
            }
        } catch (SQLException | DBCException e) {
            log.debug("Error reading table statistics", e);
        }
    }
}
