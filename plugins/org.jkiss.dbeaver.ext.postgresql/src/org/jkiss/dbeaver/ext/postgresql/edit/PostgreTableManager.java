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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistActionAtomic;
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

    private static final Class<? extends DBSObject>[] CHILD_TYPES = CommonUtils.array(
        PostgreTableColumn.class,
        PostgreTableConstraint.class,
        PostgreTableForeignKey.class,
        PostgreTablePolicy.class,
        PostgreIndex.class
    );

    @Nullable
    @Override
    public DBSObjectCache<PostgreTableContainer, PostgreTableBase> getObjectsCache(PostgreTableBase object)
    {
        return object.getContainer().getSchema().getTableCache();
    }

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return super.getMakerOptions(dataSource) | FEATURE_SUPPORTS_COPY;
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
    protected String beginCreateTableStatement(DBRProgressMonitor monitor, PostgreTableBase table, String tableName, Map<String, Object> options) throws DBException{
        String statement = "CREATE " + getCreateTableType(table) + " "; //$NON-NLS-1$ //$NON-NLS-2$
        if (table.isPartition() && table instanceof PostgreTable) {
            PostgreTable postgreTable = (PostgreTable) table;
            List<PostgreTableBase> superTables = postgreTable.getSuperTables(monitor);
            if (superTables == null || superTables.size() != 1) {
                log.error("Cant't read partition parent table name for table " + table.getFullyQualifiedName(DBPEvaluationContext.DDL));
            } else {
                String parent = superTables.get(0).getFullyQualifiedName(DBPEvaluationContext.DDL);
                String range = postgreTable.getPartitionRange(monitor);
                return statement + tableName + " PARTITION OF " + parent + " " + range;//$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        return statement + tableName + " (" + GeneralUtils.getDefaultLineSeparator();//$NON-NLS-1$
    }

    @Override
    protected boolean hasAttrDeclarations(PostgreTableBase table) {
        return !table.isPartition();
    }

    @Override
    protected String getCreateTableType(PostgreTableBase table) {
        if (table instanceof PostgreTableForeign) {
            return "FOREIGN TABLE";//$NON-NLS-1$
        } else {
            return table.getPersistence().getTableTypeClause();
        }
    }

    @Override
    protected PostgreTableBase createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, Object container, Object copyFrom, @NotNull Map<String, Object> options) throws DBException {
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
    protected void addObjectModifyActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actionList, @NotNull ObjectChangeCommand command, @NotNull Map<String, Object> options)
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
        final PostgreTable table = (PostgreTable) command.getObject();
        final String alterPrefix = "ALTER " + table.getTableTypeName() + " " + //$NON-NLS-1$
            command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL) + " ";

        if (command.hasProperty("partitionKey")) {//$NON-NLS-1$
            actionList.add(new SQLDatabasePersistAction(alterPrefix + "PARTITION BY " + table.getPartitionKey()));//$NON-NLS-1$
        }
        if (command.hasProperty("hasOids") && table.getDataSource().getServerType().supportsHasOidsColumn()) {//$NON-NLS-1$
            actionList.add(new SQLDatabasePersistAction(alterPrefix + (table.isHasOids() ? "SET WITH OIDS" : "SET WITHOUT OIDS")));//$NON-NLS-1$ //$NON-NLS-2$
        }
        if (command.hasProperty("hasRowLevelSecurity") && table.getDataSource().getServerType().supportsRowLevelSecurity()) {
            actionList.add(new SQLDatabasePersistAction(
                alterPrefix + (table.isHasRowLevelSecurity() ? "ENABLE" : "DISABLE") + " ROW LEVEL SECURITY"
            ));
        }
        if (command.hasProperty("tablespace")) {//$NON-NLS-1$
            actionList.add(new SQLDatabasePersistAction(alterPrefix + "SET TABLESPACE " + table.getTablespace(monitor).getName()));//$NON-NLS-1$
        }
    }

    @Override
    protected void appendTableModifiers(DBRProgressMonitor monitor, PostgreTableBase tableBase, NestedObjectCommand tableProps, StringBuilder ddl, boolean alter) {
        ddl.append(tableBase.getDataSource().getServerType().getTableModifiers(monitor, tableBase, alter));
    }

    @Override
    protected void addObjectRenameActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectRenameCommand command, @NotNull Map<String, Object> options)
    {
        PostgreTableBase table = command.getObject();
        actions.add(
            new SQLDatabasePersistAction(
                "Rename table",
                "ALTER " + table.getTableTypeName() + " " + //$NON-NLS-1$
                    DBUtils.getQuotedIdentifier(table.getSchema()) + "." +
                    DBUtils.getQuotedIdentifier(table.getDataSource(), command.getOldName()) +
                    " RENAME TO " + DBUtils.getQuotedIdentifier(table.getDataSource(), command.getNewName())) //$NON-NLS-1$
        );
    }

    @NotNull
    @Override
    public Class<? extends DBSObject>[] getChildTypes()
    {
        return CHILD_TYPES;
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull PostgreTableBase object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException
    {
        processObjectRename(commandContext, object, options, newName);
    }

    @Override
    protected void addObjectDeleteActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectDeleteCommand command, @NotNull Map<String, Object> options)
    {
        PostgreTableBase table = command.getObject();
        final String tableName = DBUtils.getEntityScriptName(table, options);
        String script = "DROP " + table.getTableTypeName() +  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            " " + tableName +  //$NON-NLS-1$
            (CommonUtils.getOption(options, OPTION_DELETE_CASCADE) ? " CASCADE" : "");
        SQLDatabasePersistAction action = table.getSchema().isExternal() ?
            new SQLDatabasePersistActionAtomic(ModelMessages.model_jdbc_drop_table, script) :
            new SQLDatabasePersistAction(ModelMessages.model_jdbc_drop_table, script);
        actions.add(action);
    }
    
}
