package org.jkiss.dbeaver.ext.yashandb.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.*;
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
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBTableColumnManager extends SQLTableColumnManager<YashanDBTableColumn, YashanDBTableBase> implements DBEObjectRenamer<YashanDBTableColumn> {


    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, YashanDBTableColumn> getObjectsCache(YashanDBTableColumn object) {
        return object.getParentObject().getContainer().tableCache.getChildrenCache(object.getParentObject());
    }

    @Override
    protected ColumnModifier[] getSupportedModifiers(YashanDBTableColumn column, Map<String, Object> options) {
        return new ColumnModifier[] { DataTypeModifier, DefaultModifier, NullNotNullModifierConditional };
    }

    /**
     * it depends whether the properties of columns can be edited.
     */
    @Override
    public boolean canEditObject(YashanDBTableColumn object) {
        return true;
    }

    /**
     * the initialization of column value.
     */
    @Override
    protected YashanDBTableColumn createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container,
                                                       Object copyFrom, Map<String, Object> options) throws DBException {
        YashanDBTableBase table = (YashanDBTableBase) container;

        DBSDataType columnType = findBestDataType(table, "varchar");
        final YashanDBTableColumn column = new YashanDBTableColumn(table);
        column.setName(getNewColumnName(monitor, context, table));
        column.setDataType((YashanDBDataType) columnType);
        column.setTypeName(columnType == null ? "INTEGER" : columnType.getName());
        column.setMaxLength(columnType != null && columnType.getDataKind() == DBPDataKind.STRING ? 100 : 0);
        column.setValueType(columnType == null ? Types.INTEGER : columnType.getTypeID());
        column.setOrdinalPosition(-1);
        return column;
    }

    /**
     * comment can not be initialized when create new a column or table in yashan and oracle.
     * 'command.getProperty("comment") != null' equals false always.
     */
    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
                                          List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) {
        super.addObjectCreateActions(monitor, executionContext, actions, command, options);
        if (command.getProperty("comment") != null) {
//            addColumnCommentAction(actions, command.getObject(), command.getObject().getParentObject());
            addColumnCommentAction(actions, command.getObject(),true);
        }
    }

    @Override
    public void renameObject(DBECommandContext commandContext, YashanDBTableColumn object, Map<String, Object> options, String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    /**
     * add rename object actions by executing sql.
     */
    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
                                          List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options) {
        final YashanDBTableColumn column = command.getObject();
        actions.add(
                new SQLDatabasePersistAction(
                        "Rename column",
                        "ALTER TABLE " +
                                column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) +
                                " RENAME COLUMN " +
                                DBUtils.getQuotedIdentifier(column.getDataSource(), command.getOldName()) + " TO " +
                                DBUtils.getQuotedIdentifier(column.getDataSource(), command.getNewName())
                )
        );
    }


    @Override
    public boolean canRenameObject(YashanDBTableColumn object) {
        return false;
    }


    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) {
        final YashanDBTableColumn column = command.getObject();
        boolean hasComment = command.getProperty("comment") != null;

        actionList.add(new SQLDatabasePersistAction(
                "Modify column",
                "ALTER TABLE " + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) +
                        " MODIFY " + getNestedDeclaration(monitor, column.getTable(), command, options)));

            addColumnCommentAction(actionList, column,hasComment);
//           addColumnCommentAction(actionList, column, column.getTable());

    }

    /**
     * use self-defined function,not provided by SQLTableColumnManager. It will work.
     */
    static void addColumnCommentAction(List<DBEPersistAction> actionList, YashanDBTableColumn column,boolean hasComment) {
        if(hasComment){
            actionList.add(new SQLDatabasePersistAction("Comment column",
                    "COMMENT ON COLUMN " + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + "."
                            + DBUtils.getQuotedIdentifier(column) + " IS '" +
                            column.getComment(new VoidProgressMonitor())
                            + "'"));
        }else{
            actionList.add(new SQLDatabasePersistAction("Comment column",
                    "COMMENT ON COLUMN " + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + "."
                            + DBUtils.getQuotedIdentifier(column) + " IS ''"));
        }

    }


}
