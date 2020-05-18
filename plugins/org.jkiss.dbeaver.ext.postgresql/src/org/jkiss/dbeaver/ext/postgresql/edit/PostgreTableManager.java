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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Postgre table manager
 */
public class PostgreTableManager extends PostgreTableManagerBase implements DBEObjectRenamer<PostgreTableBase> {

    private static final Class<?>[] CHILD_TYPES = {
        PostgreTableColumn.class,
        PostgreTableConstraint.class,
        PostgreTableForeignKey.class,
        PostgreIndex.class
    };

    @Nullable
    @Override
    public DBSObjectCache<PostgreTableContainer, PostgreTableBase> getObjectsCache(PostgreTableBase object)
    {
        return object.getContainer().getSchema().tableCache;
    }

    @Override
    public Collection<? extends DBSObject> getChildObjects(DBRProgressMonitor monitor, PostgreTableBase object, Class<? extends DBSObject> childType) throws DBException {
        if (childType == PostgreTableColumn.class) {
            return object.getAttributes(monitor);
        } else if (childType == PostgreTableConstraint.class) {
            return object.getConstraints(monitor);
        } else if (childType == PostgreTableForeign.class) {
            return object.getAssociations(monitor);
        } else if (childType == PostgreIndex.class) {
            return object.getIndexes(monitor);
        }
        return null;
    }

    @Override
    protected String beginCreateTableStatement(PostgreTableBase table, String tableName) {
        return "CREATE " +
            getCreateTableType(table) + " " + tableName + " (" + GeneralUtils.getDefaultLineSeparator();
    }

    @Override
    protected String getCreateTableType(PostgreTableBase table) {
        if (table instanceof PostgreTableForeign) {
            return "FOREIGN TABLE";
        } else {
            return table.getPersistence().getTableTypeClause();
        }
    }

    @Override
    protected PostgreTableBase createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options) throws DBException {
        PostgreSchema schema = (PostgreSchema)container;
        final PostgreTableBase table = schema.getDataSource().getServerType().createNewRelation(monitor, schema, PostgreClass.RelKind.r, copyFrom);
        if (CommonUtils.isEmpty(table.getName())) {
            setNewObjectName(monitor, schema, table);
        } else {
            table.setName(getNewChildName(monitor, schema, table.getName()));
        }

        return table;
    }

    @Override
    protected void addStructObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, StructCreateCommand command, Map<String, Object> options) throws DBException {
        PostgreTableBase tableBase = command.getObject();

        if (tableBase.isPersisted()) {
            String tableDDL = tableBase.getDataSource().getServerType().readTableDDL(monitor, tableBase);
            if (tableDDL != null) {
                actions.add(0, new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_table, tableDDL));
                return;
            }
        }
        super.addStructObjectCreateActions(monitor, executionContext, actions, command, options);
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        if (command.getProperties().size() > 1 || command.getProperty(DBConstants.PROP_ID_DESCRIPTION) == null) {
            if (command.getObject() instanceof PostgreTable) {
                try {
                    generateAlterActions(monitor, actionList, command);
                } catch (DBException e) {
                    log.error(e);
                }
            }
        }
    }

    private void generateAlterActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command) throws DBException {
        final PostgreTableRegular table = (PostgreTableRegular) command.getObject();
        final String alterPrefix = "ALTER TABLE " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL) + " ";

        if (command.hasProperty("partitionKey")) {
            actionList.add(new SQLDatabasePersistAction(alterPrefix + "PARTITION BY " + table.getPartitionKey()));
        }
        if (command.hasProperty("hasOids")) {
            actionList.add(new SQLDatabasePersistAction(alterPrefix + (table.isHasOids() ? "SET WITH OIDS" : "SET WITHOUT OIDS")));
        }
        if (command.hasProperty("tablespace")) {
            actionList.add(new SQLDatabasePersistAction(alterPrefix + "SET TABLESPACE " + table.getTablespace(monitor).getName()));
        }
    }

    @Override
    protected void appendTableModifiers(DBRProgressMonitor monitor, PostgreTableBase tableBase, NestedObjectCommand tableProps, StringBuilder ddl, boolean alter) {
        ddl.append(tableBase.getDataSource().getServerType().getTableModifiers(monitor, tableBase, alter));
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options)
    {
        actions.add(
            new SQLDatabasePersistAction(
                "Rename table",
                "ALTER TABLE " + DBUtils.getQuotedIdentifier(command.getObject().getSchema()) + "." + DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getOldName()) + //$NON-NLS-1$
                    " RENAME TO " + DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getNewName())) //$NON-NLS-1$
        );
    }

    @NotNull
    @Override
    public Class<?>[] getChildTypes()
    {
        return CHILD_TYPES;
    }

    @Override
    public void renameObject(DBECommandContext commandContext, PostgreTableBase object, String newName) throws DBException
    {
        processObjectRename(commandContext, object, newName);
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        PostgreTableBase table = command.getObject();
        final String tableName = DBUtils.getEntityScriptName(table, options);
        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_drop_table,
                "DROP " + (table instanceof PostgreTableForeign ? "FOREIGN TABLE" : "TABLE") +  //$NON-NLS-2$
                    " " + tableName +  //$NON-NLS-2$
                    (CommonUtils.getOption(options, OPTION_DELETE_CASCADE) ? " CASCADE" : "")
            )
        );
    }
    
}
