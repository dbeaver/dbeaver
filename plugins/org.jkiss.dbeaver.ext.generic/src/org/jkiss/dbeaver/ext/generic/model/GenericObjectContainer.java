/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.Log;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
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
    static final Log log = Log.getLog(GenericObjectContainer.class);

    @NotNull
    private final GenericDataSource dataSource;
    private final TableCache tableCache;
    private final IndexCache indexCache;
    private final ForeignKeysCache foreignKeysCache;
    private final PrimaryKeysCache primaryKeysCache;
    private List<GenericPackage> packages;
    protected List<GenericProcedure> procedures;
    protected List<GenericSequence> sequences;

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
            List<GenericTable> filtered = new ArrayList<GenericTable>();
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
            List<GenericTable> filtered = new ArrayList<GenericTable>();
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
            if (!indexCache.isCached()) {

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
                if (!indexCache.isCached() && readFromTables) {
                    // Load indexes for all tables and return copy of them
                    Collection<GenericTable> tables = getTables(monitor);
                    monitor.beginTask("Cache indexes from tables", tables.size());
                    try {
                        List<GenericTableIndex> tmpIndexMap = new ArrayList<GenericTableIndex>();
                        for (GenericTable table : tables) {
                            if (monitor.isCanceled()) {
                                return;
                            }
                            monitor.subTask("Read indexes for '" + table.getFullQualifiedName() + "'");
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
    public void cacheStructure(DBRProgressMonitor monitor, int scope)
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
    public Collection<? extends DBSObject> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        return getTables(monitor);
    }

    @Override
    public DBSObject getChild(DBRProgressMonitor monitor, String childName)
        throws DBException
    {
        return getTable(monitor, childName);
    }

    @Override
    public synchronized boolean refreshObject(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        this.tableCache.clearCache();
        this.indexCache.clearCache();
        this.primaryKeysCache.clearCache();
        this.foreignKeysCache.clearCache();
        this.packages = null;
        this.procedures = null;
        this.sequences = null;
        return true;
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
            procedures = new ArrayList<GenericProcedure>();
        }
        procedures.add(procedure);
    }

    public void addPackage(GenericPackage procedurePackage) {
        if (packages == null) {
            packages = new ArrayList<GenericPackage>();
        }
        packages.add(procedurePackage);
    }

    private synchronized void loadSequences(DBRProgressMonitor monitor)
        throws DBException
    {
        sequences = dataSource.getMetaModel().loadSequences(monitor, this);

        // Order procedures
        if (sequences == null) {
            sequences = new ArrayList<GenericSequence>();
        } else {
            DBUtils.orderObjects(sequences);
        }
    }

}
