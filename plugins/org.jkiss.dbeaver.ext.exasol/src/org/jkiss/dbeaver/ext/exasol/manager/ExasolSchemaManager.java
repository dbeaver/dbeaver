/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.manager;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.exasol.ExasolMessages;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.ext.exasol.model.ExasolSchema;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.ext.exasol.ui.ExasolCreateSchemaDialog;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;


public class ExasolSchemaManager
        extends SQLObjectEditor<ExasolSchema, ExasolDataSource> implements DBEObjectRenamer<ExasolSchema> 
{
    
    
    @Override
    public long getMakerOptions()
    {
        return FEATURE_SAVE_IMMEDIATELY;
    }
    
    @Override
    public DBSObjectCache<? extends DBSObject, ExasolSchema> getObjectsCache(
            ExasolSchema object)
    {
        ExasolDataSource source = (ExasolDataSource) object.getDataSource();
        return source.getSchemaCache();
    }
    
    
    @Override
    protected ExasolSchema createDatabaseObject(DBRProgressMonitor monitor,
            DBECommandContext context, ExasolDataSource parent, Object copyFrom)
            throws DBException
    {
        return new UITask<ExasolSchema>(){
            @Override
            protected ExasolSchema runTask()
            {
                ExasolCreateSchemaDialog dialog = new ExasolCreateSchemaDialog(DBeaverUI.getActiveWorkbenchShell(), parent);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                return new ExasolSchema(parent, dialog.getName(), dialog.getOwner().getName());
            }
        }.execute();
    }
    
    private void changeOwner(List<DBEPersistAction> actions, ExasolSchema schema , String owner)
    {
        String script = "ALTER SCHEMA " + DBUtils.getQuotedIdentifier(schema) + " CHANGE OWNER  " + owner;
        actions.add(
                new SQLDatabasePersistAction("Set schema Owner", script)
                );
        
    }
    
    
    @Override
    protected void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand command)
    {
        final ExasolSchema schema = command.getObject();
        
        String script = "CREATE SCHEMA " + DBUtils.getQuotedIdentifier(schema);
        
        actions.add(
                new SQLDatabasePersistAction("Create schema", script)
                );
        String owner = schema.getOwner();
        if (owner != null)
        {
            changeOwner(actions, schema, owner);
        }
    }
    
    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command)
    {
        int result = new UITask<Integer>() {
            protected Integer runTask() {
                ConfirmationDialog dialog = new ConfirmationDialog(
                        DBeaverUI.getActiveWorkbenchShell(),
                        ExasolMessages.dialog_schema_drop_title,
                        null,
                        ExasolMessages.dialog_schema_drop_message,
                        MessageDialog.CONFIRM,
                        new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL },
                        0,
                        ExasolMessages.dialog_general_continue,
                        false);
                return dialog.open();
            }
        }.execute();
        if (result != IDialogConstants.YES_ID) 
        {
            throw new IllegalStateException("User abort");
        }
        
        actions.add(
            new SQLDatabasePersistAction("Drop schema", "DROP SCHEMA " + DBUtils.getQuotedIdentifier(command.getObject()) + " CASCADE") //$NON-NLS-2$
        );
    }
    
    @Override
    protected void addObjectRenameActions(List<DBEPersistAction> actions,
            SQLObjectEditor<ExasolSchema, ExasolDataSource>.ObjectRenameCommand command)
    {
        ExasolSchema obj = command.getObject();
        actions.add(
                new SQLDatabasePersistAction(
                    "Rename Schema",
                    "RENAME SCHEMA " +  DBUtils.getQuotedIdentifier(obj.getDataSource(), command.getOldName()) + " to " +
                        DBUtils.getQuotedIdentifier(obj.getDataSource(), command.getNewName()))
            );
    }
    
    @Override
    public void addObjectModifyActions(List<DBEPersistAction> actionList, ObjectChangeCommand command) 
    {
        ExasolSchema schema = command.getObject();
        
        if (command.getProperties().size() >= 1 ) 
        {
            if (command.getProperties().containsKey("description"))
            {
                String script = "COMMENT ON SCHEMA " + DBUtils.getQuotedIdentifier(schema) + " IS '" +  ExasolUtils.quoteString(schema.getDescription()) + "'";
                actionList.add(
                        new SQLDatabasePersistAction("Change comment on Schema", script)
                        );
            }
            if (command.getProperties().containsKey("owner"))
            {
                changeOwner(actionList, schema, schema.getOwner());
            }
            
        }
    }

    @Override
    public void renameObject(DBECommandContext commandContext,
            ExasolSchema object, String newName) throws DBException
    {
        processObjectRename(commandContext, object, newName);
    }
    
}
