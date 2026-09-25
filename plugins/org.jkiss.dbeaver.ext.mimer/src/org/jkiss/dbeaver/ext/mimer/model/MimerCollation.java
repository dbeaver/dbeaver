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
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * Mimer SQL collation - a per-schema object, matching every other schema object here (not a
 * datasource-global one, despite how it was originally modeled), with create/drop/comment support
 * - see {@link
 * org.jkiss.dbeaver.ext.mimer.edit.MimerCollationManager}. In Mimer SQL the character set is a
 * property of the collation (there is no separate user-facing character-set object - {@code CHAR}
 * uses the server 8-bit set, {@code NCHAR} uses Unicode), so the character-set name is exposed
 * here rather than as its own tree node. The server's own built-in collations (e.g.
 * {@code "INFORMATION_SCHEMA"."ISO8BIT"}) show up the same way as any other schema's, under the
 * {@code INFORMATION_SCHEMA} schema - visible once "Show system objects" is on, same as every
 * other {@code INFORMATION_SCHEMA} object.
 * <p>
 * {@link #sourceCollation}/{@link #usingClause} are CREATE-only, never re-hydrated on load - a
 * loaded collation's Source tab instead reconstructs a full {@code CREATE COLLATION} statement
 * from {@code INFORMATION_SCHEMA.EXT_COLLATION_DEFINITIONS} (see {@link
 * MimerUtils#buildCollationSource}), same "no ALTER, read-only reconstructed Source tab" shape as
 * {@link MimerDomain}/{@link MimerSynonym} - Mimer SQL has no {@code ALTER COLLATION} either, so
 * there's nothing an editable Source tab could ever persist. Implements {@link DBPSaveableObject}
 * itself since a plain leaf {@code DBSObject} has no persisted-state tracking (same reasoning as
 * {@code MimerDomain}).
 *
 * @author Mimer Information Technology
 */
public class MimerCollation implements DBSObject, DBPSaveableObject, DBPScriptObject {

    private final MimerSchema schema;
    private String name;
    private final String characterSetSchema;
    private final String characterSetName;
    private final String padAttribute;
    private String sourceCollation;
    private String usingClause;
    private boolean persisted = true;
    private String comment;
    private final UsedByCache usedByCache = new UsedByCache();
    private final UsesCache usesCache = new UsesCache();

    public MimerCollation(@NotNull MimerSchema schema, @NotNull JDBCResultSet dbResult) {
        this.schema = schema;
        this.name = JDBCUtils.safeGetString(dbResult, "COLLATION_NAME");
        this.characterSetSchema = JDBCUtils.safeGetString(dbResult, "CHARACTER_SET_SCHEMA");
        this.characterSetName = JDBCUtils.safeGetString(dbResult, "CHARACTER_SET_NAME");
        this.padAttribute = JDBCUtils.safeGetString(dbResult, "PAD_ATTRIBUTE");
    }

    /**
     * For a brand-new, not-yet-created collation - see {@link
     * org.jkiss.dbeaver.ext.mimer.edit.MimerCollationManager}.
     */
    public MimerCollation(@NotNull MimerSchema schema, @NotNull String name) {
        this.schema = schema;
        this.name = name;
        this.characterSetSchema = null;
        this.characterSetName = null;
        this.padAttribute = null;
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
    public String getSchemaName() {
        return schema.getName();
    }

    @Property(viewable = true, order = 3)
    public String getCharacterSetName() {
        return characterSetName;
    }

    @Property(viewable = true, order = 4)
    public String getCharacterSetSchema() {
        return characterSetSchema;
    }

    @Property(viewable = true, order = 5)
    public String getPadAttribute() {
        return padAttribute;
    }

    @NotNull
    public MimerSchema getSchema() {
        return schema;
    }

    /**
     * The text to use for this collation in a {@code COLLATE} clause or a picker's value - a
     * bare, unquoted name for one of Mimer SQL's two built-in {@code INFORMATION_SCHEMA}
     * collations ({@code ISO8BIT}/{@code UCS_BASIC}), the usual quoted {@code
     * "schema"."name"} form for anything else. Every user-facing collation picker in this plugin
     * (the table column and index column Collation dropdowns) uses this, rather than each
     * building its own quoted string, so a persisted column's read-back value always matches one
     * of the choices those pickers themselves offer - unquoted-vs-quoted would otherwise show as
     * two different values for the same collation. Matches the existing "the two implicit
     * default collations aren't worth spelling out" reasoning already used in {@link
     * MimerUtils#buildDomainSource}.
     */
    @NotNull
    public static String formatReference(@NotNull String schemaName, @NotNull String name) {
        return "INFORMATION_SCHEMA".equalsIgnoreCase(schemaName) ? name : "\"" + schemaName + "\".\"" + name + "\"";
    }

    @NotNull
    public String getReference() {
        return formatReference(schema.getName(), name);
    }

    @NotNull
    @Override
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options) throws DBException {
        return MimerUtils.buildCollationSource(monitor, this, schema.getName(), name);
    }

    /**
     * CREATE-only - the schema-qualified name of the existing collation this one is based on
     * (e.g. {@code "INFORMATION_SCHEMA"."EOR"}) - a new collation in Mimer SQL is always based on
     * an existing one, never created from scratch.
     */
    public void setSourceCollation(@NotNull String sourceCollation) {
        this.sourceCollation = sourceCollation;
    }

    /**
     * CREATE-only - the optional {@code USING} delta-string appended to the source collation's
     * own definition. {@code null}/blank means an exact copy of the source collation.
     */
    public void setUsingClause(@Nullable String usingClause) {
        this.usingClause = usingClause;
    }

    /**
     * {@code CREATE COLLATION "schema"."name" FROM <sourceCollation> [USING 'delta-string']} -
     * see "CREATE COLLATION" in the <a href="https://docs.mimer.com/MimerSqlManual/latest">Mimer
     * SQL Manual</a>.
     */
    @NotNull
    public String buildCreateDDL() {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE COLLATION \"").append(schema.getName()).append("\".\"").append(name).append("\"\n");
        sb.append("FROM ").append(sourceCollation);
        if (!CommonUtils.isEmpty(usingClause)) {
            sb.append("\nUSING '").append(usingClause.replace("'", "''")).append('\'');
        }
        return sb.toString();
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    /**
     * {@code COMMENT ON COLLATION "schema"."name" IS '...'}. See {@link
     * org.jkiss.dbeaver.ext.mimer.edit.MimerCollationManager} for the write side.
     */
    @Nullable
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 100)
    public String getComment(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (comment == null && persisted) {
            comment = MimerUtils.readObjectComment(monitor, this, schema.getName(), null, name, "COLLATION");
        }
        return comment;
    }

    public void setComment(@Nullable String comment) {
        this.comment = comment;
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

    /**
     * Every column whose {@code COLLATION_SCHEMA}/{@code COLLATION_NAME} names this collation,
     * in addition to the generic {@code EXT_OBJECT_OBJECT_USED} rows.
     */
    static class UsedByCache extends JDBCObjectCache<MimerCollation, MimerObjectUsedBy> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerCollation owner) throws SQLException {
            JDBCPreparedStatement stmt = session.prepareStatement(
                "SELECT USING_OBJECT_SCHEMA, USING_OBJECT_NAME, USING_OBJECT_TYPE\n" +
                "FROM INFORMATION_SCHEMA.EXT_OBJECT_OBJECT_USED\n" +
                "WHERE USED_OBJECT_SCHEMA = ? AND USED_OBJECT_NAME = ? AND USED_OBJECT_TYPE = 'COLLATION'\n" +
                "UNION ALL\n" +
                "SELECT TABLE_SCHEMA, TABLE_NAME, 'COLUMN'\n" +
                "FROM INFORMATION_SCHEMA.COLUMNS\n" +
                "WHERE COLLATION_SCHEMA = ? AND COLLATION_NAME = ?\n" +
                "ORDER BY 1, 2");
            stmt.setString(1, owner.getSchemaName());
            stmt.setString(2, owner.getName());
            stmt.setString(3, owner.getSchemaName());
            stmt.setString(4, owner.getName());
            return stmt;
        }

        @Override
        protected MimerObjectUsedBy fetchObject(@NotNull JDBCSession session, @NotNull MimerCollation owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerObjectUsedBy(owner, resultSet);
        }
    }

    static class UsesCache extends JDBCObjectCache<MimerCollation, MimerObjectUses> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerCollation owner) throws SQLException {
            return MimerObjectUses.prepareUsesStatement(session, owner.getSchemaName(), owner.getName(), "COLLATION");
        }

        @Override
        protected MimerObjectUses fetchObject(@NotNull JDBCSession session, @NotNull MimerCollation owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerObjectUses(owner, resultSet);
        }
    }
}
