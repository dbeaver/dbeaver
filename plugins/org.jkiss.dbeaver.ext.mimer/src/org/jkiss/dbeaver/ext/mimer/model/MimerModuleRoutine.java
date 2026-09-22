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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

import java.sql.SQLException;
import java.util.Collection;

/**
 * A procedure/function declared inside a Mimer SQL module ({@code DECLARE PROCEDURE}/{@code
 * DECLARE FUNCTION} inside a {@code CREATE MODULE}) - see {@link MimerModule#getRoutines}.
 * Mimer SQL has no way to add, alter, or drop a single routine independently of the module's own
 * {@code CREATE MODULE} text, so this must be genuinely read-only.
 * <p>
 * Deliberately a separate class from {@link MimerProcedure} rather than that class plus a
 * "belongs to a module" flag: the Source tab's editability is gated by {@code
 * GenericSourceViewEditor#isReadOnly()} checking {@code instanceof DBSObjectWithScript}, a
 * class-level check that can't be conditional per instance, and {@code MimerProcedure} already
 * implements that interface for its own standalone editing. Not implementing it here renders the
 * tab read-only for free via the existing {@code ext.generic.ui} registration, and no manager is
 * registered for this class, so no create/edit/drop actions appear either.
 *
 * @author Mimer Information Technology
 */
public class MimerModuleRoutine extends GenericProcedure {

    private final MimerModule module;
    private final UsedByCache usedByCache = new UsedByCache();
    private final UsesCache usesCache = new UsesCache();

    public MimerModuleRoutine(
        @NotNull MimerModule module,
        @NotNull String procedureName,
        String specificName,
        @NotNull DBSProcedureType procedureType
    ) {
        super(module.getSchema(), procedureName, specificName, null, procedureType, null);
        this.module = module;
    }

    @NotNull
    public MimerModule getModule() {
        return module;
    }

    /**
     * Overridden to the real owning module, not the schema {@code super(...)} was constructed
     * with as its container. {@code AbstractProcedure#getParentObject()} just returns {@code
     * container} unchanged, but the container here is deliberately the schema - needed for
     * {@code getSchema()}/parameter-loading queries that key off it - while the real navigator
     * tree position is Schema -&gt; Modules -&gt; Module -&gt; Procedures/Functions -&gt; this
     * routine.
     * <p>
     * This override matters because {@link org.jkiss.dbeaver.model.DBUtils#getObjectPath} walks
     * {@code getParentObject()} to rebuild a path back to the root, when the navigator needs to
     * resolve/open an object with no existing tree node yet - e.g. opening the real target of a
     * "Used By"/"Uses" row for a module-declared routine, whose lookup ({@link
     * MimerUtils#resolveDependencyObject}) reads the module's routine cache directly and never
     * needed a navigator node. Without this override, that walk would skip the module entirely
     * and land one level too high, where no schema-level folder is typed for a {@link
     * MimerModuleRoutine} (the schema's own Procedures/Functions folders are typed {@link
     * MimerProcedure}, a sibling class, not this one). The walk would then dead-end silently -
     * {@code NavigatorHandlerObjectOpen#openEntityEditor} just returns {@code null} on failure,
     * with no error shown anywhere - so the row would look correct but clicking it would do
     * nothing.
     */
    @Override
    public DBSObject getParentObject() {
        return module;
    }

    @Override
    public void loadProcedureColumns(@NotNull DBRProgressMonitor monitor) throws DBException {
        MimerUtils.loadProcedureColumns(this, monitor);
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
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        usedByCache.clearCache();
        usesCache.clearCache();
        return super.refreshObject(monitor);
    }

    static class UsedByCache extends JDBCObjectCache<MimerModuleRoutine, MimerObjectUsedBy> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerModuleRoutine owner) throws SQLException {
            String objectType = owner.getProcedureType() == DBSProcedureType.FUNCTION ? "FUNCTION" : "PROCEDURE";
            return MimerObjectUsedBy.prepareUsedByStatementBySpecificName(session, owner.getSchema().getName(), owner.getUniqueName(), objectType);
        }

        @Override
        protected MimerObjectUsedBy fetchObject(@NotNull JDBCSession session, @NotNull MimerModuleRoutine owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerObjectUsedBy(owner, resultSet);
        }
    }

    static class UsesCache extends JDBCObjectCache<MimerModuleRoutine, MimerObjectUses> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerModuleRoutine owner) throws SQLException {
            String objectType = owner.getProcedureType() == DBSProcedureType.FUNCTION ? "FUNCTION" : "PROCEDURE";
            return MimerObjectUses.prepareUsesStatementBySpecificName(session, owner.getSchema().getName(), owner.getUniqueName(), objectType);
        }

        @Override
        protected MimerObjectUses fetchObject(@NotNull JDBCSession session, @NotNull MimerModuleRoutine owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerObjectUses(owner, resultSet);
        }
    }
}
