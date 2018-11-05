/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.ui.PostgreCreateDatabaseDialog;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistActionAtomic;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * PostgreDatabaseManager
 */
public class PostgreDatabaseManager extends SQLObjectEditor<PostgreDatabase, PostgreDataSource> implements DBEObjectRenamer<PostgreDatabase> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Nullable
    @Override
    public DBSObjectCache<PostgreDataSource, PostgreDatabase> getObjectsCache(PostgreDatabase object)
    {
        return object.getDataSource().getDatabaseCache();
    }

    @Override
    public void deleteObject(DBECommandContext commandContext, PostgreDatabase object, Map<String, Object> options) throws DBException {
        if (object == object.getDataSource().getDefaultInstance()) {
            throw new DBException("Cannot drop the currently open database." +
                "\nSwitch to another database and try again\n(Note: enable '" + PostgreMessages.dialog_setting_connection_nondefaultDatabase + "' option to see them).");
        }
        super.deleteObject(commandContext, object, options);
    }

    @Override
    protected PostgreDatabase createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, PostgreDataSource parent, Object copyFrom)
    {
        return new UITask<PostgreDatabase>() {
            @Override
            protected PostgreDatabase runTask() {
                PostgreCreateDatabaseDialog dialog = new PostgreCreateDatabaseDialog(UIUtils.getActiveWorkbenchShell(), parent);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                try {
                    return new PostgreDatabase(monitor, parent, dialog.getName(), dialog.getOwner(), dialog.getTemplateName(), dialog.getTablespace(), dialog.getEncoding());
                } catch (DBException e) {
                    // Never be here
                    return null;
                }
            }
        }.execute();
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
    {
        final PostgreDatabase database = command.getObject();
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE DATABASE ").append(DBUtils.getQuotedIdentifier(database));

        if (database.getInitialOwner() != null) {
            sql.append("\nOWNER = ").append(database.getInitialOwner().getName());
        }
        if (!CommonUtils.isEmpty(database.getTemplateName())) {
            sql.append("\nTEMPLATE = ").append(database.getTemplateName());
        }
        if (database.getInitialEncoding() != null) {
            sql.append("\nENCODING = '").append(database.getInitialEncoding().getName()).append("'");
        }
        if (database.getInitialTablespace() != null) {
            sql.append("\nTABLESPACE = ").append(database.getInitialTablespace().getName());
        }
        actions.add(new CreateDatabaseAction(database, sql));
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        actions.add(new DeleteDatabaseAction(command));
    }

    @Override
    public void renameObject(DBECommandContext commandContext, PostgreDatabase database, String newName) throws DBException
    {
        processObjectRename(commandContext, database, newName);
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options)
    {
        actions.add(
            new SQLDatabasePersistAction(
                "Rename database",
                "ALTER DATABASE " + DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getOldName()) + //$NON-NLS-1$
                    " RENAME TO " + DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getNewName())) //$NON-NLS-1$
        );
    }

    private static class DeleteDatabaseAction extends SQLDatabasePersistActionAtomic {

        private final PostgreDatabase database;

        public DeleteDatabaseAction(ObjectDeleteCommand command) {
            super("Drop database", "DROP DATABASE " + DBUtils.getQuotedIdentifier(command.getObject()));
            database = command.getObject();
        }

        @Override
        public void beforeExecute(DBCSession session) throws DBCException {
            super.beforeExecute(session);
            database.shutdown(session.getProgressMonitor());
        }
    }

    private static class CreateDatabaseAction extends SQLDatabasePersistActionAtomic {
        private final PostgreDatabase database;
        public CreateDatabaseAction(PostgreDatabase database, StringBuilder sql) {
            super("Create database", sql.toString());
            this.database = database;
        }

        @Override
        public void afterExecute(DBCSession session, Throwable error) throws DBCException {
            super.afterExecute(session, error);
            if (error == null) {
                try {
                    database.initializeMetaContext(session.getProgressMonitor());
                } catch (DBException e) {
                    log.error("Can't connect to the new database");
                }
            }
        }
    }
}

