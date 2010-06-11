package org.jkiss.dbeaver.ext.generic.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConnector;
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceInfo;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSStructureContainerActive;
import org.jkiss.dbeaver.model.struct.DBSUtils;
import org.jkiss.dbeaver.model.struct.DBSObjectAction;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
    static Log log = LogFactory.getLog(GenericDataSource.class);

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

    public GenericDataSource(DBSDataSourceContainer container)
        throws DBException
    {
        this.container = container;
        this.queryGetActiveDB = container.getDriver().getCustomQuery(QUERY_GET_ACTIVE_DB);
        this.querySetActiveDB = container.getDriver().getCustomQuery(QUERY_SET_ACTIVE_DB);
        this.connection = openConnection();
    }

    private Connection openConnection()
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
            if (!driverInstance.acceptsURL(connectionInfo.getJdbcURL())) {
                throw new DBException("Bad URL: " + connectionInfo.getJdbcURL());
            }
            Connection connection = driverInstance.connect(connectionInfo.getJdbcURL(), driverProps);
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

    public DBCSession getSession(boolean forceNew)
        throws DBException
    {
        if (forceNew) {
            return new JDBCSession(this, this.openConnection());
        } else {
            return new JDBCSession(this, this);
        }
    }

    public GenericCatalog getCatalog(DBRProgressMonitor monitor, String name)
    {
        return DBSUtils.findObject(getCatalogs(monitor), name);
    }

    public List<GenericSchema> getSchemas(DBRProgressMonitor monitor)
    {
        return schemas;
    }

    public GenericSchema getSchema(DBRProgressMonitor monitor, String name)
    {
        return DBSUtils.findObject(getSchemas(monitor), name);
    }

    public void checkConnection()
        throws DBException
    {
        if (connection == null) {
            throw new DBException("Not connected");
        }
        try {
            ResultSet dbResult = connection.getMetaData().getTables("noname", "noname", "noname", null);
            try {
                dbResult.next();
            }
            finally {
                dbResult.close();
            }
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
    }

    public void initialize(DBRProgressMonitor monitor)
        throws DBException
    {
        JDBCUtils.startBlockingOperation(monitor, this, "Initializing data source '" + getName() + "'", 5);
        try {
            monitor.subTask("Getting connection metdata");
            monitor.worked(1);
            DatabaseMetaData metaData = getConnection().getMetaData();
            info = new JDBCDataSourceInfo(metaData);
            {
                // Read table types
                monitor.subTask("Extract table types");
                monitor.worked(1);
                this.tableTypes = new ArrayList<String>();
                ResultSet dbResult = metaData.getTableTypes();
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
                    ResultSet dbResult = metaData.getCatalogs();
                    try {
                        while (dbResult.next()) {
                            String catalogName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_CAT);
                            if (!CommonUtils.isEmpty(catalogName)) {
                                catalogNames.add(catalogName);
                            }
                            monitor.subTask("Extract catalogs - " + catalogName);
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
                List<String> schemaNames = new ArrayList<String>();
                try {
                    ResultSet dbResult = metaData.getSchemas();
                    try {
                        while (dbResult.next()) {
                            String catalogName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_CATALOG);
                            String schemaName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_SCHEM);
                            if (!CommonUtils.isEmpty(schemaName)) {
                                schemaNames.add(schemaName);
                            }
                            if (!CommonUtils.isEmpty(catalogName)) {
                                log.debug("No catalogs was read but catalog name returned with schema");
                            }
                            monitor.subTask("Extract schemas - " + schemaName);
                            if (monitor.isCanceled()) {
                                break;
                            }
                        }
                    } finally {
                        dbResult.close();
                    }
                } catch (SQLException e) {
                    // Error reading schemas - just skip em
                }
                if (!schemaNames.isEmpty()) {
                    this.schemas = new ArrayList<GenericSchema>();
                    for (String schemaName : schemaNames) {
                        GenericSchema schema = new GenericSchema(this, schemaName);
                        this.schemas.add(schema);
                    }
                }
            }
        } catch (SQLException ex) {
            throw new DBException("Error reading metadata", ex);
        }
        finally {
            JDBCUtils.endBlockingOperation(monitor);
        }
    }

    public void refreshDataSource(DBRProgressMonitor monitor)
        throws DBException
    {
        refreshObject(monitor);
    }

    public void close()
    {
        if (connection != null) {
            try {
                connection.close();
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
            if (CommonUtils.isEmpty(queryGetActiveDB)) {
                return null;
            }
            String activeDbName;
            try {
                PreparedStatement dbStat = getConnection().prepareStatement(queryGetActiveDB);
                try {
                    ResultSet resultSet = dbStat.executeQuery();
                    try {
                        resultSet.next();
                        activeDbName = resultSet.getString(1);
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    dbStat.close();
                }
            } catch (SQLException e) {
                log.error(e);
                return null;
            }
            activeChild = getChild(monitor, activeDbName);

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
        try {
            PreparedStatement dbStat = getConnection().prepareStatement(changeQuery);
            try {
                dbStat.execute();
            } finally {
                dbStat.close();
            }
        } catch (SQLException e) {
            throw new DBException(e);
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
