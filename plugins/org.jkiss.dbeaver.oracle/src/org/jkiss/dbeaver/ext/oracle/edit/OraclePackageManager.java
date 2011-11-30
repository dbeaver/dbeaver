/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.oracle.OracleMessages;
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
        CreateEntityDialog dialog = new CreateEntityDialog(workbenchWindow.getShell(), parent.getDataSource(), OracleMessages.edit_oracle_package_manager_dialog_title);
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
            new AbstractDatabasePersistAction(OracleMessages.edit_oracle_package_manager_action_drop_package,
                "DROP PACKAGE " + object.getFullQualifiedName()) //$NON-NLS-1$
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
                    OracleMessages.edit_oracle_package_manager_action_create_package_header,
                    "CREATE OR REPLACE " + header)); //$NON-NLS-1$
        }
        String body = OracleUtils.normalizeSourceName(pack, true);
        if (!CommonUtils.isEmpty(body)) {
            actions.add(
                new AbstractDatabasePersistAction(
                    OracleMessages.edit_oracle_package_manager_action_create_package_body,
                    "CREATE OR REPLACE " + body)); //$NON-NLS-1$
        } else {
            actions.add(
                new AbstractDatabasePersistAction(
            		OracleMessages.edit_oracle_package_manager_action_create_package_body,
                    "DROP PACKAGE BODY " + pack.getFullQualifiedName(), IDatabasePersistAction.ActionType.OPTIONAL) //$NON-NLS-1$
                );
        }
        OracleUtils.addSchemaChangeActions(actions, pack);
        return actions.toArray(new IDatabasePersistAction[actions.size()]);
    }

}

