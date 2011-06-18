/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.edit;

import org.jkiss.utils.CommonUtils;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCObjectEditor;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;

/**
 * OracleSchemaManager
 */
public class OracleSchemaManager extends JDBCObjectEditor<OracleSchema, OracleDataSource> implements DBEObjectRenamer<OracleSchema> {

    public long getMakerOptions()
    {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Override
    protected OracleSchema createDatabaseObject(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, DBECommandContext context, OracleDataSource parent, Object copyFrom)
    {
        String schemaName = EnterNameDialog.chooseName(workbenchWindow.getShell(), "Schema name");
        if (CommonUtils.isEmpty(schemaName)) {
            return null;
        }
        OracleSchema newSchema = new OracleSchema(parent, null);
        newSchema.setName(schemaName);
        return newSchema;
    }

    @Override
    protected IDatabasePersistAction[] makeObjectCreateActions(ObjectCreateCommand command)
    {
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction("Create schema", "CREATE SCHEMA " + command.getObject().getName())
        };
    }

    @Override
    protected IDatabasePersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command)
    {
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction("Drop schema", "DROP SCHEMA " + command.getObject().getName())
        };
    }

    public void renameObject(DBECommandContext commandContext, OracleSchema schema, String newName) throws DBException
    {
        throw new DBException("Direct database rename is not yet implemented in Oracle. You should use export/import functions for that.");
    }

}

