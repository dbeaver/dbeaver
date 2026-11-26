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
package org.jkiss.dbeaver.ext.doris.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.generic.edit.GenericTableColumnManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

public class DorisTableColumnManager extends GenericTableColumnManager {

    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) {
        if (command.hasProperty(DBConstants.PROP_ID_DESCRIPTION)) {
            addCommentAction(actions, command.getObject());
        }
    }

    @Override
    protected void addObjectExtraActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull NestedObjectCommand<GenericTableColumn, PropertyHandler> command,
        @NotNull Map<String, Object> options
    ) {
        GenericTableColumn column = command.getObject();
        if (!column.isPersisted() && CommonUtils.isNotEmpty(column.getDescription())) {
            addCommentAction(actions, column);
        }
    }

    static void addCommentAction(@NotNull List<DBEPersistAction> actions, @NotNull GenericTableColumn column) {
        actions.add(new SQLDatabasePersistAction(
            "Comment column",
            "ALTER TABLE " + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL)
                + " MODIFY COLUMN " + DBUtils.getQuotedIdentifier(column)
                + " COMMENT " + SQLUtils.quoteString(column, CommonUtils.notEmpty(column.getDescription()))
        ));
    }
}
