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
import org.jkiss.dbeaver.ext.mimer.MimerConstants;
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
import org.jkiss.dbeaver.model.struct.DBSObjectWithScript;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Mimer SQL module - a named collection of routines and their shared declarations. Carries
 * **create/drop** support (see {@link org.jkiss.dbeaver.ext.mimer.edit.MimerModuleManager}) via
 * a second constructor for a not-yet-persisted module. No ALTER - Mimer SQL has no {@code ALTER
 * MODULE}, only drop-and-recreate.
 * <p>
 * Implements {@link DBSObjectWithScript} so the Source tab's editor - registered against {@code
 * org.jkiss.dbeaver.ext.mimer.ui.editors.MimerModuleSourceViewEditor}, a small local copy of
 * {@code ext.generic.ui}'s {@code GenericSourceViewEditor} (kept local to avoid a cross-plugin
 * {@code class=} reference; see that class's Javadoc) - opens editable rather than read-only.
 *
 * @author Mimer Information Technology
 */
public class MimerModule implements DBSObject, DBPScriptObject, DBPSaveableObject, DBSObjectWithScript {

    private final RoutineCache routineCache = new RoutineCache();
    private final MimerSchema schema;
    private String name;
    private String source;
    private boolean persisted = true;
    private String comment;

    public MimerModule(@NotNull MimerSchema schema, @NotNull JDBCResultSet dbResult) {
        this.schema = schema;
        this.name = JDBCUtils.safeGetString(dbResult, "MODULE_NAME");
    }

    /**
     * For a brand-new, not-yet-created module - see {@link org.jkiss.dbeaver.ext.mimer.edit.MimerModuleManager}.
     */
    public MimerModule(@NotNull MimerSchema schema, @NotNull String name) {
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

    @NotNull
    public MimerSchema getSchema() {
        return schema;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
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
     * The {@code @Property} annotation is required - without it the Source tab's Save silently
     * discards edits instead of calling {@link #setObjectDefinitionText} (see {@code
     * MimerProcedure}'s identical getter).
     * <p>
     * {@code source} must be cached on first read regardless of {@code persisted} (same pattern
     * as {@code GenericProcedure}/{@code GenericTrigger}): re-querying {@code
     * EXT_SOURCE_DEFINITION} on every call would return the old, pre-edit body when {@code
     * MimerModuleManager.addObjectModifyActions} builds its {@code CREATE MODULE} action - the
     * object is still {@code persisted == true} at that point, since the DROP hasn't run yet.
     */
    @NotNull
    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options) throws DBException {
        if (source == null) {
            source = persisted
                ? MimerUtils.readSourceDefinition(monitor, this, schema.getName(), name, MimerConstants.SOURCE_TYPE_MODULE, "OBJECT_NAME")
                : "";
        }
        return source;
    }

    @Override
    public void setObjectDefinitionText(String source) {
        this.source = source;
    }

    /**
     * {@code COMMENT ON MODULE "schema"."name" IS '...'}. See {@link
     * org.jkiss.dbeaver.ext.mimer.edit.MimerModuleManager} for the write side.
     */
    @Nullable
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 100)
    public String getComment(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (comment == null && persisted) {
            comment = MimerUtils.readObjectComment(monitor, this, schema.getName(), null, name, "MODULE");
        }
        return comment;
    }

    public void setComment(@Nullable String comment) {
        this.comment = comment;
    }

    @Association
    public Collection<MimerModuleRoutine> getRoutines(@NotNull DBRProgressMonitor monitor) throws DBException {
        return routineCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<MimerModuleRoutine> getProceduresOnly(@NotNull DBRProgressMonitor monitor) throws DBException {
        return filterByType(monitor, DBSProcedureType.PROCEDURE);
    }

    @Association
    public Collection<MimerModuleRoutine> getFunctionsOnly(@NotNull DBRProgressMonitor monitor) throws DBException {
        return filterByType(monitor, DBSProcedureType.FUNCTION);
    }

    @NotNull
    private Collection<MimerModuleRoutine> filterByType(@NotNull DBRProgressMonitor monitor, @NotNull DBSProcedureType type) throws DBException {
        List<MimerModuleRoutine> result = new ArrayList<>();
        for (MimerModuleRoutine routine : routineCache.getAllObjects(monitor, this)) {
            if (routine.getProcedureType() == type) {
                result.add(routine);
            }
        }
        return result;
    }

    /**
     * A module's own {@code DECLARE PROCEDURE}/{@code DECLARE FUNCTION} routines, shown only
     * nested here (not in the schema-level Procedures/Functions folders - {@code
     * MimerMetaModel#loadProcedures} filters them out there) since a module routine isn't
     * independently creatable, alterable, or droppable; only the module's own {@code CREATE
     * MODULE} text changes one. Uses {@link MimerModuleRoutine}, not {@code MimerProcedure}, so
     * the Source tab renders genuinely read-only - see that class's Javadoc.
     */
    static class RoutineCache extends JDBCObjectCache<MimerModule, MimerModuleRoutine> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerModule module) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT ROUTINE_NAME, SPECIFIC_NAME, ROUTINE_TYPE\n" +
                "FROM INFORMATION_SCHEMA.ROUTINES\n" +
                "WHERE MODULE_SCHEMA = ? AND MODULE_NAME = ?\n" +
                "ORDER BY ROUTINE_NAME");
            dbStat.setString(1, module.getSchema().getName());
            dbStat.setString(2, module.getName());
            return dbStat;
        }

        @Override
        protected MimerModuleRoutine fetchObject(@NotNull JDBCSession session, @NotNull MimerModule module, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            String routineName = JDBCUtils.safeGetString(resultSet, "ROUTINE_NAME");
            String specificName = JDBCUtils.safeGetString(resultSet, "SPECIFIC_NAME");
            DBSProcedureType type = "FUNCTION".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(resultSet, "ROUTINE_TYPE"))
                ? DBSProcedureType.FUNCTION : DBSProcedureType.PROCEDURE;
            return new MimerModuleRoutine(module, routineName, specificName, type);
        }
    }
}
