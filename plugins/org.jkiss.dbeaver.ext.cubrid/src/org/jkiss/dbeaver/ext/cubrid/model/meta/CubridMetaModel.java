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
package org.jkiss.dbeaver.ext.cubrid.model.meta;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.cubrid.model.*;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCFeatureNotSupportedException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceInfo;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.navigator.DBNBrowseSettings;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyDeferability;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.*;

/**
 * Cubrid meta model
 */
public class CubridMetaModel {

    private static final Log log = Log.getLog(CubridMetaModel.class);
    private static final String DEFAULT_NULL_SCHEMA_NAME = "DEFAULT";

    // Tables types which are not actually a table
    // This is needed for some strange JDBC drivers which returns not a table objects
    // in DatabaseMetaData.getTables method (PostgreSQL especially)
    private static final Set<String> INVALID_TABLE_TYPES = new HashSet<>();

    static {
        // [JDBC: PostgreSQL]
        INVALID_TABLE_TYPES.add("INDEX");
        INVALID_TABLE_TYPES.add("SEQUENCE");
        INVALID_TABLE_TYPES.add("TYPE");
        INVALID_TABLE_TYPES.add("SYSTEM INDEX");
        INVALID_TABLE_TYPES.add("SYSTEM SEQUENCE");
        // [JDBC: SQLite]
        INVALID_TABLE_TYPES.add("TRIGGER");
    }


    CubridMetaModelDescriptor descriptor;

    public CubridMetaModel()
    {
    }

    public CubridMetaObject getMetaObject(String id) {
        return descriptor == null ? null : descriptor.getObject(id);
    }

    //////////////////////////////////////////////////////
    // Datasource

    public CubridDataSource createDataSourceImpl(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new CubridDataSource(monitor, container, this, new CubridSQLDialect());
    }

    //////////////////////////////////////////////////////
    // Misc

    public JDBCBasicDataTypeCache<CubridStructContainer, ? extends JDBCDataType> createDataTypeCache(@NotNull CubridStructContainer container) {
        return new CubridDataTypeCache(container);
    }

    public DBCQueryPlanner getQueryPlanner(@NotNull CubridDataSource dataSource) {
        return null;
    }

    public DBPErrorAssistant.ErrorPosition getErrorPosition(@NotNull Throwable error) {
        return null;
    }

    public boolean supportsUpsertStatement() {
        return false;
    }

    /**
     * Returns SQL clause for table column auto-increment.
     * Null if auto-increment is not supported.
     */
    public String getAutoIncrementClause(CubridTableColumn column) {
        return null;
    }

    public boolean useCatalogInObjectNames() {
        return true;
    }

    //////////////////////////////////////////////////////
    // Schema load

    // True if schemas can be omitted.
    // App will suppress any error during schema read then
    public boolean isSchemasOptional() {
        return true;
    }

    public boolean isSystemSchema(CubridSchema schema) {
        return false;
    }

