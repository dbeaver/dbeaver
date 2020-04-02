/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mssql.model;

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.ext.mssql.model.session.SQLServerSessionManager;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformType;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.jdbc.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

public class SQLServerDataSource extends JDBCDataSource implements DBSInstanceContainer, IAdaptable {

    private static final Log log = Log.getLog(SQLServerDataSource.class);

    // Delegate data type reading to the driver
    private final SystemDataTypeCache dataTypeCache = new SystemDataTypeCache();
    private final DatabaseCache databaseCache = new DatabaseCache();

    private boolean supportsColumnProperty;
    private String serverVersion;

    public SQLServerDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container)
        throws DBException
    {
        super(monitor, container, new SQLServerDialect());
    }

    public boolean supportsColumnProperty() {
        return supportsColumnProperty;
    }

    @Override
    protected DBPDataSourceInfo createDataSourceInfo(DBRProgressMonitor monitor, @NotNull JDBCDatabaseMetaData metaData)
    {
        SQLServerDataSourceInfo info = new SQLServerDataSourceInfo(this, metaData);
        if (isDataWarehouseServer(monitor)) {
            info.setSupportsResultSetScroll(false);
        }
        return info;
    }

    public boolean isDataWarehouseServer(DBRProgressMonitor monitor) {
        return getServerVersion(monitor).contains(SQLServerConstants.SQL_DW_SERVER_LABEL);
    }

    public String getServerVersion() {
        return serverVersion;
    }

    String getServerVersion(DBRProgressMonitor monitor) {
        if (serverVersion == null) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read server version")) {
                serverVersion = JDBCUtils.queryString(session, "SELECT @@VERSION");
            } catch (Exception e) {
                log.debug("Error reading SQL Server version: " + e.getMessage());
                serverVersion = "";
            }
        }
        return serverVersion;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return this;
    }

    @Override
    protected Properties getAllConnectionProperties(@NotNull DBRProgressMonitor monitor, JDBCExecutionContext context, String purpose, DBPConnectionConfiguration connectionInfo) throws DBCException {
        Properties properties = super.getAllConnectionProperties(monitor, context, purpose, connectionInfo);

        if (!getContainer().getPreferenceStore().getBoolean(ModelPreferences.META_CLIENT_NAME_DISABLE)) {
            // App name
            properties.put(
                SQLServerUtils.isDriverJtds(getContainer().getDriver()) ? SQLServerConstants.APPNAME_CLIENT_PROPERTY : SQLServerConstants.APPLICATION_NAME_CLIENT_PROPERTY,
                CommonUtils.truncateString(DBUtils.getClientApplicationName(getContainer(), context, purpose), 64));
        }

        fillConnectionProperties(connectionInfo, properties);

        SQLServerAuthentication authSchema = SQLServerUtils.detectAuthSchema(connectionInfo);

        authSchema.getInitializer().initializeAuthentication(connectionInfo, properties);

        return properties;
    }

    @Override
    protected JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
        return new SQLServerExecutionContext(instance, type);
    }

    @Override
    protected void initializeContextState(@NotNull DBRProgressMonitor monitor, @NotNull JDBCExecutionContext context, JDBCExecutionContext initFrom) throws DBException {
        super.initializeContextState(monitor, context, initFrom);
        if (initFrom != null) {
            if (!isDataWarehouseServer(monitor)) {
                SQLServerExecutionContext ssContext = (SQLServerExecutionContext) initFrom;
                SQLServerDatabase defaultObject = ssContext.getDefaultCatalog();
                if (defaultObject != null) {
                    ((SQLServerExecutionContext)context).setCurrentDatabase(monitor, defaultObject);
                }
                SQLServerSchema defaultSchema = ssContext.getDefaultSchema();
                if (defaultSchema != null && !isDataWarehouseServer(monitor)) {
                    ((SQLServerExecutionContext)context).setDefaultSchema(monitor, defaultSchema);
                }
            }
        } else {
            ((SQLServerExecutionContext)context).refreshDefaults(monitor, true);
        }
    }

    @Override
    public Object getDataSourceFeature(String featureId) {
        switch (featureId) {
            case DBConstants.FEATURE_LIMIT_AFFECTS_DML:
                return true;
            case DBConstants.FEATURE_MAX_STRING_LENGTH:
                return 8000;
        }
        return super.getDataSourceFeature(featureId);
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.initialize(monitor);

        this.dataTypeCache.getAllObjects(monitor, this);
        this.databaseCache.getAllObjects(monitor, this);

        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load data source meta info")) {
            try {
                JDBCUtils.queryString(session, "SELECT COLUMNPROPERTY(0, NULL, NULL)");
                this.supportsColumnProperty = true;
            } catch (Exception e) {
                this.supportsColumnProperty = false;
            }
        } catch (Throwable e) {
            log.error("Error during connection initialization", e);
        }
    }

    //////////////////////////////////////////////////////////////////
    // Data types

    @NotNull
    @Override
    public DBPDataKind resolveDataKind(@NotNull String typeName, int valueType) {
        return getLocalDataType(valueType).getDataKind();
    }

    @Override
    public List<SQLServerDataType> getLocalDataTypes() {
        return dataTypeCache.getCachedObjects();
    }

    SQLServerDataType getSystemDataType(int systemTypeId) {
        for (SQLServerDataType dt : dataTypeCache.getCachedObjects()) {
            if (dt.getObjectId() == systemTypeId) {
                return dt;
            }
        }
        log.debug("System data type " + systemTypeId + " not found");
        SQLServerDataType sdt = new SQLServerDataType(this, String.valueOf(systemTypeId), systemTypeId, DBPDataKind.OBJECT, java.sql.Types.OTHER);
        dataTypeCache.cacheObject(sdt);
        return sdt;
    }

    @Override
    public SQLServerDataType getLocalDataType(String typeName) {
        return dataTypeCache.getCachedObject(typeName);
    }

    @Override
    public SQLServerDataType getLocalDataType(int typeID) {
        DBSDataType dt = super.getLocalDataType(typeID);
        if (dt == null) {
            log.debug("System data type " + typeID + " not found");
        }
        return (SQLServerDataType) dt;
    }

    @Override
    public String getDefaultDataTypeName(@NotNull DBPDataKind dataKind) {
        switch (dataKind) {
            case BOOLEAN: return "bit";
            case NUMERIC: return "int";
            case STRING: return "varchar";
            case DATETIME: return SQLServerConstants.TYPE_DATETIME;
            case BINARY: return "binary";
            case CONTENT: return "varbinary";
            case ROWID: return "uniqueidentifier";
            default:
                return super.getDefaultDataTypeName(dataKind);
        }
    }

    //////////////////////////////////////////////////////////
    // Databases

    protected boolean isShowAllSchemas() {
        return CommonUtils.toBoolean(getContainer().getConnectionConfiguration().getProviderProperty(SQLServerConstants.PROP_SHOW_ALL_SCHEMAS));
    }

    //////////////////////////////////////////////////////////
    // Windows authentication

    @Override
    protected String getConnectionUserName(@NotNull DBPConnectionConfiguration connectionInfo) {
        if (SQLServerUtils.isWindowsAuth(connectionInfo)) {
            return "";
        } else {
            return super.getConnectionUserName(connectionInfo);
        }
    }

    @Override
    protected String getConnectionUserPassword(@NotNull DBPConnectionConfiguration connectionInfo) {
        if (SQLServerUtils.isWindowsAuth(connectionInfo)) {
            return "";
        } else {
            return super.getConnectionUserPassword(connectionInfo);
        }
    }

    //////////////////////////////////////////////////////////////
    // Databases

    @Association
    public Collection<SQLServerDatabase> getDatabases(DBRProgressMonitor monitor) throws DBException {
        return databaseCache.getAllObjects(monitor, this);
    }

    public SQLServerDatabase getDatabase(DBRProgressMonitor monitor, String name) throws DBException {
        return databaseCache.getObject(monitor, this, name);
    }

    public SQLServerDatabase getDatabase(String name) {
        return databaseCache.getCachedObject(name);
    }

    public SQLServerDatabase getDefaultDatabase(@NotNull DBRProgressMonitor monitor) {
        return ((SQLServerExecutionContext)getDefaultInstance().getDefaultContext(monitor, true)).getDefaultCatalog();
    }

    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        return databaseCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        return databaseCache.getObject(monitor, this, childName);
    }

    @Override
    public Class<? extends DBSObject> getChildType(@NotNull DBRProgressMonitor monitor) throws DBException {
        return SQLServerDatabase.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
        databaseCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        databaseCache.clearCache();
        return super.refreshObject(monitor);
    }

    @Override
    public DBCQueryTransformer createQueryTransformer(DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            //if (!SQLServerUtils.isDriverAzure(getContainer().getDriver())) {
                return new QueryTransformerTop();
            //}
        }
        return super.createQueryTransformer(type);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DBSStructureAssistant.class) {
            return adapter.cast(new SQLServerStructureAssistant(this));
        } else if (adapter == DBAServerSessionManager.class) {
            return adapter.cast(new SQLServerSessionManager(this));
        }
        return super.getAdapter(adapter);
    }

    @Override
    public ErrorPosition[] getErrorPosition(DBRProgressMonitor monitor, DBCExecutionContext context, String query, Throwable error) {
        Throwable rootCause = GeneralUtils.getRootCause(error);
        if (rootCause != null && SQLServerConstants.SQL_SERVER_EXCEPTION_CLASS_NAME.equals(rootCause.getClass().getName())) {
            // Read line number from SQLServerError class
            try {
                Object serverError = rootCause.getClass().getMethod("getSQLServerError").invoke(rootCause);
                if (serverError != null) {
                    Object serverErrorLine = BeanUtils.readObjectProperty(serverError, "lineNumber");
                    if (serverErrorLine instanceof Number) {
                        ErrorPosition pos = new ErrorPosition();
                        pos.line = ((Number) serverErrorLine).intValue() - 1;
                        return new ErrorPosition[] {pos};
                    }
                }
            } catch (Throwable e) {
                // ignore
            }
        }

        return super.getErrorPosition(monitor, context, query, error);
    }

    static class DatabaseCache extends JDBCObjectCache<SQLServerDataSource, SQLServerDatabase> {
        DatabaseCache() {
            setListOrderComparator(DBUtils.nameComparator());
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull SQLServerDataSource owner) throws SQLException {
            StringBuilder sql = new StringBuilder("SELECT db.* FROM sys.databases db");

            DBSObjectFilter databaseFilters = owner.getContainer().getObjectFilter(SQLServerDatabase.class, null, false);
            if (databaseFilters != null && databaseFilters.isEnabled()) {
                JDBCUtils.appendFilterClause(sql, databaseFilters, "name", true);
            }
            sql.append("\nORDER BY db.name");
            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            if (databaseFilters != null) {
                JDBCUtils.setFilterParameters(dbStat, 1, databaseFilters);
            }
            return dbStat;
        }

        @Override
        protected SQLServerDatabase fetchObject(@NotNull JDBCSession session, @NotNull SQLServerDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new SQLServerDatabase(session, owner, resultSet);
        }

    }

    private class SystemDataTypeCache extends JDBCObjectCache<SQLServerDataSource, SQLServerDataType> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull SQLServerDataSource sqlServerDataSource) throws SQLException {
            return session.prepareStatement("SELECT * FROM sys.types WHERE is_user_defined = 0 order by name");
        }

        @Override
        protected SQLServerDataType fetchObject(@NotNull JDBCSession session, @NotNull SQLServerDataSource dataSource, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new SQLServerDataType(dataSource, resultSet);
        }
    }
}
