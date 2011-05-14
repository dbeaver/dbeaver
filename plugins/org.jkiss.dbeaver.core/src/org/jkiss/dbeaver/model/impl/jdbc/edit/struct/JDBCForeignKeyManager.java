/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit.struct;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.prop.DBECommandDeleteObject;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCForeignKey;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCConstraint;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.struct.DBSForeignKeyColumn;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * JDBC foreign key manager
 */
public abstract class JDBCForeignKeyManager<OBJECT_TYPE extends JDBCForeignKey<TABLE_TYPE, PRIMARY_KEY>, PRIMARY_KEY extends JDBCConstraint<TABLE_TYPE>, TABLE_TYPE extends JDBCTable>
    extends JDBCObjectEditor<OBJECT_TYPE>
    implements DBEObjectMaker<OBJECT_TYPE, TABLE_TYPE>, JDBCNestedEditor<OBJECT_TYPE, JDBCTable>
{

    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    public OBJECT_TYPE createNewObject(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, DBECommandContext commandContext, TABLE_TYPE parent, Object copyFrom)
    {
        OBJECT_TYPE newForeignKey = createNewForeignKey(workbenchWindow, activeEditor, parent, copyFrom);
        if (newForeignKey == null) {
            return null;
        }

        makeInitialCommands(newForeignKey, commandContext, new CommandCreateConstraint(newForeignKey));

        return newForeignKey;
    }

    public void deleteObject(DBECommandContext commandContext, OBJECT_TYPE object, Map<String, Object> options)
    {
        commandContext.addCommand(new CommandDropConstraint(object), new DeleteObjectReflector<OBJECT_TYPE>(), true);
    }

    @Override
    protected IDatabasePersistAction[] makeObjectChangeActions(ObjectChangeCommand<OBJECT_TYPE> command)
    {
        final TABLE_TYPE table = command.getObject().getTable();
        final OBJECT_TYPE constraint = command.getObject();
        List<IDatabasePersistAction> actions = new ArrayList<IDatabasePersistAction>();
        boolean newObject = !constraint.isPersisted();
        if (newObject) {
            actions.add(new AbstractDatabasePersistAction(
                "Create new foreign key",
                "ALTER TABLE " + table.getFullQualifiedName() + " ADD " + getNestedDeclaration(table, command)));
        }
        return actions.toArray(new IDatabasePersistAction[actions.size()]);
    }

    public StringBuilder getNestedDeclaration(JDBCTable owner, ObjectChangeCommand<OBJECT_TYPE> command)
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
        final Collection<? extends DBSForeignKeyColumn> columns = (Collection<? extends DBSForeignKeyColumn>) command.getObject().getColumns(VoidProgressMonitor.INSTANCE);
        boolean firstColumn = true;
        for (DBSForeignKeyColumn constraintColumn : columns) {
            if (!firstColumn) decl.append(",");
            firstColumn = false;
            decl.append(constraintColumn.getName());
        }
        decl.append(") REFERENCES ").append(foreignKey.getReferencedTable().getFullQualifiedName()).append("(");
        firstColumn = true;
        for (DBSForeignKeyColumn constraintColumn : columns) {
            if (!firstColumn) decl.append(",");
            firstColumn = false;
            decl.append(constraintColumn.getReferencedColumn().getName());
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

    protected abstract OBJECT_TYPE createNewForeignKey(
        IWorkbenchWindow workbenchWindow,
        IEditorPart activeEditor, TABLE_TYPE table,
        Object from);

    protected String getDropForeignKeyPattern(OBJECT_TYPE constraint)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP CONSTRAINT " + PATTERN_ITEM_CONSTRAINT;
    }

    private class CommandCreateConstraint extends ObjectSaveCommand<OBJECT_TYPE> {
        protected CommandCreateConstraint(OBJECT_TYPE table)
        {
            super(table, "Create foreign key");
        }
    }

    private class CommandDropConstraint extends DBECommandDeleteObject<OBJECT_TYPE> {
        protected CommandDropConstraint(OBJECT_TYPE table)
        {
            super(table, "Drop foreign key");
        }

        public IDatabasePersistAction[] getPersistActions()
        {
            return new IDatabasePersistAction[] {
                new AbstractDatabasePersistAction(
                    "Drop constraint",
                    getDropForeignKeyPattern(getObject())
                        .replace(PATTERN_ITEM_TABLE, getObject().getTable().getFullQualifiedName())
                        .replace(PATTERN_ITEM_CONSTRAINT, getObject().getName()))
            };
        }
    }

}

