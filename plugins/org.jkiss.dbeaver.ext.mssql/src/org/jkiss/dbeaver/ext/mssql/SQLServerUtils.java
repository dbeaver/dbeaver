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

package org.jkiss.dbeaver.ext.mssql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mssql.model.*;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCEntityMetaData;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCConnectionImpl;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.DBSQLException;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.utils.CommonUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQLServerUtils
 */
public class SQLServerUtils {

    private static final Log log = Log.getLog(SQLServerUtils.class);

    private static final Pattern CROSS_DATABASE_QUERY_ERROR_PATTERN =
        Pattern.compile("Reference to database and/or server name in '([^']+)' is not supported in this version of SQL Server\\.");

    public static boolean isDriverSqlServer(DBPDriver driver) {
        return driver.getSampleURL().contains(":sqlserver");
    }

    public static boolean isDriverGeneric(DBPDriver driver) {
        return driver.getId().contains("generic");
    }

    public static boolean isDriverAzure(DBPDriver driver) {
        return driver.getId().contains("azure");
    }

    public static boolean isDriverBabelfish(DBPDriver driver) {
        return driver.getId().contains("babelfish");
    }

    public static boolean isDriverJtds(DBPDriver driver) {
        return driver.getSampleURL().startsWith("jdbc:jtds");
    }

    public static boolean isWindowsAuth(DBPConnectionConfiguration connectionInfo) {
        return CommonUtils.toBoolean(connectionInfo.getProviderProperty(SQLServerConstants.PROP_CONNECTION_WINDOWS_AUTH)) ||
                CommonUtils.toBoolean(connectionInfo.getProperties().get(SQLServerConstants.PROP_CONNECTION_INTEGRATED_SECURITY));
    }

    public static boolean isActiveDirectoryAuth(DBPConnectionConfiguration connectionInfo) {
        return SQLServerConstants.AUTH_ACTIVE_DIRECTORY_PASSWORD.equals(
            connectionInfo.getProperty(SQLServerConstants.PROP_CONNECTION_AUTHENTICATION));
    }

    public static void setCurrentDatabase(JDBCSession session, String schema) throws SQLException {
        JDBCUtils.executeSQL(session,
            "use " + DBUtils.getQuotedIdentifier(session.getDataSource(), schema));
    }

    public static void setCurrentSchema(JDBCSession session, String currentUser, String schema) throws SQLException {
        if (!CommonUtils.isEmpty(currentUser)) {
            JDBCUtils.executeSQL(session,
                "alter user " + DBUtils.getQuotedIdentifier(session.getDataSource(), currentUser) +
                    " with default_schema = " + DBUtils.getQuotedIdentifier(session.getDataSource(), schema));
        }
    }

    public static String getCurrentUser(JDBCSession session) throws SQLException {
        /* dead code! */
        // See https://stackoverflow.com/questions/4101863/sql-server-current-user-name
        return JDBCUtils.queryString(
            session,
            "select original_login()");
    }

    public static String getCurrentDatabase(JDBCSession session) throws SQLException {
        /* dead code! */
        return JDBCUtils.queryString(
            session,
            "select db_name()");
    }

    public static String getCurrentSchema(JDBCSession session) throws SQLException {
        /* dead code! */
        return JDBCUtils.queryString(
            session,
            "select schema_name()");
    }

    public static boolean isShowAllSchemas(DBPDataSource dataSource) {
        final DBPDataSourceContainer container = dataSource.getContainer();
        if (isDriverBabelfish(container.getDriver()))
            return true;
        return CommonUtils.toBoolean(container.getConnectionConfiguration().getProviderProperty(SQLServerConstants.PROP_SHOW_ALL_SCHEMAS));
    }

    /**
     * Checks whether the {@code nchar} and {@code nvarchar} are stored in the UCS-2 encoding
     * which uses a byte-pair for representing a code point of the text.
     * <p>
     * Some databases return size of a given column in bytes, and in such case
     * it must be divided by {@code 2} to compensate the encoding.
     */
    public static boolean isUnicodeCharStoredAsBytePairs(@NotNull DBPDataSource dataSource) {
        return !isDriverAzure(dataSource.getContainer().getDriver());
    }

