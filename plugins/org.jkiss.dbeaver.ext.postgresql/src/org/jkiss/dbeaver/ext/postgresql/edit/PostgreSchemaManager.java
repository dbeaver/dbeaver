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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreAuthId;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.ui.PostgreCreateSchemaDialog;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;

import java.util.List;

/**
 * PostgreSchemaManager
 */
public class PostgreSchemaManager extends SQLObjectEditor<PostgreSchema, PostgreDatabase> implements DBEObjectRenamer<PostgreSchema> {

    @Override
    public long getMakerOptions()
    {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Nullable
    @Override
    public DBSObjectCache<PostgreDatabase, PostgreSchema> getObjectsCache(PostgreSchema object)
    {
        return object.getDatabase().schemaCache;
    }

    @Override
    protected PostgreSchema createDatabaseObject(DBECommandContext context, PostgreDatabase parent, Object copyFrom)
    {
        PostgreCreateSchemaDialog dialog = new PostgreCreateSchemaDialog(DBeaverUI.getActiveWorkbenchShell(), parent);
        if (dialog.open() != IDialogConstants.OK_ID) {
            return null;
        }
        return new PostgreSchema(parent, dialog.getName(), dialog.getOwner());
    }

    @Override
    protected void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand command)
    {
        final PostgreSchema schema = command.getObject();
        final StringBuilder script = new StringBuilder("CREATE SCHEMA " + DBUtils.getQuotedIdentifier(schema));
        try {
            final PostgreAuthId owner = schema.getOwner(VoidProgressMonitor.INSTANCE);
            if (owner != null) {
                script.append("\nAUTHORIZATION ").append(owner.getName());
            }
        } catch (DBException e) {
            log.error(e);
        }

        actions.add(
            new SQLDatabasePersistAction("Create schema", script.toString()) //$NON-NLS-2$
        );
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command)
    {
        actions.add(
            new SQLDatabasePersistAction("Drop schema", "DROP SCHEMA " + DBUtils.getQuotedIdentifier(command.getObject()) + " CASCADE") //$NON-NLS-2$
        );
    }

    @Override
    protected void addObjectRenameActions(List<DBEPersistAction> actions, ObjectRenameCommand command)
    {
        actions.add(
            new SQLDatabasePersistAction(
                "Rename schema",
                "ALTER SCHEMA " + DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getOldName()) + //$NON-NLS-1$
                    " RENAME TO " + DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getNewName())) //$NON-NLS-1$
        );
    }

    @Override
    public void renameObject(DBECommandContext commandContext, PostgreSchema schema, String newName) throws DBException
    {
        processObjectRename(commandContext, schema, newName);
    }

}

