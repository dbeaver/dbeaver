/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.oracle.model.OraclePackage;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCObjectEditor;
import org.jkiss.dbeaver.ui.dialogs.struct.CreateEntityDialog;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * OraclePackageManager
 */
public class OraclePackageManager extends JDBCObjectEditor<OraclePackage, OracleSchema> {

    @Override
    protected OraclePackage createDatabaseObject(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, DBECommandContext context, OracleSchema parent, Object copyFrom)
    {
        CreateEntityDialog dialog = new CreateEntityDialog(workbenchWindow.getShell(), parent.getDataSource(), "Package");
        if (dialog.open() != IDialogConstants.OK_ID) {
            return null;
        }
        return new OraclePackage(
            parent,
            dialog.getEntityName());
    }

    @Override
    protected IDatabasePersistAction[] makeObjectCreateActions(ObjectCreateCommand objectCreateCommand)
    {
        return createOrReplaceProcedureQuery(objectCreateCommand.getObject());
    }

    @Override
    protected IDatabasePersistAction[] makeObjectDeleteActions(ObjectDeleteCommand objectDeleteCommand)
    {
        final OraclePackage object = objectDeleteCommand.getObject();
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction("Drop package",
                "DROP PACKAGE " + object.getFullQualifiedName())
        };
    }

    @Override
    protected IDatabasePersistAction[] makeObjectModifyActions(ObjectChangeCommand objectChangeCommand)
    {
        return createOrReplaceProcedureQuery(objectChangeCommand.getObject());
    }

    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    private IDatabasePersistAction[] createOrReplaceProcedureQuery(OraclePackage pack)
    {
        List<IDatabasePersistAction> actions = new ArrayList<IDatabasePersistAction>();
        String header = OracleUtils.normalizeSourceName(pack, false);
        if (!CommonUtils.isEmpty(header)) {
            actions.add(
                new AbstractDatabasePersistAction(
                    "Create package header",
                    "CREATE OR REPLACE " + header));
        }
        String body = OracleUtils.normalizeSourceName(pack, true);
        if (!CommonUtils.isEmpty(body)) {
            actions.add(
                new AbstractDatabasePersistAction(
                    "Create package body",
                    "CREATE OR REPLACE " + body));
        } else {
            actions.add(
                new AbstractDatabasePersistAction(
                    "Create package body",
                    "DROP PACKAGE BODY " + pack.getFullQualifiedName(), IDatabasePersistAction.ActionType.OPTIONAL)
                );
        }
        OracleUtils.addSchemaChangeActions(actions, pack);
        return actions.toArray(new IDatabasePersistAction[actions.size()]);
    }

}

