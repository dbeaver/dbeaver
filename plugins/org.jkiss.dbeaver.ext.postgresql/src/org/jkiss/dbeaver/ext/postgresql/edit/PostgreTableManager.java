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
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * Postgre table manager
 */
public class PostgreTableManager extends SQLTableManager<PostgreTableBase, PostgreSchema> implements DBEObjectRenamer<PostgreTableBase> {

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
    protected void addObjectModifyActions(List<DBEPersistAction> actionList, ObjectChangeCommand command)
    {
        final PostgreTableBase table = command.getObject();
        if (command.getProperties().size() > 1 || command.getProperty("description") == null) {
            StringBuilder query = new StringBuilder("ALTER TABLE "); //$NON-NLS-1$
            query.append(table.getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" "); //$NON-NLS-1$
            appendTableModifiers(table, command, query);

            actionList.add(new SQLDatabasePersistAction(query.toString()));
        }
    }

    @Override
    protected void addObjectExtraActions(List<DBEPersistAction> actions, NestedObjectCommand<PostgreTableBase, PropertyHandler> command) {
        // Add comments
        if (command.getProperty("description") != null) {
            actions.add(new SQLDatabasePersistAction(
                "Comment table",
                "COMMENT ON TABLE " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    " IS " + SQLUtils.quoteString(command.getObject(), command.getObject().getDescription())));
        }
        for (PostgreTableColumn column : command.getObject().getCachedAttributes()) {
            if (!column.isPersisted() && !CommonUtils.isEmpty(column.getDescription())) {
                actions.add(new SQLDatabasePersistAction("Set column comment", "COMMENT ON COLUMN " +
                    DBUtils.getObjectFullName(command.getObject(), DBPEvaluationContext.DDL) + "." + DBUtils.getQuotedIdentifier(column) +
                    " IS " + SQLUtils.quoteString(column, column.getDescription())));
            }
        }
    }

    @Override
    protected void appendTableModifiers(PostgreTableBase tableBase, NestedObjectCommand tableProps, StringBuilder ddl)
    {
        if (tableBase instanceof PostgreTableRegular) {
            final VoidProgressMonitor monitor = new VoidProgressMonitor();
            PostgreTableRegular table =(PostgreTableRegular)tableBase;
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
                ddl.append("\nWITH (\n\tOIDS=").append(table.isHasOids() ? "TRUE" : "FALSE").append("\n)");
            } catch (DBException e) {
                log.error(e);
            }
        }
    }

    @Override
    protected void addObjectRenameActions(List<DBEPersistAction> actions, ObjectRenameCommand command)
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
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command)
    {
        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_drop_table,
                "DROP " + (command.getObject() instanceof PostgreTableForeign ? "FOREIGN TABLE" : "TABLE") +
                    " " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-2$
        );
    }

}
