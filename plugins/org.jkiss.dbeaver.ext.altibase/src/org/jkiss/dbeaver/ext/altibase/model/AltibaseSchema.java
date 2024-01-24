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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndex;
import org.jkiss.dbeaver.ext.generic.model.GenericTrigger;
import org.jkiss.dbeaver.model.DBPObjectStatisticsCollector;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AltibaseSchema extends GenericSchema implements DBPObjectStatisticsCollector {

    private volatile boolean hasStatistics;
    
    /**
     * Altibase Schema
     */
    public AltibaseSchema(GenericDataSource dataSource, GenericCatalog catalog, String schemaName) {
        super(dataSource, catalog, schemaName);
    }

    @Override
    public List<AltibaseTable> getPhysicalTables(DBRProgressMonitor monitor) throws DBException {
        List<? extends GenericTableBase> tables = getTables(monitor);
        if (tables != null) {
            List<AltibaseTable> filtered = new ArrayList<>();
            for (GenericTableBase table : tables) {
                if (table instanceof AltibaseTable) {
                    filtered.add((AltibaseTable) table);
                }
            }
            return filtered;
        }
        return null;
    }

    /**
     * Return queue table objects only from table list.
     */
    public List<AltibaseQueue> getQueueTables(DBRProgressMonitor monitor) throws DBException {
        List<? extends GenericTableBase> tables = getTables(monitor);
        if (tables != null) {
            List<AltibaseQueue> filtered = new ArrayList<>();
            for (GenericTableBase table : tables) {
                if (table instanceof AltibaseQueue) {
                    filtered.add((AltibaseQueue) table);
                }
            }
            return filtered;
        }
        return null;
    }

    @Override
    public List<AltibaseView> getViews(DBRProgressMonitor monitor) throws DBException {
        List<? extends GenericTableBase> tables = getTables(monitor);
        if (tables != null) {
            List<AltibaseView> filtered = new ArrayList<>();
            for (GenericTableBase table : tables) {
                if (table instanceof AltibaseView) {
                    filtered.add((AltibaseView) table);
                }
            }
            return filtered;
        }
        return null;
    }

    /**
     * Return materialized view only from table list.
     */
    public List<AltibaseMaterializedView> getMaterializedViews(DBRProgressMonitor monitor) throws DBException {
        List<? extends GenericTableBase> tables = getTables(monitor);
        if (tables != null) {
            List<AltibaseMaterializedView> filtered = new ArrayList<>();
            for (GenericTableBase table : tables) {
                if (table instanceof AltibaseMaterializedView) {
                    filtered.add((AltibaseMaterializedView) table);
                }
            }
            return filtered;
        }
        return null;
    }

    /**
     * Return typeset objects only from procedures
     */
    public List<AltibaseTypeset> getTypesetsOnly(DBRProgressMonitor monitor) throws DBException {
        List<AltibaseTypeset> filteredProcedures = new ArrayList<>();
        for (GenericProcedure proc : CommonUtils.safeList(getProcedures(monitor))) {
            if (proc instanceof AltibaseTypeset) {
                filteredProcedures.add((AltibaseTypeset) proc);
            }
        }
        return filteredProcedures;
    }

    @Override
    public synchronized DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.refreshObject(monitor);
        hasStatistics = false;
        return this;
    }

    public GenericTableIndex getIndex(DBRProgressMonitor monitor, String uniqueName) throws DBException {
        for (GenericTableIndex index : CommonUtils.safeCollection(getIndexes(monitor))) {
            if (uniqueName.equals(index.getName())) {
                return index;
            }
        }
        return null;
    }
    
    public GenericTrigger getTableTrigger(DBRProgressMonitor monitor, String uniqueName) throws DBException {
        for (GenericTrigger tableTrigger : CommonUtils.safeCollection(getTableTriggers(monitor))) {
            if (uniqueName.equals(tableTrigger.getName())) {
                return tableTrigger;
            }
        }
        return null;
    }
    
    public GenericProcedure getProcedureByName(DBRProgressMonitor monitor, String name) throws DBException {
        for (GenericProcedure procedure : CommonUtils.safeCollection(getProcedures(monitor))) {
            if (name.equals(procedure.getName())) {
                return procedure;
            }
        }
        return null;
    }

    ///////////////////////////////////
    // Statistics

    @Override
    public boolean isStatisticsCollected() {
        return hasStatistics;
    }

    @Override
    public void collectObjectStatistics(DBRProgressMonitor monitor, boolean totalSizeOnly, boolean forceRefresh) throws DBException {
        if (hasStatistics && !forceRefresh) {
            return;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table status")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT table_name, memory_size, disk_size FROM system_.sys_table_size_ WHERE USER_NAME = ?")) {
                dbStat.setString(1, getName());
                
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String tableName = dbResult.getString(1);
                        AltibaseTable table = (AltibaseTable) getTable(monitor, tableName);
                        if (table != null) {
                            table.fetchTableSize(dbResult);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBCException("Error reading table statistics", e);
        } finally {
            for (GenericTableBase table : getTableCache().getCachedObjects()) {
                if (table instanceof AltibaseTable && !((AltibaseTable) table).hasStatistics()) {
                    ((AltibaseTable) table).resetSize();
                }
            }
            hasStatistics = true;
        }
    }
}
