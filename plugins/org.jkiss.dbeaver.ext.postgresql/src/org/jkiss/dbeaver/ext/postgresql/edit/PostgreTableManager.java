/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.ext.postgresql.PostgreServerType;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.YellowbrickUtils;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

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
    public DBSObjectCache<PostgreSchema, PostgreTableBase> getObjectsCache(PostgreTableBase object)
    {
        return object.getContainer().tableCache;
    }

    @Override
    protected String getCreateTableType(PostgreTableBase table) {
        if (table instanceof PostgreTableForeign) {
            return "FOREIGN TABLE";
        } else {
            return "TABLE";
        }
    }

    @Override
    protected PostgreTable createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, PostgreSchema parent, Object copyFrom)
    {
        final PostgreTableRegular table = new PostgreTableRegular(parent);
        try {
            setTableName(monitor, parent, table);
        } catch (DBException e) {
            // Never be here
            log.error(e);
        }

        return table;
    }

    @Override
    protected void addStructObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, StructCreateCommand command, Map<String, Object> options) throws DBException {
        PostgreTableBase tableBase = command.getObject();
        if (tableBase.getDataSource().getServerType() == PostgreServerType.YELLOWBRICK) {
            // Extract main portion from server
            StringBuilder ddl = new StringBuilder();

            String tableDDL = YellowbrickUtils.extractTableDDL(monitor, tableBase);
            if (!CommonUtils.isEmpty(tableDDL)) {
                ddl.append(tableDDL);
                actions.add( 0, new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_table, ddl.toString()) );
                return;
            }
        }
        super.addStructObjectCreateActions(monitor, actions, command, options);
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        if (command.getProperties().size() > 1 || command.getProperty(DBConstants.PROP_ID_DESCRIPTION) == null) {
            if (command.getObject() instanceof PostgreTableRegular) {
                try {
                    generateAlterActions(actionList, command);
                } catch (DBException e) {
                    log.error(e);
                }
            }
        }
    }

    private void generateAlterActions(List<DBEPersistAction> actionList, ObjectChangeCommand command) throws DBException {
        final PostgreTableRegular table = (PostgreTableRegular) command.getObject();
        final String alterPrefix = "ALTER TABLE " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL) + " ";
        final VoidProgressMonitor monitor = new VoidProgressMonitor();
        if (command.hasProperty("hasOids")) {
            actionList.add(new SQLDatabasePersistAction(alterPrefix + (table.isHasOids() ? "SET WITH OIDS" : "SET WITHOUT OIDS")));
        }
        if (command.hasProperty("tablespace")) {
            actionList.add(new SQLDatabasePersistAction(alterPrefix + "SET TABLESPACE " + table.getTablespace(monitor).getName()));
        }
    }

    @Override
    protected void appendTableModifiers(PostgreTableBase tableBase, NestedObjectCommand tableProps, StringBuilder ddl, boolean alter) {
        final VoidProgressMonitor monitor = new VoidProgressMonitor();

        if (tableBase instanceof PostgreTable) {
            PostgreTable table = (PostgreTable) tableBase;
            if (!alter) {
                try {
                    final List<PostgreTableInheritance> superTables = table.getSuperInheritance(monitor);
                    if (!CommonUtils.isEmpty(superTables)) {
                        ddl.append("\nINHERITS (");
                        for (int i = 0; i < superTables.size(); i++) {
                            if (i > 0) ddl.append(",");
                            ddl.append(superTables.get(i).getAssociatedEntity().getFullyQualifiedName(DBPEvaluationContext.DDL));
                        }
                        ddl.append(")");
                    }
                } catch (DBException e) {
                    log.error(e);
                }
            }
        }

        if (tableBase instanceof PostgreTableRegular) {
            PostgreTableRegular table = (PostgreTableRegular) tableBase;
            try {
                if (!alter) {
                    if (table.getDataSource().getServerType().supportsOids() && table.isHasOids()) {
                        ddl.append("\nWITH (\n\tOIDS=").append(table.isHasOids() ? "TRUE" : "FALSE");
                        ddl.append("\n)");
                    }
                }
                boolean hasOtherSpecs = false;
                PostgreTablespace tablespace = table.getTablespace(monitor);
                if (tablespace != null && table.isTablespaceSpecified()) {
                    if (!alter) {
                        ddl.append("\nTABLESPACE ").append(tablespace.getName());
                    }
                    hasOtherSpecs = true;
                }
                if (!alter && hasOtherSpecs) {
                    ddl.append("\n");
                }
            } catch (DBException e) {
                log.error(e);
            }
        } else if (tableBase instanceof PostgreTableForeign) {
            PostgreTableForeign table = (PostgreTableForeign)tableBase;
            try {
                PostgreForeignServer foreignServer = table.getForeignServer(monitor);
                if (foreignServer != null ) {
                    ddl.append("\nSERVER ").append(DBUtils.getQuotedIdentifier(foreignServer));
                }
                String[] foreignOptions = table.getForeignOptions(monitor);
                if (!ArrayUtils.isEmpty(foreignOptions)) {
                    ddl.append("\nOPTIONS ").append(PostgreUtils.getOptionsString(foreignOptions));
                }
            } catch (DBException e) {
                log.error(e);
            }
        }
    }

    @Override
    protected void addObjectRenameActions(List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options)
    {
        actions.add(
            new SQLDatabasePersistAction(
                "Rename table",
                "ALTER TABLE " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL) + //$NON-NLS-1$
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
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_drop_table,
                "DROP " + (command.getObject() instanceof PostgreTableForeign ? "FOREIGN TABLE" : "TABLE") +  //$NON-NLS-2$
                    " " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL) +  //$NON-NLS-2$
                    (CommonUtils.getOption(options, OPTION_DELETE_CASCADE) ? " CASCADE" : "")
            )
        );
    }

}
