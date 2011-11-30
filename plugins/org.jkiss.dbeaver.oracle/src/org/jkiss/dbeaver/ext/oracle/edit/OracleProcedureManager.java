/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.oracle.OracleMessages;
import org.jkiss.dbeaver.ext.oracle.model.OracleProcedureStandalone;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCObjectEditor;
import org.jkiss.dbeaver.ui.dialogs.struct.CreateProcedureDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * OracleProcedureManager
 */
public class OracleProcedureManager extends JDBCObjectEditor<OracleProcedureStandalone, OracleSchema> {

/*
    public ITabDescriptor[] getTabDescriptors(IWorkbenchWindow workbenchWindow, final IDatabaseNodeEditor activeEditor, final OracleProcedureStandalone object)
    {
        return new ITabDescriptor[] {
            new PropertyTabDescriptor(
                PropertiesContributor.CATEGORY_INFO,
                "procedure.body",
                "Body",
                DBIcon.SOURCES.getImage(),
                new SectionDescriptor("default", "Body") {
                    public ISection getSectionClass()
                    {
                        return new OracleProcedureBodySection(activeEditor);
                    }
                })
        };
    }
*/

    @Override
    protected OracleProcedureStandalone createDatabaseObject(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, DBECommandContext context, OracleSchema parent, Object copyFrom)
    {
        CreateProcedureDialog dialog = new CreateProcedureDialog(workbenchWindow.getShell(), parent.getDataSource());
        if (dialog.open() != IDialogConstants.OK_ID) {
            return null;
        }
        return new OracleProcedureStandalone(
            parent,
            dialog.getProcedureName(),
            dialog.getProcedureType());
    }

    @Override
    protected IDatabasePersistAction[] makeObjectCreateActions(ObjectCreateCommand objectCreateCommand)
    {
        return createOrReplaceProcedureQuery(objectCreateCommand.getObject());
    }

    @Override
    protected IDatabasePersistAction[] makeObjectDeleteActions(ObjectDeleteCommand objectDeleteCommand)
    {
        final OracleProcedureStandalone object = objectDeleteCommand.getObject();
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(OracleMessages.edit_oracle_procedure_manager_action_drop_procedure,
                "DROP " + object.getProcedureType().name() + " " + object.getFullQualifiedName()) //$NON-NLS-1$ //$NON-NLS-2$
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

    private IDatabasePersistAction[] createOrReplaceProcedureQuery(OracleProcedureStandalone procedure)
    {
        String source = OracleUtils.normalizeSourceName(procedure, false);
        if (source == null) {
            return null;
        }
        List<IDatabasePersistAction> actions = new ArrayList<IDatabasePersistAction>();
        actions.add(new AbstractDatabasePersistAction(OracleMessages.edit_oracle_procedure_manager_action_create_procedure, "CREATE OR REPLACE " + source)); //$NON-NLS-2$
        OracleUtils.addSchemaChangeActions(actions, procedure);
        return actions.toArray(new IDatabasePersistAction[actions.size()]);
    }

}
