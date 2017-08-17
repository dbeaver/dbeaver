package org.jkiss.dbeaver.ext.exasol.manager;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.exasol.manager.security.ExasolUser;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.ext.exasol.ui.ExasolUserDialog;
import org.jkiss.dbeaver.ext.exasol.ui.ExasolUserQueryPassword;
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

public class ExasolUserManager extends SQLObjectEditor<ExasolUser, ExasolDataSource> implements DBEObjectRenamer<ExasolUser> {
	
	@Override
	public long getMakerOptions()
	{
		return FEATURE_SAVE_IMMEDIATELY;
	}

	@Override
	public DBSObjectCache<ExasolDataSource, ExasolUser> getObjectsCache(
			ExasolUser object)
	{
		ExasolDataSource ds = (ExasolDataSource) object.getDataSource();
		return ds.getUserCache();
		
	}
	
	@Override
	protected ExasolUser createDatabaseObject(DBRProgressMonitor monitor,
			DBECommandContext context, ExasolDataSource parent, Object copyFrom)
			throws DBException
	{
		return new UITask<ExasolUser>() {
			@Override
			protected ExasolUser runTask()
			{
				ExasolUserDialog dialog = new ExasolUserDialog(DBeaverUI.getActiveWorkbenchShell(), parent);
                if (dialog.open() != IDialogConstants.OK_ID)
                {
                    return null;
                }
                
                return new ExasolUser(parent, dialog.getName(), dialog.getComment(), dialog.getLDAPDN(), dialog.getPassword());
			}
		}.execute();
	}

	@Override
	protected void addObjectCreateActions(List<DBEPersistAction> actions,
			SQLObjectEditor<ExasolUser, ExasolDataSource>.ObjectCreateCommand command)
	{
		ExasolUser obj = command.getObject();
		
		StringBuilder script = new StringBuilder("CREATE USER " + DBUtils.getQuotedIdentifier(obj) + " IDENTIFIED ");
		
		if (CommonUtils.isEmpty(obj.getDn()))
		{
			script.append(" BY \"" + obj.getPassword() + "\"");
		} else {
			script.append(" AT LDAP AS '" + obj.getDn() + "'" );
		}
		
		actions.add(new SQLDatabasePersistAction("Create User", script.toString()));
		
		if (! CommonUtils.isEmpty(obj.getDescription())) {
				actions.add(Comment(obj));
		}
		
	}

	@Override
	protected void addObjectDeleteActions(List<DBEPersistAction> actions,
			SQLObjectEditor<ExasolUser, ExasolDataSource>.ObjectDeleteCommand command)
	{
		ExasolUser obj = command.getObject();
		actions.add(new SQLDatabasePersistAction("Drop User", "DROP USER " + DBUtils.getQuotedIdentifier(obj)));
	}

	@Override
	public void renameObject(DBECommandContext commandContext,
			ExasolUser object, String newName) throws DBException
	{
        processObjectRename(commandContext, object, newName);
	}

	@Override
	protected void processObjectRename(DBECommandContext commandContext,
			ExasolUser object, String newName) throws DBException
	{
        ObjectRenameCommand command = new ObjectRenameCommand(object, ModelMessages.model_jdbc_rename_object, newName);
        commandContext.addCommand(command, new RenameObjectReflector(), true);
	}
	
	@Override
	protected void addObjectRenameActions(List<DBEPersistAction> actions,
			SQLObjectEditor<ExasolUser, ExasolDataSource>.ObjectRenameCommand command)
	{
		ExasolUser obj = command.getObject();
        actions.add(
            new SQLDatabasePersistAction(
                "Rename User",
                "RENAME USER " +  DBUtils.getQuotedIdentifier(obj.getDataSource(), command.getOldName()) + " to " +
                    DBUtils.getQuotedIdentifier(obj.getDataSource(), command.getNewName()))
        );
	}
	
	private SQLDatabasePersistAction Comment(ExasolUser obj)
	{
		return new SQLDatabasePersistAction("Comment on User", "COMMENT ON USER " + DBUtils.getQuotedIdentifier(obj) + " IS '" + obj.getDescription() + "'");
	}
	
	@Override
	protected void addObjectModifyActions(List<DBEPersistAction> actionList,
			SQLObjectEditor<ExasolUser, ExasolDataSource>.ObjectChangeCommand command)
	{
		ExasolUser obj = command.getObject();
		
		if (command.getProperties().containsKey("description"))
		{
			
			actionList.add(Comment(obj));
		}
		
		
		if (command.getProperties().containsKey("dn")) 
		{
			String script =  String.format("ALTER USER " + DBUtils.getQuotedIdentifier(obj) + " IDENTIFIED AT LDAP AS '%s'", obj.getDn());
			actionList.add(new SQLDatabasePersistAction("alter user", script));
		}
		
		if (command.getProperties().containsKey("password")) {
			
			StringBuilder script = new StringBuilder("ALTER USER " + DBUtils.getQuotedIdentifier(obj) + " IDENTIFIED ");
			script.append(" BY \"" + obj.getPassword() + "\" ");
			ExasolDataSource ds = (ExasolDataSource) obj.getDataSource();
			if (! ds.hasAlterUserPrivilege()  ) {
				String oldPassword = new UITask<String>() {
					@Override
					protected String runTask()
					{
						ExasolUserQueryPassword dialog = new ExasolUserQueryPassword(DBeaverUI.getActiveWorkbenchShell());
						
		                if (dialog.open() != IDialogConstants.OK_ID)
		                {
		                    throw new IllegalStateException("Password has to be provided");
		                }
		                
		                return dialog.getPassword();
					}
				}.execute();
				
				if (CommonUtils.isEmpty(oldPassword))
				{
		            throw new IllegalStateException("Old password can not be empty");
				}
				
				script.append(" REPLACE \"" + oldPassword + "\"");
			}
			
			actionList.add(new SQLDatabasePersistAction("Modify User", script.toString()));
		}
	}

}