/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017-2017 Karl Griesser (fullref@gmail.com)
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

import java.util.ArrayList;
import java.util.List;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.ExasolMessages;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTable;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableKeyColumn;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableUniqueKey;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLConstraintManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;

public class ExasolPrimaryKeyManager
		extends SQLConstraintManager<ExasolTableUniqueKey, ExasolTable> 
		implements DBEObjectRenamer<ExasolTableUniqueKey>{

	@Override
	public DBSObjectCache<? extends DBSObject, ExasolTableUniqueKey> getObjectsCache(
			ExasolTableUniqueKey object)
	{
		return object.getTable().getSchema().getConstraintCache();
	}

	@Override
	protected ExasolTableUniqueKey createDatabaseObject(
			DBRProgressMonitor monitor, DBECommandContext context,
			ExasolTable parent, Object copyFrom) throws DBException
	{
		return new UITask<ExasolTableUniqueKey>() {
			@Override
			protected ExasolTableUniqueKey runTask()
			{
				EditConstraintPage editPage = new EditConstraintPage(
						ExasolMessages.edit_exasol_constraint_manager_dialog_title, 
						parent, 
						new DBSEntityConstraintType[] {DBSEntityConstraintType.PRIMARY_KEY },
						true);
				if (!editPage.edit()) {
					return null;
				}
				
				final ExasolTableUniqueKey constraint = new ExasolTableUniqueKey(
						parent, 
						editPage.getConstraintType(), 
						editPage.isEnableConstraint(), 
						editPage.getConstraintName()
						);
				
				List<ExasolTableKeyColumn> constColumns = new ArrayList<ExasolTableKeyColumn>();
				int ordinalPosition = 0;
				for(DBSEntityAttribute tableColumn  : editPage.getSelectedAttributes())
				{
					ExasolTableKeyColumn col;
					try {
						col = new ExasolTableKeyColumn(constraint, parent.getAttribute(monitor, tableColumn.getName()), ++ordinalPosition);
					} catch (DBException e) {
						log.error("Could not find column " + tableColumn.getName() + " in table " + parent.getFullyQualifiedName(DBPEvaluationContext.DDL));
						return null;
					}
					constColumns.add(col);
				}
				constraint.setColumns(constColumns);
				return constraint;
			}
		}.execute();
	}
	
	@Override
	protected String getDropConstraintPattern(ExasolTableUniqueKey constraint)
	{
		return "ALTER TABLE " + constraint.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + " DROP PRIMARY KEY";
	}
	
	@Override
	protected void addObjectCreateActions(List<DBEPersistAction> actions,
			SQLObjectEditor<ExasolTableUniqueKey, ExasolTable>.ObjectCreateCommand command)
	{
		ExasolTableUniqueKey obj = (ExasolTableUniqueKey) command.getObject();
		try {
			actions.add(new SQLDatabasePersistAction("Create PK", ExasolUtils.getPKDdl(obj, new VoidProgressMonitor())));
		} catch (DBException e) {
			log.error("Could not generated DDL for PK");
		}
	}

	@Override
	public void renameObject(DBECommandContext commandContext,
			ExasolTableUniqueKey object, String newName) throws DBException
	{
		processObjectRename(commandContext, object, newName);
		
	}
	
	@Override
	protected void addObjectModifyActions(List<DBEPersistAction> actionList,
			SQLObjectEditor<ExasolTableUniqueKey, ExasolTable>.ObjectChangeCommand command)
	{
		final ExasolTableUniqueKey constraint = command.getObject();
		
		if (command.getProperties().containsKey("enabled"))
		{
			actionList.add(
					new SQLDatabasePersistAction("Alter PK",
							"ALTER TABLE " + constraint.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + 
							" MODIFY CONSTRAINT " + constraint.getName() + " " +
							(constraint.getEnabled() ? "ENABLE" : "DISABLE")
							)
					);
		}
	}
	
	
	@Override
	protected void addObjectRenameActions(List<DBEPersistAction> actions,
			SQLObjectEditor<ExasolTableUniqueKey, ExasolTable>.ObjectRenameCommand command)
	{
		final ExasolTableUniqueKey key = command.getObject();
		actions.add(
				new SQLDatabasePersistAction(
					"Rename PK", 
					"ALTER TABLE " + DBUtils.getObjectFullName(key.getTable(),DBPEvaluationContext.DDL) + " RENAME CONSTRAINT " 
					+ DBUtils.getQuotedIdentifier(key.getDataSource(),command.getOldName()) + " to " +
					DBUtils.getQuotedIdentifier(key.getDataSource(), command.getNewName())
				)
		);

	}
	

}
