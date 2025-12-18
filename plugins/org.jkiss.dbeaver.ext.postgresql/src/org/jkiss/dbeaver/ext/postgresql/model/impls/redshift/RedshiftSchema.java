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
package org.jkiss.dbeaver.ext.postgresql.model.impls.redshift;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * RedshiftExternalSchema
 */
public class RedshiftSchema extends PostgreSchema {
    private static final Log log = Log.getLog(RedshiftSchema.class);

    public RedshiftSchema(PostgreDatabase database, String name, ResultSet dbResult) throws SQLException {
        super(database, name, dbResult);
    }

    @NotNull
    @Override
    protected TableCache createTableCache() {
        return new RedshiftTableCache();
    }

    public RedshiftSchema(PostgreDatabase database, String name, PostgreRole owner) {
        super(database, name, owner);
    }

    @Override
    public boolean isSystem() {
        return super.isSystem() || "catalog_history".equals(getName());
    }

    @Override
    public String getTableColumnsQueryExtraParameters(PostgreTableContainer owner, PostgreTableBase forTable) {
        return ",format_encoding(a.attencodingtype::integer) AS \"encoding\"";
    }

    public class RedshiftTableCache extends TableCache {
        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session,
                                                    @NotNull PostgreTableContainer container,
                                                    @Nullable PostgreTableBase object,
                                                    @Nullable String objectName) throws SQLException {
            boolean hasNameFilter = (object != null || objectName != null);

            StringBuilder sql = new StringBuilder();
            sql.append("""
                SELECT c.oid, c.relname::varchar AS relname, c.relnamespace, c.relowner, c.relkind, c.relpages, c.reltuples, d.description,
                       mv.name is not null as is_mv
                FROM pg_catalog.pg_class c
                JOIN SVV_TABLE_INFO t ON t.table_id = c.oid
                LEFT JOIN SVV_MV_INFO mv ON mv.schema_name = t."schema" AND mv.name = t."table"
                LEFT OUTER JOIN pg_catalog.pg_description d ON d.objoid = c.oid AND d.objsubid = 0 AND d.classoid = 'pg_class'::regclass
                WHERE t."schema" = ?
                """);
            if (hasNameFilter) {
                sql.append(" AND t.\"table\"::varchar = ?");
            }
            sql.append("""
                UNION ALL
                SELECT c.oid, c.relname::varchar AS relname, c.relnamespace, c.relowner, c.relkind, c.relpages, c.reltuples, d.description,
                       false AS is_mv
                FROM pg_catalog.pg_class c
                JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
                LEFT JOIN SVV_MV_INFO mv2 ON mv2.schema_name = n.nspname AND mv2.name = c.relname
                LEFT OUTER JOIN pg_catalog.pg_description d ON d.objoid = c.oid AND d.objsubid = 0 AND d.classoid = 'pg_class'::regclass
                WHERE n.nspname = ? AND c.relkind not in ('i','I','c','m','r')
                  AND mv2.name IS NULL
                """);
            if (hasNameFilter) {
                sql.append(" AND c.relname::varchar = ?");
            }

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            int p = 1;
            dbStat.setString(p++, container.getSchema().getName());
            if (hasNameFilter) {
                dbStat.setString(p++, object != null ? object.getName() : objectName);
            }
            dbStat.setString(p++, container.getSchema().getName());
            if (hasNameFilter) {
                dbStat.setString(p, object != null ? object.getName() : objectName);
            }
            return dbStat;
        }

        @Override
        protected PostgreTableBase fetchObject(
            @NotNull JDBCSession session,
            @NotNull PostgreTableContainer container,
            @NotNull JDBCResultSet dbResult
        ) throws SQLException, DBException {
            boolean isMv = JDBCUtils.safeGetBoolean(dbResult, "is_mv");
            if (isMv) {
                return container.getDataSource().getServerType().createRelationOfClass(RedshiftSchema.this, PostgreClass.RelKind.m, dbResult);
            }
            return super.fetchObject(session, container, dbResult);
        }

    }

    @Override
    public void collectObjectStatistics(@NotNull DBRProgressMonitor monitor, boolean totalSizeOnly, boolean forceRefresh) throws DBException {
        if (hasStatistics && !forceRefresh) {
            return;
        }
        try (DBCSession session = DBUtils.openMetaSession(monitor, this, "Read relation statistics")) {
            try (JDBCPreparedStatement dbStat = ((JDBCSession)session).prepareStatement(
                "SELECT \"table\",size,tbl_rows FROM SVV_TABLE_INFO WHERE \"schema\"=?"))
            {
                dbStat.setString(1, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String tableName = dbResult.getString(1);
                        PostgreTableBase table = getTable(monitor, tableName);
                        if (table instanceof RedshiftTable rsTable) {
                            rsTable.fetchStatistics(dbResult);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new DBCException("Error reading schema relation statistics", e);
            }
        } finally {
            hasStatistics = true;
        }
    }

}

