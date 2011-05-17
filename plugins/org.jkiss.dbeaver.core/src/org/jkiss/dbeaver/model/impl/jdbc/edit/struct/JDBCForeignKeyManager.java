/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit.struct;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCForeignKey;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCConstraint;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.struct.DBSConstraintColumn;
import org.jkiss.dbeaver.model.struct.DBSForeignKeyColumn;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;

import java.util.Collection;

/**
 * JDBC foreign key manager
 */
public abstract class JDBCForeignKeyManager<OBJECT_TYPE extends JDBCForeignKey<TABLE_TYPE, PRIMARY_KEY>, PRIMARY_KEY extends JDBCConstraint<TABLE_TYPE>, TABLE_TYPE extends JDBCTable>
    extends JDBCObjectEditor<OBJECT_TYPE, TABLE_TYPE>
{

    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected IDatabasePersistAction[] makeObjectCreateActions(ObjectChangeCommand command)
    {
        final TABLE_TYPE table = command.getObject().getTable();
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
                "Create new foreign key",
                "ALTER TABLE " + table.getFullQualifiedName() + " ADD " + getNestedDeclaration(table, command))
        };
    }

    protected IDatabasePersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command)
    {
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
                "Drop foreign key",
                getDropForeignKeyPattern(command.getObject())
                    .replace(PATTERN_ITEM_TABLE, command.getObject().getTable().getFullQualifiedName())
                    .replace(PATTERN_ITEM_CONSTRAINT, command.getObject().getName()))
        };
    }

    protected StringBuilder getNestedDeclaration(TABLE_TYPE owner, ObjectChangeCommand command)
    {
        OBJECT_TYPE foreignKey = command.getObject();

        // Create column
        String constraintName = DBUtils.getQuotedIdentifier(foreignKey.getDataSource(), foreignKey.getName());

        StringBuilder decl = new StringBuilder(40);
        decl
            .append("CONSTRAINT ").append(constraintName)
            .append(" ").append(foreignKey.getConstraintType().getName().toUpperCase())
            .append(" (");
        // Get columns using void monitor
        final Collection<? extends DBSConstraintColumn> columns = command.getObject().getColumns(VoidProgressMonitor.INSTANCE);
        boolean firstColumn = true;
        for (DBSConstraintColumn constraintColumn : columns) {
            if (!firstColumn) decl.append(",");
            firstColumn = false;
            decl.append(constraintColumn.getName());
        }
        decl.append(") REFERENCES ").append(foreignKey.getReferencedTable().getFullQualifiedName()).append("(");
        firstColumn = true;
        for (DBSConstraintColumn constraintColumn : columns) {
            if (!firstColumn) decl.append(",");
            firstColumn = false;
            decl.append(((DBSForeignKeyColumn) constraintColumn).getReferencedColumn().getName());
        }
        decl.append(")");
        if (foreignKey.getDeleteRule() != null && !CommonUtils.isEmpty(foreignKey.getDeleteRule().getClause())) {
            decl.append(" ON DELETE ").append(foreignKey.getDeleteRule().getClause());
        }
        if (foreignKey.getUpdateRule() != null && !CommonUtils.isEmpty(foreignKey.getUpdateRule().getClause())) {
            decl.append(" ON UPDATE ").append(foreignKey.getUpdateRule().getClause());
        }
        return decl;
    }

    protected String getDropForeignKeyPattern(OBJECT_TYPE constraint)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP CONSTRAINT " + PATTERN_ITEM_CONSTRAINT;
    }

}

