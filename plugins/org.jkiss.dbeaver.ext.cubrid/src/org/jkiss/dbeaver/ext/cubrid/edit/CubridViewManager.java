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
package org.jkiss.dbeaver.ext.cubrid.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.model.CubridView;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.edit.GenericViewManager;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

public class CubridViewManager extends GenericViewManager implements DBEObjectRenamer<GenericTableBase> {

    @NotNull
    @Override
    protected GenericTableBase createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @Nullable Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options) {
        GenericStructContainer structContainer = (GenericStructContainer) container;
        String tableName = getNewChildName(monitor, structContainer, SQLTableManager.BASE_VIEW_NAME);
        GenericTableBase viewImpl = structContainer.getDataSource().getMetaModel().createTableOrViewImpl(
                structContainer, tableName, GenericConstants.TABLE_TYPE_VIEW, null);
        if (viewImpl instanceof GenericView) {
            ((GenericView) viewImpl).setObjectDefinitionText("\n");
        }
        return viewImpl;
    }

    @Override
    protected void addObjectCreateActions(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBCExecutionContext executionContext,
            @NotNull List<DBEPersistAction> actions,
            @NotNull ObjectCreateCommand command,
            @NotNull Map<String, Object> options) {
        createOrReplaceViewQuery(actions, command);
    }

    @Override
    protected void addObjectModifyActions(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBCExecutionContext executionContext,
            @NotNull List<DBEPersistAction> actionList,
            @NotNull ObjectChangeCommand command,
            @NotNull Map<String, Object> options) {
        createOrReplaceViewQuery(actionList, command);
    }

    private void createOrReplaceViewQuery(
            @NotNull List<DBEPersistAction> actions,
            @NotNull DBECommandComposite<GenericTableBase, PropertyHandler> command) {
        CubridView view = (CubridView) command.getObject();
        StringBuilder query = new StringBuilder(200);
        String viewDDL = view.getDDL();
        boolean hasComment = command.hasProperty(DBConstants.PROP_ID_DESCRIPTION);
        if (viewDDL == null) {
            viewDDL = "";
        }
        if (!view.isPersisted()) {
            query.append("CREATE VIEW " + view.getUniqueName() + "\nAS ");
            query.append(viewDDL);
            if (hasComment && view.getDescription() != null) {
                query.append("\nCOMMENT = " + SQLUtils.quoteString(view, CommonUtils.notEmpty(view.getDescription())));
            }
        } else {
            if (command.hasProperty(DBConstants.PARAM_OBJECT_DEFINITION_TEXT)) {
                query.append(viewDDL).append("\n");
            }
            if (hasComment && view.getDescription() != null) {
                query.append("ALTER VIEW " + view.getUniqueName()
                        + " COMMENT = " + SQLUtils.quoteString(view, CommonUtils.notEmpty(view.getDescription())));
            }
        }
        actions.add(new SQLDatabasePersistAction("Create view", query.toString()));
    }

    @Override
    protected void addObjectRenameActions(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBCExecutionContext executionContext,
            @NotNull List<DBEPersistAction> actions,
            @NotNull ObjectRenameCommand command,
            @NotNull Map<String, Object> options) {
        CubridView view = (CubridView) command.getObject();
        actions.add(new SQLDatabasePersistAction(
                "Rename table",
                "RENAME VIEW " + view.getContainer() + "." + command.getOldName() + " TO " + command.getNewName()));
    }

    @Override
    public void renameObject(
            @NotNull DBECommandContext commandContext,
            @NotNull GenericTableBase object,
            @NotNull Map<String, Object> options,
            @NotNull String newName)
            throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    @Override
    protected void addObjectExtraActions(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBCExecutionContext executionContext,
            @NotNull List<DBEPersistAction> actions,
            @NotNull NestedObjectCommand<GenericTableBase, PropertyHandler> command,
            @NotNull Map<String, Object> options) {
        CubridView view = (CubridView) command.getObject();
        if (view.isPersisted() && view.getContainer() != view.getSchema()) {
            actions.add(new SQLDatabasePersistAction(
                    "Change Owner",
                    "ALTER VIEW " + view.getContainer() + "." + view.getName() + " OWNER TO " + view.getSchema()));
        }
    }
}
