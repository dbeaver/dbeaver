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
package org.jkiss.dbeaver.ext.clickhouse.model;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.model.DBPObjectStatistics;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.ByteNumberFormat;

import java.sql.SQLException;
import java.util.Date;

/**
 * ClickhouseTable
 */
public class ClickhouseTable extends GenericTable implements DBPObjectStatistics
{
    private static final Log log = Log.getLog(ClickhouseTable.class);

    private Long tableSize;
    private long tableRows;
    private Date lastModifyTime;
    private String maxDate;
    private String minDate;
    private String engine;

    ClickhouseTable(GenericStructContainer container, @Nullable String tableName, @Nullable String tableType, @Nullable JDBCResultSet dbResult) {
        super(container, tableName, tableType, dbResult);
    }

    @Override
    public boolean hasStatistics() {
        return tableSize != null;
    }

    @Property(category = CAT_STATISTICS, viewable = true, order = 20, formatter = ByteNumberFormat.class)
    @Override
    public long getStatObjectSize() {
        return tableSize == null ? 0 : tableSize;
    }

    @Property(category = CAT_STATISTICS, viewable = true, order = 21)
    @Nullable
    @Override
    public synchronized Long getRowCount(DBRProgressMonitor monitor) {
        readStatistics(monitor);
        return tableRows;
    }

    @Property(category = CAT_STATISTICS, order = 22)
    public Date getLastModifyTime(DBRProgressMonitor monitor) {
        readStatistics(monitor);
        return lastModifyTime;
    }

    @Property(category = CAT_STATISTICS, order = 23)
    public String getMinDate(DBRProgressMonitor monitor) {
        readStatistics(monitor);
        return minDate;
    }

    @Property(category = CAT_STATISTICS, order = 24)
    public String getMaxDate(DBRProgressMonitor monitor) {
        readStatistics(monitor);
        return maxDate;
    }

    @Property(category = CAT_STATISTICS, viewable = true, order = 25)
    public String getEngine(DBRProgressMonitor monitor) {
        readStatistics(monitor);
        return engine;
    }

    @Nullable
    @Override
    public DBPPropertySource getStatProperties() {
        return null;
    }

    private void readStatistics(DBRProgressMonitor monitor) {
        if (hasStatistics()) {
            return;
        }
        try (DBCSession session = DBUtils.openMetaSession(monitor, this, "Read relation statistics")) {
            try (JDBCPreparedStatement dbStat = ((JDBCSession)session).prepareStatement(
                "select " +
                    "sum(bytes) as table_size, " +
                    "sum(rows) as table_rows, " +
                    "max(modification_time) as latest_modification," +
                    "min(min_date) AS min_date," +
                    "max(max_date) AS max_date," +
                    "any(engine) as engine\n" +
                    "FROM system.parts\n" +
                    "WHERE database=? AND table=?\n" +
                    "GROUP BY table"))
            {
                dbStat.setString(1, getContainer().getName());
                dbStat.setString(2, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        fetchStatistics(dbResult);
                    }
                }
            } catch (SQLException e) {
                log.error("Error reading table statistics", e);
            }
        }
    }

    void fetchStatistics(JDBCResultSet dbResult) throws SQLException {
        tableSize = JDBCUtils.safeGetLong(dbResult, "table_size");
        tableRows = JDBCUtils.safeGetLong(dbResult, "table_rows");
        lastModifyTime = JDBCUtils.safeGetTimestamp(dbResult, "latest_modification");
        maxDate = JDBCUtils.safeGetString(dbResult, "max_date");
        minDate = JDBCUtils.safeGetString(dbResult, "min_date");
        engine = JDBCUtils.safeGetString(dbResult, "engine");
    }


}
