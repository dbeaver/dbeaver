/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.cubrid.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.model.CubridDataSource;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTable;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTrigger;
import org.jkiss.dbeaver.ext.generic.edit.GenericTriggerManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

public class CubridTriggerManager extends GenericTriggerManager<CubridTrigger> implements DBEObjectRenamer<CubridTrigger> {

    public static final String BASE_TRIGGER_NAME = "new_trigger";

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        CubridTable table = (CubridTable) container;
        boolean isShard = table.getDataSource().isShard();
        return !isShard && container instanceof GenericTableBase;
    }

    @Override
    protected CubridTrigger createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        Object container,
        Object copyFrom,
        @NotNull Map<String, Object> options
    ) throws DBException {
        return new CubridTrigger((GenericTableBase) container, BASE_TRIGGER_NAME, monitor);
    }

    public void createTrigger(CubridTrigger trigger, StringBuilder sb) {
        sb.append("\n").append(trigger.getActionTime()).append(" ");
        if (trigger.getEvent().equals("COMMIT") || trigger.getEvent().equals("ROLLBACK")) {
            sb.append(trigger.getEvent());
        } else {
            sb.append(trigger.getEvent());
            sb.append(" ON ").append(trigger.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL));
            if (trigger.getEvent().contains("UPDATE") && trigger.getTargetColumn() != null) {
                sb.append("(" + DBUtils.getQuotedIdentifier(trigger.getDataSource(), trigger.getTargetColumn()) + ")");
            }
            if (trigger.getCondition() != null) {
                sb.append("\nIF ").append(trigger.getCondition());
            }
        }
        sb.append("\nEXECUTE ");
        if (trigger.getActionType().equals("REJECT") || trigger.getActionType().equals("INVALIDATE TRANSACTION")) {
            sb.append(trigger.getActionType());
        } else if (trigger.getActionType().equals("PRINT")) {
            sb.append(trigger.getActionType()).append(" ");
            sb.append(trigger.getActionDefinition() == null ? "" : SQLUtils.quoteString(trigger, trigger.getActionDefinition()));
        } else {
            sb.append(trigger.getActionDefinition() == null ? "" : trigger.getActionDefinition());
        }
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) {
        CubridTrigger trigger = command.getObject();
        CubridDataSource dataSource = (CubridDataSource) trigger.getDataSource();
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TRIGGER ").append(trigger.getFullyQualifiedName(DBPEvaluationContext.DDL));
        sb.append(trigger.getActive() ? "\nSTATUS ACTIVE" : "\nSTATUS INACTIVE");
        sb.append("\nPRIORITY ").append(trigger.getPriority());
        createTrigger(trigger, sb);
        if (trigger.getDescription() != null) {
            sb.append("\nCOMMENT ").append(SQLUtils.quoteString(trigger, trigger.getDescription()));
        }
        actions.add(new SQLDatabasePersistAction("Create Trigger", sb.toString()));

        if (!dataSource.getSupportMultiSchema() && !trigger.getOwner().getName().equalsIgnoreCase(dataSource.getCurrentUser())) {
            actions.add(new SQLDatabasePersistAction(
                    "Change Owner",
                    "ALTER TRIGGER " + DBUtils.getQuotedIdentifier(trigger.getDataSource(), trigger.getName()) + " OWNER TO "
                    + DBUtils.getQuotedIdentifier(trigger.getOwner())));
        }
    }

    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actionList,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) {
        CubridTrigger trigger = command.getObject();
        String triggerName = trigger.getFullyQualifiedName(DBPEvaluationContext.DDL);

        if (command.hasProperty("active")) {
            actionList.add(new SQLDatabasePersistAction(
                "ALTER TRIGGER " + triggerName + " STATUS "
                + (trigger.getActive() ? "ACTIVE" : "INACTIVE")
            ));
        }
        if (command.hasProperty("priority")) {
            actionList.add(new SQLDatabasePersistAction(
                "ALTER TRIGGER " + triggerName + " PRIORITY " + trigger.getPriority()
            ));
        }
        if (command.hasProperty("description")) {
            actionList.add(new SQLDatabasePersistAction(
                "ALTER TRIGGER " + triggerName + " COMMENT "
                + SQLUtils.quoteString(trigger, CommonUtils.notEmpty(trigger.getDescription()))
            ));
        }
    }

    @Override
    protected void addObjectRenameActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectRenameCommand command,
        @NotNull Map<String, Object> options
    ) {
        CubridTrigger trigger = (CubridTrigger) command.getObject();
        boolean isSupportMultiSchema = ((CubridDataSource) trigger.getDataSource()).getSupportMultiSchema();
        String schemaName = isSupportMultiSchema ? DBUtils.getQuotedIdentifier(trigger.getOwner()) + "." : "";
        actions.add(new SQLDatabasePersistAction(
            "Rename Trigger",
            "RENAME TRIGGER " + schemaName + DBUtils.getQuotedIdentifier(trigger.getDataSource(), command.getOldName())
            + " AS " + schemaName + DBUtils.getQuotedIdentifier(trigger.getDataSource(), command.getNewName())
        ));
    }

    @Override
    public void renameObject(
        @NotNull DBECommandContext commandContext,
        @NotNull CubridTrigger object,
        @NotNull Map<String, Object> options,
        @NotNull String newName
    ) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    public boolean canEditObject(CubridTrigger object) {
        return !((CubridDataSource) object.getDataSource()).isShard();
    }

    @Override
    public boolean canDeleteObject(CubridTrigger object) {
        return !((CubridDataSource) object.getDataSource()).isShard();
    }
}
