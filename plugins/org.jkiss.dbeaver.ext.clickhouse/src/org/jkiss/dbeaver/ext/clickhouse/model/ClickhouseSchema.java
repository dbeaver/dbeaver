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
package org.jkiss.dbeaver.ext.clickhouse.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.DBPObjectStatisticsCollector;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.sql.SQLException;
import java.util.List;

/**
 * ClickhouseSchema
 */
public class ClickhouseSchema extends GenericSchema implements DBPObjectStatisticsCollector
{
    private boolean hasStatistics;

    public ClickhouseSchema(@NotNull GenericDataSource dataSource, @Nullable GenericCatalog catalog, @NotNull String schemaName) {
        super(dataSource, catalog, schemaName);
    }

    @Override
    public List<ClickhouseTable> getPhysicalTables(DBRProgressMonitor monitor) throws DBException {
        return (List<ClickhouseTable>) super.getPhysicalTables(monitor);
    }

    @Override
    public List<ClickhouseTable> getTables(DBRProgressMonitor monitor) throws DBException {
        return (List<ClickhouseTable>) super.getTables(monitor);
    }

    @Override
    public boolean isStatisticsCollected() {
        return hasStatistics;
    }

    @Override
    public void collectObjectStatistics(DBRProgressMonitor monitor, boolean totalSizeOnly, boolean forceRefresh) throws DBException {
        if (hasStatistics && !forceRefresh) {
            return;
        }
        try (DBCSession session = DBUtils.openMetaSession(monitor, this, "Read relation statistics")) {
            try (JDBCPreparedStatement dbStat = ((JDBCSession)session).prepareStatement(
                "select table," +
                        "sum(bytes) as table_size, " +
                        "sum(rows) as table_rows, " +
                        "max(modification_time) as latest_modification," +
                        "min(min_date) AS min_date," +
                        "max(max_date) AS max_date," +
                        "any(engine) as engine\n" +
                    "FROM system.parts\n" +
                    "WHERE database=? AND active\n" +
                    "GROUP BY table"))
            {
                dbStat.setString(1, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String tableName = dbResult.getString(1);
                        GenericTableBase table = getTable(monitor, tableName);
                        if (table instanceof ClickhouseTable) {
                            ((ClickhouseTable)table).fetchStatistics(dbResult);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new DBCException("Error reading schema statistics", e);
            }
        } finally {
            hasStatistics = true;
        }
    }

    @NotNull
    @Override
    public Class<? extends DBSEntity> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException {
        return ClickhouseTable.class;
    }
}
