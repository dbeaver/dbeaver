/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.oracle.OracleMessages;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataType;
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
 * OracleDataTypeManager
 */
public class OracleDataTypeManager extends JDBCObjectEditor<OracleDataType, OracleSchema> {

    @Override
    protected OracleDataType createDatabaseObject(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, DBECommandContext context, OracleSchema parent, Object copyFrom)
    {
        CreateEntityDialog dialog = new CreateEntityDialog(workbenchWindow.getShell(), parent.getDataSource(), OracleMessages.edit_oracle_data_type_manager_dialog_title);
        if (dialog.open() != IDialogConstants.OK_ID) {
            return null;
        }
        OracleDataType dataType = new OracleDataType(
            parent,
            dialog.getEntityName(),
            false);
        dataType.setSourceDeclaration("TYPE " + dataType.getName() + " AS OBJECT\n" + //$NON-NLS-1$ //$NON-NLS-2$
            "(\n" + //$NON-NLS-1$
            ")"); //$NON-NLS-1$
        return dataType;
    }

    @Override
    protected IDatabasePersistAction[] makeObjectCreateActions(ObjectCreateCommand objectCreateCommand)
    {
        return createOrReplaceProcedureQuery(objectCreateCommand.getObject());
    }

    @Override
    protected IDatabasePersistAction[] makeObjectDeleteActions(ObjectDeleteCommand objectDeleteCommand)
    {
        final OracleDataType object = objectDeleteCommand.getObject();
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(OracleMessages.edit_oracle_data_type_manager_action_drop_data_type,
                "DROP TYPE " + object.getFullQualifiedName()) //$NON-NLS-1$
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

    private IDatabasePersistAction[] createOrReplaceProcedureQuery(OracleDataType dataType)
    {
        List<IDatabasePersistAction> actions = new ArrayList<IDatabasePersistAction>();
        String header = OracleUtils.normalizeSourceName(dataType, false);
        if (!CommonUtils.isEmpty(header)) {
            actions.add(
                new AbstractDatabasePersistAction(
                    OracleMessages.edit_oracle_data_type_manager_action_create_type_header,
                    "CREATE OR REPLACE " + header)); //$NON-NLS-1$
        }
        String body = OracleUtils.normalizeSourceName(dataType, true);
        if (!CommonUtils.isEmpty(body)) {
            actions.add(
                new AbstractDatabasePersistAction(
                    OracleMessages.edit_oracle_data_type_manager_action_create_type_body,
                    "CREATE OR REPLACE " + body)); //$NON-NLS-1$
        }
        OracleUtils.addSchemaChangeActions(actions, dataType);
        return actions.toArray(new IDatabasePersistAction[actions.size()]);
    }

}

