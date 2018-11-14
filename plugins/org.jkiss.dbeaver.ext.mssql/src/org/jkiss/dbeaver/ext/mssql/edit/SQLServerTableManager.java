/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.ext.mssql.model.*;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLScriptCommand;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * SQLServer table manager
 */
public class SQLServerTableManager extends SQLTableManager<SQLServerTable, SQLServerSchema> implements DBEObjectRenamer<SQLServerTable> {

    private static final Class<?>[] CHILD_TYPES = {
        SQLServerTableColumn.class,
        SQLServerTableUniqueKey.class,
        SQLServerTableForeignKey.class,
        SQLServerTableIndex.class,
        SQLServerTableCheckConstraint.class,
    };

    @Override
    public DBSObjectCache<SQLServerSchema, SQLServerTable> getObjectsCache(SQLServerTable object) {
        return (DBSObjectCache) object.getSchema().getTableCache();
    }

    @Override
    protected SQLServerTable createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, SQLServerSchema parent, Object copyFrom)
    {
        SQLServerTable table = new SQLServerTable(parent);
        try {
            setTableName(monitor, parent, table);
        } catch (DBException e) {
            log.error(e);
        }
        return table; //$NON-NLS-1$
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        if (command.getProperties().size() > 1 || command.getProperty(DBConstants.PROP_ID_DESCRIPTION) == null) {
            StringBuilder query = new StringBuilder("ALTER TABLE "); //$NON-NLS-1$
            query.append(command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" "); //$NON-NLS-1$
            appendTableModifiers(monitor, command.getObject(), command, query, true);
            actionList.add(new SQLDatabasePersistAction(query.toString()));
        }
    }

    @Override
    protected void addObjectExtraActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, NestedObjectCommand<SQLServerTable, PropertyHandler> command, Map<String, Object> options) {
        final SQLServerTable table = command.getObject();
        if (command.getProperty(DBConstants.PROP_ID_DESCRIPTION) != null) {
            boolean isUpdate = SQLServerUtils.isCommentSet(
                monitor,
                table.getDatabase(),
                SQLServerObjectClass.OBJECT_OR_COLUMN,
                table.getObjectId(),
                0);
            actionList.add(
                new SQLDatabasePersistAction(
                    "Add table comment",
                    "EXEC " + SQLServerUtils.getSystemTableName(table.getDatabase(), isUpdate ? "sp_updateextendedproperty" : "sp_addextendedproperty") +
                        " 'MS_Description', " + SQLUtils.quoteString(command.getObject(), command.getObject().getDescription()) + "," +
                        " 'user', '" + table.getSchema().getName() + "'," +
                        " 'table', '" + table.getName() + "'"));
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
    protected void addObjectRenameActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options)
    {
        SQLServerTable table = command.getObject();
        actions.add(
            new SQLDatabasePersistAction(
                "Rename table",
                "EXEC " + SQLServerUtils.getSystemTableName(table.getDatabase(), "sp_rename") +
                    " '" + table.getSchema().getFullyQualifiedName(DBPEvaluationContext.DML) + "." + DBUtils.getQuotedIdentifier(table.getDataSource(), command.getOldName()) +
                    "' , '" + DBUtils.getQuotedIdentifier(table.getDataSource(), command.getNewName()) + "', 'TABLE'")
        );
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
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
    protected boolean isIncludeIndexInDDL(DBSTableIndex index) {
        return !index.isPrimary() && super.isIncludeIndexInDDL(index);
    }

    protected void addExtraDDLCommands(DBRProgressMonitor monitor, SQLServerTable table, Map<String, Object> options, StructCreateCommand createCommand) {
        SQLObjectEditor<SQLServerTableCheckConstraint, SQLServerTable> ccm = getObjectEditor(
            table.getDataSource().getContainer().getPlatform().getEditorsRegistry(),
            SQLServerTableCheckConstraint.class);
        if (ccm != null) {
            try {
                Collection<SQLServerTableCheckConstraint> checkConstraints = CommonUtils.safeCollection(table.getCheckConstraints(monitor));
                if (!CommonUtils.isEmpty(checkConstraints)) {
                    for (SQLServerTableCheckConstraint checkConstraint : checkConstraints) {
                        createCommand.aggregateCommand(ccm.makeCreateCommand(checkConstraint));
                    }
                }
            } catch (DBException e) {
                // Ignore indexes
                log.debug(e);
            }
        }
    }

}
