/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mysql.MySQLUtils;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.dpi.DPIContainer;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

/**
 * MySQLExecutionContext
 */
public class MySQLExecutionContext extends JDBCExecutionContext implements DBCExecutionContextDefaults<MySQLCatalog, DBSSchema> {
    private static final Log log = Log.getLog(MySQLExecutionContext.class);

    //private MySQLCatalog activeDatabase;
    private String activeDatabaseName;

    MySQLExecutionContext(@NotNull JDBCRemoteInstance instance, String purpose) {
        super(instance, purpose);
    }

    @DPIContainer
    @NotNull
    @Override
    public MySQLDataSource getDataSource() {
        return (MySQLDataSource) super.getDataSource();
    }

    @NotNull
    @Override
    public MySQLExecutionContext getContextDefaults() {
        return this;
    }

    public String getActiveDatabaseName() {
        return activeDatabaseName;
    }

    @Override
    public MySQLCatalog getDefaultCatalog() {
        return CommonUtils.isEmpty(activeDatabaseName) ? null : getDataSource().getCatalog(activeDatabaseName);
    }

    @Override
    public DBSSchema getDefaultSchema() {
        return null;
    }

    @Override
    public boolean supportsCatalogChange() {
        return true;
    }

    @Override
    public boolean supportsSchemaChange() {
        return false;
    }

    @Override
    public void setDefaultCatalog(DBRProgressMonitor monitor, MySQLCatalog catalog, DBSSchema schema) throws DBCException {
        if (activeDatabaseName != null && activeDatabaseName.equals(catalog.getName())) {
            return;
        }
        final MySQLCatalog oldActiveDatabase = getDefaultCatalog();

        // MySQL driver bug - it doesn't ley to change active schema in read-only mode (#9167)
        boolean connectionReadOnly = isConnectionReadOnly(monitor);
        if (connectionReadOnly) {
            setConnectionReadOnly(monitor, false);
        }
        try {
            if (!setCurrentDatabase(monitor, catalog)) {
                return;
            }
        } finally {
            if (connectionReadOnly) {
                setConnectionReadOnly(monitor, true);
            }
        }
        activeDatabaseName = catalog.getName();

        // Send notifications
        DBUtils.fireObjectSelectionChange(oldActiveDatabase, catalog, this);
    }

    private void setConnectionReadOnly(DBRProgressMonitor monitor, boolean readOnly) {
        try {
            getConnection(monitor).setReadOnly(readOnly);
        } catch (Exception e) {
            log.debug(e);
        }
    }

    private boolean isConnectionReadOnly(DBRProgressMonitor monitor) {
        try {
            return getConnection(monitor).isReadOnly();
        } catch (Exception e) {
            log.debug(e);
            return false;
        }
    }

    @Override
    public void setDefaultSchema(DBRProgressMonitor monitor, DBSSchema schema) throws DBCException {
        throw new DBCFeatureNotSupportedException();
    }

    @Override
    public boolean refreshDefaults(DBRProgressMonitor monitor, boolean useBootstrapSettings) throws DBException {
        // Check default active schema
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "Query active database")) {
            if (useBootstrapSettings) {
                DBPConnectionBootstrap bootstrap = getBootstrapSettings();
                if (!CommonUtils.isEmpty(bootstrap.getDefaultCatalogName())) {
                    setCurrentDatabaseName(monitor, bootstrap.getDefaultCatalogName());
                }
            }

            activeDatabaseName = MySQLUtils.determineCurrentDatabase(session);
        } catch (DBException e) {
            throw new DBCException(e, this);
        }

        return true;
    }

    boolean setCurrentDatabase(DBRProgressMonitor monitor, MySQLCatalog object) throws DBCException {
        if (object == null) {
            log.debug("Null current database");
            return false;
        }
        String databaseName = object.getName();
        return setCurrentDatabaseName(monitor, databaseName);
    }

    private boolean setCurrentDatabaseName(DBRProgressMonitor monitor, String databaseName) throws DBCException {
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Set active catalog")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("use " + DBUtils.getQuotedIdentifier(getDataSource(), databaseName))) {
                dbStat.execute();
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
            this.activeDatabaseName = databaseName;
            return true;
        }
    }

    @NotNull
    @Override
    public DBCCachedContextDefaults getCachedDefault() {
        return new DBCCachedContextDefaults(activeDatabaseName, null);
    }
}
