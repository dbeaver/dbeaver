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
package org.jkiss.dbeaver.ext.mysql.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.mysql.views.MySQLCreateDatabaseDialog;
import org.jkiss.dbeaver.model.DBPDataSource;
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
import java.util.Map;

/**
 * MySQLCatalogManager
 */
public class MySQLCatalogManager extends SQLObjectEditor<MySQLCatalog, MySQLDataSource> implements DBEObjectRenamer<MySQLCatalog> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource)
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
    protected void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
    {
        final MySQLCatalog catalog = command.getObject();
        final StringBuilder script = new StringBuilder("CREATE SCHEMA `" + catalog.getName() + "`");
        appendDatabaseModifiers(catalog, script);
        actions.add(
            new SQLDatabasePersistAction("Create schema", script.toString()) //$NON-NLS-2$
        );
    }

    protected void addObjectModifyActions(List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        final MySQLCatalog catalog = command.getObject();
        final StringBuilder script = new StringBuilder("ALTER DATABASE `" + catalog.getName() + "`");
        appendDatabaseModifiers(catalog, script);
        actionList.add(
            new SQLDatabasePersistAction("Alter database", script.toString()) //$NON-NLS-2$
        );
    }

    private void appendDatabaseModifiers(MySQLCatalog catalog, StringBuilder script) {
        if (catalog.getDefaultCharset() != null) {
            script.append("\nDEFAULT CHARACTER SET ").append(catalog.getDefaultCharset().getName());
        }
        if (catalog.getDefaultCollation() != null) {
            script.append("\nDEFAULT COLLATE ").append(catalog.getDefaultCollation().getName());
        }
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        actions.add(new SQLDatabasePersistAction("Drop schema", "DROP SCHEMA `" + command.getObject().getName() + "`")); //$NON-NLS-2$
    }

    @Override
    public void renameObject(DBECommandContext commandContext, MySQLCatalog catalog, String newName) throws DBException
    {
        throw new DBException("Direct database rename is not yet implemented in MySQL. You should use export/import functions for that.");
    }

}

