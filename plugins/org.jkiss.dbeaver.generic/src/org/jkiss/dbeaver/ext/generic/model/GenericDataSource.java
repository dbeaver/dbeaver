/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.sql.Driver;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * GenericDataSource
 */
public class GenericDataSource extends JDBCDataSource implements DBPDataSource, JDBCConnector, DBSEntitySelector, IAdaptable
{
    static final Log log = LogFactory.getLog(GenericDataSource.class);

    private List<String> tableTypes;
    private List<GenericCatalog> catalogs;
    private List<GenericSchema> schemas;

    private GenericEntityContainer structureContainer;

    private String queryGetActiveDB;
    private String querySetActiveDB;
    private String selectedEntityType;
    private String selectedEntityName;

    public GenericDataSource(DBSDataSourceContainer container)
        throws DBException
    {
        super(container);
        this.queryGetActiveDB = container.getDriver().getDriverParameter(GenericConstants.PARAM_QUERY_GET_ACTIVE_DB);
        this.querySetActiveDB = container.getDriver().getDriverParameter(GenericConstants.PARAM_QUERY_SET_ACTIVE_DB);
        this.selectedEntityType = container.getDriver().getDriverParameter(GenericConstants.PARAM_ACTIVE_ENTITY_TYPE);
    }

    protected DBPDataSourceInfo makeInfo(JDBCDatabaseMetaData metaData)
    {
        return new GenericDataSourceInfo(metaData);
    }

