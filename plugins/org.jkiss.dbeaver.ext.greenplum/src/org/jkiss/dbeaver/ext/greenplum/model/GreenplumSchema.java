/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class GreenplumSchema extends PostgreSchema {
    private GreenplumTableCache greenplumTableCache = new GreenplumTableCache();

    public GreenplumSchema(PostgreDatabase owner, String name, JDBCResultSet resultSet) throws SQLException {
        super(owner, name, resultSet);
    }

    @Override
    public Collection<? extends JDBCTable> getTables(DBRProgressMonitor monitor) throws DBException {
        return greenplumTableCache.getTypedObjects(monitor, this, GreenplumTable.class)
                .stream()
                .filter(table -> !table.isPartition())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public Collection<? extends JDBCTable> getExternalTables(DBRProgressMonitor monitor) throws DBException {
        return new ArrayList<>(greenplumTableCache.getTypedObjects(monitor, this, GreenplumExternalTable.class));
    }

    public class GreenplumTableCache extends TableCache {
        protected GreenplumTableCache() {
            super();
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session,
                                                    @NotNull PostgreSchema postgreSchema,
                                                    @Nullable PostgreTableBase object,
                                                    @Nullable String objectName) throws SQLException {
            StringBuilder sqlQuery = new StringBuilder("SELECT c.oid,d.description, c.*,\n" +
                    "CASE WHEN x.urilocation IS NOT NULL THEN array_to_string(x.urilocation, ',') ELSE '' END AS urilocation,\n" +
                    "CASE WHEN x.command IS NOT NULL THEN x.command ELSE '' END AS command,\n" +
                    "x.fmttype, x.fmtopts,\n" +
                    "coalesce(x.rejectlimit, 0) AS rejectlimit,\n" +
                    "coalesce(x.rejectlimittype, '') AS rejectlimittype,\n" +
                    "array_to_string(x.execlocation, ',') AS execlocation,\n" +
                    "pg_encoding_to_char(x.encoding) AS encoding,\n" +
                    "x.writable,\n")
                    .append(postgreSchema.getDataSource().isServerVersionAtLeast(9,4) ?
                            "" :
                            "case when x.fmterrtbl is not NULL then true else false end as \"is_logging_errors\",\n")
                    .append(
                            "case when c.relstorage = 'x' then true else false end as \"is_ext_table\",\n" +
                                    "case when (ns.nspname !~ '^pg_toast' and ns.nspname like 'pg_temp%') then true else false end as \"is_temp_table\"\n" +
                                    "FROM pg_catalog.pg_class c\n" +
                                    "inner join pg_catalog.pg_namespace ns\n" +
                                    "\ton ns.oid = c.relnamespace\n" +
                                    "LEFT OUTER JOIN pg_catalog.pg_description d\n" +
                                    "\tON d.objoid=c.oid AND d.objsubid=0\n" +
                                    "left outer join pg_catalog.pg_exttable x\n" +
                                    "\ton x.reloid = c.oid\n" +
                                    "left outer join pg_catalog.pg_partitions p\n" +
                                    "\ton c.relname = p.partitiontablename and ns.nspname = p.schemaname\n" +
                                    "WHERE c.relnamespace= ? AND c.relkind not in ('i','c') AND p.partitiontablename is null ")
                    .append((object == null && objectName == null ? "" : " AND relname=?"));

            final JDBCPreparedStatement dbStat = session.prepareStatement(sqlQuery.toString());
            dbStat.setLong(1, getObjectId());
            if (object != null || objectName != null)
                dbStat.setString(2, object != null ? object.getName() : objectName);
            return dbStat;
        }
    }
}
