package org.jkiss.dbeaver.ext.exasol.manager;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.ext.exasol.model.ExasolSchema;
import org.jkiss.dbeaver.ext.exasol.ui.ExasolCreateSchemaDialog;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;


public class ExasolSchemaManager
        extends SQLObjectEditor<ExasolSchema, ExasolDataSource> 
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
        actions.add(
            new SQLDatabasePersistAction("Drop schema", "DROP SCHEMA " + DBUtils.getQuotedIdentifier(command.getObject()) + " RESTRICT") //$NON-NLS-2$
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
                String script = "COMMENT ON SCHEMA " + DBUtils.getQuotedIdentifier(schema) + " IS '" +  schema.getDescription() + "'";
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
}
