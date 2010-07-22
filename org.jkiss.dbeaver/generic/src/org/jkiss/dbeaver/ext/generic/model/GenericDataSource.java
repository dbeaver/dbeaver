/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceInfo;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.api.ConnectionManagable;
import org.jkiss.dbeaver.model.jdbc.JDBCConnector;
import org.jkiss.dbeaver.model.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectAction;
import org.jkiss.dbeaver.model.struct.DBSStructureContainerActive;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * GenericDataSource
 */
public class GenericDataSource extends GenericStructureContainer implements DBPDataSource, JDBCConnector, DBSStructureContainerActive
{
    static final Log log = LogFactory.getLog(GenericDataSource.class);

    public static final String QUERY_GET_ACTIVE_DB = "GET_ACTIVE_DB";
    public static final String QUERY_SET_ACTIVE_DB = "SET_ACTIVE_DB";

    private DBSDataSourceContainer container;
    private Connection connection;

    private List<String> tableTypes;
    private List<GenericCatalog> catalogs;
    private List<GenericSchema> schemas;
    private DBSObject activeChild;
    private boolean activeChildRead;

    private DBPDataSourceInfo info;
    private String queryGetActiveDB;
    private String querySetActiveDB;

    public GenericDataSource(DBRProgressMonitor monitor, DBSDataSourceContainer container)
        throws DBException
    {
        this.container = container;
        this.queryGetActiveDB = container.getDriver().getCustomQuery(QUERY_GET_ACTIVE_DB);
        this.querySetActiveDB = container.getDriver().getCustomQuery(QUERY_SET_ACTIVE_DB);
        this.connection = openConnection(monitor);

        this.initCache();
    }

    private Connection openConnection(DBRProgressMonitor monitor)
        throws DBException
    {
        Driver driverInstance = Driver.class.cast(container.getDriver().getDriverInstance());

        // Set properties
        Properties driverProps = new Properties();
        DBPConnectionInfo connectionInfo = container.getConnectionInfo();
        if (connectionInfo.getProperties() != null) {
            driverProps.putAll(connectionInfo.getProperties());
        }
        if (!CommonUtils.isEmpty(connectionInfo.getUserName())) {
            driverProps.put("user", connectionInfo.getUserName());
        }
        if (!CommonUtils.isEmpty(connectionInfo.getUserPassword())) {
            driverProps.put("password", connectionInfo.getUserPassword());
        }

        // Obtain connection
        try {
            if (!driverInstance.acceptsURL(connectionInfo.getUrl())) {
                throw new DBException("Bad URL: " + connectionInfo.getUrl());
            }
            Connection connection = driverInstance.connect(connectionInfo.getUrl(), driverProps);
            if (connection == null) {
                throw new DBException("Null connection returned by " + driverInstance);
            }
            return connection;
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
    }

    public Connection getConnection()
    {
        return connection;
    }

    public JDBCExecutionContext openContext(DBRProgressMonitor monitor)
    {
        return openContext(monitor, null);
    }

    public JDBCExecutionContext openContext(DBRProgressMonitor monitor, String taskTitle)
    {
        return new ConnectionManagable(this, monitor, taskTitle);
    }

    public String[] getTableTypes()
    {
        return tableTypes.toArray(new String[tableTypes.size()]);
    }

    public DBSDataSourceContainer getContainer()
    {
        return container;
    }

    public DBPDataSourceInfo getInfo()
    {
        return info;
    }

    public List<GenericCatalog> getCatalogs(DBRProgressMonitor monitor)
    {
        return catalogs;
    }

    public GenericCatalog getCatalog(DBRProgressMonitor monitor, String name)
    {
        return DBUtils.findObject(getCatalogs(monitor), name);
    }

    public List<GenericSchema> getSchemas(DBRProgressMonitor monitor)
    {
        return schemas;
    }

    public GenericSchema getSchema(DBRProgressMonitor monitor, String name)
    {
        return DBUtils.findObject(getSchemas(monitor), name);
    }

    public void invalidateConnection(DBRProgressMonitor monitor)
        throws DBException
    {
        if (connection == null) {
            connection = openConnection(monitor);
            return;
        }

        if (!JDBCUtils.isConnectionAlive(connection)) {
            close(monitor);
            connection = openConnection(monitor);
        }
    }

    public void initialize(DBRProgressMonitor monitor)
        throws DBException
    {
        JDBCUtils.startConnectionBlock(monitor, this, "Initializing data source '" + getName() + "'");

        JDBCExecutionContext context = openContext(monitor);
        try {
            monitor.subTask("Getting connection metdata");
            monitor.worked(1);

            JDBCDatabaseMetaData metaData = context.getMetaData();
            info = new JDBCDataSourceInfo(metaData);
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

            if (catalogs == null) {
                // Catalogs not supported - try to read root schemas
                monitor.subTask("Extract schemas");
                monitor.worked(1);
                List<GenericSchema> tmpSchemas = loadSchemas(context, null);
                if (!tmpSchemas.isEmpty()) {
                    this.schemas = tmpSchemas;
                }
            }
        } catch (SQLException ex) {
            throw new DBException("Error reading metadata", ex);
        }
        finally {
            context.close();
            JDBCUtils.endConnectionBlock(monitor);
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

    public void refreshDataSource(DBRProgressMonitor monitor)
        throws DBException
    {
        refreshObject(monitor);
    }

    public void close(DBRProgressMonitor monitor)
    {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            }
            catch (SQLException ex) {
                log.error(ex);
            }
            connection = null;
        }
    }

    public String getName()
    {
        return container.getName();
    }

    public String getDescription()
    {
        return container.getDescription();
    }

    public DBSObject getParentObject()
    {
        return container;
    }

    public GenericDataSource getDataSource()
    {
        return this;
    }

    public boolean refreshObject(DBRProgressMonitor monitor)
        throws DBException
    {
        super.refreshObject(monitor);

        this.activeChild = null;
        this.activeChildRead = false;
        this.tableTypes = null;
        this.catalogs = null;
        this.schemas = null;

        this.info = null;

        this.initialize(monitor);

        return true;
    }

    public GenericCatalog getCatalog()
    {
        return null;
    }

    public GenericSchema getSchema()
    {
        return null;
    }

    public DBSObject getObject()
    {
        return container;
    }

    GenericTable findTable(DBRProgressMonitor monitor, String catalogName, String schemaName, String tableName)
        throws DBException
    {
        GenericStructureContainer container = this;
        if (!CommonUtils.isEmpty(catalogName)) {
            container = getCatalog(monitor, catalogName);
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
                container = this.getSchema(monitor, schemaName);
            }
            if (container == null) {
                log.error("Schema " + schemaName + " not found");
                return null;
            }
        }
        return container.getTable(monitor, tableName);
    }

