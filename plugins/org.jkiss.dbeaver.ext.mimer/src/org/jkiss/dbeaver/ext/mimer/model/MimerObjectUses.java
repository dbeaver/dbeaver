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
 * One object that a given Mimer SQL object depends on (uses) - the reverse direction of {@link
 * MimerObjectUsedBy}. Read from {@code INFORMATION_SCHEMA.EXT_OBJECT_OBJECT_USING}, filtered to
 * a specific owning object by its own per-type cache - see each owner class's own {@code
 * UsesCache} for the exact query (a table also counts the databank(s) it's stored in; a table/
 * view column also counts its domain and any sequence its default value references).
 * <p>
 * Shared, unchanged, across every object type this applies to - a plain {@link DBSObject} owner,
 * same reasoning as every other per-owner-type read-only summary class in this plugin.
 *
 * @author Mimer Information Technology
 */
public class MimerObjectUses implements DBSObject {

    private final DBSObject owner;
    private final String objectSchema;
    private final String objectName;
    private final String objectType;
    private final String specificName;

    public MimerObjectUses(@NotNull DBSObject owner, @NotNull JDBCResultSet dbResult) {
        this.owner = owner;
        this.objectSchema = JDBCUtils.safeGetString(dbResult, "USED_OBJECT_SCHEMA");
        this.objectName = JDBCUtils.safeGetString(dbResult, "USED_OBJECT_NAME");
        this.objectType = JDBCUtils.safeGetStringTrimmed(dbResult, "USED_OBJECT_TYPE");
        this.specificName = JDBCUtils.safeGetString(dbResult, "USED_SPECIFIC_NAME");
    }

    /**
     * For a dependency not read from a plain {@code EXT_OBJECT_OBJECT_USING} row - see {@link
     * MimerTableColumn#getUses}, which resolves a column's sequence dependency by parsing its
     * own already-loaded default value rather than an extra query. Never a routine, so there's no
     * specific name to carry.
     */
    public MimerObjectUses(@NotNull DBSObject owner, @NotNull String objectSchema, @NotNull String objectName, @NotNull String objectType) {
        this.owner = owner;
        this.objectSchema = objectSchema;
        this.objectName = objectName;
        this.objectType = objectType;
        this.specificName = null;
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

    /** The bare name of the used object (without the {@code " (TYPE)"} suffix {@link #getName} adds). */
    public String getObjectName() {
        return objectName;
    }

    @Property(viewable = true, order = 3)
    public String getObjectType() {
        return objectType;
    }

    /**
     * The real object this row names, when {@link MimerUtils#resolveDependencyObject} knows how
     * to look it up - see {@link MimerObjectUsedBy#getObject} for the full explanation (this is
     * its exact mirror for the "Uses" direction). Passes along {@link #specificName} so an
     * overloaded procedure/function/method resolves to the exact overload this row names, not
     * just the first one sharing its bare name.
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
     * The plain {@code EXT_OBJECT_OBJECT_USING} query, shared by every owner type that doesn't
     * need an extra {@code UNION} beyond it (most of them - a table also counts the databank(s)
     * it's stored in, a table/view column also counts its domain and any sequence its default
     * value references, so those build their own statement instead of calling this).
     * <p>
     * Filtered by bare name, not {@code USING_SPECIFIC_NAME} - only safe for an owner type that
     * can't be overloaded (Mimer SQL only overloads procedures/functions/methods by parameter
     * list). An overloadable owner must use {@link #prepareUsesStatementBySpecificName} instead,
     * or this returns every overload's dependencies mixed together under just one of them.
     */
    @NotNull
    public static JDBCPreparedStatement prepareUsesStatement(
        @NotNull JDBCSession session, @NotNull String schema, @NotNull String name, @NotNull String objectType
    ) throws SQLException {
        JDBCPreparedStatement stmt = session.prepareStatement(
            "SELECT USED_OBJECT_SCHEMA, USED_OBJECT_NAME, USED_OBJECT_TYPE, USED_SPECIFIC_NAME\n" +
            "FROM INFORMATION_SCHEMA.EXT_OBJECT_OBJECT_USING\n" +
            "WHERE USING_OBJECT_SCHEMA = ? AND USING_OBJECT_NAME = ? AND USING_OBJECT_TYPE = ?\n" +
            "ORDER BY USED_OBJECT_SCHEMA, USED_OBJECT_NAME");
        stmt.setString(1, schema);
        stmt.setString(2, name);
        stmt.setString(3, objectType);
        return stmt;
    }

    /**
     * Same as {@link #prepareUsesStatement}, but scoped by the owner's own {@code
     * USING_SPECIFIC_NAME} instead of its bare name - required for an overloadable owner
     * (procedure/function/method), confirmed live: filtering by bare name alone pulled in every
     * overload's dependencies under whichever overload's folder happened to be opened.
     */
    @NotNull
    public static JDBCPreparedStatement prepareUsesStatementBySpecificName(
        @NotNull JDBCSession session, @NotNull String schema, @NotNull String specificName, @NotNull String objectType
    ) throws SQLException {
        JDBCPreparedStatement stmt = session.prepareStatement(
            "SELECT USED_OBJECT_SCHEMA, USED_OBJECT_NAME, USED_OBJECT_TYPE, USED_SPECIFIC_NAME\n" +
            "FROM INFORMATION_SCHEMA.EXT_OBJECT_OBJECT_USING\n" +
            "WHERE USING_OBJECT_SCHEMA = ? AND USING_SPECIFIC_NAME = ? AND USING_OBJECT_TYPE = ?\n" +
            "ORDER BY USED_OBJECT_SCHEMA, USED_OBJECT_NAME");
        stmt.setString(1, schema);
        stmt.setString(2, specificName);
        stmt.setString(3, objectType);
        return stmt;
    }
}
