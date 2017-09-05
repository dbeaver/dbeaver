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
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.exasol.ExasolMessages;
import org.jkiss.dbeaver.ext.exasol.model.ExasolConnection;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.ext.exasol.ui.ExasolConnectionDialog;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;


public class ExasolConnectionManager
        extends SQLObjectEditor<ExasolConnection, ExasolDataSource> implements DBEObjectRenamer<ExasolConnection> {
    
    @Override
    public long getMakerOptions()
    {
        return FEATURE_SAVE_IMMEDIATELY;
    }
    
    
    @Override
    public DBSObjectCache<ExasolDataSource, ExasolConnection> getObjectsCache(
            ExasolConnection object)
    {
        ExasolDataSource source = (ExasolDataSource) object.getDataSource();
        return source.getConnectionCache();
    }
    
    @Override
    protected ExasolConnection createDatabaseObject(DBRProgressMonitor monitor,
            DBECommandContext context, ExasolDataSource parent, Object copyFrom)
            throws DBException
    {
        return new UITask<ExasolConnection>() {
            @Override
            protected ExasolConnection runTask()
            {
                ExasolConnectionDialog dialog = new ExasolConnectionDialog(DBeaverUI.getActiveWorkbenchShell(), parent);
                if (dialog.open() != IDialogConstants.OK_ID)
                {
                    return null;
                }
                ExasolConnection con = new ExasolConnection(parent, dialog.getName(), dialog.getUrl(), dialog.getComment(), dialog.getUrl(), dialog.getPassword());
                return con;
            }
        }.execute();
    }
    
    private SQLDatabasePersistAction Comment(ExasolConnection con)
    {
    	return new SQLDatabasePersistAction(
                	"Comment on Connection",
                	String.format("COMMENT ON CONNECTION %s is ''",
    	                DBUtils.getQuotedIdentifier(con),
    	                ExasolUtils.quoteString(con.getDescription())
    	            )                
                );
    }
    
    @Override
    protected void addObjectCreateActions(List<DBEPersistAction> actions,
            SQLObjectEditor<ExasolConnection, ExasolDataSource>.ObjectCreateCommand command)
    {
        final ExasolConnection con = command.getObject();
        
        StringBuilder script = new StringBuilder(String.format("CREATE CONNECTION %s TO ",DBUtils.getQuotedIdentifier(con)));
       
        script.append(" '" + ExasolUtils.quoteString(con.getConnectionString()) + "' ");
        
        if (! (con.getUserName().isEmpty() | con.getPassword().isEmpty()))
        {
            script.append(String.format("USER '%s' IDENTIFIED BY '%s'",
                    ExasolUtils.quoteString(con.getUserName()), 
                    ExasolUtils.quoteString(con.getPassword())
                            )
                    );
        }
        
        actions.add(
                new SQLDatabasePersistAction("Create Connection", script.toString())
                );
        
        if (! con.getDescription().isEmpty())
        {
            actions.add(Comment(con));
        }
       
        
    }
    
    @Override
    protected void addObjectRenameActions(List<DBEPersistAction> actions,
            SQLObjectEditor<ExasolConnection, ExasolDataSource>.ObjectRenameCommand command)
    {
        ExasolConnection obj = command.getObject();
        actions.add(
            new SQLDatabasePersistAction(
                "Rename Connection",
                "RENAME CONNECTION " +  DBUtils.getQuotedIdentifier(obj.getDataSource(), command.getOldName()) + " to " +
                    DBUtils.getQuotedIdentifier(obj.getDataSource(), command.getNewName()))
        );
    }
    
    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions,
            SQLObjectEditor<ExasolConnection, ExasolDataSource>.ObjectDeleteCommand command)
    {
        final ExasolConnection con = command.getObject();
        actions.add(
                new SQLDatabasePersistAction("Drop Connection","DROP CONNECTION " + DBUtils.getQuotedIdentifier(con))
                );
    }
    
    @Override
    protected void addObjectModifyActions(List<DBEPersistAction> actionList,
            SQLObjectEditor<ExasolConnection, ExasolDataSource>.ObjectChangeCommand command)
    {
        ExasolConnection con = command.getObject();
        
        Map<Object, Object> com = command.getProperties();
        
        if (com.containsKey("description"))
        {
            actionList.add(
                    Comment(con)
                    );
        }
        

        // url, username or password have changed
        if (com.containsKey("url") | com.containsKey("userName") | com.containsKey("password") )
        {
            // possible loss of information - warn
            if (
                    (com.containsKey("url") | com.containsKey("userName")  ) &  ! con.getUserName().isEmpty() & con.getPassword().isEmpty() 
               )
            {
                int result = new UITask<Integer>() {
                    protected Integer runTask() {
                        ConfirmationDialog dialog = new ConfirmationDialog(
                            DBeaverUI.getActiveWorkbenchShell(),
                            ExasolMessages.dialog_connection_alter_title,
                            null,
                            ExasolMessages.dialog_connection_alter_message,
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
            }
            StringBuilder script = new StringBuilder(String.format("ALTER CONNECTION %s ",DBUtils.getQuotedIdentifier(con)));
            script.append(" '" + ExasolUtils.quoteString(con.getConnectionString()) + "' ");
            if (! (con.getUserName().isEmpty() | con.getPassword().isEmpty()))
            {
                script.append(String.format(" USER '%s' IDENTIFIED BY '%s'",
                        ExasolUtils.quoteString(con.getUserName()), 
                        ExasolUtils.quoteString(con.getPassword())
                                )
                        );
            }
            
            actionList.add(
                    new SQLDatabasePersistAction("alter Connection", script.toString())
                    );
        }
    }


    @Override
    public void renameObject(DBECommandContext commandContext,
            ExasolConnection object, String newName) throws DBException
    {
        processObjectRename(commandContext, object, newName);
    }
    
    
    

}
