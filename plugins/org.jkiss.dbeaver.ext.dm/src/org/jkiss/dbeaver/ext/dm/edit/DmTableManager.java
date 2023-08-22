package org.jkiss.dbeaver.ext.dm.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.DmSchema;
import org.jkiss.dbeaver.ext.dm.model.DmTable;
import org.jkiss.dbeaver.ext.dm.model.DmTableColumn;
import org.jkiss.dbeaver.ext.dm.model.DmTableConstraint;
import org.jkiss.dbeaver.ext.dm.model.DmTableForeignKey;
import org.jkiss.dbeaver.ext.dm.model.DmTableIndex;
import org.jkiss.dbeaver.ext.dm.model.DmTablespace;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

/**
 * DM Table Manager
 * 
 * @author caosw
 *
 */
public class DmTableManager extends SQLTableManager<DmTable, DmSchema> implements DBEObjectRenamer<DmTable> {

	private static final  Class<?>[] CHILD_TYPES = { DmTableColumn.class, DmTableConstraint.class, DmTableIndex.class };

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, DmTable> getObjectsCache(DmTable object) {
		return (DBSObjectCache) object.getSchema().tableCache;
	}

	@Override
	protected DmTable createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container,
			Object copyFrom, Map<String, Object> options) throws DBException {
		DmSchema schema = (DmSchema) container;
		DmTable table = new DmTable(schema, "");
		setNewObjectName(monitor, schema, table);
		return table;
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, SQLObjectEditor<DmTable, DmSchema>.ObjectChangeCommand command,
			Map<String, Object> options) throws DBException {
		/**
		 * 此处三种情况:
		 * 1. tablespace和comment都改:需要进入修改tablespace
		 * 2. 只改tablespace :需要进入修改tablespace
		 * 3. 只改comment : 不需要进入修改tablespace
		 */
		if (command.getProperties().size() > 1 || (command.getProperty("comment") == null&&!command.hasProperty("comment"))) {
			StringBuilder query = new StringBuilder("ALTER TABLE ");
			query.append(command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" ");
			appendTableModifiers(monitor, command.getObject(), command, query, true);
			actionList.add(new SQLDatabasePersistAction(query.toString()));
		}
	}

	@Override
	protected void addObjectExtraActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions,
			NestedObjectCommand<DmTable, SQLObjectEditor<DmTable, DmSchema>.PropertyHandler> command,
			Map<String, Object> options) throws DBException {
		DmTable table = command.getObject();
		if (command.getProperty("comment") != null ) { //当包含comment时说明对其进行了修改
			actions.add(new SQLDatabasePersistAction("Comment table",
					"COMMENT ON TABLE " + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + " IS "
							+ SQLUtils.quoteString(table, table.getComment())));
		}else {
			if(command.hasProperty("comment")) { //当包含有comment时说明将表注释清空
				actions.add(new SQLDatabasePersistAction("Comment table",
						"COMMENT ON TABLE " + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + " IS ''"));
			}
		}

		// Column comments  去除注释
		/**for (DmTableColumn column : CommonUtils.safeCollection(table.getAttributes(monitor))) {
			if (!CommonUtils.isEmpty(column.getDescription())) {
				DmTableColumnManager.addColumnCommentAction(actions, column);
			}
		}*/
	}

	@Override
	protected void appendTableModifiers(DBRProgressMonitor monitor, DmTable table, NestedObjectCommand tableProps,
			StringBuilder ddl, boolean alter) {
		if (tableProps.getProperty("tablespace") != null) {
			Object tablespace = table.getTablespace();
			if (tablespace instanceof DmTablespace) {
				if (table.isPersisted()) {
					ddl.append("\nMOVE TABLESPACE ").append(((DmTablespace) tablespace).getName());
				} else {
					ddl.append("\nTABLESPACE ").append(((DmTablespace) tablespace).getName());
				}
			}
		}
	}

	@Override
	protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<DmTable, DmSchema>.ObjectRenameCommand command,
			Map<String, Object> options) {
		actions.add(new SQLDatabasePersistAction("Rename table",
				"ALTER TABLE " + DBUtils.getQuotedIdentifier(command.getObject().getSchema()) + "."
						+ DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getOldName())
						+ " RENAME TO "
						+ DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getNewName())));
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<DmTable, DmSchema>.ObjectDeleteCommand command,
			Map<String, Object> options) {
		DmTable object = command.getObject();
		String sqlString=				"DROP " + (object.isView() ? "VIEW" : "TABLE") + " "
				+ object.getFullyQualifiedName(DBPEvaluationContext.DDL)
				+ (!object.isView() && CommonUtils.getOption(options, OPTION_DELETE_CASCADE));
		actions.add(new SQLDatabasePersistAction(ModelMessages.model_jdbc_drop_table,
				"DROP " + (object.isView() ? "VIEW" : "TABLE") + " "
						+ object.getFullyQualifiedName(DBPEvaluationContext.DDL)
						+ (!object.isView() && CommonUtils.getOption(options, OPTION_DELETE_CASCADE)
								? " CASCADE CONSTRAINTS"
								: "")));
	}

	@NotNull
	@Override
	public  Class<? extends DBSObject>[]getChildTypes() {
		return (Class<? extends DBSObject>[])CHILD_TYPES;
	}

	@Override
	public void renameObject(DBECommandContext commandContext, DmTable object, Map<String, Object> options,
			String newName) throws DBException {
		processObjectRename(commandContext, object, options, newName);
	}

	
}
