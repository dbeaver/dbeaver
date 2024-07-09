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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.DBPObjectStatisticsCollector;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AltibaseSchema extends GenericSchema implements DBPObjectStatisticsCollector {

    private volatile boolean hasStatistics;
    
    private final DbLinkCache dbLinkCache = new DbLinkCache();
    private final LibraryCache libraryCache = new LibraryCache();
    private final DirectoryCache directoryCache = new DirectoryCache();
    
    /**
     * Altibase Schema
     */
    public AltibaseSchema(GenericDataSource dataSource, GenericCatalog catalog, String schemaName) {
        super(dataSource, catalog, schemaName);
    }

    @Override
    public synchronized DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.refreshObject(monitor);
        this.dbLinkCache.clearCache();
        this.libraryCache.clearCache();
        this.directoryCache.clearCache();
        hasStatistics = false;
        return this;
    }
    
    public List<AltibaseDbLink> getDbLinks(DBRProgressMonitor monitor) throws DBException {
        return dbLinkCache.getAllObjects(monitor, this);
    }

    public DbLinkCache getDbLinkCache() {
        return dbLinkCache;
    }
    
    public List<AltibaseLibrary> getLibraries(DBRProgressMonitor monitor) throws DBException {
        return libraryCache.getAllObjects(monitor, this);
    }
    public LibraryCache getLibraryCache() {
        return libraryCache;
    }

    public List<AltibaseDirectory> getDirectories(DBRProgressMonitor monitor) throws DBException {
        return directoryCache.getAllObjects(monitor, this);
    }
    
    public DirectoryCache getDirectoryCache() {
        return directoryCache;
    }

    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        
        DBSObject object = null;
        
        object = getTable(monitor, childName);
        if (object != null) {
            return object;
        }
        
        object = getProcedure(monitor, childName);
        if (object != null) {
            return object;
        }
        
        object = getPackage(monitor, childName);
        if (object != null) {
            return object;
        }
        
        object = getSynonym(monitor, childName);

        return object;
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
    
    static class DbLinkCache extends JDBCObjectLookupCache<GenericObjectContainer, AltibaseDbLink> {

        @Nullable
        @Override
        protected AltibaseDbLink fetchObject(@NotNull JDBCSession session, @NotNull GenericObjectContainer container, 
                @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return ((AltibaseMetaModel) container.getDataSource().getMetaModel()).createDbLinkImpl(container, resultSet);
        }

        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull GenericObjectContainer owner,
                @Nullable AltibaseDbLink object, @Nullable String objectName) throws SQLException {
            return ((AltibaseMetaModel) owner.getDataSource().getMetaModel()).prepareDbLinkLoadStatement(
                    session, owner, object, objectName);
        }
    }
    
    static class LibraryCache extends JDBCObjectLookupCache<GenericObjectContainer, AltibaseLibrary> {

        @Nullable
        @Override
        protected AltibaseLibrary fetchObject(@NotNull JDBCSession session, @NotNull GenericObjectContainer container, 
                @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return ((AltibaseMetaModel) container.getDataSource().getMetaModel()).createLibraryImpl(container, resultSet);
        }

        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull GenericObjectContainer owner,
                @Nullable AltibaseLibrary object, @Nullable String objectName) throws SQLException {
            return ((AltibaseMetaModel) owner.getDataSource().getMetaModel()).prepareLibraryLoadStatement(
                    session, owner, object, objectName);
        }
    }
    
    static class DirectoryCache extends JDBCObjectLookupCache<GenericObjectContainer, AltibaseDirectory> {

        @Nullable
        @Override
        protected AltibaseDirectory fetchObject(@NotNull JDBCSession session, @NotNull GenericObjectContainer container, 
                @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return ((AltibaseMetaModel) container.getDataSource().getMetaModel()).createDirectoryImpl(container, resultSet);
        }

        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull GenericObjectContainer owner,
                @Nullable AltibaseDirectory object, @Nullable String objectName) throws SQLException {
            return ((AltibaseMetaModel) owner.getDataSource().getMetaModel()).prepareDirectoryLoadStatement(
                    session, owner, object, objectName);
        }
    }
  
}
