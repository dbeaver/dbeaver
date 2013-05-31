/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.generic.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseTermProvider;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetadataReader;
import org.jkiss.dbeaver.ext.generic.model.meta.StandardMetadataReader;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * GenericDataSource
 */
public class GenericDataSource extends JDBCDataSource
    implements DBPDataSource, JDBCConnector, DBSObjectSelector, IDatabaseTermProvider, IAdaptable, GenericStructContainer
{
    static final Log log = LogFactory.getLog(GenericDataSource.class);

    private final TableTypeCache tableTypeCache;
    private final JDBCBasicDataTypeCache dataTypeCache;
    private List<GenericCatalog> catalogs;
    private List<GenericSchema> schemas;

    private GenericObjectContainer structureContainer;

    private String queryGetActiveDB;
    private String querySetActiveDB;
    private String selectedEntityType;
    private String selectedEntityName;
    private boolean selectedEntityFromAPI;
    private final GenericMetadataReader metadataReader;

    public GenericDataSource(DBRProgressMonitor monitor, DBSDataSourceContainer container)
        throws DBException
    {
        super(monitor, container);
        final DBPDriver driver = container.getDriver();
        this.dataTypeCache = new JDBCBasicDataTypeCache(container);
        this.tableTypeCache = new TableTypeCache();
        this.queryGetActiveDB = CommonUtils.toString(driver.getDriverParameter(GenericConstants.PARAM_QUERY_GET_ACTIVE_DB));
        this.querySetActiveDB = CommonUtils.toString(driver.getDriverParameter(GenericConstants.PARAM_QUERY_SET_ACTIVE_DB));
        this.selectedEntityType = CommonUtils.toString(driver.getDriverParameter(GenericConstants.PARAM_ACTIVE_ENTITY_TYPE));
        if (CommonUtils.isEmpty(this.selectedEntityType)) {
            this.selectedEntityType = null;
        }
        String metadataType = CommonUtils.toString(driver.getDriverParameter(GenericConstants.PARAM_METADATA_TYPE));
        if (CommonUtils.isEmpty(metadataType)) {
            metadataReader = new StandardMetadataReader();
        } else {
            metadataReader = GenericConstants.MetadataType.valueOf(metadataType.toLowerCase()).createReader();
        }
    }

    GenericMetadataReader getMetadataReader()
    {
        return metadataReader;
    }

    @Override
    protected DBPDataSourceInfo makeInfo(JDBCDatabaseMetaData metaData)
    {
        final GenericDataSourceInfo info = new GenericDataSourceInfo(metaData);

        final Object supportsReferences = getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_SUPPORTS_REFERENCES);
        if (supportsReferences != null) {
            info.setSupportsReferences(Boolean.valueOf(supportsReferences.toString()));
        }

        final Object supportsIndexes = getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_SUPPORTS_INDEXES);
        if (supportsIndexes != null) {
            info.setSupportsIndexes(Boolean.valueOf(supportsIndexes.toString()));
        }

        final Object supportsSubqueries = getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_SUPPORTS_SUBQUERIES);
        if (supportsSubqueries != null) {
            info.setSupportsIndexes(Boolean.valueOf(supportsSubqueries.toString()));
        }
        return info;
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
                    driver.connect(getContainer().getActualConnectionInfo().getUrl() + paramShutdown, null);
                }
            } catch (Exception e) {
                log.debug(e);
            }
        }
    }

    public Collection<GenericTableType> getTableTypes(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableTypeCache.getObjects(monitor, this);
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
    public void initialize(DBRProgressMonitor monitor)
        throws DBException
    {
        super.initialize(monitor);
        Object omitTypeCache = getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_OMIT_TYPE_CACHE);
        if (omitTypeCache == null || !CommonUtils.toBoolean(omitTypeCache)) {
            // Cache data types
            try {
                dataTypeCache.getObjects(monitor, this);
            } catch (DBException e) {
                log.warn("Can't fetch database data types", e);
            }
        } else {
            // Use basic data types
            dataTypeCache.fillStandardTypes(this);
        }
        JDBCExecutionContext context = openContext(monitor, DBCExecutionPurpose.META, "Read generic metadata");
        try {
            // Read metadata
            JDBCDatabaseMetaData metaData = context.getMetaData();
            boolean catalogsFiltered = false;
            {
                // Read catalogs
                monitor.subTask("Extract catalogs");
                monitor.worked(1);
                DBSObjectFilter catalogFilters = getContainer().getObjectFilter(GenericCatalog.class, null);
                List<String> catalogNames = new ArrayList<String>();
                try {
                    JDBCResultSet dbResult = metaData.getCatalogs();
                    try {
                        while (dbResult.next()) {
                            String catalogName = getMetadataReader().fetchCatalogName(dbResult);
                            if (CommonUtils.isEmpty(catalogName)) {
                                log.debug("Catalog name is empty");
                                continue;
                            }
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
                    } finally {
                        dbResult.close();
                    }
                } catch (UnsupportedOperationException e) {
                    // Just skip it
                } catch (SQLFeatureNotSupportedException e) {
                    // Just skip it
                } catch (SQLException e) {
                    // Error reading catalogs - just warn about it
                    log.warn(e);
                }
                if (!catalogNames.isEmpty() || catalogsFiltered) {
                    this.catalogs = new ArrayList<GenericCatalog>();
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
                List<GenericSchema> tmpSchemas = loadSchemas(context, null);
                if (tmpSchemas != null) {
                    this.schemas = tmpSchemas;
                }

                if (CommonUtils.isEmpty(schemas)) {
                    this.structureContainer = new DataSourceObjectContainer();
                }
            }
            determineSelectedEntity(context);


        } catch (SQLException ex) {
            throw new DBException("Error reading metadata", ex);
        }
        finally {
            context.close();
        }
    }

    List<GenericSchema> loadSchemas(JDBCExecutionContext context, GenericCatalog catalog)
        throws DBException
    {
        try {
            DBSObjectFilter schemaFilters = getContainer().getObjectFilter(GenericSchema.class, null);

            List<GenericSchema> tmpSchemas = new ArrayList<GenericSchema>();
            JDBCResultSet dbResult;
            boolean catalogSchemas;
            try {
                dbResult = context.getMetaData().getSchemas(
                    catalog == null ? null : catalog.getName(),
                    schemaFilters != null && schemaFilters.hasSingleMask() ? schemaFilters.getSingleMask() : null);
                catalogSchemas = true;
            } catch (Throwable e) {
                // This method not supported (may be old driver version)
                // Use general schema reading method
                dbResult = context.getMetaData().getSchemas();
                catalogSchemas = false;
            }

            try {
                while (dbResult.next()) {
                    if (context.getProgressMonitor().isCanceled()) {
                        break;
                    }
                    String schemaName = getMetadataReader().fetchSchemaName(dbResult, catalog == null ? null : catalog.getName());
                    if (CommonUtils.isEmpty(schemaName)) {
                        log.debug("Schema name is empty");
                        continue;
                    }
                    if (schemaFilters != null && !schemaFilters.matches(schemaName)) {
                        // Doesn't match filter
                        continue;
                    }

                    context.getProgressMonitor().subTask("Schema " + schemaName);

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
            // Schemas do not supported - jsut ignore this error
            log.warn("Could not read schema list", ex);
            return null;
        }
    }

    @Override
    public boolean refreshObject(DBRProgressMonitor monitor)
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

    GenericTable findTable(DBRProgressMonitor monitor, String catalogName, String schemaName, String tableName)
        throws DBException
    {
        GenericObjectContainer container = null;
        if (!CommonUtils.isEmpty(catalogName)) {
            container = getCatalog(catalogName);
            if (container == null) {
                log.error("Catalog " + catalogName + " not found");
                return null;
            }
        } else if (catalogs != null && catalogs.size() == 1) {
            // Catalog name is not specified but we have only one catalog - let's use it
            // It can happen in some drivers (PostgreSQL at least)
            container = catalogs.get(0);
        }
        if (!CommonUtils.isEmpty(schemaName)) {
            if (container instanceof GenericCatalog) {
                container = ((GenericCatalog)container).getSchema(monitor, schemaName);
            } else {
                container = this.getSchema(schemaName);
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
    public Collection<? extends DBSObject> getChildren(DBRProgressMonitor monitor)
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
    public DBSObject getChild(DBRProgressMonitor monitor, String childName)
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
    public Class<? extends DBSObject> getChildType(DBRProgressMonitor monitor)
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
    public void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException {
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
                } else if (catalogs.size() == 1) {
                    // Return the only catalog as selected [DBSPEC: PostgreSQL]
                    return catalogs.get(0);
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
        if (object == oldSelectedEntity) {
            return;
        }
        if (!isChild(object)) {
            throw new DBException("Bad child object specified as active: " + object);
        }

        setActiveEntityName(monitor, object);

        if (oldSelectedEntity != null) {
            DBUtils.fireObjectSelect(oldSelectedEntity, false);
        }
        DBUtils.fireObjectSelect(object, true);
    }

    String getSelectedEntityType()
    {
        return selectedEntityType;
    }

    String getSelectedEntityName()
    {
        return selectedEntityName;
    }

    void determineSelectedEntity(JDBCExecutionContext context)
    {
        // Get selected entity (catalog or schema)
        selectedEntityName = null;
        if (CommonUtils.isEmpty(queryGetActiveDB)) {
            try {
                selectedEntityName = context.getCatalog();
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
                    selectedEntityName = context.getSchema();
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
                // [JDBC: PostgreSQL and Vertica]
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
                JDBCPreparedStatement dbStat = context.prepareStatement(queryGetActiveDB);
                try {
                    JDBCResultSet resultSet = dbStat.executeQuery();
                    try {
                        resultSet.next();
                        selectedEntityName = JDBCUtils.safeGetStringTrimmed(resultSet, 1);
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    dbStat.close();
                }
            } catch (SQLException e) {
                log.debug(e);
                selectedEntityName = null;
            }
        }
    }

    void setActiveEntityName(DBRProgressMonitor monitor, DBSObject entity) throws DBException
    {
        JDBCExecutionContext context = openContext(monitor, DBCExecutionPurpose.UTIL, "Set active catalog");
        try {
            if (selectedEntityFromAPI) {
                // Use JDBC API to change entity
                if (selectedEntityType.equals(GenericConstants.ENTITY_TYPE_CATALOG)) {
                    context.setCatalog(entity.getName());
                } else if (selectedEntityType.equals(GenericConstants.ENTITY_TYPE_SCHEMA)) {
                    context.setSchema(entity.getName());
                } else {
                    throw new DBException("No API to change active entity if type '" + selectedEntityType + "'");
                }
            } else {
                if (CommonUtils.isEmpty(querySetActiveDB) || !(entity instanceof GenericObjectContainer)) {
                    throw new DBException("Active database can't be changed for this kind of datasource!");
                }
                String changeQuery = querySetActiveDB.replaceFirst("\\?", entity.getName());
                JDBCPreparedStatement dbStat = context.prepareStatement(changeQuery);
                try {
                    dbStat.execute();
                } finally {
                    dbStat.close();
                }
            }
        } catch (SQLException e) {
            throw new DBException(e);
        }
        finally {
            context.close();
        }
        selectedEntityName = entity.getName();
    }

    @Override
    public Object getAdapter(Class adapter)
    {
        if (adapter == DBSStructureAssistant.class) {
            return new GenericStructureAssistant(this);
        } else {
            return null;
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

    @Override
    public Collection<? extends DBSDataType> getDataTypes()
    {
        return dataTypeCache.getCachedObjects();
    }

    @Override
    public DBSDataType getDataType(String typeName)
    {
        return dataTypeCache.getCachedObject(typeName);
    }

    private class TableTypeCache extends JDBCObjectCache<GenericDataSource, GenericTableType> {
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, GenericDataSource owner) throws SQLException
        {
            return context.getMetaData().getTableTypes().getSource();
        }
        @Override
        protected GenericTableType fetchObject(JDBCExecutionContext context, GenericDataSource owner, ResultSet resultSet) throws SQLException, DBException
        {
            String tableType = getMetadataReader().fetchTableType(resultSet);
            if (CommonUtils.isEmpty(tableType)) {
                return null;
            }
            return new GenericTableType(
                GenericDataSource.this,
                tableType);
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
        public Class<? extends DBSEntity> getChildType(DBRProgressMonitor monitor) throws DBException {
            return GenericTable.class;
        }

        @Override
        public String getName() {
            return GenericDataSource.this.getName();
        }

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
