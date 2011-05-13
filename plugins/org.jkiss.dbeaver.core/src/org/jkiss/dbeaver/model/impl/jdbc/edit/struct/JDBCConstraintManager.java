/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit.struct;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.prop.DBECommandDeleteObject;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCConstraint;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.struct.DBSConstraintColumn;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JDBC constraint manager
 */
public abstract class JDBCConstraintManager<OBJECT_TYPE extends JDBCConstraint<TABLE_TYPE>, TABLE_TYPE extends JDBCTable>
    extends JDBCObjectEditor<OBJECT_TYPE>
    implements DBEObjectMaker<OBJECT_TYPE, TABLE_TYPE>, JDBCNestedEditor<OBJECT_TYPE, JDBCTable>
{

    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    public OBJECT_TYPE createNewObject(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, DBECommandContext commandContext, TABLE_TYPE parent, Object copyFrom)
    {
        OBJECT_TYPE newConstraint = createNewConstraint(workbenchWindow, activeEditor, parent, copyFrom);
        if (newConstraint == null) {
            return null;
        }
        makeInitialCommands(newConstraint, commandContext, new CommandCreateConstraint(newConstraint));

        return newConstraint;
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
                "Create new constraint",
                "ALTER TABLE " + table.getFullQualifiedName() + " ADD " + getNestedDeclaration(table, command)));
        }
        return actions.toArray(new IDatabasePersistAction[actions.size()]);
    }

    public String getNestedDeclaration(JDBCTable owner, ObjectChangeCommand<OBJECT_TYPE> command)
    {
        OBJECT_TYPE constraint = command.getObject();

        // Create column
        String constraintName = DBUtils.getQuotedIdentifier(
            constraint.getDataSource(),
            CommonUtils.toString(command.getProperty(DBConstants.PROP_ID_NAME)));

        StringBuilder decl = new StringBuilder(40);
        decl
            .append("CONSTRAINT ").append(constraintName)
            .append(" ").append(constraint.getConstraintType().getName().toUpperCase())
            .append(" (");
        // Get columns using void monitor
        boolean firstColumn = true;
        for (DBSConstraintColumn constraintColumn : command.getObject().getColumns(VoidProgressMonitor.INSTANCE)) {
            if (!firstColumn) decl.append(",");
            firstColumn = false;
            decl.append(constraintColumn.getName());
        }
        decl.append(")");
        return decl.toString();
    }

    protected abstract OBJECT_TYPE createNewConstraint(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, TABLE_TYPE parent, Object from);

    protected String getDropConstraintPattern(OBJECT_TYPE constraint)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP CONSTRAINT " + PATTERN_ITEM_CONSTRAINT;
    }

    private class CommandCreateConstraint extends ObjectSaveCommand<OBJECT_TYPE> {
        protected CommandCreateConstraint(OBJECT_TYPE table)
        {
            super(table, "Create constraint");
        }
    }

    private class CommandDropConstraint extends DBECommandDeleteObject<OBJECT_TYPE> {
        protected CommandDropConstraint(OBJECT_TYPE table)
        {
            super(table, "Drop constraint");
        }

        public IDatabasePersistAction[] getPersistActions()
        {
            return new IDatabasePersistAction[] {
                new AbstractDatabasePersistAction(
                    "Drop constraint",
                    getDropConstraintPattern(getObject())
                        .replace(PATTERN_ITEM_TABLE, getObject().getTable().getFullQualifiedName())
                        .replace(PATTERN_ITEM_CONSTRAINT, getObject().getName()))
            };
        }
    }


}

