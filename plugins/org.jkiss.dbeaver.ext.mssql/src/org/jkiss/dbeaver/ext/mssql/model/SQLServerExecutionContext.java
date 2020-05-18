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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

/**
 * SQLServerExecutionContext
 */
public class SQLServerExecutionContext extends JDBCExecutionContext implements DBCExecutionContextDefaults<SQLServerDatabase, SQLServerSchema> {
    private static final Log log = Log.getLog(SQLServerExecutionContext.class);

    //private SQLServerDatabase activeDatabase;
    private String activeDatabaseName;
    private String activeSchemaName;
    private String currentUser;

    SQLServerExecutionContext(@NotNull JDBCRemoteInstance instance, String purpose) {
        super(instance, purpose);
    }

    @NotNull
    @Override
    public SQLServerDataSource getDataSource() {
        return (SQLServerDataSource) super.getDataSource();
    }

    @NotNull
    @Override
    public SQLServerExecutionContext getContextDefaults() {
        return this;
    }

    public String getActiveDatabaseName() {
        return activeDatabaseName;
    }

    @Override
    public SQLServerDatabase getDefaultCatalog() {
        return getDataSource().getDatabase(activeDatabaseName);
    }

    @Override
    public SQLServerSchema getDefaultSchema() {
        if (CommonUtils.isEmpty(activeSchemaName)) {
            return null;
        }
        try {
            SQLServerDatabase defaultCatalog = getDefaultCatalog();
            return defaultCatalog == null ? null : defaultCatalog.getSchema(new VoidProgressMonitor(), activeSchemaName);
        } catch (DBException e) {
            log.error(e);
            return null;
        }
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
    public void setDefaultCatalog(DBRProgressMonitor monitor, SQLServerDatabase catalog, SQLServerSchema schema) throws DBCException {
        if (activeDatabaseName != null && activeDatabaseName.equals(catalog.getName())) {
            return;
        }
        final SQLServerDatabase oldActiveDatabase = getDefaultCatalog();

        if (!setCurrentDatabase(monitor, catalog)) {
            return;
        }
        try {
            catalog.getSchemas(monitor);
        } catch (DBException e) {
            log.debug("Error caching database schemas", e);
        }
        activeDatabaseName = catalog.getName();

        // Send notifications
        DBUtils.fireObjectSelectionChange(oldActiveDatabase, catalog);
    }

    @Override
    public void setDefaultSchema(DBRProgressMonitor monitor, SQLServerSchema schema) throws DBCException {
        if (activeSchemaName != null && activeSchemaName.equals(schema.getName())) {
            return;
        }
        final SQLServerSchema oldActiveSchema = getDefaultSchema();

//        if (!setCurrentSchema(monitor, schema.getName())) {
//            return;
//        }
        activeSchemaName = schema.getName();

        // Send notifications
        DBUtils.fireObjectSelectionChange(oldActiveSchema, schema);
    }

    @Override
    public boolean refreshDefaults(DBRProgressMonitor monitor, boolean useBootstrapSettings) throws DBException {
        // Check default active schema
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "Query active database")) {
            try {
                currentUser = SQLServerUtils.getCurrentUser(session);
            } catch (Throwable e) {
                log.debug("Error getting current user: " + e.getMessage());
            }
            try {
                activeSchemaName = SQLServerUtils.getCurrentSchema(session);
            } catch (Throwable e) {
                log.debug("Error getting current schema: " + e.getMessage());
            }
            if (CommonUtils.isEmpty(activeSchemaName)) {
                activeSchemaName = SQLServerConstants.DEFAULT_SCHEMA_NAME;
            }
            if (useBootstrapSettings) {
                DBPConnectionBootstrap bootstrap = getBootstrapSettings();
                if (!CommonUtils.isEmpty(bootstrap.getDefaultCatalogName()) && supportsCatalogChange()) {
                    setCurrentDatabase(monitor, bootstrap.getDefaultCatalogName());
                }
/*
                if (!CommonUtils.isEmpty(bootstrap.getDefaultSchemaName()) && supportsSchemaChange()) {
                    setCurrentSchema(monitor, bootstrap.getDefaultSchemaName());
                }
*/
            }

            String currentDatabase = SQLServerUtils.getCurrentDatabase(session);
            if (!CommonUtils.equalObjects(currentDatabase, activeDatabaseName)) {
                activeDatabaseName = currentDatabase;
                return true;
            }
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }

        return false;
    }

    boolean setCurrentDatabase(DBRProgressMonitor monitor, SQLServerDatabase object) throws DBCException {
        if (object == null) {
            log.debug("Null current schema");
            return false;
        }
        String databaseName = object.getName();
        return setCurrentDatabase(monitor, databaseName);
    }

    private boolean setCurrentDatabase(DBRProgressMonitor monitor, String databaseName) {
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Set active database")) {
            SQLServerUtils.setCurrentDatabase(session, databaseName);
            activeDatabaseName = databaseName;
            return true;
        } catch (SQLException e) {
            log.error(e);
            return false;
        }
    }

/*
    private boolean setCurrentSchema(DBRProgressMonitor monitor, String schemaName) {
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Set active schema")) {
            SQLServerUtils.setCurrentSchema(session, currentUser, schemaName);
            activeSchemaName = schemaName;
            return true;
        } catch (SQLException e) {
            log.error(e);
            return false;
        }
    }
*/

}
