package org.jkiss.dbeaver.ext.exasol.manager;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.exasol.manager.security.ExasolRole;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.ext.exasol.ui.ExasolRoleDialog;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.utils.CommonUtils;

public class ExasolRoleManager extends SQLObjectEditor<ExasolRole, ExasolDataSource> implements DBEObjectRenamer<ExasolRole> {
	
	@Override
	public long getMakerOptions()
	{
		return FEATURE_SAVE_IMMEDIATELY;
	}

	@Override
	public DBSObjectCache<ExasolDataSource, ExasolRole> getObjectsCache(
			ExasolRole object)
	{
		ExasolDataSource ds = (ExasolDataSource) object.getDataSource();
		return ds.getRoleCache();
		
	}
	
	@Override
	protected ExasolRole createDatabaseObject(DBRProgressMonitor monitor,
			DBECommandContext context, ExasolDataSource parent, Object copyFrom)
			throws DBException
	{
		return new UITask<ExasolRole>() {
			@Override
			protected ExasolRole runTask()
			{
				ExasolRoleDialog dialog = new ExasolRoleDialog(DBeaverUI.getActiveWorkbenchShell());
                if (dialog.open() != IDialogConstants.OK_ID)
                {
                    return null;
                }
                
                return new ExasolRole(parent, dialog.getName(), dialog.getDescription());
			}
		}.execute();
	}

	@Override
	protected void addObjectCreateActions(List<DBEPersistAction> actions,
			SQLObjectEditor<ExasolRole, ExasolDataSource>.ObjectCreateCommand command)
	{
		ExasolRole obj = command.getObject();
		
		String script = "CREATE ROLE " + DBUtils.getQuotedIdentifier(obj);
		
		actions.add(new SQLDatabasePersistAction("Create Role", script));
		
		if (! CommonUtils.isEmpty(obj.getDescription())) {
				actions.add(Comment(obj));
		}
		
	}

	@Override
	protected void addObjectDeleteActions(List<DBEPersistAction> actions,
			SQLObjectEditor<ExasolRole, ExasolDataSource>.ObjectDeleteCommand command)
	{
		ExasolRole obj = command.getObject();
		actions.add(new SQLDatabasePersistAction("Drop Role", "DROP ROLE " + DBUtils.getQuotedIdentifier(obj)));
	}

	@Override
	public void renameObject(DBECommandContext commandContext,
			ExasolRole object, String newName) throws DBException
	{
        processObjectRename(commandContext, object, newName);
	}

	@Override
	protected void processObjectRename(DBECommandContext commandContext,
			ExasolRole object, String newName) throws DBException
	{
        ObjectRenameCommand command = new ObjectRenameCommand(object, ModelMessages.model_jdbc_rename_object, newName);
        commandContext.addCommand(command, new RenameObjectReflector(), true);
	}
	
	@Override
	protected void addObjectRenameActions(List<DBEPersistAction> actions,
			SQLObjectEditor<ExasolRole, ExasolDataSource>.ObjectRenameCommand command)
	{
		ExasolRole obj = command.getObject();
        actions.add(
            new SQLDatabasePersistAction(
                "Rename ROLE",
                "RENAME ROLE " +  DBUtils.getQuotedIdentifier(obj.getDataSource(), command.getOldName()) + " to " +
                    DBUtils.getQuotedIdentifier(obj.getDataSource(), command.getNewName()))
        );
	}
	
	private SQLDatabasePersistAction Comment(ExasolRole obj)
	{
		return new SQLDatabasePersistAction("Comment on Role", "COMMENT ON ROLE " + DBUtils.getQuotedIdentifier(obj) + " IS '" + obj.getDescription() + "'");
	}
	
	@Override
	protected void addObjectModifyActions(List<DBEPersistAction> actionList,
			SQLObjectEditor<ExasolRole, ExasolDataSource>.ObjectChangeCommand command)
	{
		ExasolRole obj = command.getObject();
		
		if (command.getProperties().containsKey("description"))
		{
			
			actionList.add(Comment(obj));
		}
		
	}

}