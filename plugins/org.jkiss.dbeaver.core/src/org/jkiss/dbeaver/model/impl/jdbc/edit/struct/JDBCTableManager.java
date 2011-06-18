/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit.struct;

import org.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JDBC table manager
 */
public abstract class JDBCTableManager<OBJECT_TYPE extends JDBCTable, CONTAINER_TYPE extends DBSEntityContainer>
    extends JDBCStructEditor<OBJECT_TYPE, CONTAINER_TYPE>
{

    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    protected final IDatabasePersistAction[] makeObjectCreateActions(ObjectCreateCommand objectChangeCommand)
    {
        throw new IllegalStateException("makeObjectCreateActions should never be called in struct editor");
    }

    @Override
    protected IDatabasePersistAction[] makeStructObjectCreateActions(StructCreateCommand command)
    {
        final OBJECT_TYPE table = command.getObject();
        final NestedObjectCommand tableProps = command.getObjectCommands().get(table);
        if (tableProps == null) {
            log.warn("Object change command not found");
            return null;
        }
        List<IDatabasePersistAction> actions = new ArrayList<IDatabasePersistAction>();
        final String tableName = table.getFullQualifiedName();

        final String lineSeparator = ContentUtils.getDefaultLineSeparator();
        StringBuilder createQuery = new StringBuilder(100);
        createQuery.append("CREATE TABLE ").append(tableName).append(" (").append(lineSeparator);
        boolean hasNestedDeclarations = false;
        for (NestedObjectCommand nestedCommand : getNestedOrderedCommands(command)) {
            if (nestedCommand.getObject() == table) {
                continue;
            }
            final String nestedDeclaration = nestedCommand.getNestedDeclaration(table);
            if (!CommonUtils.isEmpty(nestedDeclaration)) {
                // Insert nested declaration
                if (hasNestedDeclarations) {
                    createQuery.append(",").append(lineSeparator);
                }
                createQuery.append("\t").append(nestedDeclaration);
                hasNestedDeclarations = true;
            } else {
                // This command should be executed separately
                final IDatabasePersistAction[] nestedActions = nestedCommand.getPersistActions();
                if (nestedActions != null) {
                    Collections.addAll(actions, nestedActions);
                }
            }
        }

        createQuery.append(lineSeparator).append(")");
        appendTableModifiers(table, tableProps, createQuery);

        actions.add( 0, new AbstractDatabasePersistAction("Create new table", createQuery.toString()) );

        return actions.toArray(new IDatabasePersistAction[actions.size()]);
    }

    @Override
    protected IDatabasePersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command)
    {
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction("Drop table", "DROP TABLE " + command.getObject().getFullQualifiedName())
        };
    }

    protected void appendTableModifiers(OBJECT_TYPE table, NestedObjectCommand tableProps, StringBuilder ddl)
    {

    }

}

