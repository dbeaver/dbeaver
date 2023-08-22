package org.jkiss.dbeaver.ext.yashandb.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.*;
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

import java.util.List;
import java.util.Map;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description: the manager of changing Properties in right UI.
 */
public class YashanDBTableManager extends SQLTableManager<YashanDBTable, YashanDBSchema> implements DBEObjectRenamer<YashanDBTable> {

    /**
     * it relates to right logic of creating new column action while creating new table.
     */
    private static final Class<? extends DBSObject>[] CHILD_TYPES = CommonUtils.array(
            YashanDBTableColumn.class,
            YashanDBTableConstraint.class,
            YashanDBTableForeignKey.class,
            YashanDBTableIndex.class,
            YashanDBTablePartition.class
    );

    /**
     * create database action and give a default table which name is "".
     */
    @Override
    protected YashanDBTable createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options) throws DBException {
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

    /**
     * add modify action into actionList when command action number more than twice.
     * make this function cannot be executed.
     */
    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
                                          List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) {
//        if (command.getProperties().size() > 1) {
//            StringBuilder query = new StringBuilder("ALTER TABLE ");
//            query.append(command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" ");
//            appendTableModifiers(monitor, command.getObject(), command, query, true);
//            actionList.add(new SQLDatabasePersistAction(query.toString()));
//        }
    }

    /**
     * when change Comments,use this method to create relative sql to execute.
     */
    @Override
    protected void addObjectExtraActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
                                         List<DBEPersistAction> actions, NestedObjectCommand<YashanDBTable, PropertyHandler> command,
                                         Map<String, Object> options) throws DBException {
        YashanDBTable table = command.getObject();
        if (command.getProperty("comment") != null) {
            actions.add(new SQLDatabasePersistAction(
                    "Comment table",
                    "COMMENT ON TABLE " +
                            table.getFullyQualifiedName(DBPEvaluationContext.DDL) +
                            " IS " + SQLUtils.quoteString(table, table.getComment())));
        }

//        if (command.getProperty("comment") == null) {
//            actions.add(new SQLDatabasePersistAction(
//                    "Comment table",
//                    "COMMENT ON TABLE " +
//                            table.getFullyQualifiedName(DBPEvaluationContext.DDL) +
//                            " IS '" + "'"));
//        }
        if (!table.isPersisted()) {
            // Column comments for the newly created table
            for (YashanDBTableColumn column : CommonUtils.safeCollection(table.getAttributes(monitor))) {
                if (!CommonUtils.isEmpty(column.getDescription())) {
                    YashanDBTableColumnManager.addColumnCommentAction(actions, column, column.getTable());
                }
            }
        }
    }

    /**
     * check table space whether need to be modified when modify table.
     */
    @Override
    protected void appendTableModifiers(DBRProgressMonitor monitor, YashanDBTable table, NestedObjectCommand tableProps, StringBuilder ddl, boolean alter) {
        // ALTER
//        if (tableProps.getProperty("tablespace") != null) {
//            Object tablespace = table.getTablespace();
//            if (tablespace instanceof YashanDBTablespace) {
//                if (table.isPersisted()) {
//                    ddl.append("\nMOVE TABLESPACE ").append(((YashanDBTablespace) tablespace).getName());
//                } else {
//                    ddl.append("\nTABLESPACE ").append(((YashanDBTablespace) tablespace).getName());
//                }
//            }
//        }

        //TMP
        ddl.append("LOGGING TABLESPACE \"USERS\"");
    }

    /**
     * rename Actions,when change Table Name, it will be acted.
     */
    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor,
                                          DBCExecutionContext executionContext, List<DBEPersistAction> actions,
                                          ObjectRenameCommand command, Map<String, Object> options) {
        actions.add(
                new SQLDatabasePersistAction(
                        "Rename table",
                        "ALTER TABLE " +
                                DBUtils.getQuotedIdentifier(command.getObject().getSchema()) + "." +
                                DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getOldName()) +
                                " RENAME TO " + DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getNewName()))
        );
    }

    /**
     * delete action on table.
     */
    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
                                          List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
        YashanDBTable object = command.getObject();
        actions.add(
                new SQLDatabasePersistAction(
                        ModelMessages.model_jdbc_drop_table,
                        "DROP " + (object.isView() ? "VIEW" : "TABLE") + " " +
                                object.getFullyQualifiedName(DBPEvaluationContext.DDL) +
                                (!object.isView() && CommonUtils.getOption(options, OPTION_DELETE_CASCADE) ? " CASCADE CONSTRAINTS" : "")
                )
        );
    }


    /**
     * This is crucial step in rename object,it will light the save button when changing TableName box.
     */
    @Override
    public void renameObject(DBECommandContext commandContext, YashanDBTable object, Map<String, Object> options, String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    /**
     * it relates to right logic of creating new column action while creating new table.
     */
    @NotNull
    @Override
    public Class<? extends DBSObject>[] getChildTypes() {
        return CHILD_TYPES;
    }
}
