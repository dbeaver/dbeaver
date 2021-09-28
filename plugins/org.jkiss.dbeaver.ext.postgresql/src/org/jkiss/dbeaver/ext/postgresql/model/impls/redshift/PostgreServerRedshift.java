/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model.impls.redshift;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerExtensionBase;
import org.jkiss.dbeaver.model.DBPErrorAssistant;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLState;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Version;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PostgreServerRedshift
 */
public class PostgreServerRedshift extends PostgreServerExtensionBase implements DBPErrorAssistant {

    private static final Log log = Log.getLog(PostgreServerRedshift.class);
    public static final int RS_ERROR_CODE_CHANNEL_CLOSE = 500366;
    public static final int RS_ERROR_CODE_NOT_CONNECTED = 500150;

    private Version redshiftVersion;

    public PostgreServerRedshift(PostgreDataSource dataSource) {
        super(dataSource);
    }
    
    private static final String[] REDSHIFT_OTHER_TYPES_FUNCTION = {
        "SYSDATE"
    };
    
    public static String[] REDSHIFT_EXTRA_KEYWORDS = new String[]{
        "AUTO",
        "BACKUP",
        "AZ64",
        "CASE_SENSITIVE",
        "CASE_INSENSITIVE",
        "COMPOUND",
        "INTERLEAVED",
        "COPY",
        "DATASHARE",
        "DISTSTYLE",
        "DISTKEY",
        "EVEN",
        "MODEL",
        "OWNER",
        "SORTKEY",
        "TEMP",
        "UNLOAD",
        "VACUUM",
        "YES"
    };
   
    public static String[] REDSHIFT_FUNCTIONS_CONDITIONAL = new String[]{
        "NVL",
        "NVL2"
    };

    private boolean isRedshiftVersionAtLeast(int major, int minor, int micro) {
        if (redshiftVersion == null) {
            String serverVersion = dataSource.getServerVersion();
            if (!CommonUtils.isEmpty(serverVersion)) {
                try {
                    Matcher matcher = Pattern.compile("Redshift ([0-9\\.]+)").matcher(serverVersion);
                    if (matcher.find()) {
                        String versionStr = matcher.group(1);
                        if (!CommonUtils.isEmpty(versionStr)) {
                            redshiftVersion = new Version(versionStr);
                        }
                    }
                } catch (Exception e) {
                    log.debug("Error getting Redshift version", e);
                    redshiftVersion = new Version(1, 0,0);
                }
            }
        }
        if (redshiftVersion != null) {
            if (redshiftVersion.getMajor() > major) {
                return true;
            } else if (redshiftVersion.getMajor() == major) {
                if (redshiftVersion.getMinor() > minor) {
                    return true;
                } else if (redshiftVersion.getMinor() == minor) {
                    return redshiftVersion.getMicro() >= micro;
                }
            }
        }
        return false;
    }

    @Override
    public String getServerTypeName() {
        return "Redshift";
    }

    @Override
    public boolean supportsOids() {
        return false;
    }

    @Override
    public boolean supportsIndexes() {
        return false;
    }

    @Override
    public boolean supportsMaterializedViews() {
        return false;
    }

    @Override
    public boolean supportsPartitions() {
        return false;
    }

    @Override
    public boolean supportsInheritance() {
        return false;
    }

    @Override
    public boolean supportsTriggers() {
        return false;
    }

    @Override
    public boolean supportsRules() {
        return false;
    }

    @Override
    public boolean supportsExtensions() {
        return false;
    }

    @Override
    public boolean supportsEncodings() {
        return false;
    }

    @Override
    public boolean supportsTablespaces() {
        return false;
    }

    @Override
    public boolean supportsSequences() {
        return false;
    }

    @Override
    public boolean supportsRoles() {
        // Redshift has support for roles only as a part of the EE extension.
        // That's a silly workaround (see #11912, #12691)
        return dataSource.getClass() != PostgreDataSource.class;
    }

    @Override
    public boolean supportsLocks() {
        return false;
    }

    @Override
    public boolean supportsForeignServers() {
        return false;
    }

    @Override
    public boolean supportsResultSetLimits() {
        return true;
    }

    @Override
    public boolean supportsClientInfo() {
        return false;
    }

    @Override
    public boolean supportsRelationSizeCalc() {
        return false;
    }

