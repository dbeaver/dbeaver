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
package org.jkiss.dbeaver.ext.postgresql.model.impls.redshift;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerExtensionBase;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Version;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PostgreServerRedshift
 */
public class PostgreServerRedshift extends PostgreServerExtensionBase {

    private static final Log log = Log.getLog(PostgreServerRedshift.class);

    private Version redshiftVersion;

    public PostgreServerRedshift(PostgreDataSource dataSource) {
        super(dataSource);
    }

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
        return false;
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
                RedshiftConstants.DDL_EXTRACT_VIEW + "\n" +
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
    public PostgreTableBase createRelationOfClass(PostgreSchema schema, PostgreClass.RelKind kind, JDBCResultSet dbResult) {
        if (kind == PostgreClass.RelKind.r) {
            return new RedshiftTable(schema, dbResult);
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
    public PostgreDatabase.SchemaCache createSchemaCache(PostgreDatabase database) {
        return new RedshiftSchemaCache();
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
                if (PostgreSchema.isUtilitySchema(name) && !owner.getDataSource().getContainer().isShowUtilityObjects()) {
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

}

