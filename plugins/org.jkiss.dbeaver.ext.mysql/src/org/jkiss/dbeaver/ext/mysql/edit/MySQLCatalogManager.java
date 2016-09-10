/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.mysql.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.mysql.views.MySQLCreateDatabaseDialog;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;

import java.util.List;

/**
 * MySQLCatalogManager
 */
public class MySQLCatalogManager extends SQLObjectEditor<MySQLCatalog, MySQLDataSource> implements DBEObjectRenamer<MySQLCatalog> {

    @Override
    public long getMakerOptions()
    {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Nullable
    @Override
    public DBSObjectCache<MySQLDataSource, MySQLCatalog> getObjectsCache(MySQLCatalog object)
    {
        return object.getDataSource().getCatalogCache();
    }

    @Override
    protected MySQLCatalog createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final MySQLDataSource parent, Object copyFrom)
    {
        return new UITask<MySQLCatalog>() {
            @Override
            protected MySQLCatalog runTask() {
                MySQLCreateDatabaseDialog dialog = new MySQLCreateDatabaseDialog(DBeaverUI.getActiveWorkbenchShell(), parent);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                String schemaName = dialog.getName();
                MySQLCatalog newCatalog = new MySQLCatalog(parent, null);
                newCatalog.setName(schemaName);
                newCatalog.setDefaultCharset(dialog.getCharset());
                newCatalog.setDefaultCollation(dialog.getCollation());
                return newCatalog;
            }
        }.execute();
    }

    @Override
    protected void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand command)
    {
        final MySQLCatalog catalog = command.getObject();
        final StringBuilder script = new StringBuilder("CREATE SCHEMA `" + catalog.getName() + "`");
        if (catalog.getDefaultCharset() != null) {
            script.append("\nDEFAULT CHARACTER SET ").append(catalog.getDefaultCharset().getName());
        }
        if (catalog.getDefaultCollation() != null) {
            script.append("\nDEFAULT COLLATE ").append(catalog.getDefaultCollation().getName());
        }
        actions.add(
            new SQLDatabasePersistAction("Create schema", script.toString()) //$NON-NLS-2$
        );
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command)
    {
        actions.add(new SQLDatabasePersistAction("Drop schema", "DROP SCHEMA `" + command.getObject().getName() + "`")); //$NON-NLS-2$
    }

    @Override
    public void renameObject(DBECommandContext commandContext, MySQLCatalog catalog, String newName) throws DBException
    {
        throw new DBException("Direct database rename is not yet implemented in MySQL. You should use export/import functions for that.");
    }

}

