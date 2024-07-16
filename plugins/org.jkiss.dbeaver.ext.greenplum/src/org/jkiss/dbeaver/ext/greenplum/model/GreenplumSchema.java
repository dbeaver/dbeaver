/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 * Copyright (C) 2019 Dmitriy Dubson (ddubson@pivotal.io)
 * Copyright (C) 2019 Gavin Shaw (gshaw@pivotal.io)
 * Copyright (C) 2019 Zach Marcin (zmarcin@pivotal.io)
 * Copyright (C) 2019 Nikhil Pawar (npawar@pivotal.io)
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
package org.jkiss.dbeaver.ext.greenplum.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class GreenplumSchema extends PostgreSchema {

    public GreenplumSchema(PostgreDatabase owner, String name, JDBCResultSet resultSet) throws SQLException {
        super(owner, name, resultSet);
    }

    @NotNull
    @Override
    protected ProceduresCache createProceduresCache() {
        return new GreenplumFunctionsCache();
    }

    @NotNull
    @Override
    protected TableCache createTableCache() {
        return new GreenplumTableCache();
    }

    @NotNull
    @Override
    public GreenplumDataSource getDataSource() {
        return (GreenplumDataSource) super.getDataSource();
    }

    @Association
    public Collection<? extends JDBCTable> getExternalTables(DBRProgressMonitor monitor) throws DBException {
        return new ArrayList<>(getTableCache().getTypedObjects(monitor, this, GreenplumExternalTable.class));
    }

    @Override
    public List<? extends PostgreTable> getTables(DBRProgressMonitor monitor) throws DBException {
        List<? extends PostgreTable> postgreTables = super.getTables(monitor);
        // Remove external tables from the list. Store them in a different folder.
        return postgreTables.stream()
            .filter(e -> !(e instanceof GreenplumExternalTable))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<? extends PostgreTable> getForeignTables(DBRProgressMonitor monitor) throws DBException {
        List<? extends PostgreTable> foreignTables = super.getForeignTables(monitor);
        GreenplumDataSource dataSource = getDataSource();
        if (dataSource.isServerVersionAtLeast(7, 0) && dataSource.isHasAccessToExttable()) {
            // Starting Greenplum version 7 external tables are marked as foreign tables.
            // Let's remove external tables from the list foreign tables. Store is the External tables folder.
            return foreignTables.stream()
                .filter(e -> !(e instanceof GreenplumExternalTable))
                .collect(Collectors.toCollection(ArrayList::new));
        }
        return foreignTables;
    }

    public class GreenplumTableCache extends TableCache {

        private boolean before7version;

        GreenplumTableCache() {
            super();
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session,
                                                    @NotNull PostgreTableContainer container,
                                                    @Nullable PostgreTableBase object,
                                                    @Nullable String objectName) throws SQLException {
            GreenplumDataSource dataSource = getDataSource();
            boolean greenplumVersionAtLeast5 = dataSource.isGreenplumVersionAtLeast(5, 0);
            String uriLocationColumn = greenplumVersionAtLeast5 ? "urilocation" : "location";
            String execLocationColumn = greenplumVersionAtLeast5 ? "execlocation" : "location";
            boolean hasAccessToExttable = dataSource.isHasAccessToExttable();
            boolean supportsRelStorageColumn = dataSource.isServerSupportsRelstorageColumn(session);
            before7version = !dataSource.isGreenplumVersionAtLeast(7, 0);
            String sqlQuery = "SELECT c.oid,c.*,d.description,\n" +
                (before7version ? "p.partitiontablename,p.partitionboundary as partition_expr," :
                    "pg_catalog.pg_get_expr(c.relpartbound, c.oid) as partition_expr, pg_catalog.pg_get_partkeydef(c.oid) as partition_key,") +
                (hasAccessToExttable ? "CASE WHEN x." + uriLocationColumn + " IS NOT NULL THEN array_to_string(x." + uriLocationColumn +
                    ", ',') ELSE '' END AS urilocation,\n" +
                "CASE WHEN x.command IS NOT NULL THEN x.command ELSE '' END AS command,\n" +
                "x.fmttype, x.fmtopts,\n" +
                "coalesce(x.rejectlimit, 0) AS rejectlimit,\n" +
                "coalesce(x.rejectlimittype, '') AS rejectlimittype,\n" +
                "array_to_string(x." + execLocationColumn + ", ',') AS execlocation,\n" +
                "pg_encoding_to_char(x.encoding) AS encoding,\n" +
                "x.writable,\n" : "") +
                (dataSource.isServerSupportFmterrtblColumn(session) ?
                    "case when x.fmterrtbl is not NULL then true else false end as \"is_logging_errors\",\n" : "") +
                (supportsRelStorageColumn ? // We want to know about table external status
                    "case when c.relstorage = 'x' then true else false end as \"is_ext_table\"" :
                    hasAccessToExttable ? "\ncase when x.fmttype is not null then true else false end as \"is_ext_table\"" :
                        "false as \"is_ext_table\"") +
                "\n,case when (ns.nspname !~ '^pg_toast' and ns.nspname like 'pg_temp%') then true else false end as \"is_temp_table\"\n" +
                (before7version ? "" : ", pa.amname\n") +
                "\nFROM pg_catalog.pg_class c\n" +
                "INNER JOIN pg_catalog.pg_namespace ns\n\ton ns.oid = c.relnamespace\n" +
                "LEFT OUTER JOIN pg_catalog.pg_description d\n\tON d.objoid=c.oid AND d.objsubid=0\n" +
                (hasAccessToExttable ? "LEFT OUTER JOIN pg_catalog.pg_exttable x\n\ton x.reloid = c.oid\n" : "") +
                (before7version ? "LEFT OUTER JOIN pg_catalog.pg_partitions p\n\ton c.relname = p.partitiontablename " +
                    "and ns.nspname = p.schemaname\n" : "") +
                (before7version ? "" : "\nLEFT JOIN pg_catalog.pg_am pa ON pa.oid = c.relam") +
                "\nWHERE c.relnamespace= ? AND c.relkind not in ('i','c') " +
                (object == null && objectName == null ? "" : " AND relname=?");
            final JDBCPreparedStatement dbStat = session.prepareStatement(sqlQuery);
            dbStat.setLong(1, getObjectId());
            if (object != null || objectName != null)
                dbStat.setString(2, object != null ? object.getName() : objectName);
            return dbStat;
        }

        @Override
        protected boolean isPartitionTableRow(@NotNull JDBCResultSet dbResult) {
            return before7version ? CommonUtils.isNotEmpty(JDBCUtils.safeGetString(dbResult, "partitiontablename")) :
               super.isPartitionTableRow(dbResult);
        }
    }

    public class GreenplumFunctionsCache extends ProceduresCache {
        GreenplumFunctionsCache() {
            super();
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session,
                                                    @NotNull PostgreSchema owner,
                                                    @Nullable PostgreProcedure object,
                                                    @Nullable String objectName) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT p.oid as poid,p.*," +
                            (session.getDataSource().isServerVersionAtLeast(8, 4) ? "pg_catalog.pg_get_expr(p.proargdefaults, 0)" : "NULL") + " as arg_defaults,d.description\n" +
                            "FROM pg_catalog.pg_proc p\n" +
                            "LEFT OUTER JOIN pg_catalog.pg_description d ON d.objoid=p.oid\n" +
                            "WHERE p.pronamespace=?" +
                            (object == null ? "" : " AND p.oid=?") +
                            "\nORDER BY p.proname"
            );
            dbStat.setLong(1, owner.getObjectId());
            if (object != null) {
                dbStat.setLong(2, object.getObjectId());
            }
            return dbStat;
        }

        @Override
        protected GreenplumFunction fetchObject(@NotNull JDBCSession session,
                                                @NotNull PostgreSchema owner,
                                                @NotNull JDBCResultSet dbResult) {
            return new GreenplumFunction(session.getProgressMonitor(), owner, dbResult);
        }
    }
}
