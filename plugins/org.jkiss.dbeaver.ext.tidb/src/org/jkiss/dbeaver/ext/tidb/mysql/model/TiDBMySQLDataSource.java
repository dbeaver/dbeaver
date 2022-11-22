/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.tidb.mysql.model;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.mysql.model.MySQLEngine;
import org.jkiss.dbeaver.ext.mysql.model.MySQLPrivilege;
import org.jkiss.dbeaver.ext.tidb.model.plan.TiDBPlanAnalyzer;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStandardValueHandlerProvider;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.osgi.framework.Version;

public class TiDBMySQLDataSource extends MySQLDataSource {
    private static final Log log = Log.getLog(MySQLDataSource.class);
    
    private final JDBCBasicDataTypeCache<MySQLDataSource, JDBCDataType> dataTypeCache;
    private final TiDBCatalogCache tidbCatalogCache = new TiDBCatalogCache();

    private String tidbVersion = "";
    
    public String getServerVersion() {
    	return this.tidbVersion;
    }
    
    public TiDBMySQLDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        super(monitor, container);
        dataTypeCache = new JDBCBasicDataTypeCache<>(this);
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.initialize(monitor);
        dataTypeCache.getAllObjects(monitor, this);
        tidbCatalogCache.getAllObjects(monitor, this);
        
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "TiDB version fetch")) {
        	try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT VERSION() AS VERSION")) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                    	this.tidbVersion = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_VERSION);
                    }
                }
            } catch (SQLException ex) {
                log.error(ex);
            }
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DBCQueryPlanner.class) {
            return adapter.cast(new TiDBPlanAnalyzer(this));
        } else if (adapter == DBDValueHandlerProvider.class) {
            return adapter.cast(new JDBCStandardValueHandlerProvider());
        } else {
            return super.getAdapter(adapter);
        }
    }

    @Override
    public MySQLEngine getEngine(String name) {
        return DBUtils.findObject(getEngines(), name, true);
    }

    @Override
    public MySQLEngine getDefaultEngine() {
        return this.getEngine("tidb");
    }

    @Override
    public MySQLPrivilege getPrivilege(DBRProgressMonitor monitor, String name) throws DBException {
        if (name.equalsIgnoreCase("SHOW DB")) {
            return DBUtils.findObject(getPrivileges(monitor), "Show databases", true);
        }
        return DBUtils.findObject(getPrivileges(monitor), name, true);
    }

    @Override
    public Collection<? extends DBSDataType> getLocalDataTypes() {
        return dataTypeCache.getCachedObjects();
    }

    @Override
    public DBSDataType getLocalDataType(String typeName) {
        return dataTypeCache.getCachedObject(typeName);
    }

    @Override
    public DBSDataType getLocalDataType(int typeID) {
        return dataTypeCache.getCachedObject(typeID);
    }

    @Override
    public Collection<? extends MySQLCatalog> getChildren(@NotNull DBRProgressMonitor monitor) {
        return getCatalogs();
    }

    @Override
    public MySQLCatalog getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) {
        return getCatalog(childName);
    }

    @NotNull
    @Override
    public Class<? extends MySQLCatalog> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) {
        return TiDBMySQLCatalog.class;
    }

    TiDBCatalogCache getTiDBCatalogCache() {
        return tidbCatalogCache;
    }

    @Override
    public MySQLCatalog getCatalog(String name) {
        return tidbCatalogCache.getCachedObject(name);
    }

    @Override
    public Collection<MySQLCatalog> getCatalogs() {
        return new ArrayList<>(tidbCatalogCache.getCachedObjects());
    }

    static class TiDBCatalogCache extends JDBCObjectCache<TiDBMySQLDataSource, TiDBMySQLCatalog> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session,
                @NotNull TiDBMySQLDataSource owner) throws SQLException {
            StringBuilder catalogQuery = new StringBuilder("show databases");
            DBSObjectFilter catalogFilters = owner.getContainer().getObjectFilter(MySQLCatalog.class, null, false);
            if (catalogFilters != null) {
                JDBCUtils.appendFilterClause(catalogQuery, catalogFilters, MySQLConstants.COL_DATABASE_NAME, true, owner);
            }
            JDBCPreparedStatement dbStat = session.prepareStatement(catalogQuery.toString());
            if (catalogFilters != null) {
                JDBCUtils.setFilterParameters(dbStat, 1, catalogFilters);
            }
            return dbStat;
        }

        @Override
        protected TiDBMySQLCatalog fetchObject(@NotNull JDBCSession session,
                                               @NotNull TiDBMySQLDataSource owner, @NotNull JDBCResultSet resultSet) {
            return new TiDBMySQLCatalog(owner, resultSet);
        }

    }

    @Override
    public boolean supportsInformationSchema() {
        return true;
    }
    
    @Override
    protected DBPDataSourceInfo createDataSourceInfo(DBRProgressMonitor monitor, @NotNull JDBCDatabaseMetaData metaData) {
    	super.createDataSourceInfo(monitor, metaData);
        return new TiDBMySQLDataSourceInfo(this, metaData);
    }
    
    @Override
    public boolean supportsSequences() {
    	return this.isServerVersionAtLeast(4, 0);
    }

    @Override
    public boolean isServerVersionAtLeast(int major, int minor) {
    	Version tidbVersion = this.getInfo().getDatabaseVersion();
        if (tidbVersion.getMajor() < major) {
            return false;
        } else if (tidbVersion.getMajor() == major && tidbVersion.getMinor() < minor) {
            return false;
        }
        return true;
    }
}
