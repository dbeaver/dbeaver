/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableBase;
import org.jkiss.dbeaver.ext.oracle.model.OracleTrigger;
import org.jkiss.dbeaver.ext.oracle.model.OracleUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCObjectEditor;
import org.jkiss.dbeaver.ui.dialogs.struct.CreateEntityDialog;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * OracleTriggerManager
 */
public class OracleTriggerManager extends JDBCObjectEditor<OracleTrigger, OracleTableBase> {

    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    protected void validateObjectProperties(ObjectChangeCommand command)
        throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Trigger name cannot be empty");
        }
    }

    @Override
    protected OracleTrigger createDatabaseObject(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, DBECommandContext context, OracleTableBase parent, Object copyFrom)
    {
        CreateEntityDialog dialog = new CreateEntityDialog(workbenchWindow.getShell(), parent.getDataSource(), "Trigger");
        if (dialog.open() != IDialogConstants.OK_ID) {
            return null;
        }
        OracleTrigger newTrigger = new OracleTrigger(parent.getContainer(), parent, dialog.getEntityName());
        newTrigger.setSourceDeclaration("TRIGGER " + dialog.getEntityName() + "\n" +
            "BEGIN\n" +
            "END;");
        return newTrigger;
    }

    @Override
    protected IDatabasePersistAction[] makeObjectCreateActions(ObjectCreateCommand command)
    {
        return createOrReplaceViewQuery(command.getObject());
    }

    @Override
    protected IDatabasePersistAction[] makeObjectModifyActions(ObjectChangeCommand command)
    {
        return createOrReplaceViewQuery(command.getObject());
    }

    @Override
    protected IDatabasePersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command)
    {
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction("Drop trigger", "DROP TRIGGER " + command.getObject().getFullQualifiedName())
        };
    }

    private IDatabasePersistAction[] createOrReplaceViewQuery(OracleTrigger trigger)
    {
        String source = OracleUtils.normalizeSourceName(trigger, false);
        if (source == null) {
            return null;
        }
        List<IDatabasePersistAction> actions = new ArrayList<IDatabasePersistAction>();
        actions.add(new AbstractDatabasePersistAction("Create trigger", "CREATE OR REPLACE " + source));
        OracleUtils.addSchemaChangeActions(actions, trigger);
        return actions.toArray(new IDatabasePersistAction[actions.size()]);
    }

}

