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
import org.jkiss.dbeaver.ext.postgresql.model.PostgreRole;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.ui.PostgreCreateSchemaDialog;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;

import java.util.List;
import java.util.Map;

/**
 * PostgreSchemaManager
 */
public class PostgreSchemaManager extends SQLObjectEditor<PostgreSchema, PostgreDatabase> implements DBEObjectRenamer<PostgreSchema> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource)
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
    protected PostgreSchema createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final PostgreDatabase parent, Object copyFrom)
    {
        return new UITask<PostgreSchema>() {
            @Override
            protected PostgreSchema runTask() {
                PostgreCreateSchemaDialog dialog = new PostgreCreateSchemaDialog(DBeaverUI.getActiveWorkbenchShell(), parent);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                return new PostgreSchema(parent, dialog.getName(), dialog.getOwner());
            }
        }.execute();
    }

    @Override
    protected void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
    {
        final PostgreSchema schema = command.getObject();
        final StringBuilder script = new StringBuilder("CREATE SCHEMA " + DBUtils.getQuotedIdentifier(schema));
        try {
            final PostgreRole owner = schema.getOwner(new VoidProgressMonitor());
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
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        actions.add(
            new SQLDatabasePersistAction("Drop schema", "DROP SCHEMA " + DBUtils.getQuotedIdentifier(command.getObject()) + " CASCADE") //$NON-NLS-2$
        );
    }

    @Override
    protected void addObjectRenameActions(List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options)
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

