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
import org.jkiss.dbeaver.model.impl.sql.edit.SQLStructEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Override
    protected void addStructObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions
            , SQLStructEditor<YashanDBTable, YashanDBSchema>.StructCreateCommand command, Map<String, Object> options) throws DBException {

        final YashanDBTable table = command.getObject();

        final NestedObjectCommand tableProps = command.getObjectCommands().get(table);
        if (tableProps == null) {
            log.warn("Object change command not found"); //$NON-NLS-1$
            return;
        }
        final String tableName = DBUtils.getEntityScriptName(table, options);

        final String slComment = SQLUtils.getDialectFromObject(table).getSingleLineComments()[0];
        final String lineSeparator = GeneralUtils.getDefaultLineSeparator();
        StringBuilder createQuery = new StringBuilder(100);
        createQuery.append(beginCreateTableStatement(monitor, table, tableName, options));
        boolean hasNestedDeclarations = false;

        final Collection<NestedObjectCommand> orderedCommands = getNestedOrderedCommands(command);

        for (NestedObjectCommand nestedCommand : orderedCommands) {
            if (nestedCommand.getObject() == table) {
                continue;
            }
            if (excludeFromDDL(nestedCommand, orderedCommands)) {
                continue;
            }
            if(nestedCommand.getObject() instanceof YashanDBTablePartition){
              continue;
            }
            final String nestedDeclaration = nestedCommand.getNestedDeclaration(monitor, table, options);

            if (!CommonUtils.isEmpty(nestedDeclaration)) {
                // Insert nested declaration
                if (hasNestedDeclarations) {
                    // Check for embedded comment
                    int lastLFPos = createQuery.lastIndexOf(lineSeparator);
                    int lastCommentPos = createQuery.lastIndexOf(slComment);
                    if (lastCommentPos != -1) {
                        while (lastCommentPos > 0 && Character.isWhitespace(createQuery.charAt(lastCommentPos - 1))) {
                            lastCommentPos--;
                        }
                    }
                    if (lastCommentPos < 0 || lastCommentPos < lastLFPos) {
                        createQuery.append(","); //$NON-NLS-1$
                    } else {
                        createQuery.insert(lastCommentPos, ","); //$NON-NLS-1$
                    }
                    createQuery.append(lineSeparator);
                }
                if (!hasNestedDeclarations && !hasAttrDeclarations(table)) {
                    createQuery.append("(\n\t").append(nestedDeclaration); //$NON-NLS-1$
                } else {
                    createQuery.append("\t").append(nestedDeclaration); //$NON-NLS-1$
                }
                hasNestedDeclarations = true;
            } else {
                // This command should be executed separately
                final DBEPersistAction[] nestedActions = nestedCommand.getPersistActions(monitor, executionContext, options);
                if (nestedActions != null) {
                    Collections.addAll(actions, nestedActions);
                }
            }
        }
        if (hasAttrDeclarations(table) || hasNestedDeclarations) {
            createQuery.append(lineSeparator);
            createQuery.append(")"); //$NON-NLS-1$
        }

        appendTableModifiers(monitor, table, tableProps, createQuery, false);
        // set tablespace, set table type
        createQuery.append(endCreateTableStatement(monitor, table, tableName, options));
        // set partitions
        List<NestedObjectCommand> partitionCommands = orderedCommands.stream().filter(o -> o.getObject() instanceof YashanDBTablePartition).collect(Collectors.toList());
        if(!partitionCommands.isEmpty()){
            List<String> partitions = new ArrayList<>();
            for (NestedObjectCommand partitionCommand : partitionCommands) {
                partitions.add(partitionCommand.getNestedDeclaration(monitor, table, options));
            }
            createQuery.append(createPartitionStatement(partitionCommands, partitions));
        }
        actions.add( 0, new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_table, createQuery.toString()) );
    }

    private String createPartitionStatement(List<NestedObjectCommand> partitionCommands, List<String> partitions) {
        NestedObjectCommand partitionCommand = partitionCommands.get(0);
        YashanDBTablePartition partition = (YashanDBTablePartition) partitionCommand.getObject();
        String partitionType = partition.getPartitionType();

        StringBuilder partSQL = new StringBuilder("PARTITION BY ");
        String collect = partition.getColumns().stream().map(YashanDBTableColumn::getName).collect(Collectors.joining(","));

        switch (partitionType) {
            case "RANGE":
                partSQL.append("RANGE");
                break;
            case "LIST":
                partSQL.append("LIST");
                break;
            default:
                partSQL.append("HASH");

        }
        partSQL.append("(")
                .append(collect)
                .append(")")
                .append("\n(")
                .append(String.join(",\n", partitions))
                .append(")");
        return partSQL.toString();
    }

    @Override
    protected String beginCreateTableStatement(DBRProgressMonitor monitor, YashanDBTable table, String tableName, Map<String, Object> options) throws DBException {
        StringBuilder createPreSQL = new StringBuilder();
        createPreSQL.append("CREATE");
        boolean isView = DBUtils.isView(table);
        if(!isView && table.isEditTemporary()){
            createPreSQL.append(" GLOBAL TEMPORARY ");
        }
        createPreSQL.append(isView ? " VIEW " : " TABLE ")
                .append(tableName)
                .append(" (")
                .append(GeneralUtils.getDefaultLineSeparator());

        return createPreSQL.toString();
    }

    @Override
    protected String endCreateTableStatement(DBRProgressMonitor monitor, YashanDBTable table, String tableName, Map<String, Object> options) throws DBException{
        StringBuilder createSufSQL = new StringBuilder(GeneralUtils.getDefaultLineSeparator());
        if(table.getTablespace() != null){
            String spaceName = getSpaceName(table);
            createSufSQL.append(table.getDataSource().isDistributed() ? "TABLESPACE SET " : "TABLESPACE ")
                    .append(spaceName)
                    .append(GeneralUtils.getDefaultLineSeparator());
        }
        if(table.getEditTableType() != null){
            createSufSQL.append("ORGANIZATION ")
                    .append(table.getEditTableType())
                    .append(GeneralUtils.getDefaultLineSeparator());
        }
        return createSufSQL.toString();
    }

    private static String getSpaceName(YashanDBTable table) throws DBException {
        String spaceName;
        if(table.getTablespace() instanceof YashanDBTablespace){
            YashanDBTablespace tablespace = (YashanDBTablespace) table.getTablespace();
            spaceName = tablespace.getName();
        }else {
            spaceName = String.valueOf(table.getTablespace());
        }
        if(table.isEditTemporary() && !"TEMP".equals(spaceName)){
            throw new DBException("The temporary table's tablespace setting is incorrect; only the TEMP tablespace is currently supported.");
        }
        return spaceName;
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
        // table comment
        if (command.getProperty("comment") != null) {
            actions.add(new SQLDatabasePersistAction(
                    "Comment table",
                    "COMMENT ON TABLE " +
                            table.getFullyQualifiedName(DBPEvaluationContext.DDL) +
                            " IS " + SQLUtils.quoteString(table, table.getComment())));
        }
        // column comments
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
//        ddl.append("LOGGING TABLESPACE \"USERS\"");
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
