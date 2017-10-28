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
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.ui.PostgreCreateDatabaseDialog;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistActionAtomic;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * PostgreDatabaseManager
 */
public class PostgreDatabaseManager extends SQLObjectEditor<PostgreDatabase, PostgreDataSource> implements DBEObjectRenamer<PostgreDatabase> {

    @Override
    public long getMakerOptions()
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
    protected PostgreDatabase createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, PostgreDataSource parent, Object copyFrom)
    {
        return new UITask<PostgreDatabase>() {
            @Override
            protected PostgreDatabase runTask() {
                PostgreCreateDatabaseDialog dialog = new PostgreCreateDatabaseDialog(DBeaverUI.getActiveWorkbenchShell(), parent);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                return new PostgreDatabase(parent, dialog.getName(), dialog.getOwner(), dialog.getTemplateName(), dialog.getTablespace(), dialog.getEncoding());
            }
        }.execute();
    }

    @Override
    protected void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
    {
        final PostgreDatabase database = command.getObject();
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE DATABASE ").append(DBUtils.getQuotedIdentifier(database));
        try {
            VoidProgressMonitor monitor = new VoidProgressMonitor();
            if (database.getDBA(monitor) != null) {
                sql.append("\nOWNER = ").append(database.getDBA(monitor).getName());
            }
            if (!CommonUtils.isEmpty(database.getTemplateName())) {
                sql.append("\nTEMPLATE = ").append(database.getTemplateName());
            }
            if (database.getDefaultEncoding(monitor) != null) {
                sql.append("\nENCODING = '").append(database.getDefaultEncoding(monitor).getName()).append("'");
            }
            if (database.getDefaultTablespace(monitor) != null) {
                sql.append("\nTABLESPACE = ").append(database.getDefaultTablespace(monitor).getName());
            }
        } catch (DBException e) {
            log.error(e);
        }
        actions.add(
            new SQLDatabasePersistActionAtomic("Create database", sql.toString())
        );
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        actions.add(
            new SQLDatabasePersistActionAtomic("Drop database", "DROP DATABASE " + DBUtils.getQuotedIdentifier(command.getObject())) //$NON-NLS-2$
        );
    }

    @Override
    public void renameObject(DBECommandContext commandContext, PostgreDatabase catalog, String newName) throws DBException
    {
        throw new DBException("Direct database rename is not yet implemented in PostgreSQL. You should use export/import functions for that.");
    }

}

