/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.starrocks;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.starrocks.model.StarRocksCatalog;
import org.jkiss.dbeaver.ext.starrocks.model.StarRocksDatabase;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPErrorAssistant;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformType;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.impl.sql.QueryTransformerLimit;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLState;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * StarRocks DataSource - extends JDBCDataSource directly to support 3-level hierarchy:
 * Catalog -> Database -> Table
 *
 * Reuses MySQL driver and adapts key functionality from MySQLDataSource.
 */
public class StarRocksDataSource extends JDBCDataSource {

    private static final Log log = Log.getLog(StarRocksDataSource.class);
    private static final Pattern ERROR_POSITION_PATTERN = Pattern.compile(" at line ([0-9]+)");
    private static final String DEFAULT_CATALOG_NAME = "default_catalog";

    private final JDBCBasicDataTypeCache<StarRocksDataSource, JDBCDataType> dataTypeCache;
    private final CatalogCache catalogCache = new CatalogCache();

    private int lowerCaseTableNames = 1;

    public StarRocksDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container)
            throws DBException {
        super(monitor, container, new StarRocksDialect());
        dataTypeCache = new JDBCBasicDataTypeCache<>(this);
    }

    @Override
    protected JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
        return new StarRocksExecutionContext(instance, type);
    }

    @Override
    protected void initializeContextState(
            @NotNull DBRProgressMonitor monitor,
            @NotNull JDBCExecutionContext context,
            JDBCExecutionContext initFrom
    ) throws DBException {
        if (initFrom != null) {
            StarRocksExecutionContext starRocksContext = (StarRocksExecutionContext) context;
            StarRocksExecutionContext starRocksInitFrom = (StarRocksExecutionContext) initFrom;
            // Copy active database from source context
            String activeCatalog = starRocksInitFrom.getActiveCatalogName();
            String activeDatabase = starRocksInitFrom.getActiveDatabaseName();
            if (!CommonUtils.isEmpty(activeCatalog)) {
                starRocksContext.setActiveCatalogName(activeCatalog);
            }
            if (!CommonUtils.isEmpty(activeDatabase)) {
                starRocksContext.setActiveDatabaseName(activeDatabase);
            }
        } else {
            ((StarRocksExecutionContext) context).refreshDefaults(monitor, true);
        }
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.initialize(monitor);

        // Load server configuration
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load StarRocks server configuration")) {
            // Get lower_case_table_names setting
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SHOW VARIABLES LIKE 'lower_case_table_names'")) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        lowerCaseTableNames = JDBCUtils.safeGetInt(dbResult, 2);
                    }
                }
            } catch (Exception e) {
                log.debug("Error reading lower_case_table_names", e);
            }

            // Load data types
            dataTypeCache.getAllObjects(monitor, this);
        }
    }

    @Override
    protected Map<String, String> getInternalConnectionProperties(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBPDriver driver,
            @NotNull JDBCExecutionContext context,
            @NotNull String purpose,
            @NotNull DBPConnectionConfiguration connectionInfo
    ) throws DBCException {
        Map<String, String> props = new HashMap<>();
        // MySQL driver properties for compatibility
        props.put("useInformationSchema", "true");

        // Handle timezone issues similar to MySQL
        if (CommonUtils.isEmpty(connectionInfo.getProviderProperty(MySQLConstants.PROP_SERVER_TIMEZONE))) {
            props.put("serverTimezone", "UTC");
        }

        return props;
    }

    @Override
    public DBCQueryTransformer createQueryTransformer(@NotNull DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            return new QueryTransformerLimit(false, true);
        }
        return super.createQueryTransformer(type);
    }

    // ======== Data Type Support ========

    @Override
    public DBSDataType getLocalDataType(String typeName) {
        return dataTypeCache.getCachedObject(typeName);
    }

    @Override
    public DBSDataType getLocalDataType(int typeID) {
        return dataTypeCache.getCachedObject(typeID);
    }

    @Override
    public Collection<? extends DBSDataType> getLocalDataTypes() {
        try {
            return dataTypeCache.getAllObjects(new org.jkiss.dbeaver.model.runtime.VoidProgressMonitor(), this);
        } catch (DBException e) {
            log.error("Error loading data types", e);
            return null;
        }
    }

    @Override
    public String getDefaultDataTypeName(@NotNull DBPDataKind dataKind) {
        switch (dataKind) {
            case BOOLEAN:
                return "BOOLEAN";
            case NUMERIC:
                return "BIGINT";
            case STRING:
                return "VARCHAR";
            case DATETIME:
                return "DATETIME";
            case BINARY:
                return "VARBINARY";
            case CONTENT:
                return "STRING";
            case ROWID:
                return "BIGINT";
            default:
                return super.getDefaultDataTypeName(dataKind);
        }
    }

    // ======== Catalog/Database Navigation - 3-Level Hierarchy ========

    public int getLowerCaseTableNames() {
        return lowerCaseTableNames;
    }

    @Association
    public Collection<StarRocksCatalog> getCatalogs(DBRProgressMonitor monitor) throws DBException {
        return catalogCache.getAllObjects(monitor, this);
    }

    public StarRocksCatalog getCatalog(DBRProgressMonitor monitor, String name) throws DBException {
        return catalogCache.getObject(monitor, this, name);
    }

    /**
     * Get the default catalog (internal catalog)
     */
    public StarRocksCatalog getDefaultCatalog(DBRProgressMonitor monitor) throws DBException {
        return getCatalog(monitor, DEFAULT_CATALOG_NAME);
    }

    /**
     * Check if a catalog is the default (internal) catalog
     */
    public boolean isDefaultCatalog(StarRocksCatalog catalog) {
        return DEFAULT_CATALOG_NAME.equalsIgnoreCase(catalog.getName());
    }

    // ======== DBSObjectContainer Implementation ========

    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        // Return all catalogs as top-level children
        return getCatalogs(monitor);
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        // First try to find a catalog with this name
        StarRocksCatalog catalog = getCatalog(monitor, childName);
        if (catalog != null) {
            return catalog;
        }

        // If not found as catalog, search for database in default catalog
        StarRocksCatalog defaultCatalog = getDefaultCatalog(monitor);
        if (defaultCatalog != null) {
            StarRocksDatabase database = defaultCatalog.getDatabase(monitor, childName);
            if (database != null) {
                return database;
            }
        }

        // Search all catalogs for a database with this name
        for (StarRocksCatalog cat : getCatalogs(monitor)) {
            StarRocksDatabase database = cat.getDatabase(monitor, childName);
            if (database != null) {
                return database;
            }
        }

        return null;
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException {
        return StarRocksCatalog.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
        catalogCache.getAllObjects(monitor, this);
    }

    // ======== Error Handling (MySQL-compatible) ========

    @Override
    public ErrorType discoverErrorType(@NotNull Throwable error) {
        String sqlState = null;
        if (error instanceof SQLException) {
            sqlState = ((SQLException) error).getSQLState();
        }
        if (SQLState.SQL_08S01.getCode().equals(sqlState) ||
            SQLState.SQL_08007.getCode().equals(sqlState) ||
            SQLState.SQL_08003.getCode().equals(sqlState)) {
            return ErrorType.CONNECTION_LOST;
        }
        return super.discoverErrorType(error);
    }

    @Nullable
    @Override
    public ErrorPosition[] getErrorPosition(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext context, @NotNull String query, @NotNull Throwable error) {
        String message = error.getMessage();
        if (!CommonUtils.isEmpty(message)) {
            Matcher matcher = ERROR_POSITION_PATTERN.matcher(message);
            if (matcher.find()) {
                DBPErrorAssistant.ErrorPosition pos = new DBPErrorAssistant.ErrorPosition();
                pos.line = Integer.parseInt(matcher.group(1)) - 1;
                return new ErrorPosition[]{pos};
            }
        }
        return null;
    }

    // ======== Misc ========

    @Override
    public Object getDataSourceFeature(String featureId) {
        switch (featureId) {
            case DBPDataSource.FEATURE_MAX_STRING_LENGTH:
                return 65535;
            case DBPDataSource.FEATURE_LIMIT_AFFECTS_DML:
                return true;
        }
        return super.getDataSourceFeature(featureId);
    }

    public boolean isServerVersionAtLeast(int major, int minor) {
        if (databaseVersion != null) {
            return databaseVersion.getMajor() > major ||
                   (databaseVersion.getMajor() == major && databaseVersion.getMinor() >= minor);
        }
        return false;
    }

    // ======== Catalog Cache ========

    class CatalogCache extends JDBCObjectCache<StarRocksDataSource, StarRocksCatalog> {
        @NotNull
        @Override
        protected JDBCPreparedStatement prepareObjectsStatement(
                @NotNull JDBCSession session,
                @NotNull StarRocksDataSource owner) throws SQLException {
            return session.prepareStatement("SHOW CATALOGS");
        }

        @NotNull
        @Override
        protected StarRocksCatalog fetchObject(
                @NotNull JDBCSession session,
                @NotNull StarRocksDataSource owner,
                @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new StarRocksCatalog(owner, resultSet);
        }
    }
}
