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
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.SQLException;

/**
 * One object that depends on (uses) a given Mimer SQL object - the reverse direction of {@link
 * MimerObjectUses}. Read from {@code INFORMATION_SCHEMA.EXT_OBJECT_OBJECT_USED}, filtered to a
 * specific owning object by its own per-type cache - see each owner class's own
 * {@code UsedByCache} for the exact query (several object types add an extra source beyond this
 * view: a table also counts its own indexes, a domain also counts columns declared with that
 * domain, a collation also counts columns using that collation).
 * <p>
 * Shared, unchanged, across every object type this applies to (tables, views, columns, sequences,
 * routines, domains, collations, UDT methods, ...) - a plain {@link DBSObject} owner, same
 * reasoning as every other per-owner-type read-only summary class in this plugin.
 *
 * @author Mimer Information Technology
 */
public class MimerObjectUsedBy implements DBSObject {

    private final DBSObject owner;
    private final String objectSchema;
    private final String objectName;
    private final String objectType;
    private final String specificName;

    public MimerObjectUsedBy(@NotNull DBSObject owner, @NotNull JDBCResultSet dbResult) {
        this.owner = owner;
        this.objectSchema = JDBCUtils.safeGetString(dbResult, "USING_OBJECT_SCHEMA");
        this.objectName = JDBCUtils.safeGetString(dbResult, "USING_OBJECT_NAME");
        this.objectType = JDBCUtils.safeGetStringTrimmed(dbResult, "USING_OBJECT_TYPE");
        this.specificName = JDBCUtils.safeGetString(dbResult, "USING_SPECIFIC_NAME");
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return objectName + " (" + objectType + ")";
    }

    @Property(viewable = true, order = 2)
    public String getObjectSchema() {
        return objectSchema;
    }

    @Property(viewable = true, order = 3)
    public String getObjectType() {
        return objectType;
    }

    /**
     * The real object this row names, when {@link MimerUtils#resolveDependencyObject} knows how
     * to look it up - rendered as a clickable link in the properties grid (any {@code @Property}
     * whose value is a {@link DBSObject} gets this treatment for free, see {@code
     * DatabaseObjectListControl}), opening the exact same detail view browsing the schema tree
     * directly would show. {@code null} (plain, non-navigable text) for a type this doesn't
     * resolve, or an object no longer there. Passes along {@link #specificName} so an overloaded
     * procedure/function/method resolves to the exact overload this row names, not just the
     * first one sharing its bare name.
     */
    @Nullable
    @Property(viewable = true, order = 4)
    public DBSObject getObject(@NotNull DBRProgressMonitor monitor) {
        return MimerUtils.resolveDependencyObject(monitor, getDataSource(), objectSchema, objectName, objectType, specificName);
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public DBSObject getParentObject() {
        return owner;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return (MimerDataSource) owner.getDataSource();
    }

    /**
     * The plain {@code EXT_OBJECT_OBJECT_USED} query, shared by every owner type that doesn't
     * need an extra {@code UNION} beyond it (most of them - a table also counts its own indexes,
     * a domain also counts columns declared with that domain, a collation also counts columns
     * using that collation, so those three build their own statement instead of calling this).
     * <p>
     * Filtered by bare name, not {@code USED_SPECIFIC_NAME} - only safe for an owner type that
     * can't be overloaded (Mimer SQL only overloads procedures/functions/methods by parameter
     * list). An overloadable owner must use {@link #prepareUsedByStatementBySpecificName} instead,
     * or this returns every overload's dependents mixed together under just one of them.
     */
    @NotNull
    public static JDBCPreparedStatement prepareUsedByStatement(
        @NotNull JDBCSession session, @NotNull String schema, @NotNull String name, @NotNull String objectType
    ) throws SQLException {
        JDBCPreparedStatement stmt = session.prepareStatement(
            "SELECT USING_OBJECT_SCHEMA, USING_OBJECT_NAME, USING_OBJECT_TYPE, USING_SPECIFIC_NAME\n" +
            "FROM INFORMATION_SCHEMA.EXT_OBJECT_OBJECT_USED\n" +
            "WHERE USED_OBJECT_SCHEMA = ? AND USED_OBJECT_NAME = ? AND USED_OBJECT_TYPE = ?\n" +
            "ORDER BY USING_OBJECT_SCHEMA, USING_OBJECT_NAME");
        stmt.setString(1, schema);
        stmt.setString(2, name);
        stmt.setString(3, objectType);
        return stmt;
    }

    /**
     * Same as {@link #prepareUsedByStatement}, but scoped by the owner's own {@code
     * USED_SPECIFIC_NAME} instead of its bare name - required for an overloadable owner
     * (procedure/function/method), confirmed live: filtering by bare name alone pulled in every
     * overload's dependents under whichever overload's folder happened to be opened.
     */
    @NotNull
    public static JDBCPreparedStatement prepareUsedByStatementBySpecificName(
        @NotNull JDBCSession session, @NotNull String schema, @NotNull String specificName, @NotNull String objectType
    ) throws SQLException {
        JDBCPreparedStatement stmt = session.prepareStatement(
            "SELECT USING_OBJECT_SCHEMA, USING_OBJECT_NAME, USING_OBJECT_TYPE, USING_SPECIFIC_NAME\n" +
            "FROM INFORMATION_SCHEMA.EXT_OBJECT_OBJECT_USED\n" +
            "WHERE USED_OBJECT_SCHEMA = ? AND USED_SPECIFIC_NAME = ? AND USED_OBJECT_TYPE = ?\n" +
            "ORDER BY USING_OBJECT_SCHEMA, USING_OBJECT_NAME");
        stmt.setString(1, schema);
        stmt.setString(2, specificName);
        stmt.setString(3, objectType);
        return stmt;
    }
}
