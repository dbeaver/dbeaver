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

package org.jkiss.dbeaver.ext.kingbase.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedureKind;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreRole;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreServerExtension;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class KingbaseSchema extends PostgreSchema {
    public long systemOid = 16384;
    private final ProceduresCache proceduresCache;
    private final FunctionsCache functionsCache;
    
    public KingbaseSchema(PostgreDatabase owner, String name, JDBCResultSet resultSet) throws SQLException {
        super(owner, name, resultSet);
        this.proceduresCache = new ProceduresCache();
        this.functionsCache = new FunctionsCache();
    }

    public KingbaseSchema(PostgreDatabase database, String name, PostgreRole owner) {
        super(database, name, owner);
        this.proceduresCache = new ProceduresCache();
        this.functionsCache = new FunctionsCache();
    }

    @Override
    public boolean isSystem() {
        return this.oid < systemOid && !this.name.toLowerCase(Locale.ENGLISH).contains("public");
    }
    
    public boolean isUtility() {
        return false;
    }

    public static boolean isUtilitySchema(String schema) {
        return false;
    }

    public ProceduresCache getKingbaseProceduresCache() {
        return this.proceduresCache;
    }

    public FunctionsCache getKingbaseFunctionsCache() {
        return this.functionsCache;
    }

 
    @Association
    public List<KingbaseProcedure> getKingbaseProcedures(DBRProgressMonitor monitor) throws DBException {
        List<KingbaseProcedure> list = getKingbaseProceduresCache().getAllObjects(monitor, this).stream()
            .filter(e -> e.getPropackageid() == 0 && e.getKind() == PostgreProcedureKind.p).collect(Collectors.toList());
        return list;
    }

    @Association
    public List<KingbaseFunction> getKingbaseFunctions(DBRProgressMonitor monitor) throws DBException {
        List<KingbaseFunction> list = getKingbaseFunctionsCache().getAllObjects(monitor, this).stream()
            .filter(e -> e.getPropackageid() == 0 && e.getKind() == PostgreProcedureKind.f).collect(Collectors.toList());
        return list;
    }

    
    public static class ProceduresCache extends JDBCObjectLookupCache<KingbaseSchema, KingbaseProcedure> {

        public ProceduresCache() {
            super();
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull KingbaseSchema owner,
            @Nullable KingbaseProcedure object, @Nullable String objectName) throws SQLException {
            PostgreServerExtension serverType = owner.getDataSource().getServerType();
            String oidColumn = serverType.getProceduresOidColumn(); // Hack for Redshift SP support
            String tableName;
            if (serverType.getProceduresSystemTable().contains("pg_")) {
                tableName = serverType.getProceduresSystemTable().replaceAll("pg_", "sys_");
            } else {
                tableName = serverType.getProceduresSystemTable();
            }
            
            JDBCPreparedStatement dbStat = session.prepareStatement("SELECT p." + oidColumn + " as poid,p.*,"
                + "sys_catalog.sys_get_expr(p.proargdefaults, 0)"
                + " as arg_defaults,d.description\n" + "FROM sys_catalog." + tableName + " p\n"
                + "LEFT OUTER JOIN sys_catalog.sys_description d ON d.objoid=p." + oidColumn
                + " AND d.objsubid = 0" + // no links to columns
                "\nWHERE p.pronamespace=?" + (object == null ? "" : " AND p." + oidColumn + "=?") + "\nORDER BY p.proname");
            dbStat.setLong(1, owner.getObjectId());
            if (object != null) {
                dbStat.setLong(2, object.getObjectId());
            }
            return dbStat;
        }

        @Override
        protected KingbaseProcedure fetchObject(@NotNull JDBCSession session, @NotNull KingbaseSchema owner,
            @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new KingbaseProcedure(session.getProgressMonitor(), owner, dbResult);
        }

    }

    public static class FunctionsCache extends JDBCObjectLookupCache<KingbaseSchema, KingbaseFunction> {

        public FunctionsCache() {
            super();
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull KingbaseSchema owner,
            @Nullable KingbaseFunction object, @Nullable String objectName) throws SQLException {
            PostgreServerExtension serverType = owner.getDataSource().getServerType();
            String oidColumn = serverType.getProceduresOidColumn(); // Hack for Redshift SP support
            String tableName;
            if (serverType.getProceduresSystemTable().contains("pg_")) {
                tableName = serverType.getProceduresSystemTable().replaceAll("pg_", "sys_");
            } else {
                tableName = serverType.getProceduresSystemTable();
            }
            JDBCPreparedStatement dbStat = session.prepareStatement("SELECT p." + oidColumn + " as poid,p.*,"
                + "sys_catalog.sys_get_expr(p.proargdefaults, 0)"
                + " as arg_defaults,d.description\n" + "FROM sys_catalog." + tableName + " p\n"
                + "LEFT OUTER JOIN sys_catalog.sys_description d ON d.objoid=p." + oidColumn
                + " AND d.objsubid = 0" + // no links to columns
                "\nWHERE p.pronamespace=?" + (object == null ? "" : " AND p." + oidColumn + "=?") + "\nORDER BY p.proname");
            dbStat.setLong(1, owner.getObjectId());
            if (object != null) {
                dbStat.setLong(2, object.getObjectId());
            }
            return dbStat;
        }

        @Override
        protected KingbaseFunction fetchObject(@NotNull JDBCSession session, @NotNull KingbaseSchema owner,
            @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new KingbaseFunction(session.getProgressMonitor(), owner, dbResult);
        }

    }
}
