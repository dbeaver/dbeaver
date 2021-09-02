/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.oceanbase.oracle.model;

import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableColumn;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableConstraint;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableForeignKey;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableIndex;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.utils.CommonUtils;

public class OceanbaseOracleTableManager extends SQLTableManager<OceanbaseOracleTable, OceanbaseOracleSchema>
		implements DBEObjectRenamer<OceanbaseOracleTable> {
	private static final Class<?>[] CHILD_TYPES = { OracleTableColumn.class, OracleTableConstraint.class,
			OracleTableForeignKey.class, OracleTableIndex.class };

	@Override
	public Class<?>[] getChildTypes() {
		return CHILD_TYPES;
	}

	@Override
	public DBSObjectCache<OceanbaseOracleSchema, OceanbaseOracleTable> getObjectsCache(OceanbaseOracleTable object) {
		return (DBSObjectCache) object.getSchema().oceanbaseTableCache;
	}

	@Override
	protected OceanbaseOracleTable createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			Object container, Object copyFrom, Map<String, Object> options) throws DBException {
		OceanbaseOracleSchema schema = (OceanbaseOracleSchema) container;

		OceanbaseOracleTable table = new OceanbaseOracleTable(schema, ""); //$NON-NLS-1$
		setNewObjectName(monitor, schema, table);
		return table;
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) {
		if (command.getProperties().size() > 1 || command.getProperty("comment") == null) { //$NON-NLS-1$
			StringBuilder query = new StringBuilder("ALTER TABLE "); //$NON-NLS-1$
			query.append(command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" "); //$NON-NLS-1$
			appendTableModifiers(monitor, command.getObject(), command, query, true);
			actionList.add(new SQLDatabasePersistAction(query.toString()));
		}
	}

	@Override
	protected void addObjectExtraActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, NestedObjectCommand<OceanbaseOracleTable, PropertyHandler> command,
			Map<String, Object> options) throws DBException {
		OceanbaseOracleTable table = command.getObject();
		if (command.getProperty("comment") != null) { //$NON-NLS-1$
			actions.add(new SQLDatabasePersistAction("Comment table",
					"COMMENT ON TABLE " + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + " IS "
							+ SQLUtils.quoteString(table, table.getComment())));
		}

		if (!table.isPersisted()) {
			// Column comments for the newly created table
			for (OracleTableColumn column : CommonUtils.safeCollection(table.getAttributes(monitor))) {
				if (!CommonUtils.isEmpty(column.getDescription())) {
					OceanbaseOracleTableColumnManager.addColumnCommentAction(actions, column, column.getTable());
				}
			}
		}
	}

	@Override
	protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options) {
		actions.add(new SQLDatabasePersistAction("Rename table",
				"ALTER TABLE " + DBUtils.getQuotedIdentifier(command.getObject().getSchema()) + "." //$NON-NLS-1$
						+ DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getOldName()) + " RENAME TO "
						+ DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getNewName())));
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
		OceanbaseOracleTable object = command.getObject();
		actions.add(new SQLDatabasePersistAction(ModelMessages.model_jdbc_drop_table,
				"DROP " + (object.isView() ? "VIEW" : "TABLE") + " "
						+ object.getFullyQualifiedName(DBPEvaluationContext.DDL)
						+ (!object.isView() && CommonUtils.getOption(options, OPTION_DELETE_CASCADE)
								? " CASCADE CONSTRAINTS"
								: "")));
	}

	@Override
	public void renameObject(DBECommandContext commandContext, OceanbaseOracleTable object, Map<String, Object> options,
			String newName) throws DBException {
		processObjectRename(commandContext, object, options, newName);
	}

}
