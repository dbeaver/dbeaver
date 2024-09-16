/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mssql.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.model.*;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLStructEditor;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * SQLServer table manager
 */
public class SQLServerTableManager extends SQLServerBaseTableManager<SQLServerTableBase> {

    private static final Class<? extends DBSObject>[] CHILD_TYPES = CommonUtils.array(
        SQLServerTableColumn.class,
        SQLServerTableUniqueKey.class,
        SQLServerTableForeignKey.class,
        SQLServerTableIndex.class,
        SQLServerTableCheckConstraint.class,
        SQLServerExtendedProperty.class
        );

    @Override
    protected SQLServerTable createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, Object container, Object copyFrom, @NotNull Map<String, Object> options)
    {
        SQLServerSchema schema = (SQLServerSchema) container;
        SQLServerTable table = new SQLServerTable(schema);
        setNewObjectName(monitor, schema, table);
        return table; //$NON-NLS-1$
    }

    @Override
    protected void addObjectModifyActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actionList, @NotNull ObjectChangeCommand command, @NotNull Map<String, Object> options)
    {
        if (command.getProperties().size() > 1 || command.getProperty(DBConstants.PROP_ID_DESCRIPTION) == null) {
            StringBuilder query = new StringBuilder("ALTER TABLE "); //$NON-NLS-1$
            query.append(command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" "); //$NON-NLS-1$
            appendTableModifiers(monitor, command.getObject(), command, query, true);
            actionList.add(new SQLDatabasePersistAction(query.toString()));
        }
    }

    @Override
    protected void appendTableModifiers(DBRProgressMonitor monitor, SQLServerTableBase table, NestedObjectCommand tableProps, StringBuilder ddl, boolean alter)
    {
        // ALTER
/*
        if (tableProps.getProperty("tablespace") != null) { //$NON-NLS-1$
            Object tablespace = table.getTablespace();
            if (tablespace instanceof SQLServerTablespace) {
                if (table.isPersisted()) {
                    ddl.append("\nMOVE TABLESPACE ").append(((SQLServerTablespace) tablespace).getName()); //$NON-NLS-1$
                } else {
                    ddl.append("\nTABLESPACE ").append(((SQLServerTablespace) tablespace).getName()); //$NON-NLS-1$
                }
            }
        }
*/
    }

    @Override
    protected void addObjectDeleteActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectDeleteCommand command, @NotNull Map<String, Object> options)
    {
        SQLServerTableBase object = command.getObject();
        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_drop_table,
                "DROP " + getCreateTableType(object) +  //$NON-NLS-2$
                    " " + object.getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    (!object.isView() && CommonUtils.getOption(options, OPTION_DELETE_CASCADE) ? " CASCADE CONSTRAINTS" : "")
            )
        );
    }

    @NotNull
    @Override
    public Class<? extends DBSObject>[] getChildTypes()
    {
        return CHILD_TYPES;
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull SQLServerTableBase object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException
    {
        processObjectRename(commandContext, object, options, newName);
    }

    @Override
    protected boolean isIncludeIndexInDDL(@NotNull DBRProgressMonitor monitor, @NotNull DBSTableIndex index) throws DBException {
        Collection<? extends DBSEntityConstraint> constraints = index.getTable().getConstraints(monitor);
        if (constraints.size() > 0 && index.isUnique()) {
            for (DBSEntityConstraint constraint : constraints) {
                if (constraint instanceof SQLServerTableUniqueKey
                    && constraint.getConstraintType() == DBSEntityConstraintType.UNIQUE_KEY
                    && ((SQLServerTableUniqueKey) constraint).getIndex() == index
                ) {
                   return false;
                }
            }
        }
        
        return !index.isPrimary() && super.isIncludeIndexInDDL(monitor, index);
    }

    protected void addExtraDDLCommands(DBRProgressMonitor monitor, SQLServerTableBase table, Map<String, Object> options, SQLStructEditor.StructCreateCommand createCommand) {
        SQLObjectEditor<SQLServerTableCheckConstraint, SQLServerTableBase> ccm = getObjectEditor(
            DBWorkbench.getPlatform().getEditorsRegistry(),
            SQLServerTableCheckConstraint.class);
        if (ccm != null) {
            try {
                if (table instanceof SQLServerTable) {
                    Collection<SQLServerTableCheckConstraint> checkConstraints = CommonUtils.safeCollection(((SQLServerTable) table).getCheckConstraints(monitor));
                    if (!CommonUtils.isEmpty(checkConstraints)) {
                        for (SQLServerTableCheckConstraint checkConstraint : checkConstraints) {
                            createCommand.aggregateCommand(ccm.makeCreateCommand(checkConstraint, options));
                        }
                    }
                }
            } catch (DBException e) {
                // Ignore indexes
                log.debug(e);
            }
        }
    }

    @Override
    protected void addObjectExtraActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actionList,
        @NotNull NestedObjectCommand<SQLServerTableBase, PropertyHandler> command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        super.addObjectExtraActions(monitor, executionContext, actionList, command, options);
        SQLServerTableBase tableBase = command.getObject();
        if (!tableBase.isPersisted()) {
            // Column comments for the newly created table
            for (SQLServerTableColumn column : CommonUtils.safeCollection(tableBase.getAttributes(monitor))) {
                if (!CommonUtils.isEmpty(column.getDescription())) {
                    SQLServerTableColumnManager.addColumnCommentAction(actionList, column, false);
                }
            }
        }
    }
}