    public Collection<? extends DBSObject> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        if (!CommonUtils.isEmpty(getCatalogs(monitor))) {
            return getCatalogs(monitor);
        } else if (!CommonUtils.isEmpty(getSchemas(monitor))) {
            return getSchemas(monitor);
        } else {
            return getTables(monitor);
        }
    }

    public DBSObject getChild(DBRProgressMonitor monitor, String childName)
        throws DBException
    {
        if (!CommonUtils.isEmpty(getCatalogs(monitor))) {
            return getCatalog(monitor, childName);
        } else if (!CommonUtils.isEmpty(getSchemas(monitor))) {
            return getSchema(monitor, childName);
        } else {
            return super.getChild(monitor, childName);
        }
    }

    public Class<? extends DBSObject> getChildType(DBRProgressMonitor monitor)
        throws DBException
    {
        if (catalogs != null) {
            return GenericCatalog.class;
        } else if (schemas != null) {
            return GenericSchema.class;
        } else {
            return GenericTable.class;
        }
    }

    public boolean isChild(DBRProgressMonitor monitor, DBSObject object)
        throws DBException
    {
        if (object instanceof GenericCatalog) {
            return getCatalogs(monitor).contains(GenericCatalog.class.cast(object));
        } else if (object instanceof GenericSchema) {
            return getSchemas(monitor).contains(GenericSchema.class.cast(object));
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
        if (CommonUtils.isEmpty(querySetActiveDB) || !(child instanceof GenericStructureContainer)) {
            throw new DBException("Active database can't be changed for this kind of datasource!");
        }
        if (!isChild(monitor, child)) {
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
            getContainer().fireEvent(DBSObjectAction.CHANGED, oldChild);
        }
        if (this.activeChild != null) {
            getContainer().fireEvent(DBSObjectAction.CHANGED, this.activeChild);
        }
    }

}
