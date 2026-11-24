/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mysql.MySQLUtils;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.utils.CommonUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MySQLExecutionContext
 */
public class MySQLExecutionContext extends JDBCExecutionContext
    implements DBCExecutionContextDefaults<MySQLCatalog, DBSSchema>, DBCTemporaryTableManager {
    private static final Log log = Log.getLog(MySQLExecutionContext.class);

    //private MySQLCatalog activeDatabase;
    private String activeDatabaseName;
    private final Map<TemporaryTableName, MySQLTemporaryTable> temporaryTables = new ConcurrentHashMap<>();

    MySQLExecutionContext(@NotNull JDBCRemoteInstance instance, String purpose) {
        super(instance, purpose);
    }

    @Nullable
    @Override
    public TemporaryTable registerTemporaryTable(
        @NotNull DBCSession session, @NotNull List<String> qualifiedName, boolean ifNotExists
    ) throws DBException {
        Connection connection = getConnectionOrNull();
        TemporaryTableName name = temporaryTableName(session, qualifiedName);
        if (connection == null || name == null) {
            return null;
        }
        MySQLTemporaryTable existing = temporaryTables.get(name);
        if (ifNotExists && existing != null && existing.getConnection() == connection) {
            return existing;
        }
        MySQLCatalog catalog = getDataSource().getCatalog(name.catalog());
        if (catalog == null) {
            catalog = new MySQLCatalog(getDataSource(), null);
            catalog.setName(name.catalog());
            catalog.setPersisted(true);
        }
        MySQLTemporaryTable table = new MySQLTemporaryTable(this, connection, catalog, qualifiedName.getLast());
        temporaryTables.put(name, table);
        return table;
    }

    @Nullable
    @Override
    public TemporaryTable temporaryTableChanged(
        @NotNull DBCSession session, @NotNull List<String> qualifiedName, boolean dropped, @NotNull List<String> newName
    )
        throws DBException
    {
        if (temporaryTables.isEmpty()) {
            return null;
        }
        TemporaryTableName name = temporaryTableName(session, qualifiedName);
        if (name == null) {
            return null;
        }
        if (!newName.isEmpty() && temporaryTables.remove(name) != null) {
            return registerTemporaryTable(session, newName.size() == 1 ? List.of(name.catalog(), newName.getFirst()) : newName, false);
        }
        if (dropped) {
            return temporaryTables.remove(name);
        }
        return temporaryTables.computeIfPresent(name, (key, table) ->
            new MySQLTemporaryTable(this, table.getConnection(), table.getContainer(), table.getName()));
    }

    @Nullable
    @Override
    public TemporaryTable findTemporaryTable(@NotNull DBCEntityMetaData metadata) {
        if (temporaryTables.isEmpty()) {
            return null;
        }
        String catalogName = CommonUtils.isEmpty(metadata.getCatalogName()) ? metadata.getSchemaName() : metadata.getCatalogName();
        if (CommonUtils.isEmpty(catalogName)) {
            catalogName = activeDatabaseName;
        }
        String tableName = metadata.getEntityName();
        if (CommonUtils.isEmpty(catalogName) || CommonUtils.isEmpty(tableName)) {
            return null;
        }
        MySQLTemporaryTable table = temporaryTables.get(temporaryTableName(catalogName, tableName));
        return table != null && table.getConnection() == getConnectionOrNull() ? table : null;
    }

    boolean isCurrentTemporaryTable(@NotNull MySQLTemporaryTable table) {
        return table.getConnection() == getConnectionOrNull() &&
            temporaryTables.get(temporaryTableName(table.getContainer().getName(), table.getName())) == table;
    }

    @Override
    protected void disconnect(boolean removeContext) {
        try {
            super.disconnect(removeContext);
        } finally {
            temporaryTables.clear();
        }
    }

    @Nullable
    private TemporaryTableName temporaryTableName(@NotNull DBCSession session, @NotNull List<String> qualifiedName) throws DBException {
        if (qualifiedName.isEmpty() || qualifiedName.size() > 2) {
            return null;
        }
        String catalog = qualifiedName.size() == 2 ? qualifiedName.getFirst() :
            MySQLUtils.determineCurrentDatabase((JDBCSession) session);
        return CommonUtils.isEmpty(catalog) ? null : temporaryTableName(catalog, qualifiedName.getLast());
    }

    @NotNull
    private TemporaryTableName temporaryTableName(@NotNull String catalog, @NotNull String table) {
        if (getDataSource().getLowerCaseTableNames() != 0) {
            return new TemporaryTableName(catalog.toLowerCase(Locale.ROOT), table.toLowerCase(Locale.ROOT));
        }
        return new TemporaryTableName(catalog, table);
    }

    private record TemporaryTableName(@NotNull String catalog, @NotNull String table) {
    }

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
