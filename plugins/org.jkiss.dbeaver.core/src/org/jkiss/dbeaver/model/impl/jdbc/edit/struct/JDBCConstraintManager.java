/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit.struct;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.prop.DBECommandDeleteObject;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCConstraint;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.struct.DBSConstraintColumn;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.dialogs.struct.ConstraintColumnsDialog;

import java.util.*;

/**
 * JDBC constraint manager
 */
public abstract class JDBCConstraintManager<OBJECT_TYPE extends JDBCConstraint<CONTAINER_TYPE>, CONTAINER_TYPE extends JDBCTable>
    extends JDBCObjectEditor<OBJECT_TYPE>
    implements DBEObjectMaker<OBJECT_TYPE, CONTAINER_TYPE>, JDBCNestedEditor<OBJECT_TYPE>
{

    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    public OBJECT_TYPE createNewObject(IWorkbenchWindow workbenchWindow, DBECommandContext commandContext, CONTAINER_TYPE parent, Object copyFrom)
    {
        ConstraintColumnsDialog editDialog = new ConstraintColumnsDialog(
            workbenchWindow.getShell(),
            getCreateTitle(),
            parent,
            getSupportedConstraintTypes());
        if (editDialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        OBJECT_TYPE newConstraint = createNewConstraint(
            workbenchWindow,
            parent,
            editDialog.getConstraintType(),
            editDialog.getConstraintColumns(),
            copyFrom);

        makeInitialCommands(newConstraint, commandContext, new CommandCreateConstraint(newConstraint));

        return newConstraint;
    }

    protected String getCreateTitle()
    {
        return "Create constraint";
    }

    protected abstract Collection<DBSConstraintType> getSupportedConstraintTypes();

    public void deleteObject(DBECommandContext commandContext, OBJECT_TYPE object, Map<String, Object> options)
    {
        commandContext.addCommand(new CommandDropConstraint(object), new DeleteObjectReflector<OBJECT_TYPE>(), true);
    }


    @Override
    protected IDatabasePersistAction[] makeObjectChangeActions(ObjectChangeCommand<OBJECT_TYPE> command)
    {
        final CONTAINER_TYPE table = command.getObject().getTable();
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

    public String getNestedDeclaration(DBPObject owner, ObjectChangeCommand<OBJECT_TYPE> command)
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
            if (!firstColumn) {
                decl.append(",");
            } else {
                firstColumn = false;
            }
            decl.append(constraintColumn.getName());
        }
        decl.append(")");
        return decl.toString();
    }

    protected abstract OBJECT_TYPE createNewConstraint(
        IWorkbenchWindow workbenchWindow,
        CONTAINER_TYPE parent,
        DBSConstraintType constraintType,
        Collection<DBSTableColumn> constraintColumns,
        Object from);

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
                    "Drop constraint", "ALTER TABLE " + getObject().getTable().getFullQualifiedName() + " DROP CONSTRAINT " + getObject().getName())
            };
        }
    }


}

