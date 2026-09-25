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
import org.jkiss.dbeaver.ext.generic.model.GenericFunctionResultType;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * An instance/static/constructor method of a Mimer SQL user-defined type, read from {@code
 * INFORMATION_SCHEMA.ROUTINES} (real catalog {@code ROUTINE_TYPE} is {@code INSTANCE METHOD}/
 * {@code STATIC METHOD}/{@code CONSTRUCTOR METHOD}, not {@code FUNCTION}/{@code PROCEDURE} - see
 * {@link MimerUtils#loadProcedureColumns(GenericProcedure, DBRProgressMonitor, String)}). Always
 * reports {@link DBSProcedureType#FUNCTION} to the base class since every method has a return
 * value, same as a function - and always {@link GenericFunctionResultType#NO_TABLE}, since a
 * method (per the SQL standard) always returns exactly one scalar value, never a table. Read-only
 * for now, same reasoning as {@link MimerModuleRoutine} -
 * no manager registered, not implementing {@code DBSObjectWithScript} (so the Source tab renders
 * non-editable, matching the "no create/alter/drop yet" scope of this whole feature).
 *
 * @author Mimer Information Technology
 */
public class MimerUdtMethod extends GenericProcedure implements DBPScriptObject {

    private final MimerUserDefinedType type;
    private final String methodKind;
    private final UsedByCache usedByCache = new UsedByCache();
    private final UsesCache usesCache = new UsesCache();

    public MimerUdtMethod(
        @NotNull MimerUserDefinedType type,
        @NotNull String methodName,
        String specificName,
        @NotNull String methodKind
    ) {
        super(type.getSchema(), methodName, specificName, null, DBSProcedureType.FUNCTION, GenericFunctionResultType.NO_TABLE);
        this.type = type;
        this.methodKind = methodKind;
    }

    @NotNull
    public MimerUserDefinedType getType() {
        return type;
    }

    @Property(viewable = true, order = 7)
    public String getMethodKind() {
        return methodKind;
    }

    /**
     * Loads the real IN parameters (if any) plus a synthetic {@code RETURN} column for the
     * method's own result type - unlike a plain function, {@code INFORMATION_SCHEMA.PARAMETERS}
     * carries no ordinal-0 row for a method's return type (see {@link
     * MimerUtils#loadMethodReturnType}).
     */
    @Override
    public void loadProcedureColumns(@NotNull DBRProgressMonitor monitor) throws DBException {
        MimerUtils.loadProcedureColumns(this, monitor, methodKind);
        MimerUtils.loadMethodReturnType(this, monitor);
    }

    /**
     * Keyed by {@code SPECIFIC_NAME} in {@code EXT_SOURCE_DEFINITION}, not {@link #getName()} -
     * Mimer SQL allows overloading a method name by parameter list, same as a plain schema-level
     * procedure/function (confirmed live - see {@code MimerMetaModel#getProcedureDDL}), and
     * {@code OBJECT_NAME} alone would return every overload's body concatenated together.
     */
    @NotNull
    @Override
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options) throws DBException {
        return MimerUtils.readSourceDefinition(monitor, this, getContainer().getName(), getUniqueName(), methodKind, "SPECIFIC_NAME");
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

    static class UsedByCache extends JDBCObjectCache<MimerUdtMethod, MimerObjectUsedBy> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerUdtMethod owner) throws SQLException {
            return MimerObjectUsedBy.prepareUsedByStatementBySpecificName(session, owner.getSchema().getName(), owner.getUniqueName(), owner.methodKind);
        }

        @Override
        protected MimerObjectUsedBy fetchObject(@NotNull JDBCSession session, @NotNull MimerUdtMethod owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerObjectUsedBy(owner, resultSet);
        }
    }

    static class UsesCache extends JDBCObjectCache<MimerUdtMethod, MimerObjectUses> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerUdtMethod owner) throws SQLException {
            return MimerObjectUses.prepareUsesStatementBySpecificName(session, owner.getSchema().getName(), owner.getUniqueName(), owner.methodKind);
        }

        @Override
        protected MimerObjectUses fetchObject(@NotNull JDBCSession session, @NotNull MimerUdtMethod owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerObjectUses(owner, resultSet);
        }
    }
}
