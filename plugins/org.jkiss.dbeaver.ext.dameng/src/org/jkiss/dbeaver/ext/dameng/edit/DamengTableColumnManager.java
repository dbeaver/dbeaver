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

package org.jkiss.dbeaver.ext.dameng.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dameng.model.DamengTableColumn;
import org.jkiss.dbeaver.ext.generic.edit.GenericTableColumnManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * @author Shengkai Bai
 */
public class DamengTableColumnManager extends GenericTableColumnManager implements DBEObjectRenamer<GenericTableColumn> {

    @Override
    protected void addObjectRenameActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull SQLObjectEditor<GenericTableColumn, GenericTableBase>.ObjectRenameCommand command, @NotNull Map<String, Object> options) {
        final GenericTableColumn column = command.getObject();
        actions.add(
                new SQLDatabasePersistAction(
                        "Rename column",
                        "ALTER TABLE " + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + " RENAME "
                                + "COLUMN " +
                                DBUtils.getQuotedIdentifier(column.getDataSource(), command.getOldName()) +
                                " TO " + DBUtils.getQuotedIdentifier(column.getDataSource(), command.getNewName())));
    }

    @Override
    public void renameObject(DBECommandContext commandContext, GenericTableColumn object, Map<String, Object> options, String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    @Override
    protected void addObjectModifyActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actionList, @NotNull SQLObjectEditor<GenericTableColumn, GenericTableBase>.ObjectChangeCommand command, @NotNull Map<String, Object> options) throws DBException {
        final DamengTableColumn column = (DamengTableColumn) command.getObject();
        actionList.add(
                new SQLDatabasePersistAction(
                        "Modify column",
                        "ALTER TABLE " + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + " MODIFY " + getNestedDeclaration(monitor, column.getTable(), command, options)));
        if (command.getProperty(DBConstants.PROP_ID_DESCRIPTION) != null) {
            addColumnCommentAction(actionList, column, column.getTable());
        }

    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, SQLObjectEditor<GenericTableColumn, GenericTableBase>.ObjectCreateCommand command, Map<String, Object> options) throws DBException {
        super.addObjectCreateActions(monitor, executionContext, actions, command, options);
        if (CommonUtils.isNotEmpty(command.getObject().getDescription())) {
            addColumnCommentAction(actions, command.getObject(), command.getObject().getParentObject());
        }
    }
}
