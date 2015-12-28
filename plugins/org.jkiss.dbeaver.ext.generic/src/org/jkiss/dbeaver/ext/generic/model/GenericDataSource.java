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
import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.*;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * GenericDataSource
 */
public class GenericDataSource extends JDBCDataSource
    implements DBSObjectSelector, DBPTermProvider, IAdaptable, GenericStructContainer
{
    static final Log log = Log.getLog(GenericDataSource.class);

    private final TableTypeCache tableTypeCache;
    private final JDBCBasicDataTypeCache dataTypeCache;
    private List<GenericCatalog> catalogs;
    private List<GenericSchema> schemas;
    private final GenericMetaModel metaModel;
    private GenericObjectContainer structureContainer;

    private String queryGetActiveDB;
    private String querySetActiveDB;
    private String selectedEntityType;
    private String selectedEntityName;
    private boolean selectedEntityFromAPI;
    private String allObjectsPattern;
    private boolean supportsStructCache;
    private DBCQueryPlanner queryPlanner;

    public GenericDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, GenericMetaModel metaModel)
        throws DBException
    {
        super(monitor, container);
        this.metaModel = metaModel;
        final DBPDriver driver = container.getDriver();
        this.dataTypeCache = metaModel.createDataTypeCache(container);
        this.tableTypeCache = new TableTypeCache();
        this.queryGetActiveDB = CommonUtils.toString(driver.getDriverParameter(GenericConstants.PARAM_QUERY_GET_ACTIVE_DB));
        this.querySetActiveDB = CommonUtils.toString(driver.getDriverParameter(GenericConstants.PARAM_QUERY_SET_ACTIVE_DB));
        this.selectedEntityType = CommonUtils.toString(driver.getDriverParameter(GenericConstants.PARAM_ACTIVE_ENTITY_TYPE));
        if (CommonUtils.isEmpty(this.selectedEntityType)) {
            this.selectedEntityType = null;
        }
        this.allObjectsPattern = CommonUtils.toString(driver.getDriverParameter(GenericConstants.PARAM_ALL_OBJECTS_PATTERN));
        if (CommonUtils.isEmpty(this.allObjectsPattern)) {
            this.allObjectsPattern = "%";
        } else if ("null".equalsIgnoreCase(this.allObjectsPattern)) {
            this.allObjectsPattern = null;
        }
    }

    protected void initializeContextState(@NotNull DBRProgressMonitor monitor, @NotNull JDBCExecutionContext context, boolean setActiveObject) throws DBCException {
        if (setActiveObject) {
            setActiveEntityName(monitor, context, getSelectedObject());
        }
    }

    public String getAllObjectsPattern()
    {
        return allObjectsPattern;
    }

    @NotNull
    public GenericMetaModel getMetaModel() {
        return metaModel;
    }

    @Nullable
    public GenericMetaObject getMetaObject(String id)
    {
        return metaModel.getObject(id);
    }

    @Override
    protected DBPDataSourceInfo createDataSourceInfo(@NotNull JDBCDatabaseMetaData metaData)
    {
        final GenericDataSourceInfo info = new GenericDataSourceInfo(getContainer().getDriver(), metaData);
        final GenericSQLDialect dialect = (GenericSQLDialect)getSQLDialect();

        final Object supportsReferences = getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_SUPPORTS_REFERENCES);
        if (supportsReferences != null) {
            info.setSupportsReferences(Boolean.valueOf(supportsReferences.toString()));
        }

        final Object supportsIndexes = getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_SUPPORTS_INDEXES);
        if (supportsIndexes != null) {
            info.setSupportsIndexes(Boolean.valueOf(supportsIndexes.toString()));
        }

        final Object supportsStoredCode = getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_SUPPORTS_STORED_CODE);
        if (supportsStoredCode != null) {
            info.setSupportsStoredCode(Boolean.valueOf(supportsStoredCode.toString()));
        }

        final Object supportsSubqueries = getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_SUPPORTS_SUBQUERIES);
        if (supportsSubqueries != null) {
            dialect.setSupportsSubqueries(Boolean.valueOf(supportsSubqueries.toString()));
        }

        final Object supportsStructCacheParam = getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_SUPPORTS_STRUCT_CACHE);
        if (supportsStructCacheParam != null) {
            this.supportsStructCache = CommonUtils.toBoolean(supportsStructCacheParam);
        }
        return info;
    }

    @Override
    protected SQLDialect createSQLDialect(@NotNull JDBCDatabaseMetaData metaData) {
        return new GenericSQLDialect(this, metaData);
    }

    @Override
    public void close()
    {
        super.close();
        String paramShutdown = CommonUtils.toString(getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_SHUTDOWN_URL_PARAM));
        if (!CommonUtils.isEmpty(paramShutdown)) {
            try {
                final Driver driver = getDriverInstance(VoidProgressMonitor.INSTANCE); // Use void monitor - driver already loaded
                if (driver != null) {
                    driver.connect(getContainer().getActualConnectionConfiguration().getUrl() + paramShutdown, null);
                }
            } catch (Exception e) {
                log.debug(e);
            }
        }
    }

    public boolean supportsStructCache() {
        return supportsStructCache;
    }

    public Collection<GenericTableType> getTableTypes(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableTypeCache.getAllObjects(monitor, this);
    }

    public Collection<GenericCatalog> getCatalogs()
    {
        return catalogs;
    }

    public GenericCatalog getCatalog(String name)
    {
        return DBUtils.findObject(getCatalogs(), name);
    }

    public Collection<GenericSchema> getSchemas()
    {
        return schemas;
    }

    public GenericSchema getSchema(String name)
    {
        return DBUtils.findObject(getSchemas(), name);
    }

    @NotNull
    @Override
    public GenericDataSource getDataSource() {
        return this;
    }

    @Override
    public DBSObject getObject() {
        return getContainer();
    }

    @Override
    public GenericCatalog getCatalog() {
        return null;
    }

    @Override
    public GenericSchema getSchema() {
        return null;
    }

    @Override
    public TableCache getTableCache() {
        return structureContainer.getTableCache();
    }

    @Override
    public IndexCache getIndexCache() {
        return structureContainer.getIndexCache();
    }

    @Override
    public PrimaryKeysCache getPrimaryKeysCache() {
        return structureContainer.getPrimaryKeysCache();
    }

    @Override
    public ForeignKeysCache getForeignKeysCache() {
        return structureContainer.getForeignKeysCache();
    }


    @Override
    public Collection<GenericTable> getViews(DBRProgressMonitor monitor) throws DBException {
        return structureContainer == null ? null : structureContainer.getViews(monitor);
    }

    @Override
    public Collection<GenericTable> getPhysicalTables(DBRProgressMonitor monitor) throws DBException {
        return structureContainer == null ? null : structureContainer.getPhysicalTables(monitor);
    }

    @Override
    public Collection<GenericTable> getTables(DBRProgressMonitor monitor)
        throws DBException
    {
        return structureContainer == null ? null : structureContainer.getTables(monitor);
    }

    @Override
    public GenericTable getTable(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return structureContainer == null ? null : structureContainer.getTable(monitor, name);
    }

    @Override
    public Collection<GenericPackage> getPackages(DBRProgressMonitor monitor)
        throws DBException
    {
        return structureContainer == null ? null : structureContainer.getPackages(monitor);
    }

    @Override
    public Collection<GenericTableIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        return structureContainer == null ? null : structureContainer.getIndexes(monitor);
    }

    @Override
    public Collection<GenericProcedure> getProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        return structureContainer == null ? null : structureContainer.getProcedures(monitor);
    }

    @Override
    public GenericProcedure getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException
    {
        return structureContainer == null ? null : structureContainer.getProcedure(monitor, uniqueName);
    }

    @Override
    public Collection<GenericProcedure> getProcedures(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return structureContainer == null ? null : structureContainer.getProcedures(monitor, name);
    }

    @Override
    public Collection<GenericSequence> getSequences(DBRProgressMonitor monitor) throws DBException {
        return structureContainer == null ? null : structureContainer.getSequences(monitor);
    }

    @Override
    public Collection<? extends GenericTrigger> getTriggers(DBRProgressMonitor monitor) throws DBException {
        return structureContainer == null ? null : structureContainer.getTriggers(monitor);
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        super.initialize(monitor);
        Object omitCatalog = getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_OMIT_CATALOG);
        Object omitTypeCache = getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_OMIT_TYPE_CACHE);
        if (omitTypeCache == null || !CommonUtils.toBoolean(omitTypeCache)) {
            // Cache data types
            try {
                dataTypeCache.getAllObjects(monitor, this);
            } catch (DBException e) {
                log.warn("Can't fetch database data types", e);
            }
        } else {
            // Use basic data types
            dataTypeCache.fillStandardTypes(this);
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read generic metadata")) {
            // Read metadata
            JDBCDatabaseMetaData metaData = session.getMetaData();
            boolean catalogsFiltered = false;
            if (omitCatalog == null || !CommonUtils.toBoolean(omitCatalog)) {
                // Read catalogs
                monitor.subTask("Extract catalogs");
                monitor.worked(1);
                final GenericMetaObject catalogObject = getMetaObject(GenericConstants.OBJECT_CATALOG);
                final DBSObjectFilter catalogFilters = getContainer().getObjectFilter(GenericCatalog.class, null, false);
                final List<String> catalogNames = new ArrayList<>();
                try {
                    try (JDBCResultSet dbResult = metaData.getCatalogs()) {
                        int totalCatalogs = 0;
                        while (dbResult.next()) {
                            String catalogName = GenericUtils.safeGetString(catalogObject, dbResult, JDBCConstants.TABLE_CAT);
                            if (CommonUtils.isEmpty(catalogName)) {
                                // Some drivers uses TABLE_QUALIFIER instead of catalog
                                catalogName = GenericUtils.safeGetStringTrimmed(catalogObject, dbResult, JDBCConstants.TABLE_QUALIFIER);
                                if (CommonUtils.isEmpty(catalogName)) {
                                    continue;
                                }
                            }
                            totalCatalogs++;
                            if (catalogFilters == null || catalogFilters.matches(catalogName)) {
                                catalogNames.add(catalogName);
                                monitor.subTask("Extract catalogs - " + catalogName);
                            } else {
                                catalogsFiltered = true;
                            }
                            if (monitor.isCanceled()) {
                                break;
                            }
                        }
                        if (totalCatalogs == 1) {
                            // Just one catalog. Looks like DB2 or PostgreSQL
                            // Let's just skip it and use only schemas
                            // It's ok to use "%" instead of catalog name anyway
                            catalogNames.clear();
                        }
                    }
                } catch (UnsupportedOperationException e) {
                    // Just skip it
                    log.debug(e);
                } catch (SQLFeatureNotSupportedException e) {
                    // Just skip it
                    log.debug(e);
                } catch (SQLException e) {
                    // Error reading catalogs - just warn about it
                    log.warn(e);
                }
                if (!catalogNames.isEmpty() || catalogsFiltered) {
                    this.catalogs = new ArrayList<>();
                    for (String catalogName : catalogNames) {
                        GenericCatalog catalog = new GenericCatalog(this, catalogName);
                        this.catalogs.add(catalog);
                    }
                }
            }

            if (CommonUtils.isEmpty(catalogs) && !catalogsFiltered) {
                // Catalogs not supported - try to read root schemas
                monitor.subTask("Extract schemas");
                monitor.worked(1);
                List<GenericSchema> tmpSchemas = loadSchemas(session, null);
                if (tmpSchemas != null) {
                    this.schemas = tmpSchemas;
                }

                if (CommonUtils.isEmpty(schemas)) {
                    this.structureContainer = new DataSourceObjectContainer();
                }
            }
            determineSelectedEntity(session);


        } catch (SQLException ex) {
            throw new DBException("Error reading metadata", ex, this);
        }
    }

    List<GenericSchema> loadSchemas(JDBCSession session, GenericCatalog catalog)
        throws DBException
    {
        try {
            final GenericMetaObject schemaObject = getMetaObject(GenericConstants.OBJECT_SCHEMA);
            final DBSObjectFilter schemaFilters = getContainer().getObjectFilter(GenericSchema.class, null, false);

            final List<GenericSchema> tmpSchemas = new ArrayList<>();
            JDBCResultSet dbResult = null;
            boolean catalogSchemas = false;
            if (catalog != null) {
                try {
                    dbResult = session.getMetaData().getSchemas(
                        catalog.getName(),
                        schemaFilters != null && schemaFilters.hasSingleMask() ? schemaFilters.getSingleMask() : getAllObjectsPattern());
                    catalogSchemas = true;
                } catch (Throwable e) {
                    // This method not supported (may be old driver version)
                    // Use general schema reading method
                    log.debug("Error reading schemas in catalog '" + catalog.getName() + "' - " + e.getMessage());
                }
            }
            if (dbResult == null) {
                dbResult = session.getMetaData().getSchemas();
            }

            try {
                while (dbResult.next()) {
                    if (session.getProgressMonitor().isCanceled()) {
                        break;
                    }
                    String schemaName = GenericUtils.safeGetString(schemaObject, dbResult, JDBCConstants.TABLE_SCHEM);
                    if (CommonUtils.isEmpty(schemaName)) {
                        // some drivers uses TABLE_OWNER column instead of TABLE_SCHEM
                        schemaName = GenericUtils.safeGetString(schemaObject, dbResult, JDBCConstants.TABLE_OWNER);
                    }
                    if (CommonUtils.isEmpty(schemaName)) {
                        continue;
                    }
                    if (schemaFilters != null && !schemaFilters.matches(schemaName)) {
                        // Doesn't match filter
                        continue;
                    }
                    String catalogName = GenericUtils.safeGetString(schemaObject, dbResult, JDBCConstants.TABLE_CATALOG);

                    if (!CommonUtils.isEmpty(catalogName)) {
                        if (catalog == null) {
                            // Invalid schema's catalog or schema without catalog (then do not use schemas as structure)
                            log.debug("Catalog name (" + catalogName + ") found for schema '" + schemaName + "' while schema doesn't have parent catalog");
                        } else if (!catalog.getName().equals(catalogName)) {
                            if (!catalogSchemas) {
                                // Just skip it - we have list of all existing schemas and this one belongs to another catalog
                                continue;
                            }
                            log.debug("Catalog name '" + catalogName + "' differs from schema's catalog '" + catalog.getName() + "'");
                        }
                    }

                    session.getProgressMonitor().subTask("Schema " + schemaName);

                    GenericSchema schema;
                    if (catalog == null) {
                        schema = new GenericSchema(this, schemaName);
                    } else {
                        schema = new GenericSchema(catalog, schemaName);
                    }
                    tmpSchemas.add(schema);
                }
            } finally {
                dbResult.close();
            }
            if (catalog == null && tmpSchemas.size() == 1 && (schemaFilters == null || schemaFilters.isEmpty())) {
                // Only one schema and no catalogs
                // Most likely it is a fake one, let's skip it
                // Anyway using "%" instead is ok
                tmpSchemas.clear();
            }
            return tmpSchemas;
        } catch (UnsupportedOperationException e) {
            // Schemas are not supported
            log.debug(e);
            return null;
        } catch (SQLFeatureNotSupportedException e) {
            // Schemas are not supported
            log.debug(e);
            return null;
        } catch (Exception ex) {
            // Schemas do not supported - just ignore this error
            log.warn("Can't read schema list", ex);
            return null;
        }
    }

    @Override
    public boolean refreshObject(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        super.refreshObject(monitor);

        this.selectedEntityName = null;
        this.tableTypeCache.clearCache();
        this.catalogs = null;
        this.schemas = null;

        this.initialize(monitor);

        return true;
    }

    @Nullable
    @Override
    public DBCQueryTransformer createQueryTransformer(@NotNull DBCQueryTransformType type) {
        if (metaModel instanceof DBCQueryTransformProvider) {
            DBCQueryTransformer transformer = ((DBCQueryTransformProvider) metaModel).createQueryTransformer(type);
            if (transformer != null) {
                return transformer;
            }
        }
        return super.createQueryTransformer(type);
    }

    GenericTable findTable(@NotNull DBRProgressMonitor monitor, String catalogName, String schemaName, String tableName)
        throws DBException
    {
        GenericObjectContainer container = null;
        if (!CommonUtils.isEmpty(catalogName) && !CommonUtils.isEmpty(catalogs)) {
            container = getCatalog(catalogName);
            if (container == null) {
                log.error("Catalog " + catalogName + " not found");
                return null;
            }
        }
        if (!CommonUtils.isEmpty(schemaName)) {
            if (container instanceof GenericCatalog) {
                container = ((GenericCatalog)container).getSchema(monitor, schemaName);
            } else if (!CommonUtils.isEmpty(schemas)) {
                container = this.getSchema(schemaName);
            } else {
                container = structureContainer;
            }
            if (container == null) {
                log.error("Schema '" + schemaName + "' not found");
                return null;
            }
        }
        if (container == null) {
            container = structureContainer;
        }
        return container.getTable(monitor, tableName);
    }

    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        if (!CommonUtils.isEmpty(getCatalogs())) {
            return getCatalogs();
        } else if (!CommonUtils.isEmpty(getSchemas())) {
            return getSchemas();
        } else if (structureContainer != null) {
            return structureContainer.getTables(monitor);
        } else {
            return null;
        }
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName)
        throws DBException
    {
        if (!CommonUtils.isEmpty(getCatalogs())) {
            return getCatalog(childName);
        } else if (!CommonUtils.isEmpty(getSchemas())) {
            return getSchema(childName);
        } else if (structureContainer != null) {
            return structureContainer.getChild(monitor, childName);
        } else {
            return null;
        }
    }

    @Override
    public Class<? extends DBSObject> getChildType(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        if (!CommonUtils.isEmpty(catalogs)) {
            return GenericCatalog.class;
        } else if (!CommonUtils.isEmpty(schemas)) {
            return GenericSchema.class;
        } else {
            return GenericTable.class;
        }
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
        if (!CommonUtils.isEmpty(catalogs)) {
            for (GenericCatalog catalog : catalogs) catalog.cacheStructure(monitor, scope);
        } else if (!CommonUtils.isEmpty(schemas)) {
            for (GenericSchema schema : schemas) schema.cacheStructure(monitor, scope);
        } else if (structureContainer != null) {
            structureContainer.cacheStructure(monitor, scope);
        }
    }

    public boolean isChild(DBSObject object)
        throws DBException
    {
        if (object instanceof GenericCatalog) {
            return !CommonUtils.isEmpty(catalogs) && catalogs.contains(GenericCatalog.class.cast(object));
        } else if (object instanceof GenericSchema) {
            return !CommonUtils.isEmpty(schemas) && schemas.contains(GenericSchema.class.cast(object));
        }
        return false;
    }

    @Override
    public boolean supportsObjectSelect()
    {
        if (selectedEntityFromAPI) {
            return true;
        }
        if (!CommonUtils.isEmpty(querySetActiveDB)) {
            if (CommonUtils.isEmpty(selectedEntityType)) {
                return !CommonUtils.isEmpty(getCatalogs()) || !CommonUtils.isEmpty(getSchemas());
            }
            if (!CommonUtils.isEmpty(getCatalogs())) {
                return GenericConstants.ENTITY_TYPE_CATALOG.equals(selectedEntityType);
            } else if (!CommonUtils.isEmpty(getSchemas())) {
                return GenericConstants.ENTITY_TYPE_SCHEMA.equals(selectedEntityType);
            }
        }
        return false;
    }

    @Override
    public DBSObject getSelectedObject()
    {
        if (!CommonUtils.isEmpty(selectedEntityName)) {
            if (!CommonUtils.isEmpty(catalogs)) {
                if (selectedEntityType == null || selectedEntityType.equals(GenericConstants.ENTITY_TYPE_CATALOG)) {
                    return getCatalog(selectedEntityName);
                }
            } else if (!CommonUtils.isEmpty(schemas)) {
                if (selectedEntityType == null || selectedEntityType.equals(GenericConstants.ENTITY_TYPE_SCHEMA)) {
                    return getSchema(selectedEntityName);
                }
            }
        }
        return null;
    }

    @Override
    public void selectObject(DBRProgressMonitor monitor, DBSObject object)
        throws DBException
    {
        final DBSObject oldSelectedEntity = getSelectedObject();
        // Check removed because we can select the same object on invalidate
//        if (object == oldSelectedEntity) {
//            return;
//        }
        if (!isChild(object)) {
            throw new DBException("Bad child object specified as active: " + object);
        }

        for (JDBCExecutionContext context : getAllContexts()) {
            setActiveEntityName(monitor, context, object);
        }

        if (oldSelectedEntity != null) {
            DBUtils.fireObjectSelect(oldSelectedEntity, false);
        }
        DBUtils.fireObjectSelect(object, true);
    }

    String getSelectedEntityType()
    {
        return selectedEntityType;
    }

    public String getSelectedEntityName()
    {
        return selectedEntityName;
    }

    void determineSelectedEntity(JDBCSession session)
    {
        // Get selected entity (catalog or schema)
        selectedEntityName = null;
        if (CommonUtils.isEmpty(queryGetActiveDB)) {
            try {
                selectedEntityName = session.getCatalog();
                if (selectedEntityType == null && !CommonUtils.isEmpty(selectedEntityName)) {
                    selectedEntityType = GenericConstants.ENTITY_TYPE_CATALOG;
                    selectedEntityFromAPI = true;
                }
            }
            catch (SQLException e) {
                // Seems to be not supported
                log.debug(e);
            }
            if (CommonUtils.isEmpty(selectedEntityName)) {
                // Try to use current schema
                try {
                    selectedEntityName = session.getSchema();
                    if (selectedEntityType == null && !CommonUtils.isEmpty(selectedEntityName)) {
                        selectedEntityType = GenericConstants.ENTITY_TYPE_SCHEMA;
                        selectedEntityFromAPI = true;
                    }
                } catch (SQLException e) {
                    log.debug(e);
                }
            }
            if (CommonUtils.isEmpty(selectedEntityName)) {
                // If we have only one catalog then it is our selected entity
                if (!CommonUtils.isEmpty(catalogs) && catalogs.size() == 1) {
                    selectedEntityType = GenericConstants.ENTITY_TYPE_CATALOG;
                    selectedEntityName = catalogs.get(0).getName();
                } else if (!CommonUtils.isEmpty(schemas) && schemas.size() == 1) {
                    selectedEntityType = GenericConstants.ENTITY_TYPE_SCHEMA;
                    selectedEntityName = schemas.get(0).getName();
                }
            }
        } else {
            try {
                try (JDBCPreparedStatement dbStat = session.prepareStatement(queryGetActiveDB)) {
                    try (JDBCResultSet resultSet = dbStat.executeQuery()) {
                        resultSet.next();
                        selectedEntityName = JDBCUtils.safeGetStringTrimmed(resultSet, 1);
                        if (!CommonUtils.isEmpty(selectedEntityName)) {
                            // [PostgreSQL]
                            int divPos = selectedEntityName.lastIndexOf(',');
                            if (divPos != -1) {
                                selectedEntityName = selectedEntityName.substring(divPos + 1);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                log.debug(e);
                selectedEntityName = null;
            }
        }
    }

    void setActiveEntityName(DBRProgressMonitor monitor, JDBCExecutionContext context, DBSObject entity) throws DBCException
    {
        if (entity == null) {
            log.debug("Null current entity");
            return;
        }
        try (JDBCSession session = context.openSession(monitor, DBCExecutionPurpose.UTIL, "Set active catalog")) {
            if (selectedEntityFromAPI) {
                // Use JDBC API to change entity
                if (selectedEntityType.equals(GenericConstants.ENTITY_TYPE_CATALOG)) {
                    session.setCatalog(entity.getName());
                } else if (selectedEntityType.equals(GenericConstants.ENTITY_TYPE_SCHEMA)) {
                    session.setSchema(entity.getName());
                } else {
                    throw new DBCException("No API to change active entity if type '" + selectedEntityType + "'");
                }
            } else {
                if (CommonUtils.isEmpty(querySetActiveDB) || !(entity instanceof GenericObjectContainer)) {
                    throw new DBCException("Active database can't be changed for this kind of datasource!");
                }
                String changeQuery = querySetActiveDB.replaceFirst("\\?", entity.getName());
                try (JDBCPreparedStatement dbStat = session.prepareStatement(changeQuery)) {
                    dbStat.execute();
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, context.getDataSource());
        }
        selectedEntityName = entity.getName();
    }

    @Override
    public Object getAdapter(Class adapter)
    {
        if (adapter == DBSStructureAssistant.class) {
            return new GenericStructureAssistant(this);
        } else if (adapter == DBCQueryPlanner.class) {
            if (queryPlanner == null) {
                queryPlanner = metaModel.getQueryPlanner(this);
            }
            return queryPlanner;
        } else {
            return super.getAdapter(adapter);
        }
    }

    @Override
    public String getObjectTypeTerm(String path, String objectType, boolean multiple)
    {
        String term = null;
        if (GenericConstants.TERM_CATALOG.equals(objectType)) {
            term = getInfo().getCatalogTerm();
        } else if (GenericConstants.TERM_SCHEMA.equals(objectType)) {
            term = getInfo().getSchemaTerm();
        } else if (GenericConstants.TERM_PROCEDURE.equals(objectType)) {
            term = getInfo().getProcedureTerm();
        }
        if (term != null && multiple) {
            term += "s";
        }
        return term;
    }

    @Nullable
    @Override
    public ErrorPosition[] getErrorPosition(@NotNull Throwable error) {
        ErrorPosition position = metaModel.getErrorPosition(error);
        return position == null ? null : new ErrorPosition[] { position };
    }

    @Override
    public Collection<? extends DBSDataType> getLocalDataTypes()
    {
        return dataTypeCache.getCachedObjects();
    }

    @Override
    public DBSDataType getLocalDataType(String typeName)
    {
        return dataTypeCache.getCachedObject(typeName);
    }

    @NotNull
    public DBPDataKind resolveDataKind(@NotNull String typeName, int valueType)
    {
        DBSDataType dataType = dataTypeCache.getCachedObject(typeName);
        if (dataType != null) {
            return super.resolveDataKind(dataType.getTypeName(), dataType.getTypeID());
        }
        return super.resolveDataKind(typeName, valueType);
    }

    private class TableTypeCache extends JDBCObjectCache<GenericDataSource, GenericTableType> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull GenericDataSource owner) throws SQLException
        {
            return session.getMetaData().getTableTypes().getSourceStatement();
        }
        @Override
        protected GenericTableType fetchObject(@NotNull JDBCSession session, @NotNull GenericDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new GenericTableType(
                GenericDataSource.this,
                GenericUtils.safeGetString(
                    getMetaObject(GenericConstants.OBJECT_TABLE_TYPE),
                    resultSet,
                    JDBCConstants.TABLE_TYPE));
        }
    }

    private class DataSourceObjectContainer extends GenericObjectContainer
    {
        private DataSourceObjectContainer()
        {
            super(GenericDataSource.this);
        }

        @Override
        public GenericCatalog getCatalog() {
            return null;
        }

        @Override
        public GenericSchema getSchema() {
            return null;
        }

        @Override
        public DBSObject getObject() {
            return GenericDataSource.this.getContainer();
        }

        @Override
        public Class<? extends DBSEntity> getChildType(@NotNull DBRProgressMonitor monitor) throws DBException {
            return GenericTable.class;
        }

        @NotNull
        @Override
        public String getName() {
            return GenericDataSource.this.getName();
        }

        @Nullable
        @Override
        public String getDescription() {
            return GenericDataSource.this.getDescription();
        }

        @Override
        public DBSObject getParentObject() {
            return GenericDataSource.this.getParentObject();
        }
    }

}
