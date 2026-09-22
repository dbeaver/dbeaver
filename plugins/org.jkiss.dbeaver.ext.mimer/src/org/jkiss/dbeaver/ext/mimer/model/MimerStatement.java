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
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
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
import org.jkiss.dbeaver.model.struct.DBSObjectWithScript;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * A Mimer SQL {@code STATEMENT} - a precompiled, stored SQL statement (like PostgreSQL {@code
 * PREPARE}), executed by name once created. Needs {@code EXECUTE} privilege to use (see {@link
 * MimerStatementPrivilege}).
 * <p>
 * {@link #scrollable}/{@link #forwardOnly} back the {@code CREATE [SCROLL|NO SCROLL] STATEMENT}
 * header (from {@code INFORMATION_SCHEMA.EXT_STATEMENTS}), omitted entirely when both are
 * {@code true}. The body text is read separately from {@code EXT_STATEMENT_DEFINITION} (see
 * {@link MimerUtils#readStatementDefinition}) - a different view from every other Mimer SQL
 * schema object's {@code EXT_SOURCE_DEFINITION}, though both views hold the same body text for
 * a statement.
 * <p>
 * There's no {@code ALTER STATEMENT} that changes the text - only {@code ALTER STATEMENT ...
 * REFRESH}, a recompile with no text change, not modeled here - so editing an existing
 * statement's source drops and recreates it, same as {@link MimerModule}/{@link
 * MimerProcedure}/{@link MimerTableTrigger}. See {@link
 * org.jkiss.dbeaver.ext.mimer.edit.MimerStatementManager}.
 *
 * @author Mimer Information Technology
 */
public class MimerStatement implements DBSObject, DBPSaveableObject, DBPRefreshableObject, DBSObjectWithScript {

    private final PrivilegeCache privilegeCache = new PrivilegeCache();
    private final UsesCache usesCache = new UsesCache();
    private final MimerSchema schema;
    private String name;
    private boolean scrollable;
    private boolean forwardOnly;
    private String source;
    private boolean persisted = true;
    private String comment;

    public MimerStatement(@NotNull MimerSchema schema, @NotNull JDBCResultSet dbResult) {
        this.schema = schema;
        this.name = JDBCUtils.safeGetString(dbResult, "STATEMENT_NAME");
        this.scrollable = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_SCROLLABLE"));
        this.forwardOnly = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_FORWARD_ONLY"));
    }

    /**
     * For a brand-new, not-yet-created statement - see {@link
     * org.jkiss.dbeaver.ext.mimer.edit.MimerStatementManager}. The create dialog seeds {@link
     * #source} (header + placeholder body) directly via {@link #setObjectDefinitionText} rather
     * than through {@link #scrollable}/{@link #forwardOnly}, which are unused until the object
     * is actually loaded back from the catalog.
     */
    public MimerStatement(@NotNull MimerSchema schema, @NotNull String name) {
        this.schema = schema;
        this.name = name;
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
    public boolean isScrollable() {
        return scrollable;
    }

    @Property(viewable = true, order = 3)
    public boolean isForwardOnly() {
        return forwardOnly;
    }

    @NotNull
    public MimerSchema getSchema() {
        return schema;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    /**
     * The {@code @Property} annotation is required - without it the Source tab's Save silently
     * discards edits (see {@code MimerModule}'s identical getter for the full explanation).
     * {@code source} is cached on first read regardless of {@code persisted}, same reason as
     * {@code MimerModule}: {@code MimerStatementManager.addObjectModifyActions} reads this same
     * getter to build its {@code CREATE STATEMENT} action while the object is still {@code
     * persisted == true} (the DROP hasn't run yet), so a re-query here would silently return the
     * old, pre-edit body instead of what the Source tab's Save just set.
     */
    @NotNull
    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options) throws DBException {
        if (source == null) {
            source = persisted ? buildFullSource(monitor) : "";
        }
        return source;
    }

    @Override
    public void setObjectDefinitionText(String source) {
        this.source = source;
    }

    @NotNull
    private String buildFullSource(@NotNull DBRProgressMonitor monitor) throws DBException {
        String body = MimerUtils.readStatementDefinition(monitor, this, schema.getName(), name);
        return buildHeader() + "\n" + body;
    }

    /**
     * {@code CREATE [SCROLL|NO SCROLL] STATEMENT "schema"."name"} - the clause is only emitted
     * when {@link #scrollable}/{@link #forwardOnly} say so: {@code NO SCROLL} when not scrollable
     * at all, {@code SCROLL} when scrollable and not forward-only, omitted (both cursor modes
     * allowed) otherwise.
     */
    @NotNull
    private String buildHeader() {
        String clause;
        if (!scrollable) {
            clause = " NO SCROLL";
        } else if (!forwardOnly) {
            clause = " SCROLL";
        } else {
            clause = "";
        }
        return "CREATE" + clause + " STATEMENT \"" + schema.getName() + "\".\"" + name + "\"";
    }

    /**
     * {@code COMMENT ON STATEMENT "schema"."name" IS '...'}. See {@link
     * org.jkiss.dbeaver.ext.mimer.edit.MimerStatementManager} for the write side.
     */
    @Nullable
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 100)
    public String getComment(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (comment == null && persisted) {
            comment = MimerUtils.readObjectComment(monitor, this, schema.getName(), null, name, "STATEMENT");
        }
        return comment;
    }

    public void setComment(@Nullable String comment) {
        this.comment = comment;
    }

    @Association
    public Collection<MimerStatementPrivilege> getPrivileges(DBRProgressMonitor monitor) throws DBException {
        return privilegeCache.getAllObjects(monitor, this);
    }

    public DBSObjectCache<MimerStatement, MimerStatementPrivilege> getPrivilegeCache() {
        return privilegeCache;
    }

    @Association
    public Collection<MimerObjectUses> getUses(@NotNull DBRProgressMonitor monitor) throws DBException {
        return usesCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) {
        privilegeCache.clearCache();
        usesCache.clearCache();
        source = null;
        comment = null;
        return this;
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

    /**
     * {@code EXECUTE} grants on this statement - see {@link MimerStatementPrivilege}.
     */
    /**
     * {@code GRANTOR = '_SYSTEM'} rows (Mimer SQL's synthetic "the owner implicitly holds
     * EXECUTE" grant) are excluded - {@code EXT_OBJECT_PRIVILEGES} carries these for a
     * databank's owner too (see {@code MimerDatabankPrivilege.PrivilegeCache}); same
     * convention applied here on the assumption it's a general Mimer SQL behaviour, not
     * databank-specific.
     */
    static class PrivilegeCache extends JDBCObjectCache<MimerStatement, MimerStatementPrivilege> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerStatement statement) throws SQLException {
            JDBCPreparedStatement stmt = session.prepareStatement(
                "SELECT GRANTEE, GRANTOR, IS_GRANTABLE\n" +
                "FROM INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES\n" +
                "WHERE OBJECT_TYPE = 'STATEMENT' AND OBJECT_SCHEMA = ? AND OBJECT_NAME = ? AND PRIVILEGE_TYPE = 'EXECUTE'\n" +
                "  AND GRANTOR <> '_SYSTEM'\n" +
                "ORDER BY GRANTEE");
            stmt.setString(1, statement.getSchema().getName());
            stmt.setString(2, statement.getName());
            return stmt;
        }

        @Override
        protected MimerStatementPrivilege fetchObject(@NotNull JDBCSession session, @NotNull MimerStatement statement, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerStatementPrivilege(statement, resultSet);
        }
    }

    static class UsesCache extends JDBCObjectCache<MimerStatement, MimerObjectUses> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerStatement owner) throws SQLException {
            return MimerObjectUses.prepareUsesStatement(session, owner.getSchema().getName(), owner.getName(), "STATEMENT");
        }

        @Override
        protected MimerObjectUses fetchObject(@NotNull JDBCSession session, @NotNull MimerStatement owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerObjectUses(owner, resultSet);
        }
    }
}