    @Override
    public String readTableDDL(DBRProgressMonitor monitor, PostgreTableBase table) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, table, "Load Redshift table DDL")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                RedshiftQueries.DDL_EXTRACT_VIEW + "\n" +
                    "WHERE schemaname=? AND tablename=?")) {
                dbStat.setString(1, table.getSchema().getName());
                dbStat.setString(2, table.getName());
                try (JDBCResultSet resultSet = dbStat.executeQuery()) {
                    StringBuilder sql = new StringBuilder();
                    while (resultSet.next()) {
                        String line = resultSet.getString("ddl");
                        if (line == null) {
                            continue;
                        }
                        sql.append(line).append("\n");
                    }
                    String ddl = sql.toString().trim();
                    if (ddl.endsWith(";")) {
                        ddl = ddl.substring(0, ddl.length() - 1).trim();
                    }
                    return ddl;
                }
            }
        } catch (Exception e) {
            throw new DBException(e, table.getDataSource());
        }
    }

    @Override
    public PostgreTableBase createNewRelation(DBRProgressMonitor monitor, PostgreSchema schema, PostgreClass.RelKind kind, Object copyFrom) throws DBException {
        if (kind == PostgreClass.RelKind.r) {
            return new RedshiftTable(schema);
        } else if (kind == PostgreClass.RelKind.v) {
            return new RedshiftView(schema);
        }
        return super.createNewRelation(monitor, schema, kind, copyFrom);
    }


    public PostgreTableBase createRelationOfClass(PostgreSchema schema, PostgreClass.RelKind kind, JDBCResultSet dbResult) {
        if (kind == PostgreClass.RelKind.r) {
            return new RedshiftTable(schema, dbResult);
        } else if (kind == PostgreClass.RelKind.v) {
            return new RedshiftView(schema, dbResult);
        }
        return super.createRelationOfClass(schema, kind, dbResult);
    }

    @Override
    public PostgreTableColumn createTableColumn(DBRProgressMonitor monitor, PostgreSchema schema, PostgreTableBase table, JDBCResultSet dbResult) throws DBException {
        if (table instanceof RedshiftTable) {
            return new RedshiftTableColumn(monitor, (RedshiftTable)table, dbResult);
        }
        return super.createTableColumn(monitor, schema, table, dbResult);
    }

    @Override
    public boolean supportsStoredProcedures() {
        return isRedshiftVersionAtLeast(1, 0, 7562);
    }

    @Override
    public String getProceduresSystemTable() {
        return supportsStoredProcedures() ? "pg_proc_info" : super.getProceduresSystemTable();
    }

    @Override
    public String getProceduresOidColumn() {
        return supportsStoredProcedures() ? "prooid" : super.getProceduresOidColumn();
    }

    @Override
    public boolean isAlterTableAtomic() {
        return true;
    }

    @Override
    public boolean supportSerialTypes() {
        return false;
    }

    @Override
    public boolean supportsEntityMetadataInResults() {
        return true;
    }

    @Override
    public PostgreDatabase.SchemaCache createSchemaCache(PostgreDatabase database) {
        return new RedshiftSchemaCache();
    }

    @Override
    public ErrorType discoverErrorType(@NotNull Throwable error) {
        int errorCode = SQLState.getCodeFromException(error);
        if (errorCode == RS_ERROR_CODE_CHANNEL_CLOSE || errorCode == RS_ERROR_CODE_NOT_CONNECTED) {
            return ErrorType.CONNECTION_LOST;
        }
        return null;
    }

    @Nullable
    @Override
    public ErrorPosition[] getErrorPosition(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext context, @NotNull String query, @NotNull Throwable error) {
        return null;
    }

    private class RedshiftSchemaCache extends PostgreDatabase.SchemaCache {
        private final Map<String, String> esSchemaMap = new HashMap<>();

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase database, PostgreSchema object, String objectName) throws SQLException {
            // 1. Read all external schemas info
            esSchemaMap.clear();
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM " + DBUtils.getQuotedIdentifier(database) + ".pg_catalog.svv_external_schemas")) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String esSchemaName = dbResult.getString("schemaname");
                        String esSchemaOptions = dbResult.getString("esoptions");
                        esSchemaMap.put(esSchemaName, esSchemaOptions);
                    }
                }
            } catch (Throwable e) {
                log.debug("Error reading Redshift external schemas", e);
            }

            // 2. Rad standard schemas
            return super.prepareLookupStatement(session, database, object, objectName);
        }

        @Override
        protected PostgreSchema fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            String name = JDBCUtils.safeGetString(resultSet, "nspname");
            String esOptions = esSchemaMap.get(name);
            if (esOptions != null) {
                // External schema
                return new RedshiftExternalSchema(owner, name, esOptions, resultSet);
            } else {
                if (CommonUtils.isEmpty(name) ||
                    (PostgreSchema.isUtilitySchema(name) && !owner.getDataSource().getContainer().getNavigatorSettings().isShowUtilityObjects())) {
                    return null;
                }
                return new RedshiftSchema(owner, name, resultSet);
            }
        }

        @Override
        public void clearCache() {
            super.clearCache();
            esSchemaMap.clear();
        }
    }
    
    @Override
    public void configureDialect(PostgreDialect dialect) {
        dialect.addExtraKeywords(REDSHIFT_EXTRA_KEYWORDS);
        dialect.addKeywords(Arrays.asList(REDSHIFT_OTHER_TYPES_FUNCTION), DBPKeywordType.OTHER);
        dialect.addExtraFunctions(REDSHIFT_FUNCTIONS_CONDITIONAL);
    }

    @Override
    public boolean supportsBackslashStringEscape() {
        return true;
    }

    @Override
    public int getParameterBindType(DBSTypedObject type, Object value) {
        if (value instanceof String) {
            return Types.VARCHAR;
        }
        return super.getParameterBindType(type, value);
    }

    @Override
    public boolean supportsDatabaseSize() {
        return true;
    }

    @Override
    public boolean supportsFunctionDefRead() {
        return false;
    }

    @Override
    public boolean supportsExternalTypes() {
        return true;
    }

    @Override
    public int getTruncateToolModes() {
        return 0;
    }
}