    public static boolean supportsCrossDatabaseQueries(JDBCDataSource dataSource) {
        final DBPDriver driver = dataSource.getContainer().getDriver();
        if (isDriverBabelfish(driver)) {
            return false;
        }
        boolean isSqlServer = isDriverSqlServer(driver);
        if (isSqlServer && !dataSource.isServerVersionAtLeast(SQLServerConstants.SQL_SERVER_2005_VERSION_MAJOR,0)) {
            return false;
        }
        boolean isDriverAzure = isSqlServer && isDriverAzure(driver);
        if (isDriverAzure && !dataSource.isServerVersionAtLeast(SQLServerConstants.SQL_SERVER_2012_VERSION_MAJOR, 0)) {
            return false;
        }
        return true;
    }

    public static String getSystemSchemaFQN(JDBCDataSource dataSource, String catalog, String systemSchema) {
        return catalog != null && supportsCrossDatabaseQueries(dataSource) ?
                DBUtils.getQuotedIdentifier(dataSource, catalog) + "." + systemSchema :
                systemSchema;
    }

    public static String getSystemTableName(SQLServerDatabase database, String tableName) {
        final SQLServerDataSource dataSource = database.getDataSource();
        if (isDriverBabelfish(dataSource.getContainer().getDriver())) {
            switch (tableName) {
                case "default_constraints":
                    return "(SELECT CAST(CONCAT('DF_',  o.relname, '_', d.oid) AS VARCHAR(20)) AS name, d.oid AS object_id, CAST(NULL AS INT) AS principal_id, o.relnamespace AS schema_id, d.adrelid AS parent_object_id, CAST('D' AS CHAR(2)) AS type, CAST('DEFAULT_CONSTRAINT' AS VARCHAR(60)) AS type_desc, CAST(NULL AS sys.datetime) AS create_date, CAST(NULL AS sys.datetime) AS modified_date, CAST(0 AS sys.bit) AS is_ms_shipped, CAST(0 AS sys.bit) AS is_published, d.adnum AS parent_column_id, pg_get_expr( d.adbin, d.adrelid ) AS definition, d.adbin, CAST(1 AS sys.bit) AS is_system_named FROM pg_catalog.pg_attrdef AS d INNER JOIN pg_catalog.pg_class AS o ON d.adrelid = o.oid) AS";
                case "check_constraints":
                    return "(SELECT CAST(c.conname AS sys.sysname) AS name, CAST(oid AS INTEGER) AS object_id, CAST(c.connamespace AS INTEGER) AS principal_id, CAST(c.connamespace AS INTEGER) AS schema_id, CAST(conrelid AS INTEGER) AS parent_object_id, CAST('C' AS CHAR(2)) AS type, CAST('CHECK_CONSTRAINT' AS VARCHAR(60)) AS type_desc, CAST(NULL AS sys.datetime) AS create_date, CAST(NULL AS sys.datetime) AS modify_date, CAST(0 AS sys.bit) AS is_ms_shipped, CAST(0 AS sys.bit) AS is_published, CAST(0 AS sys.bit) AS is_schema_published, CAST(0 AS sys.bit) AS is_disabled, CAST(0 AS sys.bit) AS is_not_for_replication, CAST(0 AS sys.bit) AS is_not_trusted, (SELECT TOP(1) CAST(parent_column_id AS INTEGER) FROM unnest(c.conkey) parent_column_id), CAST(pg_get_constraintdef(c.oid) AS text) AS definition, CAST(1 AS sys.bit) AS uses_database_collation, CAST(0 AS sys.bit) AS is_system_named FROM pg_catalog.pg_constraint AS c WHERE c.contype = 'c' AND c.conrelid <> 0) AS";
                case "index_columns":
                    return "(SELECT i.indrelid AS object_id , i.indexrelid AS index_id , a.attrelid AS index_column_id , a.attnum AS column_id , a.attnum AS key_ordinal , CAST(0 AS SMALLINT) AS partition_ordinal , CAST(0 AS SMALLINT) AS is_descending_key , CAST(1 AS SMALLINT) AS is_included_column FROM pg_index AS i INNER JOIN pg_catalog.pg_attribute a ON i.indexrelid = a.attrelid) AS";
                case "computed_columns":
                    return "(SELECT d.adrelid AS object_id, a.attname AS name, a.attnum AS column_id, a.atttypid AS system_type_id, a.atttypid AS user_type_id, CAST(0 AS SMALLINT) AS is_persisted, CAST(1 AS SMALLINT) AS is_computed, CAST(1 AS SMALLINT) AS uses_database_collation, pg_get_expr(d.adbin, d.adrelid) AS definition FROM pg_attrdef d JOIN pg_attribute a ON d.adrelid = a.attrelid AND d.adnum = a.attnum WHERE a.attgenerated = 's') AS";
                case "all_columns":
                    return "(SELECT c.oid AS object_id, a.attname AS name, a.attnum AS column_id, t.oid AS system_type_id, t.oid AS user_type_id, a.attlen AS max_length, CAST(NULL AS INTEGER) AS precision, CAST(NULL AS INTEGER) AS scale, coll.collname AS collation_name, (CASE a.attnotnull WHEN TRUE THEN 0 ELSE 1 END) AS is_nullable, 0 AS is_ansi_padded, 0 AS is_rowguidcol, 0 AS is_identity, 0 AS is_computed, 0 AS is_filestream, 0 AS is_replicated, 0 AS is_non_sql_subscribed, 0 AS is_merge_published, 0 AS is_dts_replicated, 0 AS is_xml_document, 0 AS xml_collection_id, COALESCE(d.oid, CAST(0 AS oid)) AS default_object_id, COALESCE((SELECT TOP(1) pg_constraint.oid FROM pg_constraint WHERE pg_constraint.conrelid = t.oid AND pg_constraint.contype = 'c' AND array_position(pg_constraint.conkey, a.attnum) IS NOT NULL), CAST(0 AS oid)) AS rule_object_id, 0 AS is_sparse, 0 AS is_column_set, 0 AS generated_always_type, CAST('NOT_APPLICABLE' AS VARCHAR(60)) AS generated_always_type_desc, CAST(NULL AS integer) AS encryption_type, CAST(NULL AS VARCHAR(64)) AS encryption_type_desc, CAST(NULL AS TEXT) AS encryption_algorithm_name, CAST(NULL AS INTEGER) AS column_encryption_key_id, CAST(NULL AS TEXT) AS column_encryption_key_database_name, 0 AS is_hidden, 0 AS is_masked FROM pg_attribute a JOIN pg_class c ON c.oid = a.attrelid JOIN pg_type t ON t.oid = a.atttypid JOIN pg_namespace s ON s.oid = c.relnamespace LEFT JOIN pg_attrdef d ON c.oid = d.adrelid AND a.attnum = d.adnum LEFT JOIN pg_collation coll ON coll.oid = t.typcollation WHERE a.attnum > 0 AND a.attisdropped = FALSE AND (c.relkind = 'r' OR c.relkind = 'v' OR c.relkind = 'm' OR c.relkind = 'f' OR c.relkind = 'p') AND has_column_privilege(CONCAT(quote_ident(s.nspname), '.', quote_ident(c.relname)), a.attname, 'SELECT,INSERT,UPDATE,REFERENCES') = TRUE AND has_schema_privilege(s.oid, 'USAGE') = TRUE) AS";
                case "all_objects":
                    return "(SELECT t.name, t.object_id, t.principal_id, t.schema_id, t.parent_object_id, CAST('U' AS text) AS type, CAST('USER_TABLE' AS text) AS type_desc, t.create_date, t.modify_date, t.is_ms_shipped, t.is_published, t.is_schema_published FROM sys.tables t WHERE has_schema_privilege(t.schema_id, 'USAGE') = TRUE UNION ALL SELECT v.name, v.object_id, v.principal_id, v.schema_id, v.parent_object_id, CAST('V' AS text) AS type, CAST('VIEW' AS text) AS type_desc, v.create_date, v.modify_date, v.is_ms_shipped, v.is_published, v.is_schema_published FROM sys.all_views v WHERE has_schema_privilege(v.schema_id, 'USAGE') = TRUE UNION ALL SELECT f.name, f.object_id, f.principal_id, f.schema_id, f.parent_object_id, CAST('F' AS text) AS type, CAST('FOREIGN_KEY_CONSTRAINT' AS text) AS type_desc, f.create_date, f.modify_date, f.is_ms_shipped, f.is_published, f.is_schema_published FROM sys.foreign_keys f WHERE has_schema_privilege(f.schema_id, 'USAGE') = TRUE UNION ALL SELECT p.name, p.object_id, p.principal_id, p.schema_id, p.parent_object_id, CAST('PK' AS text) AS type, CAST('PRIMARY_KEY_CONSTRAINT' AS text) AS type_desc, p.create_date, p.modify_date, p.is_ms_shipped, p.is_published, p.is_schema_published FROM sys.key_constraints p WHERE has_schema_privilege(p.schema_id, 'USAGE') = TRUE UNION ALL SELECT pr.name, pr.object_id, pr.principal_id, pr.schema_id, pr.parent_object_id, pr.type, pr.type_desc, pr.create_date, pr.modify_date, pr.is_ms_shipped, pr.is_published, pr.is_schema_published FROM sys.procedures pr WHERE has_schema_privilege(pr.schema_id, 'USAGE') = TRUE UNION ALL SELECT p.relname AS name, p.oid AS object_id, CAST(NULL AS integer) AS principal_id, s.oid AS schema_id, 0 AS parent_object_id, CAST('SO' AS VARCHAR(2)) AS type, CAST('SEQUENCE_OBJECT' AS VARCHAR(60)) AS type_desc, CAST(NULL AS sys.datetime) AS create_date, CAST(NULL AS sys.datetime) AS modify_date, 0 AS is_ms_shipped, 0 AS is_published, 0 AS is_schema_published FROM pg_class p JOIN pg_namespace s ON s.oid = p.relnamespace WHERE s.nspname <> 'information_schema' AND s.nspname <> 'pg_catalog' AND p.relkind = 'S' AND has_schema_privilege(s.oid, 'USAGE') = TRUE) AS";
                case "indexes":
                    return "(SELECT i.indrelid AS object_id, c.relname AS name, CASE i.indisclustered WHEN TRUE THEN 1 ELSE 2 END AS type, CAST(CASE i.indisclustered WHEN TRUE THEN 'CLUSTERED' ELSE 'NONCLUSTERED' END AS VARCHAR(60)) AS type_desc, CASE i.indisunique WHEN TRUE THEN 1 ELSE 0 END AS is_unique, c.reltablespace AS data_space_id, 0 AS ignore_dup_key, CASE i.indisprimary WHEN TRUE THEN 1 ELSE 0 END AS is_primary_key, 1 AS is_unique_constraint, 0 AS fill_factor, 0 AS is_padded, CASE i.indisready WHEN TRUE THEN 0 ELSE 1 END AS is_disabled, 0 AS is_hypothetical, 1 AS allow_row_locks, 1 AS allow_page_locks, 0 AS has_filter, CAST(NULL AS TEXT) AS filter_definition, 0 AS auto_created, c.oid AS index_id FROM pg_class c JOIN pg_namespace s ON s.oid = c.relnamespace JOIN pg_index i ON i.indexrelid = c.oid LEFT JOIN pg_constraint constr ON constr.conindid = c.oid WHERE c.relkind = 'i' AND i.indislive = TRUE AND s.nspname <> 'information_schema' AND s.nspname <> 'pg_catalog') AS";
                case "extended_properties":
                    return "(SELECT CAST(1 AS SMALLINT) AS class, CAST('OBJECT_OR_COLUMN' AS VARCHAR(60)) AS class_desc, d.objoid AS major_id, d.objsubid AS minor_id, CAST('MS_Description' AS VARCHAR(128)) AS name, d.description AS value FROM pg_description d) AS";
                case "table_types":
                    return "(SELECT format_type(t.oid, NULL) AS name, t.oid AS system_type_id, t.oid AS user_type_id, s.oid AS schema_id, CAST(NULL AS INTEGER) AS principal_id, t.typlen AS max_length, 0 AS precision, 0 AS scale, c.collname AS collation_name, CASE t.typnotnull WHEN TRUE THEN 0 ELSE 1 END AS is_nullable, CASE t.typcategory WHEN CAST('U' AS CHAR) THEN 1 ELSE 0 END AS is_user_defined, 0 AS is_assembly_type, 0 AS default_object_id, 0 AS rule_object_id, 0 AS is_table_type, t.oid AS type_table_object_id, 0 AS is_memory_optimized FROM pg_type t JOIN pg_namespace s ON s.oid = t.typnamespace LEFT JOIN pg_collation c ON c.oid = t.typcollation) AS";
                case "all_parameters":
                    return "(SELECT parameters.object_id, parameters.name, parameters.parameter_id, parameters.system_type_id, parameters.user_type_id, CAST(CASE WHEN t.typlen < 0 THEN -1 ELSE t.typlen END AS smallint) AS max_length, CAST(0 AS sys.tinyint) AS precision, CAST(0 AS sys.tinyint) AS scale, CAST(CASE WHEN parameters.mode = 'o' OR parameters.mode = 'b' THEN 1 ELSE 0 END AS sys.bit) AS is_output, CAST(0 AS sys.bit) AS is_cursor_ref, CAST(0 AS sys.bit) AS has_default_value, CAST(0 AS sys.bit) AS is_xml_document, NULL AS default_value, CAST(0 AS sys.int) AS xml_collection_id, CAST(CASE WHEN parameters.mode <> 'o' AND parameters.mode <> 'b' THEN 1 ELSE 0 END AS sys.bit) AS is_readonly, CAST(1 AS sys.bit) AS is_nullable, CAST(1 AS sys.int) AS encryption_type, CAST(NULL AS sys.nvarchar(64)) AS encryption_type_desc, CAST(NULL AS sys.sysname) AS encryption_algorithm_name, CAST(NULL AS sys.int) AS column_encryption_key_id, CAST(NULL AS sys.sysname) AS column_encryption_key_database_name FROM (SELECT p.oid AS object_id, unnest(p.proargnames) AS name, generate_subscripts(p.proallargtypes, 1) AS parameter_id, unnest(p.proallargtypes) AS system_type_id, unnest(p.proallargtypes) AS user_type_id, unnest(p.proargmodes) AS mode FROM pg_catalog.pg_proc p UNION ALL SELECT p.oid AS object_id, '' AS name, 0 AS parameter_id, p.prorettype AS system_type_id, p.prorettype AS user_type_id, 'o' AS mode FROM pg_catalog.pg_proc p) AS parameters INNER JOIN pg_type t ON t.oid = parameters.user_type_id) AS";
                case "synonyms":
                    return "(SELECT TOP 0 *, CAST('' AS sys.nvarchar(1035)) AS base_object_name FROM sys.objects WHERE type = 'SN') AS";
                case "sequences":
                    return "(SELECT TOP 0 *, CAST(0 AS sys.SQL_VARIANT) AS start_value, CAST(1 AS sys.SQL_VARIANT) AS increment, CAST(0 AS sys.SQL_VARIANT) AS minimum_value, CAST(1 AS sys.SQL_VARIANT) AS maximum_value, CAST(0 AS sys.bit) AS is_cycling, CAST(0 AS sys.bit) AS is_cached, CAST(NULL AS INT) AS cache_size, CAST(to_regtype('int') AS sys.int) AS system_type_id, CAST(to_regtype('int') AS sys.int) AS user_type_id, CAST(1 AS sys.tinyint) AS precision, CAST(0 AS sys.tinyint) AS scale, CAST(0 AS sys.SQL_VARIANT) AS current_value, CAST(0 AS sys.bit) AS is_exhausted, CAST(NULL AS sys.SQL_VARIANT) AS last_used_value FROM sys.objects WHERE type = 'SO') AS";
                case "triggers":
                    return "(SELECT name, object_id, CAST(1 AS sys.bit) AS parent_class, CAST('OBJECT_OR_COLUMN' AS sys.nvarchar(60)) AS parent_class_desc, parent_object_id AS parent_id, type, type_desc, create_date, modify_date, is_ms_shipped, CAST(0 AS sys.bit) AS is_disabled, CAST(1 AS sys.bit) AS is_not_for_replication, CAST(0 AS sys.bit) AS is_instead_of_trigger FROM sys.objects WHERE type = 'TR' OR type = 'TA') AS";
                case "allocation_units":
                case "partitions":
                case "master_files":
                    // not expected to be necessary, usage should be disabled
                    log.debug("Babelfish doesn't currently provide filesystem information for " + tableName + "!");
                    break;
                default:
                    break;
            }
        }
        return SQLServerUtils.getSystemSchemaFQN(dataSource, database.getName(), SQLServerConstants.SQL_SERVER_SYSTEM_SCHEMA) + "." + tableName;
    }

