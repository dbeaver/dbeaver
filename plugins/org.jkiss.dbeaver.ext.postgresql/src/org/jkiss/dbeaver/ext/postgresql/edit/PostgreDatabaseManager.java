/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;

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
/*
        PostgreCreateSchemaDialog dialog = new PostgreCreateSchemaDialog(DBeaverUI.getActiveWorkbenchShell(), parent);
        if (dialog.open() != IDialogConstants.OK_ID) {
            return null;
        }
        return new PostgreSchema(parent, dialog.getName(), dialog.getOwner());
*/
        return null;
    }

    @Override
    protected void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand command)
    {
        final PostgreDatabase database = command.getObject();
        actions.add(
            new SQLDatabasePersistAction("Create database", "CREATE DATABASE " + DBUtils.getQuotedIdentifier(database)) //$NON-NLS-2$
        );
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command)
    {
        actions.add(
            new SQLDatabasePersistAction("Drop database", "DROP DATABASE " + DBUtils.getQuotedIdentifier(command.getObject())) //$NON-NLS-2$
        );
    }

    @Override
    public void renameObject(DBECommandContext commandContext, PostgreDatabase catalog, String newName) throws DBException
    {
        throw new DBException("Direct database rename is not yet implemented in Postgre. You should use export/import functions for that.");
    }

}

