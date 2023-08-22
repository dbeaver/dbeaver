package org.jkiss.dbeaver.ext.dm.edit;

import java.sql.Types;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.DmDataType;
import org.jkiss.dbeaver.ext.dm.model.DmTable;
import org.jkiss.dbeaver.ext.dm.model.DmTableBase;
import org.jkiss.dbeaver.ext.dm.model.DmTableColumn;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableColumnManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

/**
 * DM Table Column Manager
 * 
 *
 * 如果需要修改创建表时列相关操作，在本类中进行操作即可
 */
public class DmTableColumnManager extends SQLTableColumnManager<DmTableColumn, DmTableBase>
		implements DBEObjectRenamer<DmTableColumn> {

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, DmTableColumn> getObjectsCache(DmTableColumn object) {
		return object.getParentObject().getContainer().tableCache.getChildrenCache(object.getParentObject());
	}

	@Override
	protected ColumnModifier[] getSupportedModifiers(DmTableColumn column, Map<String, Object> options) {
		return new ColumnModifier[] { DataTypeModifier, DefaultModifier, NullNotNullModifierConditional };
	}

	@Override
	public boolean canEditObject(DmTableColumn object) {
		return true;
	}

	@Override
	protected DmTableColumn createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			Object container, Object copyFrom, Map<String, Object> options) throws DBException {
        DmTableBase table = (DmTableBase) container;

        DBSDataType columnType = findBestDataType(table.getDataSource(), "varchar2"); //$NON-NLS-1$

        final DmTableColumn column = new DmTableColumn(table);
        column.setName(getNewColumnName(monitor, context, table));
        column.setDataType((DmDataType) columnType);
        column.setTypeName(columnType == null ? "INTEGER" : columnType.getName()); //$NON-NLS-1$
        column.setMaxLength(columnType != null && columnType.getDataKind() == DBPDataKind.STRING ? 100 : 0);
        /**
         * 表的列图标显示和valueType 这个属性的值有关，DM之前未显示是因为其都是默认的1111 值 即和java.sql.Types.OTHER 属性值有关
         */
        column.setValueType(columnType == null ? Types.INTEGER : columnType.getTypeID());
        column.setOrdinalPosition(-1);
        return column;
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<DmTableColumn, DmTableBase>.ObjectCreateCommand command,
			Map<String, Object> options) {
		super.addObjectCreateActions(monitor, executionContext, actions, command, options);
		if (command.getProperty("comment") != null) {
			addColumnCommentAction(actions, command.getObject());
		}
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, SQLObjectEditor<DmTableColumn, DmTableBase>.ObjectChangeCommand command,
			Map<String, Object> options) throws DBException {
		final DmTableColumn column = command.getObject();
		boolean hasComment = command.getProperty("comment") != null;
		if (!hasComment || command.getProperties().size() > 1) {
			actionList.add(new SQLDatabasePersistAction("Modify column",
					"ALTER TABLE " + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + " MODIFY "
							+ getNestedDeclaration(monitor, column.getTable(), command, options)));
		}
		if (hasComment) {
			addColumnCommentAction(actionList, column);
		}
	}

	static void addColumnCommentAction(List<DBEPersistAction> actionList, DmTableColumn column) {
		actionList.add(new SQLDatabasePersistAction("Comment column",
				"COMMENT ON COLUMN " + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + "."
						+ DBUtils.getQuotedIdentifier(column) + " IS '" + column.getComment(new VoidProgressMonitor())
						+ "'"));
	}
	
	
    @Override //获取表相关配置，例如自增等等
    protected StringBuilder getNestedDeclaration(DBRProgressMonitor monitor, DmTableBase owner, DBECommandAbstract<DmTableColumn> command, Map<String, Object> options)
    {
        StringBuilder decl = super.getNestedDeclaration(monitor, owner, command, options);
        final DmTableColumn column = command.getObject();
        /**
         * DM 只能在创建表时添加自增 此处需要进行判断
         */
        if (column.isAutoGenerated())   
            {
                decl.append(" IDENTITY"); 
            }
        return decl;
    }



	@Override
	protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<DmTableColumn, DmTableBase>.ObjectRenameCommand command,
			Map<String, Object> options) {
		final DmTableColumn column = command.getObject();
		actions.add(new SQLDatabasePersistAction("Rename column",
				"ALTER TABLE " + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + " RENAME COLUMN "
						+ DBUtils.getQuotedIdentifier(column.getDataSource(), command.getOldName()) + " TO "
						+ DBUtils.getQuotedIdentifier(column.getDataSource(), command.getNewName())));
	}

	@Override
	public void renameObject(DBECommandContext commandContext, DmTableColumn object, Map<String, Object> options,
			String newName) throws DBException {
			processObjectRename(commandContext, object,options,newName);
	}

}
