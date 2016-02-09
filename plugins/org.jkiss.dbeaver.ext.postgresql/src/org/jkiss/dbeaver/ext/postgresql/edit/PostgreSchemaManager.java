/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;

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
/*
        PostgreCreateDatabaseDialog dialog = new PostgreCreateDatabaseDialog(DBeaverUI.getActiveWorkbenchShell(), parent);
        if (dialog.open() != IDialogConstants.OK_ID) {
            return null;
        }
        String schemaName = dialog.getName();
        PostgreSchema newCatalog = new PostgreSchema(parent, null);
        newCatalog.setName(schemaName);
        newCatalog.setDefaultCharset(dialog.getCharset());
        newCatalog.setDefaultCollation(dialog.getCollation());
        return newCatalog;
*/
        return null;
    }

    @Override
    protected DBEPersistAction[] makeObjectCreateActions(ObjectCreateCommand command)
    {
        final PostgreSchema catalog = command.getObject();
        final StringBuilder script = new StringBuilder("CREATE SCHEMA `" + catalog.getName() + "`");
/*
        if (catalog.getDefaultCharset() != null) {
            script.append("\nDEFAULT CHARACTER SET ").append(catalog.getDefaultCharset().getName());
        }
        if (catalog.getDefaultCollation() != null) {
            script.append("\nDEFAULT COLLATE ").append(catalog.getDefaultCollation().getName());
        }
*/
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction("Create schema", script.toString()) //$NON-NLS-2$
        };
    }

    @Override
    protected DBEPersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command)
    {
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction("Drop schema", "DROP SCHEMA `" + command.getObject().getName() + "`") //$NON-NLS-2$
        };
    }

    @Override
    public void renameObject(DBECommandContext commandContext, PostgreSchema catalog, String newName) throws DBException
    {
        throw new DBException("Direct database rename is not yet implemented in Postgre. You should use export/import functions for that.");
        //super.addCommand(new CommandRenameCatalog(newName), null);
        //saveChanges(monitor);
    }

}

