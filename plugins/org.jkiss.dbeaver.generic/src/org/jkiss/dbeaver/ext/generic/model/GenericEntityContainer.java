/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * GenericEntityContainer
 */
public abstract class GenericEntityContainer implements GenericStructContainer
{
    static final Log log = LogFactory.getLog(GenericEntityContainer.class);

    private GenericDataSource dataSource;
    private final TableCache tableCache;
    private final IndexCache indexCache;
    private final ForeignKeysCache foreignKeysCache;
    private final PrimaryKeysCache primaryKeysCache;
    private Map<String, GenericPackage> packageMap;
    private List<GenericProcedure> procedures;

    protected GenericEntityContainer(GenericDataSource dataSource)
    {
        this.dataSource = dataSource;
        this.tableCache = new TableCache();
        this.indexCache = new IndexCache(tableCache);
        this.primaryKeysCache = new PrimaryKeysCache(tableCache);
        this.foreignKeysCache = new ForeignKeysCache(tableCache);
    }

    public final TableCache getTableCache()
    {
        return tableCache;
    }

    public final IndexCache getIndexCache()
    {
        return indexCache;
    }

    public final PrimaryKeysCache getPrimaryKeysCache()
    {
        return primaryKeysCache;
    }

    public final ForeignKeysCache getForeignKeysCache()
    {
        return foreignKeysCache;
    }

    public GenericDataSource getDataSource()
    {
        return dataSource;
    }


    public boolean isPersisted()
    {
        return true;
    }

    public Collection<GenericTable> getTables(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getObjects(monitor, this);
    }

    public GenericTable getTable(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return tableCache.getObject(monitor, this, name);
    }

    public Collection<GenericIndex> getIndexes(DBRProgressMonitor monitor)
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
                    indexCache.getObjects(monitor, this, null);
                } catch (Exception e) {
                    // Failed
                    if (readFromTables) {
                        // Load indexes for all tables and return copy of them
                        List<GenericIndex> tmpIndexMap = new ArrayList<GenericIndex>();
                        for (GenericTable table : getTables(monitor)) {
                            tmpIndexMap.addAll(table.getIndexes(monitor));
                        }
                        indexCache.setCache(tmpIndexMap);
                    }
                }
            }
        }
    }

    public void cacheStructure(DBRProgressMonitor monitor, int scope)
        throws DBException
    {
        // Cache tables
        if ((scope & STRUCT_ENTITIES) != 0) {
            monitor.subTask("Cache tables");
            tableCache.getObjects(monitor, this);
        }

        // Cache attributes
        if ((scope & STRUCT_ATTRIBUTES) != 0) {
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
        if ((scope & STRUCT_ASSOCIATIONS) != 0) {
            // Try to read all PKs
            // Try to read all FKs
            try {
                monitor.subTask("Cache primary keys");
                primaryKeysCache.getObjects(monitor, this, null);
            } catch (Exception e) {
                // Failed - seems to be unsupported feature
                log.debug(e);
            }

            if (getDataSource().getInfo().supportsIndexes()) {
                // Try to read all indexes
                monitor.subTask("Cache indexes");
                cacheIndexes(monitor, false);
            }

            if (getDataSource().getInfo().supportsReferentialIntegrity()) {
                // Try to read all FKs
                try {
                    monitor.subTask("Cache foreign keys");
                    foreignKeysCache.getObjects(monitor, this, null);
                } catch (Exception e) {
                    // Failed - seems to be unsupported feature
                    log.debug(e);
                }
            }
        }
    }

    public Collection<GenericPackage> getPackages(DBRProgressMonitor monitor)
        throws DBException
    {
        if (procedures == null) {
            loadProcedures(monitor);
        }
        return packageMap == null ? null : packageMap.values();
    }

    public GenericPackage getPackage(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return DBUtils.findObject(getPackages(monitor), name);
    }

    public Collection<GenericProcedure> getProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        if (procedures == null) {
            loadProcedures(monitor);
        }
        return procedures;
    }

    public Collection<GenericProcedure> getProcedures(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return DBUtils.findObjects(getProcedures(monitor), name);
    }

    public Collection<? extends DBSEntity> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        return getTables(monitor);
    }

    public DBSEntity getChild(DBRProgressMonitor monitor, String childName)
        throws DBException
    {
        return getTable(monitor, childName);
    }

    public boolean refreshEntity(DBRProgressMonitor monitor)
        throws DBException
    {
        this.tableCache.clearCache();
        this.indexCache.clearCache();
        this.primaryKeysCache.clearCache();
        this.foreignKeysCache.clearCache();
        this.packageMap = null;
        this.procedures = null;
        return true;
    }

    public String toString()
    {
        return getName() == null ? "<NONE>" : getName();
    }

    private synchronized void loadProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load procedures");
        try {
            JDBCResultSet dbResult = context.getMetaData().getProcedures(
                getCatalog() == null ? null : getCatalog().getName(),
                getSchema() == null ? null : getSchema().getName(),
                null);
            try {
                while (dbResult.next()) {
                    String procedureCatalog = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.PROCEDURE_CAT);
                    String procedureName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.PROCEDURE_NAME);
                    String specificName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.SPECIFIC_NAME);
                    int procTypeNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.PROCEDURE_TYPE);
                    String remarks = JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS);
                    DBSProcedureType procedureType;
                    switch (procTypeNum) {
                        case DatabaseMetaData.procedureNoResult: procedureType = DBSProcedureType.PROCEDURE; break;
                        case DatabaseMetaData.procedureReturnsResult: procedureType = DBSProcedureType.FUNCTION; break;
                        case DatabaseMetaData.procedureResultUnknown: procedureType = DBSProcedureType.PROCEDURE; break;
                        default: procedureType = DBSProcedureType.UNKNOWN; break;
                    }
                    // Check for packages. Oracle (and may be some other databases) uses catalog name as storage for package name
                    String packageName = null;
                    GenericPackage procedurePackage = null;
                    if (!CommonUtils.isEmpty(procedureCatalog) && CommonUtils.isEmpty(getDataSource().getCatalogs())) {
                        // Catalog name specified while there are no catalogs in data source
                        packageName = procedureCatalog;
                    }

                    if (!CommonUtils.isEmpty(packageName)) {
                        if (packageMap == null) {
                            packageMap = new TreeMap<String, GenericPackage>();
                        }
                        procedurePackage = packageMap.get(packageName);
                        if (procedurePackage == null) {
                            procedurePackage = new GenericPackage(GenericEntityContainer.this, packageName, true);
                            packageMap.put(packageName, procedurePackage);
                        }
                    }

                    final GenericProcedure procedure = new GenericProcedure(
                        procedurePackage != null ? procedurePackage : this,
                        procedureName,
                        specificName,
                        remarks,
                        procedureType);
                    if (procedurePackage != null) {
                        procedurePackage.addProcedure(procedure);
                    } else {
                        if (procedures == null) {
                            procedures = new ArrayList<GenericProcedure>();
                        }
                        procedures.add(procedure);
                    }
                }
                // Order procedures
                if (procedures != null) {
                    DBUtils.orderObjects(procedures);
                }
                if (packageMap != null) {
                    for (GenericPackage pack : packageMap.values()) {
                        pack.orderProcedures();
                    }
                }
            }
            finally {
                dbResult.close();
            }
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            context.close();
        }
    }

}
