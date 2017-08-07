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
package org.jkiss.dbeaver.ext.generic.model.meta;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPErrorAssistant;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * Generic meta model
 */
public class GenericMetaModel {

    private static final Log log = Log.getLog(GenericMetaModel.class);
    GenericMetaModelDescriptor descriptor;

    public GenericMetaModel()
    {
    }

    public GenericMetaObject getMetaObject(String id) {
        return descriptor == null ? null : descriptor.getObject(id);
    }

    public GenericDataSource createDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new GenericDataSource(monitor, container, this, new GenericSQLDialect());
    }

    public void loadProcedures(DBRProgressMonitor monitor, @NotNull GenericObjectContainer container)
        throws DBException
    {
        Map<String, GenericPackage> packageMap = null;

        GenericDataSource dataSource = container.getDataSource();
        GenericMetaObject procObject = dataSource.getMetaObject(GenericConstants.OBJECT_PROCEDURE);
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Load procedures")) {
            // Read procedures
            JDBCResultSet dbResult = session.getMetaData().getProcedures(
                container.getCatalog() == null ? null : container.getCatalog().getName(),
                container.getSchema() == null ? null : container.getSchema().getName(),
                dataSource.getAllObjectsPattern());
            try {
                while (dbResult.next()) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    String procedureCatalog = GenericUtils.safeGetStringTrimmed(procObject, dbResult, JDBCConstants.PROCEDURE_CAT);
                    String procedureName = GenericUtils.safeGetStringTrimmed(procObject, dbResult, JDBCConstants.PROCEDURE_NAME);
                    String specificName = GenericUtils.safeGetStringTrimmed(procObject, dbResult, JDBCConstants.SPECIFIC_NAME);
                    int procTypeNum = GenericUtils.safeGetInt(procObject, dbResult, JDBCConstants.PROCEDURE_TYPE);
                    String remarks = GenericUtils.safeGetString(procObject, dbResult, JDBCConstants.REMARKS);
                    DBSProcedureType procedureType;
                    switch (procTypeNum) {
                        case DatabaseMetaData.procedureNoResult: procedureType = DBSProcedureType.PROCEDURE; break;
                        case DatabaseMetaData.procedureReturnsResult: procedureType = DBSProcedureType.FUNCTION; break;
                        case DatabaseMetaData.procedureResultUnknown: procedureType = DBSProcedureType.PROCEDURE; break;
                        default: procedureType = DBSProcedureType.UNKNOWN; break;
                    }
                    if (specificName == null && procedureName.indexOf(';') != -1) {
                        // [JDBC: SQL Server native driver]
                        specificName = procedureName;
                        procedureName = procedureName.substring(0, procedureName.lastIndexOf(';'));
                    }
                    // Check for packages. Oracle (and may be some other databases) uses catalog name as storage for package name
                    String packageName = null;
                    GenericPackage procedurePackage = null;
                    if (!CommonUtils.isEmpty(procedureCatalog) && CommonUtils.isEmpty(dataSource.getCatalogs())) {
                        // Catalog name specified while there are no catalogs in data source
                        packageName = procedureCatalog;
                    }

                    if (!CommonUtils.isEmpty(packageName)) {
                        if (packageMap == null) {
                            packageMap = new TreeMap<>();
                        }
                        procedurePackage = packageMap.get(packageName);
                        if (procedurePackage == null) {
                            procedurePackage = new GenericPackage(container, packageName, true);
                            packageMap.put(packageName, procedurePackage);
                            container.addPackage(procedurePackage);
                        }
                    }

                    final GenericProcedure procedure = createProcedureImpl(
                        procedurePackage != null ? procedurePackage : container,
                        procedureName,
                        specificName,
                        remarks,
                        procedureType,
                        null);
                    if (procedurePackage != null) {
                        procedurePackage.addProcedure(procedure);
                    } else {
                        container.addProcedure(procedure);
                    }
                }
            }
            finally {
                dbResult.close();
            }

            try {
                // Try to read functions (note: this function appeared only in Java 1.6 so it maybe not implemented by many drivers)
                // Read procedures
                dbResult = session.getMetaData().getFunctions(
                    container.getCatalog() == null ? null : container.getCatalog().getName(),
                    container.getSchema() == null ? null : container.getSchema().getName(),
                    dataSource.getAllObjectsPattern());
                try {
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        String functionName = GenericUtils.safeGetStringTrimmed(procObject, dbResult, JDBCConstants.FUNCTION_NAME);
                        if (functionName == null) {
                            //functionName = GenericUtils.safeGetStringTrimmed(procObject, dbResult, JDBCConstants.PROCEDURE_NAME);
                            // Apparently some drivers return the same results for getProcedures and getFunctions -
                            // so let's skip yet another procedure list
                            continue;
                        }
                        String specificName = GenericUtils.safeGetStringTrimmed(procObject, dbResult, JDBCConstants.SPECIFIC_NAME);
                        if (specificName == null && functionName.indexOf(';') != -1) {
                            // [JDBC: SQL Server native driver]
                            specificName = functionName;
                            functionName = functionName.substring(0, functionName.lastIndexOf(';'));
                        }
                        if (container.hasProcedure(functionName)) {
                            // Seems to be a duplicate
                            continue;
                        }
                        int funcTypeNum = GenericUtils.safeGetInt(procObject, dbResult, JDBCConstants.FUNCTION_TYPE);
                        String remarks = GenericUtils.safeGetString(procObject, dbResult, JDBCConstants.REMARKS);
                        GenericFunctionResultType functionResultType;
                        switch (funcTypeNum) {
                            //case DatabaseMetaData.functionResultUnknown: functionResultType = GenericFunctionResultType.UNKNOWN; break;
                            case DatabaseMetaData.functionNoTable: functionResultType = GenericFunctionResultType.NO_TABLE; break;
                            case DatabaseMetaData.functionReturnsTable: functionResultType = GenericFunctionResultType.TABLE; break;
                            default: functionResultType = GenericFunctionResultType.UNKNOWN; break;
                        }

                        final GenericProcedure procedure = createProcedureImpl(
                            container,
                            functionName,
                            specificName,
                            remarks,
                            DBSProcedureType.FUNCTION,
                            functionResultType);
                        container.addProcedure(procedure);
                    }
                }
                finally {
                    dbResult.close();
                }
            } catch (Throwable e) {
                log.debug("Can't read generic functions", e);
            }

        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        }
    }

    public GenericProcedure createProcedureImpl(
        GenericStructContainer container,
        String procedureName,
        String specificName,
        String remarks,
        DBSProcedureType procedureType,
        GenericFunctionResultType functionResultType)
    {
        return new GenericProcedure(
            container,
            procedureName,
            specificName,
            remarks,
            procedureType,
            functionResultType);
    }

    public String getViewDDL(DBRProgressMonitor monitor, GenericTable sourceObject) throws DBException {
        return "-- View definition not available";
    }

    public String getTableDDL(DBRProgressMonitor monitor, GenericTable sourceObject) throws DBException {
        return JDBCUtils.generateTableDDL(monitor, sourceObject, false);
    }

    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure sourceObject) throws DBException {
        return "-- Source code not available";
    }

    public boolean supportsSequences(@NotNull GenericDataSource dataSource) {
        return false;
    }

    public List<GenericSequence> loadSequences(@NotNull DBRProgressMonitor monitor, @NotNull GenericStructContainer container) throws DBException {
        return new ArrayList<>();
    }

    public boolean supportsTriggers(@NotNull GenericDataSource dataSource) {
        return false;
    }

    public boolean supportsDatabaseTriggers(@NotNull GenericDataSource dataSource) {
        return false;
    }

    public List<? extends GenericTrigger> loadTriggers(DBRProgressMonitor monitor, @NotNull GenericStructContainer container, @Nullable GenericTable table) throws DBException {
        return new ArrayList<>();
    }

    public String getTriggerDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericTrigger trigger) throws DBException {
        return "-- Source code not available";
    }

    public JDBCBasicDataTypeCache<GenericStructContainer, ? extends JDBCDataType> createDataTypeCache(@NotNull GenericStructContainer container) {
        return new GenericDataTypeCache(container);
    }

    public DBCQueryPlanner getQueryPlanner(@NotNull GenericDataSource dataSource) {
        return null;
    }

    public DBPErrorAssistant.ErrorPosition getErrorPosition(@NotNull Throwable error) {
        return null;
    }

    public boolean supportsUpsertStatement() {
        return false;
    }

    public GenericTable createTableImpl(
        GenericStructContainer container,
        @Nullable String tableName,
        @Nullable String tableType,
        @Nullable JDBCResultSet dbResult)
    {
        return new GenericTable(
            container,
            tableName,
            tableType,
            dbResult);
    }

    public GenericTableIndex createIndexImpl(
        GenericTable table,
        boolean nonUnique,
        String qualifier,
        long cardinality,
        String indexName,
        DBSIndexType indexType,
        boolean persisted)
    {
        return new GenericTableIndex(
            table,
            nonUnique,
            qualifier,
            cardinality,
            indexName,
            indexType,
            persisted);
    }

    /**
     * Returns SQL clause for table column auto-increment.
     * Null if auto-increment is not supported.
     */
    public String getAutoIncrementClause(GenericTableColumn column) {
        return null;
    }

    public boolean useCatalogInObjectNames() {
        return true;
    }

    public boolean isSystemTable(GenericTable table) {
        final String tableType = table.getTableType().toUpperCase(Locale.ENGLISH);
        return tableType.contains("SYSTEM");
    }

    public boolean isView(GenericTable table) {
        return table.getTableType().toUpperCase(Locale.ENGLISH).contains("VIEW");
    }

}
