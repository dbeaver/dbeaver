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

package org.jkiss.dbeaver.ext.gbase8s.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.gbase8s.GBase8sConstants;
import org.jkiss.dbeaver.ext.generic.edit.GenericTableManager;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
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

/**
 * @author Chao Tian
 */
public class GBase8sTableManager extends GenericTableManager implements DBEObjectRenamer<GenericTableBase> {

    @Override
    public boolean canEditObject(GenericTableBase object) {
        return true;
    }

    @Override
    public boolean canRenameObject(GenericTableBase object) {
        return false;
    }

    @Override
    protected boolean isIncludeDropInDDL(GenericTableBase table) {
        return false;
    }

    @Override
    public void renameObject(DBECommandContext commandContext, GenericTableBase object, Map<String, Object> options,
            String newName) throws DBException {
        if (object.isView()) {
            throw new DBException("View rename is not supported");
        }
        processObjectRename(commandContext, object, options, newName);
    }

    @Override
    protected void addStructObjectCreateActions(
            DBRProgressMonitor monitor,
            DBCExecutionContext executionContext,
            List<DBEPersistAction> actions,
            StructCreateCommand command,
            Map<String, Object> options) throws DBException {
        super.addStructObjectCreateActions(monitor, executionContext, actions, command, options);
        // Add a table comment
        DBEPersistAction commentAction = buildTableCommentAction(command.getObject());
        if (commentAction != null) {
            actions.add(commentAction);
        }
        for (GenericTableColumn column : command.getObject().getAttributes(monitor)) {
            // Add a column comment
            DBEPersistAction action = buildColumnCommentAction(column);
            if (action != null) {
                actions.add(action);
            }
        }
    }

    @Override
    protected void addObjectRenameActions(@NotNull DBRProgressMonitor monitor,
            @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions,
            @NotNull SQLObjectEditor<GenericTableBase, GenericStructContainer>.ObjectRenameCommand command,
            @NotNull Map<String, Object> options) {
        final GenericDataSource dataSource = command.getObject().getDataSource();
        actions.add(
                new SQLDatabasePersistAction("Rename table",
                        "ALTER TABLE "
                                + (command.getObject().getSchema() != null
                                        ? DBUtils.getQuotedIdentifier(dataSource,
                                                command.getObject().getSchema().getName()) + "."
                                        : "")
                                + DBUtils.getQuotedIdentifier(dataSource, command.getOldName()) + " RENAME TO "
                                + DBUtils.getQuotedIdentifier(dataSource, command.getNewName())));
    }

    private DBEPersistAction buildTableCommentAction(GenericTableBase table) {
        String description = table.getDescription();
        if (CommonUtils.isNotEmpty(description)) {
            String tableName = table.getFullyQualifiedName(DBPEvaluationContext.DDL);
            String commentSQL = String.format(GBase8sConstants.SQL_TABLE_COMMENT, tableName, description);
            return new SQLDatabasePersistAction("Comment on Table", commentSQL);
        }
        return null;
    }

    private DBEPersistAction buildColumnCommentAction(GenericTableColumn column) {
        String description = column.getDescription();
        if (CommonUtils.isNotEmpty(description)) {
            String tableName = column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL);
            String columnName = column.getName();
            String commentSQL = String.format(GBase8sConstants.SQL_COLUMN_COMMENT, tableName, columnName, description);
            return new SQLDatabasePersistAction("Comment on Column", commentSQL);
        }
        return null;
    }
}
