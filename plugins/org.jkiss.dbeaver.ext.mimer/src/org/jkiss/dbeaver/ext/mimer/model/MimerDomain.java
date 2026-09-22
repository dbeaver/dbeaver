/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * Mimer SQL domain (a named, constrained data type), with create/drop support - see {@link
 * org.jkiss.dbeaver.ext.mimer.edit.MimerDomainManager}. {@link #collation}/{@link
 * #constraintName}/{@link #checkClause} are CREATE-only, never re-hydrated on load. Implements
 * {@link DBPSaveableObject} itself since a plain leaf {@code DBSObject} has no persisted-state
 * tracking. Does not implement {@code DBPNamedObject2} - Mimer SQL has no {@code ALTER DOMAIN ...
 * RENAME}, so {@link #setName} exists only for the create flow, not an inline rename gesture.
 *
 * @author Mimer Information Technology
 */
public class MimerDomain implements DBSObject, DBPScriptObject, DBPSaveableObject {

    private final MimerSchema schema;
    private String name;
    private String dataType;
    private final long charLength;
    private final Integer numPrecision;
    private final Integer numScale;
    private String defaultValue;
    private String collation;
    private String constraintName;
    private String checkClause;
    private boolean persisted = true;
    private String comment;
    private final PrivilegeCache privilegeCache = new PrivilegeCache();
    private final UsedByCache usedByCache = new UsedByCache();
    private final UsesCache usesCache = new UsesCache();

    public MimerDomain(@NotNull MimerSchema schema, @NotNull JDBCResultSet dbResult) {
        this.schema = schema;
        this.name = JDBCUtils.safeGetString(dbResult, "DOMAIN_NAME");
        this.dataType = JDBCUtils.safeGetString(dbResult, "DATA_TYPE");
        this.charLength = JDBCUtils.safeGetLong(dbResult, "CHARACTER_MAXIMUM_LENGTH");
        this.numPrecision = JDBCUtils.safeGetInteger(dbResult, "NUMERIC_PRECISION");
        this.numScale = JDBCUtils.safeGetInteger(dbResult, "NUMERIC_SCALE");
        this.defaultValue = JDBCUtils.safeGetString(dbResult, "DOMAIN_DEFAULT");
    }

    /**
     * For a brand-new, not-yet-created domain - see {@link org.jkiss.dbeaver.ext.mimer.edit.MimerDomainManager}.
     */
    public MimerDomain(@NotNull MimerSchema schema, @NotNull String name) {
        this.schema = schema;
        this.name = name;
        this.dataType = "";
        this.charLength = 0;
        this.numPrecision = null;
        this.numScale = null;
        this.persisted = false;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    @Property(viewable = true, order = 2)
    public String getDataType() {
        return MimerUtils.formatDomainDataType(dataType, charLength, numPrecision, numScale);
    }

    /**
     * CREATE-only - the full type spec as typed in the create dialog (e.g. {@code
     * "VARCHAR(20)"}), unlike a loaded domain's decomposed {@link #getDataType()}. {@link
     * MimerUtils#formatDomainDataType} passes a pre-formatted string like this through
     * unchanged.
     */
    public void setDataType(@NotNull String dataType) {
        this.dataType = dataType;
    }

    @Property(viewable = true, order = 3)
    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(@Nullable String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setCollation(@Nullable String collation) {
        this.collation = collation;
    }

    public void setConstraintName(@Nullable String constraintName) {
        this.constraintName = constraintName;
    }

    public void setCheckClause(@Nullable String checkClause) {
        this.checkClause = checkClause;
    }

    @NotNull
    public MimerSchema getSchema() {
        return schema;
    }

    /**
     * {@code CREATE DOMAIN "schema"."name" AS <type> [COLLATE <collation>] [DEFAULT <expr>]
     * [CONSTRAINT "name"] [CHECK(...)]} - constraint name is only emitted alongside a check
     * clause, since alone it isn't meaningful DDL.
     */
    @NotNull
    public String buildCreateDDL() {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE DOMAIN \"").append(schema.getName()).append("\".\"").append(name).append("\" AS\n");
        sb.append(dataType);
        if (!CommonUtils.isEmpty(collation)) {
            sb.append(" COLLATE ").append(collation);
        }
        if (!CommonUtils.isEmpty(defaultValue)) {
            sb.append("\nDEFAULT ").append(defaultValue);
        }
        if (!CommonUtils.isEmpty(checkClause)) {
            sb.append('\n');
            if (!CommonUtils.isEmpty(constraintName)) {
                sb.append("CONSTRAINT \"").append(constraintName).append("\" ");
            }
            sb.append("CHECK(").append(checkClause).append(')');
        }
        return sb.toString();
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    /**
     * {@code COMMENT ON DOMAIN "schema"."name" IS '...'}. See {@link
     * org.jkiss.dbeaver.ext.mimer.edit.MimerDomainManager} for the write side.
     */
    @Nullable
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 100)
    public String getComment(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (comment == null && persisted) {
            comment = MimerUtils.readObjectComment(monitor, this, schema.getName(), null, name, "DOMAIN");
        }
        return comment;
    }

    public void setComment(@Nullable String comment) {
        this.comment = comment;
    }

    /**
     * {@code USAGE} privileges granted on this domain - see {@link MimerDomainPrivilege}.
     */
    @Association
    public Collection<MimerDomainPrivilege> getPrivileges(@NotNull DBRProgressMonitor monitor) throws DBException {
        return privilegeCache.getAllObjects(monitor, this);
    }

    @NotNull
    public DBSObjectCache<MimerDomain, MimerDomainPrivilege> getPrivilegeCache() {
        return privilegeCache;
    }

    @Association
    public Collection<MimerObjectUsedBy> getUsedBy(@NotNull DBRProgressMonitor monitor) throws DBException {
        return usedByCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<MimerObjectUses> getUses(@NotNull DBRProgressMonitor monitor) throws DBException {
        return usesCache.getAllObjects(monitor, this);
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }

    @Override
    public DBSObject getParentObject() {
        return schema;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return (MimerDataSource) schema.getDataSource();
    }

    @NotNull
    @Override
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options) throws DBException {
        return MimerUtils.buildDomainSource(monitor, this, schema.getName(), name);
    }

    /**
     * {@code GRANTOR = '_SYSTEM'} rows (Mimer SQL's synthetic "the owner implicitly holds USAGE"
     * grant) are excluded - {@code EXT_OBJECT_PRIVILEGES} carries these for a databank's owner
     * too (see {@code MimerDatabankPrivilege.PrivilegeCache}); same convention applied here on
     * the assumption it's a general Mimer SQL behaviour, not databank-specific.
     */
    static class PrivilegeCache extends JDBCObjectCache<MimerDomain, MimerDomainPrivilege> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerDomain owner) throws SQLException {
            JDBCPreparedStatement stmt = session.prepareStatement(
                "SELECT GRANTEE, GRANTOR, IS_GRANTABLE\n" +
                "FROM INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES\n" +
                "WHERE OBJECT_TYPE = 'DOMAIN' AND OBJECT_SCHEMA = ? AND OBJECT_NAME = ? AND PRIVILEGE_TYPE = 'USAGE'\n" +
                "  AND GRANTOR <> '_SYSTEM'\n" +
                "ORDER BY GRANTEE");
            stmt.setString(1, owner.getSchema().getName());
            stmt.setString(2, owner.getName());
            return stmt;
        }

        @Override
        protected MimerDomainPrivilege fetchObject(@NotNull JDBCSession session, @NotNull MimerDomain owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerDomainPrivilege(owner, resultSet);
        }
    }

    /**
     * A column declared with this domain counts as using it too, in addition to whatever {@code
     * EXT_OBJECT_OBJECT_USED} itself reports.
     */
    static class UsedByCache extends JDBCObjectCache<MimerDomain, MimerObjectUsedBy> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerDomain owner) throws SQLException {
            JDBCPreparedStatement stmt = session.prepareStatement(
                "SELECT USING_OBJECT_SCHEMA, USING_OBJECT_NAME, USING_OBJECT_TYPE\n" +
                "FROM INFORMATION_SCHEMA.EXT_OBJECT_OBJECT_USED\n" +
                "WHERE USED_OBJECT_SCHEMA = ? AND USED_OBJECT_NAME = ? AND USED_OBJECT_TYPE = 'DOMAIN'\n" +
                "UNION ALL\n" +
                "SELECT TABLE_SCHEMA, TABLE_NAME, 'COLUMN'\n" +
                "FROM INFORMATION_SCHEMA.COLUMN_DOMAIN_USAGE\n" +
                "WHERE DOMAIN_SCHEMA = ? AND DOMAIN_NAME = ?\n" +
                "ORDER BY 1, 2");
            stmt.setString(1, owner.getSchema().getName());
            stmt.setString(2, owner.getName());
            stmt.setString(3, owner.getSchema().getName());
            stmt.setString(4, owner.getName());
            return stmt;
        }

        @Override
        protected MimerObjectUsedBy fetchObject(@NotNull JDBCSession session, @NotNull MimerDomain owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerObjectUsedBy(owner, resultSet);
        }
    }

    static class UsesCache extends JDBCObjectCache<MimerDomain, MimerObjectUses> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerDomain owner) throws SQLException {
            return MimerObjectUses.prepareUsesStatement(session, owner.getSchema().getName(), owner.getName(), "DOMAIN");
        }

        @Override
        protected MimerObjectUses fetchObject(@NotNull JDBCSession session, @NotNull MimerDomain owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerObjectUses(owner, resultSet);
        }
    }
}
