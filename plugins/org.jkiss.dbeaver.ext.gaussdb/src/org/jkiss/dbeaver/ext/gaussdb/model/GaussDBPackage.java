package org.jkiss.dbeaver.ext.gaussdb.model;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreObject;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedureKind;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreScriptObject;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreServerExtension;
import org.jkiss.dbeaver.model.DBPSystemInfoObject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

public class GaussDBPackage implements PostgreObject, PostgreScriptObject, DBPSystemInfoObject {

    private static final Log log = Log.getLog(GaussDBPackage.class);

    private final ProceduresCache proceduresCache;

    private GaussDBSchema schema;
    protected long ownnerId;
    private long oid;
    private String name;
    private String description;
    private String sourceDeclaration = "";
    private String sourceDefinition = "";

    public GaussDBPackage(GaussDBSchema schema, DBRProgressMonitor monitor, String name) {
        this.schema = schema;
        this.name = name;
        this.proceduresCache = new ProceduresCache();
    }

    public GaussDBPackage(JDBCSession session, GaussDBSchema schema, JDBCResultSet dbResult) {
        this.schema = schema;
        this.oid = JDBCUtils.safeGetLong(dbResult, "oid");
        this.name = JDBCUtils.safeGetString(dbResult, "name");
        initialize(session, oid);
        this.proceduresCache = new ProceduresCache();

    }

    private void initialize(JDBCSession session, long oid) {
        JDBCPreparedStatement prepareStatement;
        try {
            prepareStatement = session
                        .prepareStatement("select pkg.src from DBE_PLDEVELOP.gs_source pkg where pkg.id = ? and type = ?");
            prepareStatement.setLong(1, oid);
            prepareStatement.setString(2, "package");
            JDBCResultSet executeQuery = prepareStatement.executeQuery();
            if (executeQuery.nextRow()) {
                this.sourceDeclaration = JDBCUtils.safeGetString(executeQuery, "src");
            }
            prepareStatement.setString(2, "body");
            executeQuery = prepareStatement.executeQuery();
            if (executeQuery.nextRow()) {
                this.sourceDefinition = JDBCUtils.safeGetString(executeQuery, "src");
            }
        } catch (SQLException | DBCException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public DBSObject getParentObject() {
        return null;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Property(viewable = true, order = 1)
    public String getPkgName() {
        return this.name;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    @Property(viewable = true, order = 2)
    public long getObjectId() {
        return this.oid;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (CommonUtils.isEmpty(sourceDefinition)) {
            return this.sourceDeclaration;
        }
        return sourceDeclaration.trim() + "\n" + sourceDefinition;
    }

    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText() {
        return sourceDeclaration;
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException {
        this.sourceDeclaration = sourceText;
    }

    @Override
    public PostgreDataSource getDataSource() {
        return (GaussDBDataSource) this.schema.getDataSource();
    }

    @Override
    public PostgreDatabase getDatabase() {
        return (GaussDBDatabase) this.schema.getDatabase();
    }

    public GaussDBSchema getSchema() {
        return this.schema;
    }

    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getExtendedDefinitionText() {
        return sourceDefinition;
    }

    public void setExtendedDefinitionText(String sourceText) {
        this.sourceDefinition = sourceText;
    }

    public List<GaussDBProcedure> getPackageProcedures(DBRProgressMonitor monitor) throws DBException {
        List<GaussDBProcedure> list = new ArrayList<>();
        if (oid != 0) {
            list = getGaussDBProceduresCache().getAllObjects(monitor, this.schema).stream()
                        .filter(e -> e.getProPackageId() == oid && e.getKind() == PostgreProcedureKind.f)
                        .collect(Collectors.toList());
        }
        return list;
    }

    public ProceduresCache getGaussDBProceduresCache() {
        return this.proceduresCache;
    }

    static class ProceduresCache extends JDBCObjectLookupCache<GaussDBSchema, GaussDBProcedure> {

        public ProceduresCache() {
            super();
        }

        @Override
        protected GaussDBProcedure fetchObject(@NotNull JDBCSession session,
                                               @NotNull GaussDBSchema owner,
                                               @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new GaussDBProcedure(session.getProgressMonitor(), owner, dbResult);
        }

        @Override
        public JDBCStatement prepareLookupStatement(JDBCSession session,
                                                    GaussDBSchema owner,
                                                    GaussDBProcedure object,
                                                    String objectName) throws SQLException {
            PostgreServerExtension serverType = owner.getDataSource().getServerType();
            String oidColumn = serverType.getProceduresOidColumn();
            JDBCPreparedStatement dbStat = session.prepareStatement("SELECT p." + oidColumn + " as poid,p.*,"
                        + (session.getDataSource().isServerVersionAtLeast(8, 4) ? "pg_catalog.pg_get_expr(p.proargdefaults, 0)"
                                    : "NULL")
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

    }

}