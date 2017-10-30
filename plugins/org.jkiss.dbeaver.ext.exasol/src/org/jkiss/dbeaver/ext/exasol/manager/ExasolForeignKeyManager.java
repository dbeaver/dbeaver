package org.jkiss.dbeaver.ext.exasol.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTable;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableForeignKey;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableKeyColumn;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.ext.exasol.ui.ExasolCreateForeignKeyDialog;
import org.jkiss.dbeaver.ext.exasol.ui.ExasolCreateForeignKeyDialog.FKColumnInfo;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;

public class ExasolForeignKeyManager
		extends SQLForeignKeyManager<ExasolTableForeignKey, ExasolTable> implements DBEObjectRenamer<ExasolTableForeignKey> {

	@Override
	public DBSObjectCache<? extends DBSObject, ExasolTableForeignKey> getObjectsCache(
			ExasolTableForeignKey object)
	{
		final ExasolTable parent = object.getParentObject();
		return parent.getContainer().getAssociationCache();
	}

	@Override
	protected ExasolTableForeignKey createDatabaseObject(
			DBRProgressMonitor monitor, DBECommandContext context,
			ExasolTable parent, Object copyFrom) throws DBException
	{
		return new UITask<ExasolTableForeignKey>() {
			@Override
			protected ExasolTableForeignKey runTask()
			{
				ExasolCreateForeignKeyDialog editPage = new ExasolCreateForeignKeyDialog("Create Foreign Key",parent);
				
				if (!editPage.edit()) {
					return null;
				}
				
				final ExasolTableForeignKey foreignKey = new ExasolTableForeignKey(
						parent, editPage.getUniqueConstraint(), editPage.isEnabled(), editPage.getName()
						);

				List<ExasolTableKeyColumn> columns = new ArrayList<ExasolTableKeyColumn>();
				int cnt = 0;
				for (FKColumnInfo column : editPage.getColumns()) {
			        try {
						columns.add(new ExasolTableKeyColumn(foreignKey, parent.getAttribute(monitor, column.getOwnColumn().getName()), ++cnt));
					} catch (DBException e) {
						log.error("Could not get Attribute Information from Table");
						return null;
					}
		        }
				
				foreignKey.setColumns(columns);
				
				return foreignKey;
			}
		}.execute();
	}
	
	@Override
	protected String getDropForeignKeyPattern(ExasolTableForeignKey constraint)
	{
		return "ALTER TABLE " + DBUtils.getObjectFullName(constraint.getTable(), DBPEvaluationContext.DDL) + " DROP CONSTRAINT " 
				+ DBUtils.getObjectFullName(constraint, DBPEvaluationContext.DDL) 
				;
	}
	
	@Override
	protected void addObjectCreateActions(List<DBEPersistAction> actions,
                                          ObjectCreateCommand command, Map<String, Object> options)
	{
		final ExasolTableForeignKey key = command.getObject();
		
		try {
			actions.add(new SQLDatabasePersistAction("Create Foreign Key",ExasolUtils.getFKDdl(key, new VoidProgressMonitor())));
		} catch (DBException e) {
			log.error("Could not created DDL for Exasol FK: " + key.getFullyQualifiedName(DBPEvaluationContext.DDL));
			log.error(e.getMessage());
		}
		
		
	}	
	
	
	@Override
	protected void addObjectRenameActions(List<DBEPersistAction> actions,
                                          ObjectRenameCommand command, Map<String, Object> options)
	{
		final ExasolTableForeignKey key = command.getObject();
		
		actions.add(
				new SQLDatabasePersistAction(
					"Rename FK", 
					"ALTER TABLE " + DBUtils.getObjectFullName(key.getTable(),DBPEvaluationContext.DDL) + " RENAME CONSTRAINT " 
					+ DBUtils.getQuotedIdentifier(key.getDataSource(),command.getOldName()) + " to " +
					DBUtils.getQuotedIdentifier(key.getDataSource(), command.getNewName())
				)
		);
	}

	@Override
	protected void addObjectModifyActions(List<DBEPersistAction> actionList,
										  ObjectChangeCommand command, Map<String, Object> options)
	{
		final ExasolTableForeignKey constraint = command.getObject();
		
		if (command.getProperties().containsKey("enabled"))
		{
			actionList.add(
					new SQLDatabasePersistAction("Alter FK",
							"ALTER TABLE " + constraint.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + 
							" MODIFY CONSTRAINT " + constraint.getName() + " " +
							(constraint.getEnabled() ? "ENABLE" : "DISABLE")
							)
					);
		}
	}
	
	@Override
    protected void processObjectRename(DBECommandContext commandContext, ExasolTableForeignKey object, String newName) throws DBException
    {
        ObjectRenameCommand command = new ObjectRenameCommand(object, ModelMessages.model_jdbc_rename_object, newName);
        commandContext.addCommand(command, new RenameObjectReflector(), true);
    }
	

	@Override
	public void renameObject(DBECommandContext commandContext,
			ExasolTableForeignKey object, String newName) throws DBException
	{
		processObjectRename(commandContext, object, newName);
	}

}
