/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectSelector;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Collection;

public class MSSQLDataSource extends JDBCDataSource implements DBSObjectSelector {

    private static final Log log = Log.getLog(MSSQLDataSource.class);

    private final CatalogCache catalogCache = new CatalogCache();
    private final JDBCBasicDataTypeCache<MSSQLDataSource, JDBCDataType> dataTypeCache;
    private String activeDatabaseName;

    public MSSQLDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container)
        throws DBException
    {
        super(monitor, container, new MSSQLDialect());
        dataTypeCache = new JDBCBasicDataTypeCache<>(this);
    }

    @Override
    public Object getDataSourceFeature(String featureId) {
        if (DBConstants.FEATURE_LIMIT_AFFECTS_DML.equals(featureId)) {
            return true;
        }
        return super.getDataSourceFeature(featureId);
    }

    @Override
    public void initialize(DBRProgressMonitor monitor) throws DBException {
        super.initialize(monitor);
        dataTypeCache.getAllObjects(monitor, this);

        catalogCache.getAllObjects(monitor, this);
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load mssql datasource metadata")) {
            activeDatabaseName = determineCurrentDatabase(session);
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
    protected String getConnectionUserName(DBPConnectionConfiguration connectionInfo) {
        if (SQLServerUtils.isWindowsAuth(connectionInfo)) {
            return "";
        } else {
            return super.getConnectionUserName(connectionInfo);
        }
    }

    @Override
    protected String getConnectionUserPassword(DBPConnectionConfiguration connectionInfo) {
        if (SQLServerUtils.isWindowsAuth(connectionInfo)) {
            return "";
        } else {
            return super.getConnectionUserPassword(connectionInfo);
        }
    }

    @Override
    public DBPDataSource getDataSource() {
        return this;
    }

    //////////////////////////////////////////////////////////
    // Databases

    @Override
    public Collection<? extends DBSDataType> getLocalDataTypes() {
        return dataTypeCache.getCachedObjects();
    }

    @Override
    public DBSDataType getLocalDataType(String typeName) {
        return dataTypeCache.getCachedObject(typeName);
    }

    //////////////////////////////////////////////////////////
    // Databases

    @Association
    public Collection<MSSQLDatabase> getDatabases(DBRProgressMonitor monitor) throws DBException {
        return catalogCache.getAllObjects(monitor, this);
    }

    public MSSQLDatabase getDatabase(DBRProgressMonitor monitor, String childName) throws DBException {
        return catalogCache.getObject(monitor, this, childName);
    }

    @Override
    public Collection<? extends DBSObject> getChildren(DBRProgressMonitor monitor) throws DBException {
        return getDatabases(monitor);
    }

    @Override
    public DBSObject getChild(DBRProgressMonitor monitor, String childName) throws DBException {
        return getDatabase(monitor, childName);
    }

    @Override
    public Class<? extends DBSObject> getChildType(DBRProgressMonitor monitor) throws DBException {
        return MSSQLDatabase.class;
    }

    @Override
    public void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException {
        catalogCache.getAllObjects(monitor, this);
    }

    //////////////////////////////////////////////////////////
    // Default object

    @Override
    public boolean supportsDefaultChange() {
        return true;
    }

    @Override
    public MSSQLDatabase getDefaultObject() {
        return CommonUtils.isEmpty(activeDatabaseName) ? null : catalogCache.getCachedObject(activeDatabaseName);
    }

    @Override
    public void setDefaultObject(DBRProgressMonitor monitor, DBSObject object) throws DBException {
        final MSSQLDatabase oldSelectedEntity = getDefaultObject();
        if (!(object instanceof MSSQLDatabase)) {
            throw new DBException("Invalid object type: " + object);
        }
        for (JDBCExecutionContext context : getAllContexts()) {
            useDatabase(monitor, context, (MSSQLDatabase) object);
        }
        activeDatabaseName = object.getName();

        // Send notifications
        if (oldSelectedEntity != null) {
            DBUtils.fireObjectSelect(oldSelectedEntity, false);
        }
        if (this.activeDatabaseName != null) {
            DBUtils.fireObjectSelect(object, true);
        }
    }

    @Override
    public boolean refreshDefaultObject(DBCSession session) throws DBException {
        final String newCatalogName = determineCurrentDatabase((JDBCSession) session);
        if (!CommonUtils.equalObjects(newCatalogName, activeDatabaseName)) {
            final MSSQLDatabase newCatalog = catalogCache.getCachedObject(newCatalogName);
            if (newCatalog != null) {
                setDefaultObject(session.getProgressMonitor(), newCatalog);
                return true;
            }
        }
        return false;
    }

    private String determineCurrentDatabase(JDBCSession session) {
        try {
            return JDBCUtils.queryString(session, "SELECT db_name()");
        } catch (SQLException e) {
            log.error(e);
            return null;
        }
    }

    private void useDatabase(DBRProgressMonitor monitor, JDBCExecutionContext context, MSSQLDatabase database) throws DBCException {
        if (database == null) {
            log.debug("Null current database");
            return;
        }
        try (JDBCSession session = context.openSession(monitor, DBCExecutionPurpose.UTIL, "Set active database")) {
            JDBCUtils.executeSQL(session, "use " + DBUtils.getQuotedIdentifier(database));
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }
    }

    @Override
    public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
        this.activeDatabaseName = null;

        return super.refreshObject(monitor);
    }

    static class CatalogCache extends JDBCObjectCache<MSSQLDataSource, MSSQLDatabase>
    {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MSSQLDataSource owner) throws SQLException
        {
            return session.prepareStatement("EXEC sp_tables '','','%',NULL");
        }

        @Override
        protected MSSQLDatabase fetchObject(@NotNull JDBCSession session, @NotNull MSSQLDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new MSSQLDatabase(owner, resultSet);
        }

    }

}
