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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreObject;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedureKind;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
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
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

public class GaussDBPackage implements PostgreObject, PostgreScriptObject, DBPSystemInfoObject {

    private static final Log log = Log.getLog(GaussDBPackage.class);
    private GaussDBSchema schema;
    protected long ownerId;
    private long oid;
    private String name;
    private String description;
    private String sourceDeclaration = "";
    private String sourceDefinition = "";

    private final ProceduresCache proceduresCache;

    public GaussDBPackage(@NotNull JDBCSession session, @NotNull GaussDBSchema schema, @NotNull JDBCResultSet dbResult) {
        this.schema = schema;
        this.oid = JDBCUtils.safeGetLong(dbResult, "oid");
        this.name = JDBCUtils.safeGetString(dbResult, "name");
        initialize(session, oid);
        this.proceduresCache = new ProceduresCache();
    }

    public GaussDBPackage(GaussDBSchema schema, DBRProgressMonitor unusedMnitor, String name) {
        this.schema = schema;
        this.name = name;
        this.proceduresCache = new ProceduresCache();
    }

    private void initialize(JDBCSession session, long objectId) {
        JDBCPreparedStatement prepareStatement;
        try {
            prepareStatement = session
                .prepareStatement("select pkg.src from DBE_PLDEVELOPER.gs_source pkg where pkg.id = ? and type = ?");
            prepareStatement.setLong(1, objectId);
            prepareStatement.setString(2, "package");
            JDBCResultSet dbResult = prepareStatement.executeQuery();
            if (dbResult.nextRow()) {
                this.sourceDeclaration = JDBCUtils.safeGetString(dbResult, "src");
            }
            prepareStatement.setString(2, "package body");
            dbResult = prepareStatement.executeQuery();
            if (dbResult.nextRow()) {
                this.sourceDefinition = JDBCUtils.safeGetString(dbResult, "src");
            }
        } catch (SQLException | DBCException e) {
            log.error(e);
        }
    }

    public GaussDBSchema getSchema() {
        return schema;
    }

    @Override
    public DBSObject getParentObject() {
        return null;
    }

    @Override
    public String getName() {
        return this.name;
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

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (CommonUtils.isEmpty(sourceDefinition)) {
            return sourceDeclaration;
        }
        return sourceDeclaration.trim() + "\n" + sourceDefinition;
    }

    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText() {
        return sourceDeclaration;
    }

    @Override
    public void setObjectDefinitionText(String sourceText) {
        sourceDeclaration = sourceText;
    }

    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getExtendedDefinitionText() {
        return sourceDefinition;
    }

    public void setExtendedDefinitionText(String source) {
        this.sourceDefinition = source;
    }

    @Override
    public GaussDBDataSource getDataSource() {
        return (GaussDBDataSource) schema.getDataSource();
    }

    @Override
    public GaussDBDatabase getDatabase() {
        return (GaussDBDatabase) schema.getDatabase();
    }

    @Association
    public List<GaussDBProcedure> getPackageProcedures(DBRProgressMonitor monitor) throws DBException {
        List<GaussDBProcedure> list = new ArrayList<>();
        if (oid != 0) {
            list = getGaussDBProceduresCache().getAllObjects(monitor, this.schema).stream()
                .filter(e -> e.getPropackageid() == oid && e.getKind() == PostgreProcedureKind.p).collect(Collectors.toList());
        }
        return list;
    }

    @Association
    public List<GaussDBProcedure> getPackageFunctions(DBRProgressMonitor monitor) throws DBException {
        List<GaussDBProcedure> list = new ArrayList<>();
        if (oid != 0) {
            list = getGaussDBProceduresCache().getAllObjects(monitor, this.schema).stream()
                .filter(e -> e.getPropackageid() == oid && e.getKind() == PostgreProcedureKind.f).collect(Collectors.toList());
        }
        return list;
    }

    public ProceduresCache getGaussDBProceduresCache() {
        return this.proceduresCache;
    }

    public static class ProceduresCache extends JDBCObjectLookupCache<PostgreSchema, GaussDBProcedure> {

        public ProceduresCache() {
            super();
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull PostgreSchema owner,
            @Nullable GaussDBProcedure object, @Nullable String objectName) throws SQLException {
            PostgreServerExtension serverType = owner.getDataSource().getServerType();
            String oidColumn = serverType.getProceduresOidColumn(); // Hack for Redshift SP support
            JDBCPreparedStatement dbStat = session.prepareStatement("SELECT p." + oidColumn + " as poid,p.*,"
                + (session.getDataSource().isServerVersionAtLeast(8, 4) ? "pg_catalog.pg_get_expr(p.proargdefaults, 0)" : "NULL")
                + " as arg_defaults,d.description\n" + "FROM pg_catalog." + serverType.getProceduresSystemTable() + " p\n"
                + "LEFT OUTER JOIN pg_catalog.pg_description d ON d.objoid=p." + oidColumn
                + (session.getDataSource().isServerVersionAtLeast(7, 2) ? " AND d.objsubid = 0" : "") + // no links to
                                                                                                        // columns
                "\nWHERE p.pronamespace=?" + (object == null ? "" : " AND p." + oidColumn + "=?") + "\nORDER BY p.proname");
            dbStat.setLong(1, owner.getObjectId());
            if (object != null) {
                dbStat.setLong(2, object.getObjectId());
            }
            return dbStat;
        }

        @Override
        protected GaussDBProcedure fetchObject(@NotNull JDBCSession session, @NotNull PostgreSchema owner,
            @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new GaussDBProcedure(session.getProgressMonitor(), owner, dbResult);
        }
    }
}
