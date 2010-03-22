package org.jkiss.dbeaver.model.impl.meta;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.dbc.DBCConnector;
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceInfo;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSStructureContainer;
import org.jkiss.dbeaver.model.struct.DBSUtils;

import java.sql.*;
import java.util.Properties;

/**
 * GenericDataSource
 */
public abstract class AbstractDataSource
    implements
        DBPDataSource,
        DBCConnector,
        DBSStructureContainer,
        DBSObject
{
    static Log log = LogFactory.getLog(AbstractDataSource.class);

    private DBSDataSourceContainer container;
    private Connection connection;

    private DBPDataSourceInfo info;

    public AbstractDataSource(DBSDataSourceContainer container)
        throws DBException
    {
        this.container = container;
        this.connection = openConnection();
    }

    protected Connection openConnection()
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

    public DBCSession getSession(boolean forceNew)
        throws DBException
    {
        if (forceNew) {
            return new JDBCSession(this, this.openConnection());
        } else {
            return new JDBCSession(this, this);
        }
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
        } catch (SQLException ex) {
            throw new DBException("Error getting JDBC metadata", ex);
        }
    }

    protected void reconnect()
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

    public AbstractDataSource getDataSource()
    {
        return this;
    }

    public String getFullTableName(String catalogName, String tableName)
    {
        return getFullTableName(catalogName,  null, tableName);
    }

    public String getFullTableName(String catalogName, String schemaName, String tableName)
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

}
