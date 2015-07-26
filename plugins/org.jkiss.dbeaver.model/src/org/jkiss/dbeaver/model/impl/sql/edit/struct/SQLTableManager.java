/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.impl.sql.edit.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLStructEditor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JDBC table manager
 */
public abstract class SQLTableManager<OBJECT_TYPE extends JDBCTable, CONTAINER_TYPE extends DBSObjectContainer>
    extends SQLStructEditor<OBJECT_TYPE, CONTAINER_TYPE>
{

    private static final String BASE_TABLE_NAME = "NewTable"; //$NON-NLS-1$

    @Override
    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected final DBEPersistAction[] makeObjectCreateActions(ObjectCreateCommand objectChangeCommand)
    {
        throw new IllegalStateException("makeObjectCreateActions should never be called in struct editor");
    }

    @Override
    protected DBEPersistAction[] makeStructObjectCreateActions(StructCreateCommand command)
    {
        final OBJECT_TYPE table = command.getObject();
        final NestedObjectCommand tableProps = command.getObjectCommands().get(table);
        if (tableProps == null) {
            log.warn("Object change command not found"); //$NON-NLS-1$
            return null;
        }
        List<DBEPersistAction> actions = new ArrayList<DBEPersistAction>();
        final String tableName = table.getFullQualifiedName();

        final String lineSeparator = GeneralUtils.getDefaultLineSeparator();
        StringBuilder createQuery = new StringBuilder(100);
        createQuery.append("CREATE TABLE ").append(tableName).append(" (").append(lineSeparator); //$NON-NLS-1$ //$NON-NLS-2$
        boolean hasNestedDeclarations = false;
        for (NestedObjectCommand nestedCommand : getNestedOrderedCommands(command)) {
            if (nestedCommand.getObject() == table) {
                continue;
            }
            final String nestedDeclaration = nestedCommand.getNestedDeclaration(table);
            if (!CommonUtils.isEmpty(nestedDeclaration)) {
                // Insert nested declaration
                if (hasNestedDeclarations) {
                    createQuery.append(",").append(lineSeparator); //$NON-NLS-1$
                }
                createQuery.append("\t").append(nestedDeclaration); //$NON-NLS-1$
                hasNestedDeclarations = true;
            } else {
                // This command should be executed separately
                final DBEPersistAction[] nestedActions = nestedCommand.getPersistActions();
                if (nestedActions != null) {
                    Collections.addAll(actions, nestedActions);
                }
            }
        }

        createQuery.append(lineSeparator).append(")"); //$NON-NLS-1$
        appendTableModifiers(table, tableProps, createQuery);

        actions.add( 0, new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_table, createQuery.toString()) );

        return actions.toArray(new DBEPersistAction[actions.size()]);
    }

    @Override
    protected DBEPersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command)
    {
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_drop_table,
                "DROP " + (command.getObject().isView() ? "VIEW" : "TABLE") +
                " " + command.getObject().getFullQualifiedName()) //$NON-NLS-2$
        };
    }

    protected void appendTableModifiers(OBJECT_TYPE table, NestedObjectCommand tableProps, StringBuilder ddl)
    {

    }

    protected void setTableName(CONTAINER_TYPE container, OBJECT_TYPE table) throws DBException {
        table.setName(getTableName(container));
    }

    protected String getTableName(CONTAINER_TYPE container) throws DBException {
        for (int i = 0; ; i++) {
            String tableName = DBObjectNameCaseTransformer.transformName(container.getDataSource(), i == 0 ? BASE_TABLE_NAME : (BASE_TABLE_NAME + "_" + i));
            DBSObject child = container.getChild(VoidProgressMonitor.INSTANCE, tableName);
            if (child == null) {
                return tableName;
            }
        }
    }

}

