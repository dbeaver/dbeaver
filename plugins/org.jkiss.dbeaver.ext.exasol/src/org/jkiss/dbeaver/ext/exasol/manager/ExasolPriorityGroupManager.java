/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2019 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.manager;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.ExasolMessages;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.ext.exasol.model.ExasolPriorityGroup;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.ext.exasol.ui.ExasolPriorityGroupDialog;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.List;
import java.util.Map;

public class ExasolPriorityGroupManager extends SQLObjectEditor<ExasolPriorityGroup, ExasolDataSource> implements DBEObjectRenamer<ExasolPriorityGroup> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Override
    public DBSObjectCache<ExasolDataSource, ExasolPriorityGroup> getObjectsCache(ExasolPriorityGroup object) {
        return object.getDataSource().getPriorityGroupCache();
    }

    @Override
    protected ExasolPriorityGroup createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
                                                       Object container, Object copyFrom, Map<String, Object> options) throws DBException {
        ExasolPriorityGroup group = new ExasolPriorityGroup((ExasolDataSource) container, "PG", null, 0);
        return new UITask<ExasolPriorityGroup>() {
            @Override
            protected ExasolPriorityGroup runTask() {
                ExasolPriorityGroupDialog dialog = new ExasolPriorityGroupDialog(UIUtils.getActiveWorkbenchShell(), group);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                group.setName(dialog.getName());
                group.setDescription(dialog.getComment());
                group.setWeight(dialog.getWeight());
                return group;
            }
        }.execute();
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions,
                                          ObjectCreateCommand command,
                                          Map<String, Object> options) {
        final ExasolPriorityGroup group = command.getObject();

        String script = String.format("CREATE PRIORITY GROUP %s WITH WEIGHT = %d", DBUtils.getQuotedIdentifier(group), group.getWeight());

        actions.add(new SQLDatabasePersistAction(ExasolMessages.manager_priority_create, script));

        if (!group.getDescription().isEmpty()) {
            actions.add(getCommentCommand(group));
        }
    }

    private SQLDatabasePersistAction getCommentCommand(ExasolPriorityGroup group) {
        return new SQLDatabasePersistAction(
            ExasolMessages.manager_priority_group_comment,
            String.format("COMMENT ON PRIORITY GROUP %s is '%s'",
                DBUtils.getQuotedIdentifier(group),
                ExasolUtils.quoteString(group.getDescription())
            )
        );
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList,
                                          ObjectChangeCommand command,
                                          Map<String, Object> options) throws DBException {
        ExasolPriorityGroup group = command.getObject();

        Map<Object, Object> com = command.getProperties();

        if (com.containsKey("description")) {
            actionList.add(
                getCommentCommand(group)
            );
        }

        if (com.containsKey("weight")) {
            String script = String.format("ALTER PRIORITY GROUP %s SET WEIGHT = %d", DBUtils.getQuotedIdentifier(group), group.getWeight());
            actionList.add(new SQLDatabasePersistAction(ExasolMessages.manager_priority_alter, script));
        }
    }


    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions,
                                          ObjectDeleteCommand command,
                                          Map<String, Object> options) {

        ExasolPriorityGroup group = command.getObject();

        String script = String.format("DROP PRIORITY GROUP %s", DBUtils.getQuotedIdentifier(group));

        actions.add(new SQLDatabasePersistAction(ExasolMessages.manager_priority_drop, script));
    }

    @Override
    public void renameObject(DBECommandContext commandContext, ExasolPriorityGroup object, String newName)
        throws DBException {
        processObjectRename(commandContext, object, newName);
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions,
                                          ObjectRenameCommand command,
                                          Map<String, Object> options) {
        // TODO Auto-generated method stub
        ExasolPriorityGroup group = command.getObject();

        String script = String.format(
            "RENAME PRIORITY GROUP %s to %s",
            DBUtils.getQuotedIdentifier(group.getDataSource(), command.getOldName()),
            DBUtils.getQuotedIdentifier(group.getDataSource(), command.getNewName())
        );
        actions.add(new SQLDatabasePersistAction(ExasolMessages.manager_priority_rename, script));
    }


}
