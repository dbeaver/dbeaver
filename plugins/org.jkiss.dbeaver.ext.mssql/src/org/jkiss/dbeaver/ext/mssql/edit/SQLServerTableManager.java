/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * SQLServer table manager
 */
public class SQLServerTableManager extends SQLServerBaseTableManager<SQLServerTable> {

    private static final Class<?>[] CHILD_TYPES = {
        SQLServerTableColumn.class,
        SQLServerTableUniqueKey.class,
        SQLServerTableForeignKey.class,
        SQLServerTableIndex.class,
        SQLServerTableCheckConstraint.class,
    };

    @Override
    protected SQLServerTable createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options)
    {
        SQLServerSchema schema = (SQLServerSchema) container;
        SQLServerTable table = new SQLServerTable(schema);
        setNewObjectName(monitor, schema, table);
        return table; //$NON-NLS-1$
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        if (command.getProperties().size() > 1 || command.getProperty(DBConstants.PROP_ID_DESCRIPTION) == null) {
            StringBuilder query = new StringBuilder("ALTER TABLE "); //$NON-NLS-1$
            query.append(command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" "); //$NON-NLS-1$
            appendTableModifiers(monitor, command.getObject(), command, query, true);
            actionList.add(new SQLDatabasePersistAction(query.toString()));
        }
    }

    @Override
    protected void appendTableModifiers(DBRProgressMonitor monitor, SQLServerTable table, NestedObjectCommand tableProps, StringBuilder ddl, boolean alter)
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
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        SQLServerTable object = command.getObject();
        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_drop_table,
                "DROP " + (object.isView() ? "VIEW" : "TABLE") +  //$NON-NLS-2$
                    " " + object.getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    (!object.isView() && CommonUtils.getOption(options, OPTION_DELETE_CASCADE) ? " CASCADE CONSTRAINTS" : "")
            )
        );
    }

    @NotNull
    @Override
    public Class<?>[] getChildTypes()
    {
        return CHILD_TYPES;
    }

    @Override
    public void renameObject(DBECommandContext commandContext, SQLServerTable object, String newName) throws DBException
    {
        processObjectRename(commandContext, object, newName);
    }

    @Override
    protected boolean isIncludeIndexInDDL(DBRProgressMonitor monitor, DBSTableIndex index) throws DBException {
        return !index.isPrimary() && super.isIncludeIndexInDDL(monitor, index);
    }

    protected void addExtraDDLCommands(DBRProgressMonitor monitor, SQLServerTable table, Map<String, Object> options, SQLStructEditor.StructCreateCommand createCommand) {
        SQLObjectEditor<SQLServerTableCheckConstraint, SQLServerTable> ccm = getObjectEditor(
            table.getDataSource().getContainer().getPlatform().getEditorsRegistry(),
            SQLServerTableCheckConstraint.class);
        if (ccm != null) {
            try {
                Collection<SQLServerTableCheckConstraint> checkConstraints = CommonUtils.safeCollection(table.getCheckConstraints(monitor));
                if (!CommonUtils.isEmpty(checkConstraints)) {
                    for (SQLServerTableCheckConstraint checkConstraint : checkConstraints) {
                        createCommand.aggregateCommand(ccm.makeCreateCommand(checkConstraint, options));
                    }
                }
            } catch (DBException e) {
                // Ignore indexes
                log.debug(e);
            }
        }
    }

}
