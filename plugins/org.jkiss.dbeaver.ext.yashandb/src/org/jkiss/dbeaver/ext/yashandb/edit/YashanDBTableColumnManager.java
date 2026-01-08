/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.yashandb.edit;

import java.sql.Types;
import java.util.List;
import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBDataType;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableBase;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableColumn;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableColumnManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

public class YashanDBTableColumnManager extends SQLTableColumnManager<YashanDBTableColumn, YashanDBTableBase>
		implements DBEObjectRenamer<YashanDBTableColumn> {

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, YashanDBTableColumn> getObjectsCache(YashanDBTableColumn object) {
		return object.getParentObject().getContainer().tableCache.getChildrenCache(object.getParentObject());
	}

	protected ColumnModifier[] getSupportedModifiers(YashanDBTableColumn column, Map<String, Object> options) {
		return new ColumnModifier[] { DataTypeModifier, DefaultModifier, NullNotNullModifierConditional };
	}

	@Override
	public boolean canEditObject(YashanDBTableColumn object) {
		return true;
	}

	@Override
	protected YashanDBTableColumn createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			Object container, Object copyFrom, Map<String, Object> options) throws DBException {
		YashanDBTableBase table = (YashanDBTableBase) container;
		DBSDataType columnType = findBestDataType(table, "varchar2");
		final YashanDBTableColumn column = new YashanDBTableColumn(table);
		column.setName(getNewColumnName(monitor, context, table));
		column.setDataType((YashanDBDataType) columnType);
		column.setTypeName(columnType == null ? "INTEGER" : columnType.getName());
		column.setMaxLength(columnType != null && columnType.getDataKind() == DBPDataKind.STRING ? 100 : 0);
		column.setValueType(columnType == null ? Types.INTEGER : columnType.getTypeID());
		column.setOrdinalPosition(-1);
		return column;
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
			throws DBException {
		super.addObjectCreateActions(monitor, executionContext, actions, command, options);
		if (command.getProperties().size() > 1 || command.getProperty("comment") == null) {
			final YashanDBTableColumn column = command.getObject();
			actions.add(new SQLDatabasePersistAction("Comment column",
					"COMMENT ON COLUMN " + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + "."
							+ DBUtils.getQuotedIdentifier(column) + " IS '"
							+ column.getComment(new VoidProgressMonitor()) + "'"));
		}
	}

	@Override
	public void renameObject(DBECommandContext commandContext, YashanDBTableColumn object, Map<String, Object> options,
			String newName) throws DBException {
		processObjectRename(commandContext, object, options, newName);
	}

	@Override
	protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options) {
		final YashanDBTableColumn column = command.getObject();
		actions.add(new SQLDatabasePersistAction("Rename column",
				"ALTER TABLE " + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + " RENAME COLUMN "
						+ DBUtils.getQuotedIdentifier(column.getDataSource(), command.getOldName()) + " TO "
						+ DBUtils.getQuotedIdentifier(column.getDataSource(), command.getNewName())));
	}

	@Override
	public boolean canRenameObject(YashanDBTableColumn object) {
		return false;
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) {
		final YashanDBTableColumn column = command.getObject();
		actionList.add(new SQLDatabasePersistAction("Modify column",
				"ALTER TABLE " + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + " MODIFY "
						+ getNestedDeclaration(monitor, column.getTable(), command, options)));
		if (command.getProperties().size() > 1 || command.getProperty("comment") == null) {
			actionList.add(new SQLDatabasePersistAction("Comment column",
					"COMMENT ON COLUMN " + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + "."
							+ DBUtils.getQuotedIdentifier(column) + " IS '"
							+ column.getComment(new VoidProgressMonitor()) + "'"));
		}
	}

}
