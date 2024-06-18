package org.jkiss.dbeaver.ext.gaussdb.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreRole;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreServerExtension;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

public class GaussDBSchema extends PostgreSchema {

    public final PackageCache packageCache = new PackageCache();;
    private final ProceduresCache proceduresCache = new ProceduresCache();;
    private final FunctionCache functionCache = new FunctionCache();;

    public GaussDBSchema(PostgreDatabase database, String name, PostgreRole owner) {
        super(database, name, owner);
    }

    public GaussDBSchema(PostgreDatabase database, String name, ResultSet dbResult) throws SQLException {
        super(database, name, dbResult);
    }

    public DBSObjectCache<? extends DBSObject, GaussDBFunction> getGaussDBFunctionCache() {
        return null;
    }

    public static class PackageCache extends JDBCObjectCache<GaussDBSchema, GaussDBPackage> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session,
                                                        @NotNull GaussDBSchema owner) throws SQLException {
            JDBCPreparedStatement dbStat = session
                        .prepareStatement("SELECT g.oid, g.pkgnamespace, g.pkgname as name from gs_package g where g.pkgnamespace = ?");
            dbStat.setLong(1, owner.getObjectId());
            return dbStat;
        }

        @Override
        protected GaussDBPackage fetchObject(@NotNull JDBCSession session,
                                             @NotNull GaussDBSchema owner,
                                             @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new GaussDBPackage(session, owner, dbResult);
        }

    }

    /**
     * Procedures cache implementation
     */
    public static class ProceduresCache extends JDBCObjectLookupCache<GaussDBSchema, GaussDBProcedure> {

        public ProceduresCache() {
            super();
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session,
                                                    @NotNull GaussDBSchema owner,
                                                    @Nullable GaussDBProcedure object,
                                                    @Nullable String objectName) throws SQLException {
            PostgreServerExtension serverType = owner.getDataSource().getServerType();
            String oidColumn = serverType.getProceduresOidColumn(); // Hack for Redshift SP support
            boolean versionAtLeast7 = session.getDataSource().isServerVersionAtLeast(7, 2);
            JDBCPreparedStatement dbStat = session.prepareStatement("SELECT p." + oidColumn + " as poid,p.*,"
                        + (session.getDataSource().isServerVersionAtLeast(8, 4) ? "pg_catalog.pg_get_expr(p.proargdefaults, 0)"
                                    : "NULL")
                        + " as arg_defaults,d.description\n" + "FROM pg_catalog." + serverType.getProceduresSystemTable() + " p\n"
                        + "LEFT OUTER JOIN pg_catalog.pg_description d ON d.objoid=p." + oidColumn
                        + (versionAtLeast7 ? " and d.classoid='pg_proc'::regclass " : "") + // to avoid objects duplication
                        (versionAtLeast7 ? " AND d.objsubid = 0" : "") + // no links to columns
                        "\nWHERE p.pronamespace=?" + (object == null ? "" : " AND p." + oidColumn + "=?") + "\nORDER BY p.proname");
            dbStat.setLong(1, owner.getObjectId());
            if (object != null) {
                dbStat.setLong(2, object.getObjectId());
            }
            return dbStat;
        }

        @Override
        protected GaussDBProcedure fetchObject(@NotNull JDBCSession session,
                                               @NotNull GaussDBSchema owner,
                                               @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new GaussDBProcedure(session.getProgressMonitor(), owner, dbResult);
        }

    }

    /**
     * Procedures cache implementation
     */
    public static class FunctionCache extends JDBCObjectLookupCache<GaussDBSchema, GaussDBFunction> {

        public FunctionCache() {
            super();
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session,
                                                    @NotNull GaussDBSchema owner,
                                                    @Nullable GaussDBFunction object,
                                                    @Nullable String objectName) throws SQLException {
            PostgreServerExtension serverType = owner.getDataSource().getServerType();
            String oidColumn = serverType.getProceduresOidColumn(); // Hack for Redshift SP support
            boolean versionAtLeast7 = session.getDataSource().isServerVersionAtLeast(7, 2);
            JDBCPreparedStatement dbStat = session.prepareStatement("SELECT p." + oidColumn + " as poid,p.*,"
                        + (session.getDataSource().isServerVersionAtLeast(8, 4) ? "pg_catalog.pg_get_expr(p.proargdefaults, 0)"
                                    : "NULL")
                        + " as arg_defaults,d.description\n" + "FROM pg_catalog." + serverType.getProceduresSystemTable() + " p\n"
                        + "LEFT OUTER JOIN pg_catalog.pg_description d ON d.objoid=p." + oidColumn
                        + (versionAtLeast7 ? " and d.classoid='pg_proc'::regclass " : "") + // to avoid objects duplication
                        (versionAtLeast7 ? " AND d.objsubid = 0" : "") + // no links to columns
                        "\nWHERE p.pronamespace=?" + (object == null ? "" : " AND p." + oidColumn + "=?") + "\nORDER BY p.proname");
            dbStat.setLong(1, owner.getObjectId());
            if (object != null) {
                dbStat.setLong(2, object.getObjectId());
            }
            return dbStat;
        }

        @Override
        protected GaussDBFunction fetchObject(@NotNull JDBCSession session,
                                              @NotNull GaussDBSchema owner,
                                              @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new GaussDBFunction(session.getProgressMonitor(), owner, dbResult);
        }

    }

}