package org.jkiss.dbeaver.ext.generic.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.dbc.DBCConnector;
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceInfo;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSStructureContainerActive;
import org.jkiss.dbeaver.model.struct.DBSUtils;

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
public class GenericDataSource extends GenericStructureContainer implements DBPDataSource, DBCConnector, DBSStructureContainerActive
{
    static Log log = LogFactory.getLog(GenericDataSource.class);

    public static final String QUERY_GET_ACTIVE_DB = "GET_ACTIVE_DB";
    public static final String QUERY_SET_ACTIVE_DB = "SET_ACTIVE_DB";

    private DBSDataSourceContainer container;
    private Connection connection;

    private List<String> tableTypes;
    private List<GenericCatalog> catalogs;
    private List<GenericSchema> schemas;

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
        Driver driverInstance = container.getDriver().getDriverInstance();

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
         throws DBException
    {
        if (info == null) {
            try {
                info = new JDBCDataSourceInfo(connection.getMetaData());
            } catch (SQLException e) {
                throw new DBException(e);
            }
        }
        return info;
    }

    public List<GenericCatalog> getCatalogs()
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

    public GenericCatalog getCatalog(String name)
    {
        return DBSUtils.findObject(catalogs, name);
    }

    public List<GenericSchema> getSchemas()
    {
        return schemas;
    }

    public GenericSchema getSchema(String name)
    {
        return DBSUtils.findObject(schemas, name);
    }

    public void checkConnection()
        throws DBException
    {
        if (connection == null) {
            throw new DBException("Not connected");
        }
        try {
            ResultSet dbResult = connection.getMetaData().getTables(null, null, null, null);
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

    public void initialize()
        throws DBException
    {
        try {
            DatabaseMetaData metaData = getConnection().getMetaData();
            info = new JDBCDataSourceInfo(metaData);
            {
                // Read table types
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
                List<String> catalogNames = new ArrayList<String>();
                try {
                    ResultSet dbResult = metaData.getCatalogs();
                    try {
                        while (dbResult.next()) {
                            String catalogName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_CAT);
                            if (!CommonUtils.isEmpty(catalogName)) {
                                catalogNames.add(catalogName);
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
    }

    public void refreshDataSource()
        throws DBException
    {
        this.tableTypes = null;
        this.catalogs = null;
        this.schemas = null;

        this.initialize();
    }

    private void reconnect()
        throws DBException
    {
        close();
        this.connection = openConnection();
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

    public void cancelCurrentOperation()
    {
        try {
            reconnect();
        } catch (Exception ex) {
            log.error(ex);
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

    public boolean refreshObject()
        throws DBException
    {
        return false;
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

    GenericTable findTable(String catalogName, String schemaName, String tableName)
        throws DBException
    {
        GenericStructureContainer container = this;
        if (!CommonUtils.isEmpty(catalogName)) {
            container = getCatalog(catalogName);
            if (container == null) {
                log.error("Catalog " + catalogName + " not found");
                return null;
            }
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
        return container.getTable(tableName);
    }

    String getFullTableName(String catalogName, String schemaName, String tableName)
    {
        StringBuilder name=  new StringBuilder();
        if (catalogName != null) {
            name.append(DBSUtils.getQuotedIdentifier(this, catalogName)).append(info.getCatalogSeparator());
        }
        if (schemaName != null) {
            name.append(DBSUtils.getQuotedIdentifier(this, schemaName)).append(info.getCatalogSeparator());
        }
        name.append(DBSUtils.getQuotedIdentifier(this, tableName));
        return name.toString();
    }

    public Collection<? extends DBSObject> getChildren()
        throws DBException
    {
        if (!CommonUtils.isEmpty(getCatalogs())) {
            return getCatalogs();
        } else if (!CommonUtils.isEmpty(getSchemas())) {
            return getSchemas();
        } else {
            return getTables();
        }
    }

    public DBSObject getChild(String childName)
        throws DBException
    {
        if (!CommonUtils.isEmpty(getCatalogs())) {
            return getCatalog(childName);
        } else if (!CommonUtils.isEmpty(getSchemas())) {
            return getSchema(childName);
        } else {
            return super.getChild(childName);
        }
    }

    public DBSObject getActiveChild()
        throws DBException
    {
        if (CommonUtils.isEmpty(queryGetActiveDB) || catalogs == null) {
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
        return getCatalog(activeDbName);
    }

    public boolean supportsActiveChildChange()
    {
        return !CommonUtils.isEmpty(querySetActiveDB);
    }

    public void setActiveChild(DBSObject child)
        throws DBException
    {
        if (CommonUtils.isEmpty(querySetActiveDB) || !(child instanceof GenericCatalog)) {
            throw new DBException("Active database can't be changed for this kind of datasource!");
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
    }
}
