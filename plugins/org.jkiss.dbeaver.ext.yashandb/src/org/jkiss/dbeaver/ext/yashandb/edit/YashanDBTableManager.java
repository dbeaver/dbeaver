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

import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBPartitionBase;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSchema;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTable;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableColumn;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableConstraint;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableForeignKey;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableIndex;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTablespace;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

public class YashanDBTableManager extends SQLTableManager<YashanDBTable, YashanDBSchema>
		implements DBEObjectRenamer<YashanDBTable> {

	private static final Class<? extends DBSObject>[] CHILD_TYPES = CommonUtils.array(YashanDBTableColumn.class,
			YashanDBTableConstraint.class, YashanDBTableForeignKey.class, YashanDBTableIndex.class,
			YashanDBPartitionBase.class);

	@Override
	protected YashanDBTable createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			Object container, Object copyFrom, Map<String, Object> options) throws DBException {
		YashanDBSchema schema = (YashanDBSchema) container;
		YashanDBTable table = new YashanDBTable(schema, "");
		setNewObjectName(monitor, schema, table);
		return table;
	}

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, YashanDBTable> getObjectsCache(YashanDBTable object) {
		return (DBSObjectCache) object.getSchema().tableCache;
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
			throws DBException {
		if (command.getProperties().size() > 1 || command.getProperty("comment") == null) {
			StringBuilder query = new StringBuilder("ALTER TABLE ");
			query.append(command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" ");
			appendTableModifiers(monitor, command.getObject(), command, query, true, options);
			actionList.add(new SQLDatabasePersistAction(query.toString()));
		}
	}

	@Override
	protected void addObjectExtraActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, NestedObjectCommand<YashanDBTable, PropertyHandler> command,
			Map<String, Object> options) throws DBException {
		YashanDBTable table = command.getObject();
		if (command.getProperty("comment") != null) {
			actions.add(new SQLDatabasePersistAction("Comment table",
					"COMMENT ON TABLE " + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + " IS "
							+ SQLUtils.quoteString(table, table.getComment())));
		}
		if (!table.isPersisted()) {
			for (YashanDBTableColumn column : CommonUtils.safeCollection(table.getAttributes(monitor))) {
				if (!CommonUtils.isEmpty(column.getDescription())) {
					YashanDBTableColumnManager.addColumnCommentAction(actions, column, column.getTable());
				}
			}
		}
	}

	@Override
	protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options) {
		actions.add(new SQLDatabasePersistAction("Rename table",
				"ALTER TABLE " + DBUtils.getQuotedIdentifier(command.getObject().getSchema()) + "."
						+ DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getOldName())
						+ " RENAME TO "
						+ DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getNewName())));
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
		YashanDBTable object = command.getObject();
		actions.add(new SQLDatabasePersistAction(ModelMessages.model_jdbc_drop_table,
				"DROP " + (object.isView() ? "VIEW" : "TABLE") + " "
						+ object.getFullyQualifiedName(DBPEvaluationContext.DDL)
						+ (!object.isView() && CommonUtils.getOption(options, OPTION_DELETE_CASCADE)
								? " CASCADE CONSTRAINTS"
								: "")));
	}

	@Override
	public void renameObject(DBECommandContext commandContext, YashanDBTable object, Map<String, Object> options,
			String newName) throws DBException {
		processObjectRename(commandContext, object, options, newName);
	}

	@Override
	protected void appendTableModifiers(DBRProgressMonitor monitor, YashanDBTable table, NestedObjectCommand tableProps,
			StringBuilder ddl, boolean alter, Map<String, Object> options) throws DBException {
		super.appendTableModifiers(monitor, table, tableProps, ddl, alter, options);
		if (tableProps.getProperty("tablespace") != null) {
			Object tablespace = table.getTablespace();
			if (tablespace instanceof YashanDBTablespace) {
				if (table.isPersisted()) {
					if (table.isPersisted()) {
						ddl.append(" MOVE TABLESPACE ").append(((YashanDBTablespace) tablespace).getName());
					} else {
						ddl.append(" TABLESPACE ").append(((YashanDBTablespace) tablespace).getName());
					}
				}
			}
		}
	}

	@NotNull
	@Override
	public Class<? extends DBSObject>[] getChildTypes() {
		return CHILD_TYPES;
	}
}
