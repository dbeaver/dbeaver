/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.edit;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.oracle.OracleMessages;
import org.jkiss.dbeaver.ext.oracle.model.OracleMaterializedView;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCObjectEditor;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

/**
 * OracleMaterializedViewManager
 */
public class OracleMaterializedViewManager extends JDBCObjectEditor<OracleMaterializedView, OracleSchema> {

    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    protected void validateObjectProperties(ObjectChangeCommand command)
        throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("View name cannot be empty"); //$NON-NLS-1$
        }
        if (CommonUtils.isEmpty(command.getObject().getSourceDeclaration(null))) {
            throw new DBException("View definition cannot be empty"); //$NON-NLS-1$
        }
    }

    @Override
    protected OracleMaterializedView createDatabaseObject(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, DBECommandContext context, OracleSchema parent, Object copyFrom)
    {
        OracleMaterializedView newView = new OracleMaterializedView(parent, "NewView"); //$NON-NLS-1$
        return newView;
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
            new AbstractDatabasePersistAction(OracleMessages.edit_oracle_materialized_view_manager_action_drop_view, "DROP MATERIALIZED VIEW " + command.getObject().getFullQualifiedName()) //$NON-NLS-2$
        };
    }

    private IDatabasePersistAction[] createOrReplaceViewQuery(OracleMaterializedView view)
    {
        StringBuilder decl = new StringBuilder(200);
        final String lineSeparator = ContentUtils.getDefaultLineSeparator();
        decl.append("CREATE MATERIALIZED VIEW ").append(view.getFullQualifiedName()).append(lineSeparator) //$NON-NLS-1$
            .append("AS ").append(view.getSourceDeclaration(null)); //$NON-NLS-1$
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(OracleMessages.edit_oracle_materialized_view_manager_action_drop_mater_view, "DROP MATERIALIZED VIEW " + view.getFullQualifiedName()), //$NON-NLS-2$
            new AbstractDatabasePersistAction(OracleMessages.edit_oracle_materialized_view_manager_action_create_mater_view, decl.toString())
        };
    }

}

