/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * GenericEntityContainer
 */
public abstract class GenericObjectContainer implements GenericStructContainer,DBPRefreshableObject
{
    private static final Log log = Log.getLog(GenericObjectContainer.class);

    @NotNull
    private final GenericDataSource dataSource;
    private final TableCache tableCache;
    private final IndexCache indexCache;
    private final ForeignKeysCache foreignKeysCache;
    private final PrimaryKeysCache primaryKeysCache;
    private List<GenericPackage> packages;
    protected List<GenericProcedure> procedures;
    protected List<GenericSequence> sequences;
    private List<? extends GenericTrigger> triggers;

    protected GenericObjectContainer(@NotNull GenericDataSource dataSource)
    {
        this.dataSource = dataSource;
        this.tableCache = new TableCache(dataSource);
        this.indexCache = new IndexCache(tableCache);
        this.primaryKeysCache = new PrimaryKeysCache(tableCache);
        this.foreignKeysCache = new ForeignKeysCache(tableCache);
    }

    @Override
    public final TableCache getTableCache()
    {
        return tableCache;
    }

    @Override
    public final IndexCache getIndexCache()
    {
        return indexCache;
    }

    @Override
    public final PrimaryKeysCache getPrimaryKeysCache()
    {
        return primaryKeysCache;
    }

    @Override
    public final ForeignKeysCache getForeignKeysCache()
    {
        return foreignKeysCache;
    }

    @NotNull
    @Override
    public GenericDataSource getDataSource()
    {
        return dataSource;
    }


    @Override
    public boolean isPersisted()
    {
        return true;
    }

    @Override
    public Collection<GenericTable> getViews(DBRProgressMonitor monitor) throws DBException {
        Collection<GenericTable> tables = getTables(monitor);
        if (tables != null) {
            List<GenericTable> filtered = new ArrayList<>();
            for (GenericTable table : tables) {
                if (table.isView()) {
                    filtered.add(table);
                }
            }
            return filtered;
        }
        return null;
    }

    @Override
    public Collection<GenericTable> getPhysicalTables(DBRProgressMonitor monitor) throws DBException {
        Collection<GenericTable> tables = getTables(monitor);
        if (tables != null) {
            List<GenericTable> filtered = new ArrayList<>();
            for (GenericTable table : tables) {
                if (!table.isView()) {
                    filtered.add(table);
                }
            }
            return filtered;
        }
        return null;
    }

    @Override
    public Collection<GenericTable> getTables(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getAllObjects(monitor, this);
    }

    @Override
    public GenericTable getTable(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return tableCache.getObject(monitor, this, name);
    }

