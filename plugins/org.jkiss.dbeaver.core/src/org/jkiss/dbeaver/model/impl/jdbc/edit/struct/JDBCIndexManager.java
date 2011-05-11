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
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCIndex;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.struct.DBSIndexColumn;
import org.jkiss.dbeaver.model.struct.DBSIndexType;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.dialogs.struct.EditIndexDialog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * JDBC constraint manager
 */
public abstract class JDBCIndexManager<OBJECT_TYPE extends JDBCIndex<CONTAINER_TYPE>, CONTAINER_TYPE extends JDBCTable>
    extends JDBCObjectEditor<OBJECT_TYPE>
    implements DBEObjectMaker<OBJECT_TYPE, CONTAINER_TYPE>, JDBCNestedEditor<OBJECT_TYPE>
{

    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    public OBJECT_TYPE createNewObject(IWorkbenchWindow workbenchWindow, DBECommandContext commandContext, CONTAINER_TYPE parent, Object copyFrom)
    {
        EditIndexDialog editDialog = new EditIndexDialog(
            workbenchWindow.getShell(),
            getCreateTitle(),
            parent,
            getSupportedIndexTypes());
        if (editDialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        OBJECT_TYPE newConstraint = createNewIndex(
            workbenchWindow,
            parent,
            editDialog.getIndexType(),
            editDialog.getSelectedColumns(),
            copyFrom);

        makeInitialCommands(newConstraint, commandContext, new CommandCreateIndex(newConstraint));

        return newConstraint;
    }

    protected String getCreateTitle()
    {
        return "Create index";
    }

    protected abstract Collection<DBSIndexType> getSupportedIndexTypes();

    public void deleteObject(DBECommandContext commandContext, OBJECT_TYPE object, Map<String, Object> options)
    {
        commandContext.addCommand(new CommandDropIndex(object), new DeleteObjectReflector<OBJECT_TYPE>(), true);
    }

    @Override
    protected IDatabasePersistAction[] makeObjectChangeActions(ObjectChangeCommand<OBJECT_TYPE> command)
    {
        final CONTAINER_TYPE table = command.getObject().getTable();
        final OBJECT_TYPE index = command.getObject();
        List<IDatabasePersistAction> actions = new ArrayList<IDatabasePersistAction>();

        // Create index
        final String indexName = DBUtils.getQuotedIdentifier(index.getDataSource(), CommonUtils.toString(command.getProperty(DBConstants.PROP_ID_NAME)));
        index.setName(indexName);

        StringBuilder decl = new StringBuilder(40);
        decl
            .append("CREATE INDEX ").append(indexName)
            .append(" ON ").append(table.getFullQualifiedName())
            .append(" (");
        // Get columns using void monitor
        boolean firstColumn = true;
        for (DBSIndexColumn indexColumn : command.getObject().getColumns(VoidProgressMonitor.INSTANCE)) {
            if (!firstColumn) {
                decl.append(",");
            } else {
                firstColumn = false;
            }
            decl.append(indexColumn.getName());
            if (!indexColumn.isAscending()) {
                decl.append(" DESC");
            }
        }
        decl.append(")");

        actions.add(new AbstractDatabasePersistAction("Create new index", decl.toString()));

        return actions.toArray(new IDatabasePersistAction[actions.size()]);
    }

    public String getNestedDeclaration(DBPObject owner, ObjectChangeCommand<OBJECT_TYPE> command)
    {
        return null;
    }

    protected abstract OBJECT_TYPE createNewIndex(
        IWorkbenchWindow workbenchWindow,
        CONTAINER_TYPE parent,
        DBSIndexType indexType,
        Collection<DBSTableColumn> indexColumns,
        Object from);

    private class CommandCreateIndex extends ObjectSaveCommand<OBJECT_TYPE> {
        protected CommandCreateIndex(OBJECT_TYPE table)
        {
            super(table, "Create index");
        }
    }

    private class CommandDropIndex extends DBECommandDeleteObject<OBJECT_TYPE> {
        protected CommandDropIndex(OBJECT_TYPE table)
        {
            super(table, "Drop index");
        }

        public IDatabasePersistAction[] getPersistActions()
        {
            return new IDatabasePersistAction[] {
                new AbstractDatabasePersistAction(
                    "Drop index", "DROP INDEX " + getObject().getFullQualifiedName())
            };
        }
    }


}

