/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPObjectStatistics;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.ByteNumberFormat;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

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
    private ClickhouseTableEngine engine;
    private String engineMessage;
    private String metadataPath;

    ClickhouseTable(GenericStructContainer container, @Nullable String tableName, @Nullable String tableType, @Nullable JDBCResultSet dbResult) {
        super(container, tableName, tableType, dbResult);
        if (dbResult != null) {
            tableSize = JDBCUtils.safeGetLong(dbResult, "total_bytes");
            tableRows = JDBCUtils.safeGetLong(dbResult, "total_rows");
            lastModifyTime = JDBCUtils.safeGetTimestamp(dbResult, "metadata_modification_time");
            String engineName = JDBCUtils.safeGetString(dbResult, "engine");
            engine = searchEngine(engineName);
            engineMessage = JDBCUtils.safeGetString(dbResult, "engine_full");
            metadataPath = JDBCUtils.safeGetString(dbResult, "metadata_path");
        } else {
            setDefaultEngine();
        }
    }

    @Nullable
    private ClickhouseTableEngine searchEngine(String engineName) {
        if (CommonUtils.isNotEmpty(engineName)) {
            return getDataSource().getEngineByName(engineName);
        }
        return null;
    }

    private void setDefaultEngine() {
        final List<ClickhouseTableEngine> tableEngines = getDataSource().getTableEngines();
        if (!CommonUtils.isEmpty(tableEngines)) {
            // Log is one of the simplest ClickHouse engines. It doesn't need special engine parameters
            engine = tableEngines.stream().filter(e -> e.getName().equals("Log")).findFirst().orElse(tableEngines.get(0));
        }
    }

    @Override
    public String getTableType() {
        // We have engine here already
        return super.getTableType();
    }

    @Override
    public boolean hasStatistics() {
        return tableSize != null;
    }

    @Property(category = DBConstants.CAT_STATISTICS, viewable = true, order = 20, formatter = ByteNumberFormat.class)
    @Override
    public long getStatObjectSize() {
        return tableSize == null ? 0 : tableSize;
    }

    @Property(category = DBConstants.CAT_STATISTICS, viewable = true, order = 21)
    @Nullable
    @Override
    public synchronized Long getRowCount(DBRProgressMonitor monitor) {
        return tableRows;
    }

    @Property(category = DBConstants.CAT_STATISTICS, order = 22)
    public Date getLastModifyTime(DBRProgressMonitor monitor) {
        return lastModifyTime;
    }

    @Property(category = DBConstants.CAT_STATISTICS, order = 23)
    public String getMinDate(DBRProgressMonitor monitor) {
        return minDate;
    }

    @Property(category = DBConstants.CAT_STATISTICS, order = 24)
    public String getMaxDate(DBRProgressMonitor monitor) {
        return maxDate;
    }

    @Property(viewable = true, order = 25, editable = true, listProvider = EngineListProvider.class)
    public ClickhouseTableEngine getEngine() {
        return engine;
    }

    public void setEngine(ClickhouseTableEngine engine) {
        this.engine = engine;
    }

    @Property(viewable = true, order = 26, editable = true, length = PropertyLength.MULTILINE)
    public String getEngineMessage() {
        return engineMessage;
    }

    public void setEngineMessage(String engineMessage) {
        this.engineMessage = engineMessage;
    }

    @Property(category = DBConstants.CAT_STATISTICS, viewable = true, order = 27)
    public String getMetadataPath() {
        return metadataPath;
    }

    @Nullable
    @Override
    @Property(viewable = true,
        editableExpr = "object.dataSource.isServerVersionAtLeast(21, 6)",
        updatableExpr = "object.dataSource.isServerVersionAtLeast(21, 6)",
        length = PropertyLength.MULTILINE, order = 100)
    public String getDescription() {
        return super.getDescription();
    }

    @Nullable
    @Override
    public DBPPropertySource getStatProperties() {
        return null;
    }

    private void readStatistics(DBRProgressMonitor monitor) {
        // Now this is a spare method of reading statistics, the main statistics will be read when reading the table data
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
                    "max(max_date) AS max_date " +
                    "FROM system.parts\n" +
                    "WHERE active AND database=? AND table=?\n" +
                    "GROUP BY table"))
            {
                dbStat.setString(1, getContainer().getName());
                dbStat.setString(2, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        fetchStatistics(dbResult);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error reading relation statistics", e);
        }
    }

    void fetchStatistics(JDBCResultSet dbResult) throws SQLException {
        tableSize = JDBCUtils.safeGetLong(dbResult, "table_size");
        tableRows = JDBCUtils.safeGetLong(dbResult, "table_rows");
        lastModifyTime = JDBCUtils.safeGetTimestamp(dbResult, "latest_modification");
        maxDate = JDBCUtils.safeGetString(dbResult, "max_date");
        minDate = JDBCUtils.safeGetString(dbResult, "min_date");
    }

    @Override
    public String generateTableUpdateBegin(String tableName) {
        return "ALTER TABLE " + tableName + " UPDATE ";
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        DBSObject dbsObject = super.refreshObject(monitor);
        readStatistics(monitor);
        return dbsObject;
    }

    @Override
    public String generateTableUpdateSet() {
        return "";
    }

    @Override
    public String generateTableDeleteFrom(String tableName) {
        return "ALTER TABLE " + tableName + " DELETE ";
    }

    @NotNull
    @Override
    public ClickhouseDataSource getDataSource() {
        return (ClickhouseDataSource) super.getDataSource();
    }

    public static class EngineListProvider implements IPropertyValueListProvider<ClickhouseTable> {

        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @Override
        public Object[] getPossibleValues(ClickhouseTable object) {
            return object.getDataSource().getTableEngines().toArray();
        }
    }
}
