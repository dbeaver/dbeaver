/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit.struct;

import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.struct.DBSTableConstraintColumn;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;

/**
 * JDBC constraint manager
 */
public abstract class JDBCConstraintManager<OBJECT_TYPE extends JDBCTableConstraint<TABLE_TYPE>, TABLE_TYPE extends JDBCTable>
    extends JDBCObjectEditor<OBJECT_TYPE, TABLE_TYPE>
{

    @Override
    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected IDatabasePersistAction[] makeObjectCreateActions(ObjectCreateCommand command)
    {
        final TABLE_TYPE table = command.getObject().getTable();

        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
                CoreMessages.model_jdbc_create_new_constraint,
                "ALTER TABLE " + table.getFullQualifiedName() + " ADD " + getNestedDeclaration(table, command))}; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    protected IDatabasePersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command)
    {
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
                CoreMessages.model_jdbc_drop_constraint,
                getDropConstraintPattern(command.getObject())
                    .replace(PATTERN_ITEM_TABLE, command.getObject().getTable().getFullQualifiedName())
                    .replace(PATTERN_ITEM_CONSTRAINT, command.getObject().getName()))
        };
    }

    @Override
    public StringBuilder getNestedDeclaration(TABLE_TYPE owner, DBECommandComposite<OBJECT_TYPE, PropertyHandler> command)
    {
        OBJECT_TYPE constraint = command.getObject();

        // Create column
        String constraintName = DBUtils.getQuotedIdentifier(constraint.getDataSource(), constraint.getName());

        StringBuilder decl = new StringBuilder(40);
        decl
            .append("CONSTRAINT ").append(constraintName) //$NON-NLS-1$
            .append(" ").append(constraint.getConstraintType().getName().toUpperCase()) //$NON-NLS-1$
            .append(" ("); //$NON-NLS-1$
        // Get columns using void monitor
        boolean firstColumn = true;
        for (DBSTableConstraintColumn constraintColumn : command.getObject().getColumns(VoidProgressMonitor.INSTANCE)) {
            if (!firstColumn) decl.append(","); //$NON-NLS-1$
            firstColumn = false;
            decl.append(constraintColumn.getName());
        }
        decl.append(")"); //$NON-NLS-1$
        return decl;
    }

    protected String getDropConstraintPattern(OBJECT_TYPE constraint)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP CONSTRAINT " + PATTERN_ITEM_CONSTRAINT; //$NON-NLS-1$ //$NON-NLS-2$
    }

}