    @Override
    public Collection<GenericTableIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        cacheIndexes(monitor, true);
        return indexCache.getObjects(monitor, this, null);
    }

    private void cacheIndexes(DBRProgressMonitor monitor, boolean readFromTables)
        throws DBException
    {
        // Cache indexes (read all tables, all columns and all indexes in this container)
        // This doesn't work for generic datasource because metadata facilities
        // allows index query only by certain table name
        //cacheIndexes(monitor, null);
        synchronized (indexCache) {
            if (!indexCache.isFullyCached()) {

                try {
                    // Try to load all indexes with one query
                    Collection<GenericTableIndex> indexes = indexCache.getObjects(monitor, this, null);
                    if (CommonUtils.isEmpty(indexes)) {
                        // Nothing was read, Maybe driver doesn't support mass indexes reading
                        indexCache.clearCache();
                    }
                } catch (Exception e) {
                    log.debug(e);
                }

                // Failed
                if (!indexCache.isFullyCached() && readFromTables) {
                    // Load indexes for all tables and return copy of them
                    Collection<GenericTable> tables = getTables(monitor);
                    monitor.beginTask("Cache indexes from tables", tables.size());
                    try {
                        List<GenericTableIndex> tmpIndexMap = new ArrayList<>();
                        for (GenericTable table : tables) {
                            if (monitor.isCanceled()) {
                                return;
                            }
                            monitor.subTask("Read indexes for '" + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
                            Collection<GenericTableIndex> tableIndexes = table.getIndexes(monitor);
                            tmpIndexMap.addAll(tableIndexes);
                            monitor.worked(1);
                        }
                        indexCache.setCache(tmpIndexMap);
                    } finally {
                        monitor.done();
                    }
                }

            }
        }
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope)
        throws DBException
    {
        // Cache tables
        if ((scope & STRUCT_ENTITIES) != 0) {
            monitor.subTask("Cache tables");
            tableCache.getAllObjects(monitor, this);
        }

        // Cache attributes
        if ((scope & STRUCT_ATTRIBUTES) != 0 && dataSource.supportsStructCache()) {
            // Try to cache columns
            // Cannot be sure that all jdbc drivers support reading of all catalog columns
            // So error here is not fatal
            try {
                monitor.subTask("Cache tables' columns");
                tableCache.loadChildren(monitor, this, null);
            } catch (Exception e) {
                log.debug(e);
            }
        }
        // Cache associations
        if ((scope & STRUCT_ASSOCIATIONS) != 0 && dataSource.supportsStructCache()) {
            // Try to read all PKs
            // Try to read all FKs
            try {
                monitor.subTask("Cache primary keys");
                Collection<GenericPrimaryKey> objects = primaryKeysCache.getObjects(monitor, this, null);
                if (CommonUtils.isEmpty(objects)) {
                    // Nothing was read, Maybe driver doesn't support mass keys reading
                    primaryKeysCache.clearCache();
                }
            } catch (Exception e) {
                // Failed - seems to be unsupported feature
                log.debug(e);
            }

            if (dataSource.getInfo().supportsIndexes()) {
                // Try to read all indexes
                monitor.subTask("Cache indexes");
                cacheIndexes(monitor, false);
            }

            if (dataSource.getInfo().supportsReferentialIntegrity()) {
                // Try to read all FKs
                try {
                    monitor.subTask("Cache foreign keys");
                    Collection<GenericTableForeignKey> foreignKeys = foreignKeysCache.getObjects(monitor, this, null);
                    if (CommonUtils.isEmpty(foreignKeys)) {
                        // Nothing was read, Maybe driver doesn't support mass keys reading
                        foreignKeysCache.clearCache();
                    }
                } catch (Exception e) {
                    // Failed - seems to be unsupported feature
                    log.debug(e);
                }
            }
        }
    }

    @Override
    public synchronized Collection<GenericPackage> getPackages(DBRProgressMonitor monitor)
        throws DBException
    {
        if (procedures == null) {
            loadProcedures(monitor);
        }
        return packages == null ? null : packages;
    }

    public GenericPackage getPackage(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return DBUtils.findObject(getPackages(monitor), name);
    }

    @Override
    public synchronized Collection<GenericProcedure> getProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        if (procedures == null) {
            loadProcedures(monitor);
        }
        return procedures;
    }

    @Override
    public GenericProcedure getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException
    {
        for (GenericProcedure procedure : CommonUtils.safeCollection(getProcedures(monitor))) {
            if (uniqueName.equals(procedure.getUniqueName())) {
                return procedure;
            }
        }
        return null;
    }

    @Override
    public Collection<GenericProcedure> getProcedures(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return DBUtils.findObjects(getProcedures(monitor), name);
    }

    @Override
    public Collection<GenericSequence> getSequences(DBRProgressMonitor monitor) throws DBException {
        if (sequences == null) {
            loadSequences(monitor);
        }
        return sequences;
    }

    @Override
    public Collection<? extends GenericTrigger> getTriggers(DBRProgressMonitor monitor) throws DBException {
        if (triggers == null) {
            triggers = loadTriggers(monitor);
        }
        return triggers;
    }

    @Override
    public Collection<? extends GenericTrigger> getTableTriggers(DBRProgressMonitor monitor) throws DBException {
        List<GenericTrigger> tableTriggers = new ArrayList<>();
        for (GenericTable table : getTables(monitor)) {
            Collection<? extends GenericTrigger> tt = table.getTriggers(monitor);
            if (!CommonUtils.isEmpty(tt)) {
                tableTriggers.addAll(tt);
            }
        }
        return tableTriggers;
    }

    @Association
    public Collection<? extends DBSDataType> getDataTypes(DBRProgressMonitor monitor) throws DBException {
        return getDataSource().getDataTypes(monitor);
    }

    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return getTables(monitor);
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName)
        throws DBException
    {
        return getTable(monitor, childName);
    }

    @Override
    public synchronized DBSObject refreshObject(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        this.tableCache.clearCache();
        this.indexCache.clearCache();
        this.primaryKeysCache.clearCache();
        this.foreignKeysCache.clearCache();
        this.packages = null;
        this.procedures = null;
        this.sequences = null;
        return this;
    }

    public String toString()
    {
        return getName() == null ? "<NONE>" : getName();
    }

    private synchronized void loadProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        dataSource.getMetaModel().loadProcedures(monitor, this);

        // Order procedures
        if (procedures != null) {
            DBUtils.orderObjects(procedures);
        }
        if (packages != null) {
            for (GenericPackage pack : packages) {
                pack.orderProcedures();
            }
        }
    }

    public void addProcedure(GenericProcedure procedure) {
        if (procedures == null) {
            procedures = new ArrayList<>();
        }
        procedures.add(procedure);
    }

    public boolean hasProcedure(String name) {
        if (procedures != null) {
            for (GenericProcedure proc : procedures) {
                if (proc.getName().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void addPackage(GenericPackage procedurePackage) {
        if (packages == null) {
            packages = new ArrayList<>();
        }
        packages.add(procedurePackage);
    }

    private synchronized void loadSequences(DBRProgressMonitor monitor)
        throws DBException
    {
        sequences = dataSource.getMetaModel().loadSequences(monitor, this);

        // Order procedures
        if (sequences == null) {
            sequences = new ArrayList<>();
        } else {
            DBUtils.orderObjects(sequences);
        }
    }

    private synchronized List<? extends GenericTrigger> loadTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        List<? extends GenericTrigger> triggers = dataSource.getMetaModel().loadTriggers(monitor, this, null);

        // Order procedures
        if (this.triggers == null) {
            this.triggers = new ArrayList<>();
        } else {
            DBUtils.orderObjects(this.triggers);
        }
        return triggers;
    }

}