    public List<CubridSchema> loadSchemas(JDBCSession session, CubridDataSource dataSource, CubridCatalog catalog)
        throws DBException
    {
        if (dataSource.isOmitSchema()) {
            return null;
        }

        try {
            final CubridMetaObject schemaObject = getMetaObject(CubridConstants.OBJECT_SCHEMA);
            final DBSObjectFilter schemaFilters = dataSource.getContainer().getObjectFilter(CubridSchema.class, catalog, false);

            final List<CubridSchema> tmpSchemas = new ArrayList<>();
            JDBCResultSet dbResult = null;
            boolean catalogSchemas = false, schemasFiltered = false;
            if (catalog != null) {
                try {
                    dbResult = session.getMetaData().getSchemas(
                        catalog.getName(),
                        schemaFilters != null && schemaFilters.hasSingleMask() ?
                            schemaFilters.getSingleMask() :
                            dataSource.getAllObjectsPattern());
                    catalogSchemas = true;
                } catch (Throwable e) {
                    if (isSchemasOptional()) {
                        // This method not supported (may be old driver version)
                        // Use general schema reading method
                        log.debug("Error reading schemas in catalog '" + catalog.getName() + "' - " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    } else {
                        throw e;
                    }
                }
            } else if (dataSource.isSchemaFiltersEnabled()) {
                // In some drivers (e.g. jt400) reading schemas with empty catalog leads to
                // incorrect results.
                try {
                    dbResult = session.getMetaData().getSchemas(
                        null,
                        schemaFilters != null && schemaFilters.hasSingleMask() ?
                            schemaFilters.getSingleMask() :
                            dataSource.getAllObjectsPattern());
                } catch (Throwable e) {
                    if (isSchemasOptional()) {
                        log.debug("Error reading global schemas " + " - " + e.getMessage());
                    } else {
                        throw e;
                    }
                }
            }
            if (dbResult == null) {

                String oldCatalog = null;
                if (supportsCatalogChange() && catalog != null) {
                    // Try to set catalog explicitly. May be needed for old drivers (Netezza)
                    try {
                        oldCatalog = session.getCatalog();
                    } catch (Throwable ignored) {
                    }
                    if (oldCatalog != null && !CommonUtils.equalObjects(oldCatalog, catalog.getName())) {
                        try {
                            session.setCatalog(catalog.getName());
                        } catch (Throwable e) {
                            oldCatalog = null;
                        }
                    } else {
                        oldCatalog = null;
                    }
                }

                try {
                    dbResult = session.getMetaData().getSchemas();
                } finally {
                    if (oldCatalog != null) {
                        try {
                            session.setCatalog(oldCatalog);
                        } catch (Throwable e) {
                            log.debug("Error while setting active catalog name back to '" + oldCatalog + "'", e);
                        }
                    }
                }
            }

            try {
                while (dbResult.next()) {
                    if (session.getProgressMonitor().isCanceled()) {
                        break;
                    }
                    String schemaName = CubridUtils.safeGetString(schemaObject, dbResult, JDBCConstants.TABLE_SCHEM);
                    if (CommonUtils.isEmpty(schemaName)) {
                        // some drivers uses TABLE_OWNER column instead of TABLE_SCHEM
                        schemaName = CubridUtils.safeGetString(schemaObject, dbResult, JDBCConstants.TABLE_OWNER);
                    }
                    boolean nullSchema = false;
                    if (CommonUtils.isEmpty(schemaName)) {
                        if (supportsNullSchemas()) {
                            schemaName = DEFAULT_NULL_SCHEMA_NAME;
                            nullSchema = true;
                        } else {
                            continue;
                        }
                    }
                    if (schemaFilters != null && !schemaFilters.matches(schemaName)) {
                        // Doesn't match filter
                        schemasFiltered = true;
                        continue;
                    }
                    String catalogName = CubridUtils.safeGetString(schemaObject, dbResult, JDBCConstants.TABLE_CATALOG);

                    if (!CommonUtils.isEmpty(catalogName)) {
                        if (catalog == null) {
                            if (!dataSource.isOmitCatalog()) {
                                // Invalid schema's catalog or schema without catalog (then do not use schemas as structure)
                                log.debug("Catalog name (" + catalogName + ") found for schema '" + schemaName + "' while schema doesn't have parent catalog");
                            }
                        } else if (!catalog.getName().equals(catalogName)) {
                            if (!catalogSchemas) {
                                // Just skip it - we have list of all existing schemas and this one belongs to another catalog
                                continue;
                            }
                            log.debug("Catalog name '" + catalogName + "' differs from schema's catalog '" + catalog.getName() + "'");
                        }
                    }

                    //session.getProgressMonitor().subTask("Schema " + schemaName);

                    CubridSchema schema = createSchemaImpl(dataSource, catalog, schemaName);
                    if (nullSchema) {
                        schema.setVirtual(true);
                    }
                    tmpSchemas.add(schema);
                }
            } finally {
                dbResult.close();
            }
            if (tmpSchemas.isEmpty() && catalogSchemas && !schemasFiltered && dataSource.getCatalogs().size() == 1) {
                // There is just one catalog and empty schema list. Try to read global schemas
                return loadSchemas(session, dataSource, null);
            }
            if (dataSource.isOmitSingleSchema() && catalog == null && tmpSchemas.size() == 1 && (schemaFilters == null || schemaFilters.isNotApplicable())) {
                // Only one schema and no catalogs
                // Most likely it is a fake one, let's skip it
                // Anyway using "%" instead is ok
                tmpSchemas.clear();
            }
            return tmpSchemas;
        } catch (UnsupportedOperationException | SQLFeatureNotSupportedException e) {
            // Schemas are not supported
            log.debug("Can't read schema list: " + e.getMessage());
            return null;
        } catch (Throwable ex) {
            if (isSchemasOptional()) {
                // Schemas are not supported - just ignore this error
                log.warn("Can't read schema list", ex);
                return null;
            } else {
                log.error("Can't read schema list", ex);
                throw new DBException(ex, dataSource);
            }
        }
    }

    protected boolean supportsCatalogChange() {
        return false;
    }

    // Schema with NULL name is a valid schema [Phoenix]
    public boolean supportsNullSchemas() {
        return false;
    }

    public CubridSchema createSchemaImpl(@NotNull CubridDataSource dataSource, @Nullable CubridCatalog catalog, @NotNull String schemaName) throws DBException {
        return new CubridSchema(dataSource, catalog, schemaName);
    }

    //////////////////////////////////////////////////////
    // Procedure load

    public void loadProcedures(DBRProgressMonitor monitor, @NotNull CubridObjectContainer container)
        throws DBException
    {
        Map<String, CubridPackage> packageMap = null;

        Map<String, CubridProcedure> funcMap = new LinkedHashMap<>();

        CubridDataSource dataSource = container.getDataSource();
        CubridMetaObject procObject = dataSource.getMetaObject(CubridConstants.OBJECT_PROCEDURE);
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Load procedures")) {
            boolean supportsFunctions = false;
            if (hasFunctionSupport()) {
                try {
                    // Try to read functions (note: this function appeared only in Java 1.6 so it maybe not implemented by many drivers)
                    // Read procedures
                    JDBCResultSet dbResult = session.getMetaData().getFunctions(
                            container.getCatalog() == null ? null : container.getCatalog().getName(),
                            container.getSchema() == null || DBUtils.isVirtualObject(container.getSchema()) ? null : JDBCUtils.escapeWildCards(session, container.getSchema().getName()),
                            dataSource.getAllObjectsPattern());
                    try {
                        supportsFunctions = true;
                        while (dbResult.next()) {
                            if (monitor.isCanceled()) {
                                break;
                            }
                            String functionName = CubridUtils.safeGetStringTrimmed(procObject, dbResult, JDBCConstants.FUNCTION_NAME);
                            if (functionName == null) {
                                //functionName = CubridUtils.safeGetStringTrimmed(procObject, dbResult, JDBCConstants.PROCEDURE_NAME);
                                // Apparently some drivers return the same results for getProcedures and getFunctions -
                                // so let's skip yet another procedure list
                                continue;
                            }
                            String specificName = CubridUtils.safeGetStringTrimmed(procObject, dbResult, JDBCConstants.SPECIFIC_NAME);
                            if (specificName == null && functionName.indexOf(';') != -1) {
                                // [JDBC: SQL Server native driver]
                                specificName = functionName;
                                functionName = functionName.substring(0, functionName.lastIndexOf(';'));
                            }
                            if (container.hasProcedure(functionName)) {
                                // Seems to be a duplicate
                                continue;
                            }
                            int funcTypeNum = CubridUtils.safeGetInt(procObject, dbResult, JDBCConstants.FUNCTION_TYPE);
                            String remarks = CubridUtils.safeGetString(procObject, dbResult, JDBCConstants.REMARKS);
                            CubridFunctionResultType functionResultType;
                            switch (funcTypeNum) {
                                //case DatabaseMetaData.functionResultUnknown: functionResultType = CubridFunctionResultType.UNKNOWN; break;
                                case DatabaseMetaData.functionNoTable:
                                    functionResultType = CubridFunctionResultType.NO_TABLE;
                                    break;
                                case DatabaseMetaData.functionReturnsTable:
                                    functionResultType = CubridFunctionResultType.TABLE;
                                    break;
                                default:
                                    functionResultType = CubridFunctionResultType.UNKNOWN;
                                    break;
                            }

                            final CubridProcedure procedure = createProcedureImpl(
                                    container,
                                    functionName,
                                    specificName,
                                    remarks,
                                    DBSProcedureType.FUNCTION,
                                    functionResultType);
                            container.addProcedure(procedure);

                            funcMap.put(specificName == null ? functionName : specificName, procedure);
                        }
                    } finally {
                        dbResult.close();
                    }
                } catch (Throwable e) {
                    log.debug("Can't read cubrid functions", e);
                }
            }

            if (hasProcedureSupport()) {
                {
                    // Read procedures
                    JDBCResultSet dbResult = session.getMetaData().getProcedures(
                            container.getCatalog() == null ? null : container.getCatalog().getName(),
                            container.getSchema() == null || DBUtils.isVirtualObject(container.getSchema()) ? null : JDBCUtils.escapeWildCards(session, container.getSchema().getName()),
                            dataSource.getAllObjectsPattern());
                    try {
                        while (dbResult.next()) {
                            if (monitor.isCanceled()) {
                                break;
                            }
                            String procedureCatalog = CubridUtils.safeGetStringTrimmed(procObject, dbResult, JDBCConstants.PROCEDURE_CAT);
                            String procedureName = CubridUtils.safeGetStringTrimmed(procObject, dbResult, JDBCConstants.PROCEDURE_NAME);
                            String specificName = CubridUtils.safeGetStringTrimmed(procObject, dbResult, JDBCConstants.SPECIFIC_NAME);
                            int procTypeNum = CubridUtils.safeGetInt(procObject, dbResult, JDBCConstants.PROCEDURE_TYPE);
                            String remarks = CubridUtils.safeGetString(procObject, dbResult, JDBCConstants.REMARKS);
                            DBSProcedureType procedureType;
                            switch (procTypeNum) {
                                case DatabaseMetaData.procedureNoResult:
                                    procedureType = DBSProcedureType.PROCEDURE;
                                    break;
                                case DatabaseMetaData.procedureReturnsResult:
                                    procedureType = supportsFunctions ? DBSProcedureType.PROCEDURE : DBSProcedureType.FUNCTION;
                                    break;
                                case DatabaseMetaData.procedureResultUnknown:
                                    procedureType = DBSProcedureType.PROCEDURE;
                                    break;
                                default:
                                    procedureType = DBSProcedureType.UNKNOWN;
                                    break;
                            }
                            if (CommonUtils.isEmpty(specificName)) {
                                specificName = procedureName;
                            }
                            CubridProcedure function = funcMap.get(specificName);
                            if (function != null && !supportsEqualFunctionsAndProceduresNames()) {
                                // Broken driver
                                log.debug("Broken driver [" + session.getDataSource().getContainer().getDriver().getName() + "] - returns the same list for getProcedures and getFunctons");
                                break;
                            }
                            procedureName = CubridUtils.normalizeProcedureName(procedureName);

                            CubridPackage procedurePackage = null;
                            // FIXME: remove as a silly workaround
                            String packageName = getPackageName(dataSource, procedureCatalog, procedureName, specificName);
                            if (packageName != null) {
                                if (!CommonUtils.isEmpty(packageName)) {
                                    if (packageMap == null) {
                                        packageMap = new TreeMap<>();
                                    }
                                    procedurePackage = packageMap.get(packageName);
                                    if (procedurePackage == null) {
                                        procedurePackage = new CubridPackage(container, packageName, true);
                                        packageMap.put(packageName, procedurePackage);
                                        container.addPackage(procedurePackage);
                                    }
                                }
                            }

                            final CubridProcedure procedure = createProcedureImpl(
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
                    } finally {
                        dbResult.close();
                    }
                }
            }

        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        }
    }

    /**
     * Many databases can not have procedures and functions with equal specific names - this is database restriction.
     * They can have procedures/functions with equal names and different parameters (overloaded).
     *
     * @return true if the database can have in one container procedure and function with equal names (considering parameters)
     */
    public boolean supportsEqualFunctionsAndProceduresNames() {
        return false;
    }

    public CubridProcedure createProcedureImpl(
        CubridStructContainer container,
        String procedureName,
        String specificName,
        String remarks,
        DBSProcedureType procedureType,
        CubridFunctionResultType functionResultType)
    {
        return new CubridProcedure(
            container,
            procedureName,
            specificName,
            remarks,
            procedureType,
            functionResultType);
    }

    public String getProcedureDDL(DBRProgressMonitor monitor, CubridProcedure sourceObject) throws DBException {
        return "-- Source code not available";
    }

    public String getPackageName(CubridDataSource dataSource, String catalogName, String procedureName, String specificName) {

        // Caused problems in #6241. Probably we should remove it (for now getPackageName always returns null so it is disabled anyway)
        if (!CommonUtils.isEmpty(catalogName) && CommonUtils.isEmpty(dataSource.getCatalogs())) {
            // Check for packages. Oracle (and may be some other databases) uses catalog name as a storage for package name
            // In fact it is a legacy code from ancient times (before Oracle extension was added).

            // Catalog name specified while there are no catalogs in data source
            //return catalogName;
        }

        return null;
    }



    public boolean supportsOverloadedProcedureNames() {
        return false;
    }

    public boolean showProcedureParamNames() {
        return false;
    }

    //////////////////////////////////////////////////////
    // Catalog load

    // True if catalogs can be omitted.
    // App will suppress any error during catalog read then
    public boolean isCatalogsOptional() {
        return true;
    }

    public CubridCatalog createCatalogImpl(@NotNull CubridDataSource dataSource, @NotNull String catalogName) {
        return new CubridCatalog(dataSource, catalogName);
    }

    //////////////////////////////////////////////////////
    // Tables

    /**
     * Prepares statement which returns results with following columns (the same as in JDBC spec).
     * May also contain any other db-specific columns
     *  <OL>
     *  <LI><B>TABLE_CAT</B> String {@code =>} table catalog (may be <code>null</code>)
     *  <LI><B>TABLE_SCHEM</B> String {@code =>} table schema (may be <code>null</code>)
     *  <LI><B>TABLE_NAME</B> String {@code =>} table name
     *  <LI><B>TABLE_TYPE</B> String {@code =>} table type.  Typical types are "TABLE",
     *                  "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY",
     *                  "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
     *  <LI><B>REMARKS</B> String {@code =>} explanatory comment on the table
     *  <LI><B>TYPE_CAT</B> String {@code =>} the types catalog (may be <code>null</code>)
     *  <LI><B>TYPE_SCHEM</B> String {@code =>} the types schema (may be <code>null</code>)
     *  <LI><B>TYPE_NAME</B> String {@code =>} type name (may be <code>null</code>)
     *  <LI><B>SELF_REFERENCING_COL_NAME</B> String {@code =>} name of the designated
     *                  "identifier" column of a typed table (may be <code>null</code>)
     *  <LI><B>REF_GENERATION</B> String {@code =>} specifies how values in
     *                  SELF_REFERENCING_COL_NAME are created. Values are
     *                  "SYSTEM", "USER", "DERIVED". (may be <code>null</code>)
     *  </OL>
     */
    public JDBCStatement prepareTableLoadStatement(@NotNull JDBCSession session, @NotNull CubridStructContainer owner, @Nullable CubridTableBase object, @Nullable String objectName)
            throws SQLException
    {
	   String sql= "select a.*, case when class_type = 'CLASS' then 'TABLE' \r\n"
	   		+ "when class_type = 'VCLASS' then 'VIEW' end as TABLE_TYPE, \r\n"
	   		+ "b.current_val from db_class a LEFT JOIN db_serial b on \r\n"
	   		+ "a.class_name = b.class_name where a.is_system_class='NO'";
       final JDBCPreparedStatement dbStat = session.prepareStatement(sql);

       return dbStat;
    }
    
    public JDBCStatement prepareSystemTableLoadStatement(@NotNull JDBCSession session, @NotNull CubridStructContainer owner, @Nullable CubridTableBase object, @Nullable String objectName)
            throws SQLException
    {
       String sql= "select *, class_name as TABLE_NAME, case when class_type = 'CLASS' \r\n"
    		+ "then 'TABLE' end as TABLE_TYPE from db_class\r\n"
       		+ "where class_type = 'CLASS' \r\n"
       		+ "and is_system_class = 'YES'";
       final JDBCPreparedStatement dbStat = session.prepareStatement(sql);

       return dbStat;
    }

    /**
     * Some drivers return columns, tables or other objects names with extra spaces around (like FireBird)
     * For this reason we usually trim it from our side
     * But other databases can have tables, columns, etc. with spaces around their names
     *
     * @return true if we trim objects names, false - if not
     */
    public boolean isTrimObjectNames() {
        return false;
    }

    public CubridTableBase createTableImpl(@NotNull JDBCSession session, @NotNull CubridStructContainer owner, @NotNull CubridMetaObject tableObject, @NotNull JDBCResultSet dbResult) {	
    	String tableName = CubridUtils.safeGetStringTrimmed(tableObject, dbResult, CubridConstants.CLASS_NAME);
        String tableType = CubridUtils.safeGetStringTrimmed(tableObject, dbResult, JDBCConstants.TABLE_TYPE);
        String tableSchema = CubridUtils.safeGetStringTrimmed(tableObject, dbResult, JDBCConstants.TABLE_SCHEM);
        CubridDataSource dataSource = owner.getDataSource();
        if (!CommonUtils.isEmpty(tableSchema) && dataSource.isOmitSchema()) {
            // Ignore tables with schema [Google Spanner]
            log.debug("Ignore table " + tableSchema + "." + tableName + " (schemas are omitted)");
            return null;
        }

        if (CommonUtils.isEmpty(tableName)) {
            log.debug("Empty table name " + (owner == null ? "" : " in container " + owner.getName()));
            return null;
        }

        if (tableType != null && INVALID_TABLE_TYPES.contains(tableType)) {
            // Bad table type. Just skip it
            return null;
        }
        if (DBUtils.isVirtualObject(owner) && !CommonUtils.isEmpty(tableSchema)) {
            // Wrong schema - this may happen with virtual schemas
            return null;
        }
        CubridTableBase table = this.createTableImpl(
            owner,
            tableName,
            tableType,
            dbResult);
        if (table == null) {
            return null;
        }

        DBNBrowseSettings navigatorSettings = dataSource.getContainer().getNavigatorSettings();
        boolean isSystemTable = table.isSystem();
        if (isSystemTable && !navigatorSettings.isShowSystemObjects()) {
            return null;
        }
        boolean isUtilityTable = table.isUtility();
        if (isUtilityTable && !navigatorSettings.isShowUtilityObjects()) {
            return null;
        }
        return table;
    }

    public CubridTableBase createTableImpl(
        CubridStructContainer container,
        @Nullable String tableName,
        @Nullable String tableType,
        @Nullable JDBCResultSet dbResult)
    {
        if (tableType != null && isView(tableType)) {
            return new CubridView(
                container,
                tableName,
                tableType,
                dbResult);
        }

        return new CubridTable(
            container,
            tableName,
            tableType,
            dbResult);
    }

    public String getViewDDL(DBRProgressMonitor monitor, CubridView sourceObject, Map<String, Object> options) throws DBException {
        return "-- View definition not available";
    }

    public String getTableDDL(DBRProgressMonitor monitor, CubridTableBase sourceObject, Map<String, Object> options) throws DBException {
        return DBStructUtils.generateTableDDL(monitor, sourceObject, options, false);
    }

    public boolean supportsTableDDLSplit(CubridTableBase sourceObject) {
        return true;
    }

    // Some database (like Informix) do not support foreign key declaration as nested.
    // DDL for these tables must contain definition of FK outside main brackets (ALTER TABLE ... ADD CONSTRAINT FOREIGN KEY)
    public boolean supportNestedForeignKeys() {
        return true;
    }

    public boolean isSystemTable(CubridTableBase table) {
        final String tableType = table.getTableType().toUpperCase(Locale.ENGLISH);
        return tableType.contains("SYSTEM");
    }

    public boolean isUtilityTable(@NotNull CubridTableBase table) {
        return false;
    }

    public boolean isView(String tableType) {
        return tableType.toUpperCase(Locale.ENGLISH).contains(CubridConstants.TABLE_TYPE_VIEW);
    }

    //////////////////////////////////////////////////////
    // Table columns

    public JDBCStatement prepareTableColumnLoadStatement(@NotNull JDBCSession session, @NotNull CubridStructContainer owner, @Nullable CubridTableBase forTable) throws SQLException {
        return session.getMetaData().getColumns(
            owner.getCatalog() == null ? null : owner.getCatalog().getName(),
            owner.getSchema() == null || DBUtils.isVirtualObject(owner.getSchema()) ? null : JDBCUtils.escapeWildCards(session, owner.getSchema().getName()),
            forTable == null ?
                owner.getDataSource().getAllObjectsPattern() :
                JDBCUtils.escapeWildCards(session, forTable.getName()),
            owner.getDataSource().getAllObjectsPattern())
            .getSourceStatement();
    }

    public CubridTableColumn createTableColumnImpl(@NotNull DBRProgressMonitor monitor, @Nullable JDBCResultSet dbResult, @NotNull CubridTableBase table, String columnName, String typeName, int valueType, int sourceType, int ordinalPos, long columnSize, long charLength, Integer scale, Integer precision, int radix, boolean notNull, String remarks, String defaultValue, boolean autoIncrement, boolean autoGenerated) throws DBException {
        return new CubridTableColumn(table,
            columnName,
            typeName, valueType, sourceType, ordinalPos,
            columnSize,
            charLength, scale, precision, radix, notNull,
            remarks, defaultValue, autoIncrement, autoGenerated
        );
    }

    //////////////////////////////////////////////////////
    // Constraints

    public JDBCStatement prepareUniqueConstraintsLoadStatement(@NotNull JDBCSession session, @NotNull CubridStructContainer owner, @Nullable CubridTableBase forParent)
            throws SQLException, DBException {
        return session.getMetaData().getPrimaryKeys(
            owner.getCatalog() == null ? null : owner.getCatalog().getName(),
            owner.getSchema() == null || DBUtils.isVirtualObject(owner.getSchema()) ? null : owner.getSchema().getName(),
            forParent == null ? owner.getDataSource().getAllObjectsPattern() : forParent.getName())
            .getSourceStatement();
    }

    public DBSEntityConstraintType getUniqueConstraintType(JDBCResultSet dbResult) throws DBException, SQLException {
        return DBSEntityConstraintType.PRIMARY_KEY;
    }

    @NotNull
    public CubridTableForeignKey createTableForeignKeyImpl(CubridTableBase table, String name, @Nullable String remarks, DBSEntityReferrer referencedKey, DBSForeignKeyModifyRule deleteRule, DBSForeignKeyModifyRule updateRule, DBSForeignKeyDeferability deferability, boolean persisted) {
        return new CubridTableForeignKey(table, name, remarks, referencedKey, deleteRule, updateRule, deferability, persisted);
    }

    public JDBCStatement prepareForeignKeysLoadStatement(@NotNull JDBCSession session, @NotNull CubridStructContainer owner, @Nullable CubridTableBase forParent) throws SQLException {
        return session.getMetaData().getImportedKeys(
                owner.getCatalog() == null ? null : owner.getCatalog().getName(),
                owner.getSchema() == null || DBUtils.isVirtualObject(owner.getSchema()) ? null : owner.getSchema().getName(),
                forParent == null ?
                        owner.getDataSource().getAllObjectsPattern() :
                        forParent.getName())
                .getSourceStatement();
    }

    public boolean isFKConstraintWordDuplicated() {
        return false;
    }

    public String generateOnDeleteFK(DBSForeignKeyModifyRule deleteRule) {
        String deleteClause = deleteRule.getClause();
        if (!CommonUtils.isEmpty(deleteClause)) {
            return "ON DELETE " + deleteClause;
        }
        return null;
    }

    public String generateOnUpdateFK(DBSForeignKeyModifyRule updateRule) {
        String updateClause = updateRule.getClause();
        if (!CommonUtils.isEmpty(updateClause)) {
            return "ON UPDATE " + updateClause;
        }
        return null;
    }

    //////////////////////////////////////////////////////
    // Indexes

    public CubridTableIndex createIndexImpl(
        CubridTableBase table,
        boolean nonUnique,
        String qualifier,
        long cardinality,
        String indexName,
        DBSIndexType indexType,
        boolean persisted)
    {
        return new CubridTableIndex(
            table,
            nonUnique,
            qualifier,
            cardinality,
            indexName,
            indexType,
            persisted);
    }

    public CubridUniqueKey createConstraintImpl(CubridTableBase table, String constraintName, DBSEntityConstraintType constraintType, JDBCResultSet dbResult, boolean persisted) {
        return new CubridUniqueKey(table, constraintName, null, constraintType, persisted);
    }

    public CubridTableConstraintColumn[] createConstraintColumnsImpl(JDBCSession session,
                                                                      CubridTableBase parent, CubridUniqueKey object, CubridMetaObject pkObject, JDBCResultSet dbResult) throws DBException {
        String columnName = isTrimObjectNames() ?
            CubridUtils.safeGetStringTrimmed(pkObject, dbResult, JDBCConstants.COLUMN_NAME)
            : CubridUtils.safeGetString(pkObject, dbResult, JDBCConstants.COLUMN_NAME);
        if (CommonUtils.isEmpty(columnName)) {
            log.debug("Null primary key column for '" + object.getName() + "'");
            return null;
        }
        if ((columnName.startsWith("[") && columnName.endsWith("]")) ||
                (columnName.startsWith(SQLConstants.DEFAULT_IDENTIFIER_QUOTE) && columnName.endsWith(SQLConstants.DEFAULT_IDENTIFIER_QUOTE))) {
            // [JDBC: SQLite] Escaped column name. Let's un-escape it
            columnName = columnName.substring(1, columnName.length() - 1);
        }
        int keySeq = CubridUtils.safeGetInt(pkObject, dbResult, JDBCConstants.KEY_SEQ);

        CubridTableColumn tableColumn = parent.getAttribute(session.getProgressMonitor(), columnName);
        if (tableColumn == null) {
            log.warn("Column '" + columnName + "' not found in table '" + parent.getFullyQualifiedName(DBPEvaluationContext.DDL) + "' for PK '" + object.getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
            return null;
        }

        return new CubridTableConstraintColumn[] {
                new CubridTableConstraintColumn(object, tableColumn, keySeq) };
    }

    //////////////////////////////////////////////////////
    // Sequences

    public boolean supportsSequences(@NotNull CubridDataSource dataSource) {
        return false;
    }

    public JDBCStatement prepareSequencesLoadStatement(@NotNull JDBCSession session, @NotNull CubridStructContainer container) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public CubridSequence createSequenceImpl(@NotNull JDBCSession session, @NotNull CubridStructContainer container, @NotNull JDBCResultSet dbResult) throws DBException {
        throw new DBCFeatureNotSupportedException();
    }

    public boolean handleSequenceCacheReadingError(Exception error) {
        return false;
    }

    //////////////////////////////////////////////////////
    // Synonyms

    public boolean supportsSynonyms(@NotNull CubridDataSource dataSource) {
        return false;
    }

    public JDBCStatement prepareSynonymsLoadStatement(@NotNull JDBCSession session, @NotNull CubridStructContainer container) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public CubridSynonym createSynonymImpl(@NotNull JDBCSession session, @NotNull CubridStructContainer container, @NotNull JDBCResultSet dbResult) throws DBException {
        throw new DBCFeatureNotSupportedException();
    }

    //////////////////////////////////////////////////////
    // Triggers

    public boolean supportsTriggers(@NotNull CubridDataSource dataSource) {
        return false;
    }

    public JDBCStatement prepareTableTriggersLoadStatement(@NotNull JDBCSession session, @NotNull CubridStructContainer cubridStructContainer, @Nullable CubridTableBase forParent) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public CubridTrigger createTableTriggerImpl(@NotNull JDBCSession session, @NotNull CubridStructContainer cubridStructContainer, @NotNull CubridTableBase cubridTableBase, String triggerName, @NotNull JDBCResultSet resultSet) throws DBException {
        throw new DBCFeatureNotSupportedException();
    }

    // Container triggers (not supported by default)

    public boolean supportsDatabaseTriggers(@NotNull CubridDataSource dataSource) {
        return false;
    }

    public JDBCStatement prepareContainerTriggersLoadStatement(@NotNull JDBCSession session, @Nullable CubridStructContainer forParent) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public CubridTrigger createContainerTriggerImpl(@NotNull CubridStructContainer container, @NotNull JDBCResultSet resultSet) throws DBException {
        throw new DBCFeatureNotSupportedException();
    }

    public List<? extends CubridTrigger> loadTriggers(DBRProgressMonitor monitor, @NotNull CubridStructContainer container, @Nullable CubridTableBase table) throws DBException {
        return new ArrayList<>();
    }

    public String getTriggerDDL(@NotNull DBRProgressMonitor monitor, @NotNull CubridTrigger trigger) throws DBException {
        return "-- Source code not available";
    }
    
    // User
    public CubridUser createCubridUserImpl(CubridStructContainer container, String name, String comment)
    {
        return new CubridUser(container,name, comment);
    }

    public JDBCStatement prepareCubridUserLoadStatement(@NotNull JDBCSession session, @NotNull CubridStructContainer container) 
    		throws SQLException 
    {
    	String sql= "select * from db_user";
        final JDBCPreparedStatement dbStat = session.prepareStatement(sql);

        return dbStat;
    }

    // Comments

    public boolean isTableCommentEditable() {
        return false;
    }

    public boolean isTableColumnCommentEditable() {
        return false;
    }

    public boolean supportsNotNullColumnModifiers(DBSObject object) {
        return true;
    }

    public boolean isColumnNotNullByDefault() {
        return false;
    }

    public boolean hasProcedureSupport() {
        return true;
    }

    public boolean hasFunctionSupport() {
        return true;
    }

    public boolean supportsCheckConstraints() {
        return false;
    }

    public boolean supportsViews(@NotNull CubridDataSource dataSource) {
        DBPDataSourceInfo dataSourceInfo = dataSource.getInfo();
        return !(dataSourceInfo instanceof JDBCDataSourceInfo) ||
            ((JDBCDataSourceInfo) dataSourceInfo).supportsViews();
    }
}
