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
package org.jkiss.dbeaver.ext.hana.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPObjectStatistics;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.utils.ByteNumberFormat;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

public class HANATable extends GenericTable implements DBPObjectStatistics {

    private final PartitionCache partitionCache = new PartitionCache();
    private long tableSize = -1;

    public HANATable(
        GenericStructContainer container,
        @Nullable String tableName,
        @Nullable String tableType,
        @Nullable JDBCResultSet dbResult)
    {
        super(container, tableName, tableType, dbResult);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public List<? extends HANATableColumn> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return (List<? extends HANATableColumn>) super.getAttributes(monitor);
    }

    @Override
    public HANATableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException {
        return (HANATableColumn) super.getAttribute(monitor, attributeName);
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1, visibleIf = HANANotPartitionedTable.class)
    public String getName() {
        return super.getName();
    }

    @Override
    @Property(viewable = true, order = 2, visibleIf = HANANotPartitionedTable.class)
    public String getTableType() {
        return super.getTableType();
    }

    @Override
    @NotNull
    @Property(viewable = true, order = 3, visibleIf = HANANotPartitionedTable.class)
    public GenericSchema getSchema() {
        return super.getSchema();
    }

    @Override
    @Property(viewable = true, order = 13, visibleIf = HANANotPartitionedTable.class)
    public String getDescription() {
        return super.getDescription();
    }

    @Override
    public boolean hasStatistics() {
        return tableSize != -1;
    }

    @Override
    public long getStatObjectSize() {
        return tableSize;
    }

    void fetchStatistics(JDBCResultSet dbResult) throws SQLException {
        tableSize = dbResult.getLong("DISK_SIZE");
    }

    @Property(category = DBConstants.CAT_STATISTICS, formatter = ByteNumberFormat.class)
    public Long getTableSize(DBRProgressMonitor monitor) throws DBException {
        if (tableSize == -1) {
            ((HANASchema) getSchema()).collectObjectStatistics(monitor, false, false);
        }
        return tableSize;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (tableSize != -1) {
            tableSize = -1;
            ((HANASchema) getSchema()).resetStatistics();
        }
        return super.refreshObject(monitor);
    }

    @Nullable
    @Override
    public DBPPropertySource getStatProperties() {
        return null;
    }

    @NotNull
    public Collection<HANAPartition> getPartitions(@NotNull DBRProgressMonitor monitor) throws DBException {
        return partitionCache.getAllObjects(monitor, this);
    }

    private static class PartitionCache extends JDBCObjectCache<HANATable, HANAPartition> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull HANATable table) throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement("SELECT tp.*, "
            + "\nMEMORY_SIZE_IN_TOTAL, RECORD_COUNT, MEMORY_SIZE_IN_MAIN, MEMORY_SIZE_IN_DELTA, CREATE_TIME, LAST_MERGE_TIME, LAST_REPLAY_LOG_TIME, LOADED"
            + "\nFROM M_CS_TABLES mcs JOIN TABLE_PARTITIONS tp ON mcs.SCHEMA_NAME=tp.SCHEMA_NAME AND mcs.TABLE_NAME=tp.TABLE_NAME AND mcs.PART_ID=tp.PART_ID"
            + "\nWHERE tp.TABLE_NAME = ? AND tp.SCHEMA_NAME = ? ORDER BY tp.PART_ID");
            dbStat.setString(1, table.getName());
            dbStat.setString(2, table.getContainer().getName());
            return dbStat;
        }

        @Override
        protected HANAPartition fetchObject(
            @NotNull JDBCSession session,
            @NotNull HANATable table,
            @NotNull JDBCResultSet dbResult
        ) throws SQLException, DBException {
            int partition_id = JDBCUtils.safeGetInt(dbResult, "PART_ID");
            if (dbResult == null) {
                return null;
            }
            return new HANAPartition(table, partition_id, dbResult);
        }
    }
}
