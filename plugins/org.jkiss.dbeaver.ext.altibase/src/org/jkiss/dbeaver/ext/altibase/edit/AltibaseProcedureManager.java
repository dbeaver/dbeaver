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
package org.jkiss.dbeaver.ext.altibase.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.altibase.model.AltibaseProcedureStandAlone;
import org.jkiss.dbeaver.ext.generic.edit.GenericProcedureManager;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

public class AltibaseProcedureManager extends GenericProcedureManager  {
    
    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return true;
    }
    
    @Override
    public boolean canEditObject(GenericProcedure object) {
        return true;
    }

    @Override
    protected void addObjectCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext,
                                          @NotNull List<DBEPersistAction> actions, @NotNull ObjectCreateCommand command, @NotNull Map<String, Object> options) {
        createOrReplaceProcedureQuery(actions, command.getObject());
    }

    @Override
    protected GenericProcedure createDatabaseObject(
        @NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, final Object container,
        Object from, @NotNull Map<String, Object> options) {
        return new AltibaseProcedureStandAlone(
                (GenericStructContainer) container,
                "NEW_PROCEDURE",
                DBSProcedureType.PROCEDURE);
    }

    @Override
    protected void addObjectModifyActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext,
                                          @NotNull List<DBEPersistAction> actionList, @NotNull ObjectChangeCommand command, @NotNull Map<String, Object> options) throws DBException {
        createOrReplaceProcedureQuery(actionList, command.getObject());
    }

    @Override
    protected void addObjectDeleteActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext,
                                          @NotNull List<DBEPersistAction> actions, @NotNull ObjectDeleteCommand command, @NotNull Map<String, Object> options) {
        
        final AltibaseProcedureStandAlone object = (AltibaseProcedureStandAlone) command.getObject();

        actions.add(new SQLDatabasePersistAction(
                "Drop procedure", "DROP " + object.getProcedureTypeName() + " " + object.getFullyQualifiedName(DBPEvaluationContext.DDL)));
    }
    
    @Override
    protected void validateObjectProperties(DBRProgressMonitor monitor, ObjectChangeCommand command,
            Map<String, Object> options) throws DBException {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Procedure name cannot be empty");
        }
        if (CommonUtils.isEmpty(command.getObject().getSource())) {
            throw new DBException("Procedure body cannot be empty");
        }
    }

    private void createOrReplaceProcedureQuery(List<DBEPersistAction> actions, GenericProcedure procedure) {
        actions.add(new SQLDatabasePersistAction("Create procedure", procedure.getSource()));
    }
}
