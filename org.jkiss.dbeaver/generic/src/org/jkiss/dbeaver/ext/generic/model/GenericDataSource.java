/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.jdbc.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * GenericDataSource
 */
public class GenericDataSource extends JDBCDataSource implements DBPDataSource, JDBCConnector, DBSEntitySelector, DBSStructureAssistant
{
    static final Log log = LogFactory.getLog(GenericDataSource.class);

    public static final String QUERY_GET_ACTIVE_DB = "GET_ACTIVE_DB";
    public static final String QUERY_SET_ACTIVE_DB = "SET_ACTIVE_DB";

    private List<String> tableTypes;
    private List<GenericCatalog> catalogs;
    private List<GenericSchema> schemas;
    private DBSObject activeChild;
    private boolean activeChildRead;

    private GenericEntityContainer structureContainer;

    private String queryGetActiveDB;
    private String querySetActiveDB;

    public GenericDataSource(DBRProgressMonitor monitor, DBSDataSourceContainer container)
        throws DBException
    {
        super(monitor, container);
        this.queryGetActiveDB = container.getDriver().getCustomQuery(QUERY_GET_ACTIVE_DB);
        this.querySetActiveDB = container.getDriver().getCustomQuery(QUERY_SET_ACTIVE_DB);
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

    public void initialize(DBRProgressMonitor monitor)
        throws DBException
    {
        super.initialize(monitor);

        JDBCExecutionContext context = openContext(monitor, "Read generic metadata");
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
            {
                // Read catalogs
                monitor.subTask("Extract catalogs");
                monitor.worked(1);
                List<String> catalogNames = new ArrayList<String>();
                try {
                    JDBCResultSet dbResult = metaData.getCatalogs();
                    try {
                        while (dbResult.next()) {
                            String catalogName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_CAT);
                            if (CommonUtils.isEmpty(catalogName)) {
                                catalogName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_QUALIFIER);
                            }
                            if (!CommonUtils.isEmpty(catalogName)) {
                                catalogNames.add(catalogName);
                                monitor.subTask("Extract catalogs - " + catalogName);
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
                if (!catalogNames.isEmpty()) {
                    this.catalogs = new ArrayList<GenericCatalog>();
                    for (String catalogName : catalogNames) {
                        GenericCatalog catalog = new GenericCatalog(this, catalogName);
                        this.catalogs.add(catalog);
                    }
                }
            }

            if (CommonUtils.isEmpty(catalogs)) {
                // Catalogs not supported - try to read root schemas
                monitor.subTask("Extract schemas");
                monitor.worked(1);
                List<GenericSchema> tmpSchemas = loadSchemas(context, null);
                if (!tmpSchemas.isEmpty()) {
                    this.schemas = tmpSchemas;
                }

                if (CommonUtils.isEmpty(schemas)) {
                    structureContainer = new DataSourceEntityContainer();
                    structureContainer.initCache();
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
            List<GenericSchema> tmpSchemas = new ArrayList<GenericSchema>();
            JDBCResultSet dbResult;
            if (catalog == null) {
                dbResult = context.getMetaData().getSchemas();
            } else {
                try {
                    dbResult = context.getMetaData().getSchemas(catalog.getName(), null);
                } catch (Throwable e) {
                    // This method not supported (may be old driver version)
                    // Use general schema reading method
                    dbResult = context.getMetaData().getSchemas();
                }
            }

            try {
                while (dbResult.next()) {
                    String schemaName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_SCHEM);
                    if (CommonUtils.isEmpty(schemaName)) {
                        schemaName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_OWNER);
                    }
                    if (CommonUtils.isEmpty(schemaName)) {
                        continue;
                    }

                    String catalogName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_CATALOG);

                    if (!CommonUtils.isEmpty(catalogName)) {
                        if (catalog == null) {
                            // Invalid schema's catalog or schema without catalog (then do not use schemas as structure)
                            log.warn("Catalog name (" + catalogName + ") found for schema '" + schemaName + "' while no parent catalog specified");
                        } else if (!catalog.getName().equals(catalogName)) {
                            log.warn("Catalog name '" + catalogName + "' differs from schema's catalog '" + catalog.getName() + "'");
                        }
                    }

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
        } catch (SQLException ex) {
            // Schemas do not supported - jsut ignore this error
            return null;
        }
    }

    public boolean refreshEntity(DBRProgressMonitor monitor)
        throws DBException
    {
        super.refreshEntity(monitor);

        this.activeChild = null;
        this.activeChildRead = false;
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
                container = ((GenericCatalog)container).getSchema(schemaName);
            } else {
                container = this.getSchema(schemaName);
            }
            if (container == null) {
                log.error("Schema " + schemaName + " not found");
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

    public DBSObject getActiveChild(DBRProgressMonitor monitor)
        throws DBException
    {
        if (activeChildRead) {
            return activeChild;
        }
        synchronized (this) {
            activeChildRead = true;
            String activeDbName;
            JDBCExecutionContext context = openContext(monitor);
            try {
                if (CommonUtils.isEmpty(queryGetActiveDB)) {
                    try {
                        activeDbName = context.getCatalog();
                    }
                    catch (SQLException e) {
                        // Seems to be not supported
                        return null;
                    }
                } else {
                    JDBCPreparedStatement dbStat = context.prepareStatement(queryGetActiveDB);
                    dbStat.setDescription("Reading active database");
                    try {
                        JDBCResultSet resultSet = dbStat.executeQuery();
                        try {
                            resultSet.next();
                            activeDbName = resultSet.getString(1);
                        } finally {
                            resultSet.close();
                        }
                    } finally {
                        dbStat.close();
                    }
                }
            } catch (SQLException e) {
                log.error(e);
                return null;
            }
            finally {
                context.close();
            }
            if (activeDbName != null) {
                activeChild = getChild(monitor, activeDbName);
            } else {
                activeChild = null;
            }

            return activeChild;
        }
    }

    public boolean supportsActiveChildChange()
    {
        return !CommonUtils.isEmpty(querySetActiveDB);
    }

    public void setActiveChild(DBRProgressMonitor monitor, DBSObject child)
        throws DBException
    {
        if (child == activeChild) {
            return;
        }
        if (CommonUtils.isEmpty(querySetActiveDB) || !(child instanceof GenericEntityContainer)) {
            throw new DBException("Active database can't be changed for this kind of datasource!");
        }
        if (!isChild(child)) {
            throw new DBException("Bad child object specified as active: " + child);
        }

        String changeQuery = querySetActiveDB.replaceFirst("\\?", child.getName());
        JDBCExecutionContext context = openContext(monitor);
        try {
            JDBCPreparedStatement dbStat = context.prepareStatement(changeQuery);
            dbStat.setDescription("Change active database");
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

        DBSObject oldChild = this.activeChild;
        this.activeChild = child;

        if (oldChild != null) {
            getContainer().fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, oldChild));
        }
        if (this.activeChild != null) {
            getContainer().fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, this.activeChild));
        }
    }

    public List<DBSTablePath> findTableNames(DBRProgressMonitor monitor, String tableMask, int maxResults) throws DBException
    {
        JDBCUtils.startConnectionBlock(monitor, getDataSource(), "Looking for tables in '" + getName() + "'");

        List<DBSTablePath> pathList = new ArrayList<DBSTablePath>();
        JDBCExecutionContext context = getDataSource().openContext(monitor);
        try {
            JDBCDatabaseMetaData metaData = context.getMetaData();

            // Make table mask uppercase
            tableMask = tableMask.toUpperCase();

            // Load tables
            JDBCResultSet dbResult = metaData.getTables(
                null,
                null,
                tableMask,
                null);
            try {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {

                    String catalogName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_CAT);
                    String schemaName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_SCHEM);
                    String tableName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_NAME);
                    String tableType = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_TYPE);
                    String remarks = JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS);

                    pathList.add(
                        new DBSTablePath(
                            catalogName,
                            schemaName,
                            tableName,
                            tableType,
                            remarks));
                }
            }
            finally {
                dbResult.close();
            }
            return pathList;
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
        finally {
            context.close();
            JDBCUtils.endConnectionBlock(monitor);
        }
    }

    private class DataSourceEntityContainer extends GenericEntityContainer {
        public GenericDataSource getDataSource() {
            return GenericDataSource.this;
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