    public static String getSystemTableFQN(@NotNull JDBCDataSource dataSource, @NotNull DBSCatalog database, @NotNull String tableName, boolean isSQLServer) {
        return SQLServerUtils.getSystemSchemaFQN(
            dataSource,
            database.getName(),
            isSQLServer? SQLServerConstants.SQL_SERVER_SYSTEM_SCHEMA : SQLServerConstants.SYBASE_SYSTEM_SCHEMA)
            + "." + tableName;
    }

    public static String getExtendedPropsTableName(SQLServerDatabase database) {
        return getSystemTableName(database, SQLServerConstants.SYS_TABLE_EXTENDED_PROPERTIES);
    }

    public static DBSForeignKeyModifyRule getForeignKeyModifyRule(int actionNum) {
        switch (actionNum) {
            case 0: return DBSForeignKeyModifyRule.NO_ACTION;
            case 1: return DBSForeignKeyModifyRule.CASCADE;
            case 2: return DBSForeignKeyModifyRule.SET_NULL;
            case 3: return DBSForeignKeyModifyRule.SET_DEFAULT;
            default:
                return DBSForeignKeyModifyRule.NO_ACTION;
        }
    }

    public static String extractSource(@NotNull DBRProgressMonitor monitor, @NotNull SQLServerDatabase database, @NotNull SQLServerObject object) throws DBException {
        SQLServerDataSource dataSource = database.getDataSource();
        String systemSchema = getSystemSchemaFQN(dataSource, database.getName(), SQLServerConstants.SQL_SERVER_SYSTEM_SCHEMA);
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Read source code")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT definition FROM " + systemSchema + ".sql_modules WHERE object_id = ?")) {
                dbStat.setLong(1, object.getObjectId());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    StringBuilder sql = new StringBuilder();
                    while (dbResult.nextRow()) {
                        sql.append(dbResult.getString(1));
                    }
                    return sql.toString();
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, dataSource);
        }
    }

    public static String extractSource(@NotNull DBRProgressMonitor monitor, @NotNull SQLServerSchema schema, @NotNull  String objectName) throws DBException {
        SQLServerDataSource dataSource = schema.getDataSource();
        String systemSchema = getSystemSchemaFQN(dataSource, schema.getDatabase().getName(), SQLServerConstants.SQL_SERVER_SYSTEM_SCHEMA);
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Read source code")) {
            String objectFQN = DBUtils.getQuotedIdentifier(dataSource, schema.getName()) + "." + DBUtils.getQuotedIdentifier(dataSource, objectName);
            String sqlQuery = systemSchema + ".sp_helptext '" + objectFQN + "'";
            if (dataSource.isDataWarehouseServer(monitor) || isDriverBabelfish(dataSource.getContainer().getDriver()) || dataSource.isSynapseDatabase()) {
                sqlQuery = "SELECT definition FROM sys.sql_modules WHERE object_id = (OBJECT_ID(N'" + objectFQN + "'))";
            }
            try (JDBCPreparedStatement dbStat = session.prepareStatement(sqlQuery)) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    StringBuilder sql = new StringBuilder();
                    while (dbResult.nextRow()) {
                        sql.append(dbResult.getString(1));
                    }
                    return sql.toString();
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, dataSource);
        }
    }

    public static boolean isCommentSet(DBRProgressMonitor monitor, SQLServerDatabase database, SQLServerObjectClass objectClass, long majorId, long minorId) {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, database, "Check extended property")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT 1 FROM "+ SQLServerUtils.getExtendedPropsTableName(database) +
                " WHERE [class] = ? AND [major_id] = ? AND [minor_id] = ? AND [name] = N'MS_Description'")) {
                dbStat.setInt(1, objectClass.getClassId());
                dbStat.setLong(2, majorId);
                dbStat.setLong(3, minorId);
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        return true;
                    }
                    return false;
                }
            }
        } catch (Throwable e) {
            log.debug("Error checking extended property in dictionary", e);
            return false;
        }
    }

    @NotNull
    public static SQLServerAuthentication detectAuthSchema(DBPConnectionConfiguration connectionInfo) {
        // Detect auth schema
        // Now we use only PROP_AUTHENTICATION but here we support all legacy SQL Server configs
        SQLServerAuthentication auth = isWindowsAuth(connectionInfo) ? SQLServerAuthentication.WINDOWS_INTEGRATED :
            (isActiveDirectoryAuth(connectionInfo) ? SQLServerAuthentication.AD_PASSWORD : SQLServerAuthentication.SQL_SERVER_PASSWORD);

        {
            String authProp = connectionInfo.getProviderProperty(SQLServerConstants.PROP_AUTHENTICATION);
            if (authProp != null) {
                try {
                    auth = SQLServerAuthentication.valueOf(authProp);
                } catch (IllegalArgumentException e) {
                    log.warn("Bad auth schema: " + authProp);
                }
            }
        }

        return auth;
    }

    public static String changeCreateToAlterDDL(SQLDialect sqlDialect, String ddl) {
        String firstKeyword = SQLUtils.getFirstKeyword(sqlDialect, ddl);
        if ("CREATE".equalsIgnoreCase(firstKeyword)) {
            return ddl.replaceFirst(firstKeyword, "ALTER");
        }
        return ddl;
    }

    /**
     * To change original database sql with the "create" word to the "create and replace" for altering
     *
     * @param sql string query (can be nullable, will be checked)
     * @return changed SQL or original SQL if the "create and replace" already exists
     */
    public static String changeCreateToCreateOrReplace(@Nullable String sql) {
        if (CommonUtils.isNotEmpty(sql) && sql.contains("create") && !sql.contains("create or replace")) {
            sql = sql.replaceFirst("create", "create or replace");
        }
        return sql;
    }

    public static boolean isTableType(SQLServerTableBase table) {
        return table instanceof SQLServerTableType;
    }

    public static SQLServerTableBase getTableFromQuery(DBCSession session, SQLQuery sqlQuery, SQLServerDataSource dataSource) throws DBException, SQLException {
        DBCEntityMetaData singleSource = sqlQuery.getEntityMetadata(false);
        String catalogName = null;
        if (singleSource != null) {
            catalogName = singleSource.getCatalogName();
        }
        Connection original = null;
        if (session instanceof JDBCConnectionImpl) {
            original = ((JDBCConnectionImpl) session).getOriginal();
        }
        if (catalogName == null && original != null) {
            catalogName = original.getCatalog();
        }
        if (catalogName != null) {
            SQLServerDatabase database = dataSource.getDatabase(catalogName);
            String schemaName = null;
            if (singleSource != null) {
                schemaName = singleSource.getSchemaName();
            }
            if (schemaName == null && original != null) {
                schemaName = original.getSchema();
            }
            if (database != null && schemaName != null) {
                SQLServerSchema schema = database.getSchema(schemaName);
                if (schema != null && singleSource != null) {
                    return schema.getTable(session.getProgressMonitor(), singleSource.getEntityName());
                }
            }
        }
        return null;
    }

    public static JDBCPreparedStatement prepareTableStatisticLoadStatement(@NotNull JDBCSession session, @NotNull JDBCDataSource dataSource, @NotNull DBSCatalog catalog, long schemaId, @Nullable DBSTable table, boolean isSQLServer) throws SQLException {
        String query;
        if (isSQLServer) {
            query = "SELECT t.name, p.rows, SUM(a.total_pages) * 8 AS totalSize, SUM(a.used_pages) * 8 AS usedSize\n" +
                "FROM " + SQLServerUtils.getSystemTableFQN(dataSource, catalog, "tables", true) + " t\n" +
                "INNER JOIN " + SQLServerUtils.getSystemTableFQN(dataSource, catalog, "indexes", true) + " i ON t.OBJECT_ID = i.object_id\n" +
                "INNER JOIN " + SQLServerUtils.getSystemTableFQN(dataSource, catalog, "partitions", true) + " p ON i.object_id = p.OBJECT_ID AND i.index_id = p.index_id\n" +
                "INNER JOIN " + SQLServerUtils.getSystemTableFQN(dataSource, catalog, "allocation_units", true) + " a ON p.partition_id = a.container_id\n" +
                "LEFT OUTER JOIN " + SQLServerUtils.getSystemTableFQN(dataSource, catalog, "schemas", true) + " s ON t.schema_id = s.schema_id\n" +
                "WHERE t.schema_id = ?\n" + (table != null ? "AND t.object_id=?\n" : "") +
                "GROUP BY t.name, p.rows";
        } else {
            query = "SELECT convert(varchar(100),o.name) AS 'name',\n" +
                "row_count(db_id(), o.id) AS 'rows',\n" +
                "data_pages(db_id(), o.id, 0) AS 'pages',\n" +
                "data_pages(db_id(), o.id, 0) * (@@maxpagesize) AS 'totalSize'\n" +
                "FROM " + SQLServerUtils.getSystemTableFQN(dataSource, catalog, "sysobjects", false) +  " o\n" +
                "WHERE type = 'U'\n" +
                "AND o.uid = ?\n" +
                (table != null ? " AND 'name'=?\n" : "") +
                "ORDER BY 'name'";
        }
        JDBCPreparedStatement dbStat = session.prepareStatement(query);
        dbStat.setLong(1, schemaId);
        if (table != null) {
            if (isSQLServer) {
                SQLServerTable sqlServerTable = (SQLServerTable) table;
                dbStat.setLong(2, sqlServerTable.getObjectId());
            } else {
                dbStat.setString(2, table.getName());
            }
        }
        return dbStat;
    }

    @NotNull
    public static DBException mapException(@NotNull DBException e) {
        if (e instanceof DBSQLException dbsqlException) {
            Matcher croosDatabaseMatcher = CROSS_DATABASE_QUERY_ERROR_PATTERN.matcher(dbsqlException.getMessage());
            if (croosDatabaseMatcher.find()) {
                return new DBException(
                    "Cross-database queries are not supported in this version of SQL Server. Create a new connection to the '"
                    + croosDatabaseMatcher.group(1).split("\\.")[0]
                    + "' database.");
            }
        }

        return e;
    }
}
