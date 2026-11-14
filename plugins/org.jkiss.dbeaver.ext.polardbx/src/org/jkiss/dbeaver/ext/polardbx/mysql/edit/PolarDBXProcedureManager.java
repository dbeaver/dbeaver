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
package org.jkiss.dbeaver.ext.polardbx.mysql.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLProcedure;
import org.jkiss.dbeaver.ext.polardbx.mysql.model.PolarDBXProcedure;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * PolarDBXProcedureManager - PolarDB-X dedicated stored procedure manager.
 *
 * Core feature: creates PolarDBXProcedure objects instead of MySQLProcedure,
 * ensuring the PolarDBX-specific getFullyQualifiedName() implementation is used.
 */
public class PolarDBXProcedureManager extends SQLObjectEditor<MySQLProcedure, MySQLCatalog> {

    @Override
    @Nullable
    public DBSObjectCache<MySQLCatalog, MySQLProcedure> getObjectsCache(@NotNull MySQLProcedure object) {
        return object.getContainer().getProceduresCache();
    }

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(
        @NotNull DBRProgressMonitor monitor,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Procedure name cannot be empty");
        }
        if (CommonUtils.isEmpty(command.getObject().getDeclaration())) {
            throw new DBException("Procedure body cannot be empty");
        }
    }

    /**
     * Core method: creates a PolarDBXProcedure instead of a MySQLProcedure.
     * This ensures the PolarDBX-specific getFullyQualifiedName() implementation is used.
     */
    @Override
    @NotNull
    protected MySQLProcedure createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull final Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        return new PolarDBXProcedure((MySQLCatalog) container);
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) {
        createOrReplaceProcedureQuery(actions, command.getObject());
    }

    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actionList,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) {
        createOrReplaceProcedureQuery(actionList, command.getObject());
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(
            new SQLDatabasePersistAction("Drop procedure",
                "DROP " + command.getObject().getProcedureType() + " " +
                command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL))
        );
    }

    private void createOrReplaceProcedureQuery(
        @NotNull List<DBEPersistAction> actions,
        @NotNull MySQLProcedure procedure
    ) {
        actions.add(
            new SQLDatabasePersistAction(
                "Drop procedure",
                "DROP " + procedure.getProcedureType() + " IF EXISTS "
                    + procedure.getFullyQualifiedName(DBPEvaluationContext.DDL)));
        actions.add(
            new SQLDatabasePersistAction("Create procedure", procedure.getDeclaration(), true));
    }
}
