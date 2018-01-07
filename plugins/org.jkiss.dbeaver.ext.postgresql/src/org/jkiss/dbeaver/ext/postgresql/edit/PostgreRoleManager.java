/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreRole;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.ui.PostgreCreateRoleDialog;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * PostgreRoleManager
 */
public class PostgreRoleManager extends SQLObjectEditor<PostgreRole, PostgreDatabase> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, PostgreRole> getObjectsCache(PostgreRole object)
    {
        return null;
    }

    @Override
    protected PostgreRole createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, PostgreDatabase parent, Object copyFrom) throws DBException {
        return new UITask<PostgreRole>() {
            @Override
            protected PostgreRole runTask() {
                PostgreCreateRoleDialog dialog = new PostgreCreateRoleDialog(DBeaverUI.getActiveWorkbenchShell(), parent);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                return new PostgreRole(parent, dialog.getName(), dialog.getPassword(), dialog.isUser());
            }
        }.execute();
    }

    @Override
    protected void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) {
        final PostgreRole role = command.getObject();
        final StringBuilder script = new StringBuilder("CREATE ROLE " + DBUtils.getQuotedIdentifier(role));
        addRoleOptions(script, role, true);

        actions.add(
            new SQLDatabasePersistAction("Create role", script.toString()) //$NON-NLS-2$
        );
    }

    @Override
    protected void addObjectModifyActions(List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) {
        final PostgreRole role = command.getObject();
        final StringBuilder script = new StringBuilder("ALTER ROLE " + DBUtils.getQuotedIdentifier(role));
        addRoleOptions(script, role, false);

        actionList.add(
            new SQLDatabasePersistAction("Alter role", script.toString()) //$NON-NLS-2$
        );
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
        actions.add(
            new SQLDatabasePersistAction("Drop role", "DROP ROLE " + DBUtils.getQuotedIdentifier(command.getObject())) //$NON-NLS-2$
        );
    }

    private void addRoleOptions(StringBuilder script, PostgreRole role, boolean create) {
        if (role.isSuperUser()) script.append(" SUPERUSER"); else script.append(" NOSUPERUSER");
        if (role.isCreateDatabase()) script.append(" CREATEDB"); else script.append(" NOCREATEDB");
        if (role.isCreateRole()) script.append(" CREATEROLE"); else script.append(" NOCREATEROLE");
        if (role.isInherit()) script.append(" INHERIT"); else script.append(" NOINHERIT");
        if (role.isCanLogin()) script.append(" LOGIN"); else script.append(" NOLOGIN");

        if (create && role.isUser() && !CommonUtils.isEmpty(role.getPassword())) {
            script.append(" PASSWORD ").append("'").append(role.getDataSource().getSQLDialect().escapeString(role.getPassword())).append("'");
        }
        //if (role.isCreateDatabase()) script.append(" CONNECTION LIMIT connlimit");
/*
PASSWORD password
VALID UNTIL 'timestamp'
*/
    }


}