    @Override
    public void close(DBRProgressMonitor monitor)
    {
        super.close(monitor);
        String paramShutdown = getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_SHUTDOWN_URL_PARAM);
        if (!CommonUtils.isEmpty(paramShutdown)) {
            try {
                final Driver driver = getDriverInstance();
                if (driver != null) {
                    driver.connect(getContainer().getConnectionInfo().getUrl() + paramShutdown, null);
                }
            } catch (Exception e) {
                log.debug(e);
            }
        }
    }

    public String[] getTableTypes()
    {
        return tableTypes.toArray(new String[tableTypes.size()]);
    }

    public List<GenericCatalog> getCatalogs()
    {
        return catalogs;
    }

    public GenericCatalog getCatalog(String name)
    {
        return DBUtils.findObject(getCatalogs(), name);
    }

    public List<GenericSchema> getSchemas()
    {
        return schemas;
    }

    public GenericSchema getSchema(String name)
    {
        return DBUtils.findObject(getSchemas(), name);
    }

    public List<GenericTable> getTables(DBRProgressMonitor monitor)
        throws DBException
    {
        return structureContainer == null ? null : structureContainer.getTables(monitor);
    }

    public GenericTable getTable(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return DBUtils.findObject(getTables(monitor), name);
    }

    public List<GenericIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        return structureContainer == null ? null : structureContainer.getIndexes(monitor);
    }

    public List<GenericProcedure> getProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        return structureContainer == null ? null : structureContainer.getProcedures(monitor);
    }

    public List<GenericProcedure> getProcedures(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return structureContainer == null ? null : structureContainer.getProcedures(monitor, name);
    }

    public void initialize(DBRProgressMonitor monitor)
        throws DBException
    {
        super.initialize(monitor);

        JDBCExecutionContext context = openContext(monitor, DBCExecutionPurpose.META, "Read generic metadata");
        try {
            JDBCDatabaseMetaData metaData = context.getMetaData();
            {
                // Read table types
                monitor.subTask("Extract table types");
                monitor.worked(1);
                this.tableTypes = new ArrayList<String>();
                JDBCResultSet dbResult = metaData.getTableTypes();
                try {
                    while (dbResult.next()) {
                        String tableType = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_TYPE);
                        if (!CommonUtils.isEmpty(tableType)) {
                            if (!tableTypes.contains(tableType)) {
                                tableTypes.add(tableType);
                            }
                        }
                    }
                } finally {
                    dbResult.close();
                }
            }
            boolean catalogsFiltered = false;
            {
                // Read catalogs
                monitor.subTask("Extract catalogs");
                monitor.worked(1);
                List<String> catalogFilters = SQLUtils.splitFilter(getContainer().getCatalogFilter());
                List<String> catalogNames = new ArrayList<String>();
                try {
                    JDBCResultSet dbResult = metaData.getCatalogs();
                    try {
                        while (dbResult.next()) {
                            String catalogName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_CAT);
                            if (CommonUtils.isEmpty(catalogName)) {
                                // Some drivers uses TABLE_QUALIFIER instead of catalog
                                catalogName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TABLE_QUALIFIER);
                                if (CommonUtils.isEmpty(catalogName)) {
                                    continue;
                                }
                            }
                            if (catalogFilters.isEmpty() || SQLUtils.matchesAnyLike(catalogName, catalogFilters)) {
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
                } catch (SQLException e) {
                    // Error reading catalogs - just skip em
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
                    this.structureContainer = new DataSourceEntityContainer();
                }
            }

            // Get selected entity (catalog or schema)
            if (CommonUtils.isEmpty(queryGetActiveDB)) {
                try {
                    selectedEntityName = context.getCatalog();
                }
                catch (SQLException e) {
                    // Seems to be not supported
                    log.debug(e);
                    selectedEntityName = null;
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
            List<String> schemaFilters = SQLUtils.splitFilter(getContainer().getSchemaFilter());

            List<GenericSchema> tmpSchemas = new ArrayList<GenericSchema>();
            JDBCResultSet dbResult;
            boolean catalogSchemas;
            try {
                dbResult = context.getMetaData().getSchemas(
                    catalog == null ? null : catalog.getName(),
                    schemaFilters.size() == 1 ? schemaFilters.get(0) : null);
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
                    String schemaName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_SCHEM);
                    if (CommonUtils.isEmpty(schemaName)) {
                        // some drivers uses TABLE_OWNER column instead of TABLE_SCHEM
                        schemaName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_OWNER);
                    }
                    if (CommonUtils.isEmpty(schemaName)) {
                        continue;
                    }
                    if (!schemaFilters.isEmpty() && !SQLUtils.matchesAnyLike(schemaName, schemaFilters)) {
                        // Check pattern
                        continue;
                    }
                    String catalogName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_CATALOG);

                    if (!CommonUtils.isEmpty(catalogName)) {
                        if (catalog == null) {
                            // Invalid schema's catalog or schema without catalog (then do not use schemas as structure)
                            log.warn("Catalog name (" + catalogName + ") found for schema '" + schemaName + "' while schema doesn't have parent catalog");
                        } else if (!catalog.getName().equals(catalogName)) {
                            if (!catalogSchemas) {
                                // Just skip it - we have list of all existing schemas and this one belongs to another catalog
                                continue;
                            }
                            log.warn("Catalog name '" + catalogName + "' differs from schema's catalog '" + catalog.getName() + "'");
                        }
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
        } catch (Exception ex) {
            // Schemas do not supported - jsut ignore this error
            log.warn("Could not read schema list", ex);
            return null;
        }
    }

    public boolean refreshEntity(DBRProgressMonitor monitor)
        throws DBException
    {
        super.refreshEntity(monitor);

        this.selectedEntityName = null;
        this.tableTypes = null;
        this.catalogs = null;
        this.schemas = null;

        this.initialize(monitor);

        return true;
    }

    GenericTable findTable(DBRProgressMonitor monitor, String catalogName, String schemaName, String tableName)
        throws DBException
    {
        GenericEntityContainer container = null;
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

    public Collection<? extends DBSEntity> getChildren(DBRProgressMonitor monitor)
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

    public DBSEntity getChild(DBRProgressMonitor monitor, String childName)
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

    public Class<? extends DBSEntity> getChildType(DBRProgressMonitor monitor)
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
            return getCatalogs().contains(GenericCatalog.class.cast(object));
        } else if (object instanceof GenericSchema) {
            return getSchemas().contains(GenericSchema.class.cast(object));
        }
        return false;
    }

    public DBSEntity getSelectedEntity()
    {
        if (!CommonUtils.isEmpty(selectedEntityName)) {
            if (!CommonUtils.isEmpty(getCatalogs())) {
                if (selectedEntityType == null || selectedEntityType.equals(GenericConstants.ENTITY_TYPE_CATALOG)) {
                    return getCatalog(selectedEntityName);
                } else if (getCatalogs().size() == 1) {
                    // Return the only catalog as selected [DBSPEC: PostgreSQL]
                    return getCatalogs().get(0);
                }
            } else if (!CommonUtils.isEmpty(getSchemas())) {
                if (selectedEntityType == null || selectedEntityType.equals(GenericConstants.ENTITY_TYPE_SCHEMA)) {
                    return getSchema(selectedEntityName);
                }
            }
        }
        return null;
    }

    public boolean supportsEntitySelect()
    {
        return !CommonUtils.isEmpty(querySetActiveDB);
    }

    public void selectEntity(DBRProgressMonitor monitor, DBSEntity entity)
        throws DBException
    {
        final DBSEntity oldSelectedEntity = getSelectedEntity();
        if (entity == oldSelectedEntity) {
            return;
        }
        if (CommonUtils.isEmpty(querySetActiveDB) || !(entity instanceof GenericEntityContainer)) {
            throw new DBException("Active database can't be changed for this kind of datasource!");
        }
        if (!isChild(entity)) {
            throw new DBException("Bad child object specified as active: " + entity);
        }

        String changeQuery = querySetActiveDB.replaceFirst("\\?", entity.getName());
        JDBCExecutionContext context = openContext(monitor, DBCExecutionPurpose.META, "Set active catalog");
        try {
            JDBCPreparedStatement dbStat = context.prepareStatement(changeQuery);
            try {
                dbStat.execute();
            } finally {
                dbStat.close();
            }
        } catch (SQLException e) {
            throw new DBException(e);
        }
        finally {
            context.close();
        }
        selectedEntityName = entity.getName();

        if (oldSelectedEntity != null) {
            getContainer().fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_SELECT, oldSelectedEntity, false));
        }
        getContainer().fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_SELECT, entity, true));
    }

    public Object getAdapter(Class adapter)
    {
        if (adapter == DBSStructureAssistant.class) {
            return new GenericStructureAssistant(this);
        } else {
            return null;
        }
    }

    private class DataSourceEntityContainer extends GenericEntityContainer {
        private DataSourceEntityContainer()
        {
            super(GenericDataSource.this);
        }

        public GenericCatalog getCatalog() {
            return null;
        }

        public GenericSchema getSchema() {
            return null;
        }

        public DBSObject getObject() {
            return GenericDataSource.this.getContainer();
        }

        public Class<? extends DBSEntity> getChildType(DBRProgressMonitor monitor) throws DBException {
            return GenericTable.class;
        }

        public String getName() {
            return GenericDataSource.this.getName();
        }

        public String getObjectId() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String getDescription() {
            return GenericDataSource.this.getDescription();
        }

        public DBSObject getParentObject() {
            return GenericDataSource.this.getParentObject();
        }
    }
}
