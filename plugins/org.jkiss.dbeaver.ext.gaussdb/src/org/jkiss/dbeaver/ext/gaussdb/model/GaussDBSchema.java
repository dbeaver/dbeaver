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

package org.jkiss.dbeaver.ext.gaussdb.model;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedureKind;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreRole;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreServerExtension;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class GaussDBSchema extends PostgreSchema {

    public final PackageCache packageCache;
    private final ProceduresCache proceduresCache;
    private final FunctionsCache functionsCache;

    public GaussDBSchema(PostgreDatabase owner, String name, JDBCResultSet resultSet) throws SQLException {
        super(owner, name, resultSet);
        this.packageCache = new PackageCache();
        this.proceduresCache = new ProceduresCache();
        this.functionsCache = new FunctionsCache();
    }

    public GaussDBSchema(PostgreDatabase database, String name, PostgreRole owner) {
        super(database, name, owner);
        this.packageCache = new PackageCache();
        this.proceduresCache = new ProceduresCache();
        this.functionsCache = new FunctionsCache();
    }

    @Override
    public boolean isSystem() {
        return this.oid < 16384 && !this.name.toLowerCase(Locale.ENGLISH).contains("public");
    }
    
    public boolean isUtility() {
        return false;
    }

    public static boolean isUtilitySchema(String schema) {
        return false;
    }

    public ProceduresCache getGaussDBProceduresCache() {
        return this.proceduresCache;
    }

    public FunctionsCache getGaussDBFunctionsCache() {
        return this.functionsCache;
    }

    @Association
    public List<GaussDBPackage> getPackages(DBRProgressMonitor monitor) throws DBException {
        return packageCache.getAllObjects(monitor, this);
    }

    @Association
    public List<GaussDBProcedure> getGaussDBProcedures(DBRProgressMonitor monitor) throws DBException {
        List<GaussDBProcedure> list = getGaussDBProceduresCache().getAllObjects(monitor, this).stream()
            .filter(e -> e.getPropackageid() == 0 && e.getKind() == PostgreProcedureKind.p).collect(Collectors.toList());
        return list;
    }

    @Association
    public List<GaussDBFunction> getGaussDBFunctions(DBRProgressMonitor monitor) throws DBException {
        List<GaussDBFunction> list = getGaussDBFunctionsCache().getAllObjects(monitor, this).stream()
            .filter(e -> e.getPropackageid() == 0 && e.getKind() == PostgreProcedureKind.f).collect(Collectors.toList());
        return list;
    }

    class PackageCache extends JDBCObjectCache<GaussDBSchema, GaussDBPackage> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session,
            @NotNull GaussDBSchema owner) throws SQLException {
            final JDBCPreparedStatement dbStat = session
                .prepareStatement("select g.oid, g.pkgnamespace, g.pkgname as name from gs_package g where g.pkgnamespace = ?");
            dbStat.setLong(1, GaussDBSchema.this.getObjectId());
            return dbStat;
        }

        @Override
        protected GaussDBPackage fetchObject(@NotNull JDBCSession session, @NotNull GaussDBSchema owner,
            @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new GaussDBPackage(session, owner, dbResult);
        }
    }

    public static class ProceduresCache extends JDBCObjectLookupCache<GaussDBSchema, GaussDBProcedure> {

        public ProceduresCache() {
            super();
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull GaussDBSchema owner,
            @Nullable GaussDBProcedure object, @Nullable String objectName) throws SQLException {
            PostgreServerExtension serverType = owner.getDataSource().getServerType();
            String oidColumn = serverType.getProceduresOidColumn(); // Hack for Redshift SP support
            JDBCPreparedStatement dbStat = session.prepareStatement("SELECT p." + oidColumn + " as poid,p.*,"
                + (session.getDataSource().isServerVersionAtLeast(8, 4) ? "pg_catalog.pg_get_expr(p.proargdefaults, 0)" : "NULL")
                + " as arg_defaults,d.description\n" + "FROM pg_catalog." + serverType.getProceduresSystemTable() + " p\n"
                + "LEFT OUTER JOIN pg_catalog.pg_description d ON d.objoid=p." + oidColumn
                + (session.getDataSource().isServerVersionAtLeast(7, 2) ? " AND d.objsubid = 0" : "") + // no links to columns
                "\nWHERE p.pronamespace=?" + (object == null ? "" : " AND p." + oidColumn + "=?") + "\nORDER BY p.proname");
            dbStat.setLong(1, owner.getObjectId());
            if (object != null) {
                dbStat.setLong(2, object.getObjectId());
            }
            return dbStat;
        }

        @Override
        protected GaussDBProcedure fetchObject(@NotNull JDBCSession session, @NotNull GaussDBSchema owner,
            @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new GaussDBProcedure(session.getProgressMonitor(), owner, dbResult);
        }

    }

    public static class FunctionsCache extends JDBCObjectLookupCache<GaussDBSchema, GaussDBFunction> {

        public FunctionsCache() {
            super();
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull GaussDBSchema owner,
            @Nullable GaussDBFunction object, @Nullable String objectName) throws SQLException {
            PostgreServerExtension serverType = owner.getDataSource().getServerType();
            String oidColumn = serverType.getProceduresOidColumn(); // Hack for Redshift SP support
            JDBCPreparedStatement dbStat = session.prepareStatement("SELECT p." + oidColumn + " as poid,p.*,"
                + (session.getDataSource().isServerVersionAtLeast(8, 4) ? "pg_catalog.pg_get_expr(p.proargdefaults, 0)" : "NULL")
                + " as arg_defaults,d.description\n" + "FROM pg_catalog." + serverType.getProceduresSystemTable() + " p\n"
                + "LEFT OUTER JOIN pg_catalog.pg_description d ON d.objoid=p." + oidColumn
                + (session.getDataSource().isServerVersionAtLeast(7, 2) ? " AND d.objsubid = 0" : "") + // no links to columns
                "\nWHERE p.pronamespace=?" + (object == null ? "" : " AND p." + oidColumn + "=?") + "\nORDER BY p.proname");
            dbStat.setLong(1, owner.getObjectId());
            if (object != null) {
                dbStat.setLong(2, object.getObjectId());
            }
            return dbStat;
        }

        @Override
        protected GaussDBFunction fetchObject(@NotNull JDBCSession session, @NotNull GaussDBSchema owner,
            @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new GaussDBFunction(session.getProgressMonitor(), owner, dbResult);
        }

    }
}
