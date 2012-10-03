/*
 * Copyright (C) 2010-2012 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.oracle.edit;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.oracle.OracleMessages;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCObjectEditor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.utils.CommonUtils;

/**
 * OracleSchemaManager
 */
public class OracleSchemaManager extends JDBCObjectEditor<OracleSchema, OracleDataSource> implements DBEObjectRenamer<OracleSchema> {

    @Override
    public long getMakerOptions()
    {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Override
    protected DBSObjectCache<? extends DBSObject, OracleSchema> getObjectsCache(OracleSchema object)
    {
        return object.getDataSource().schemaCache;
    }

    @Override
    protected OracleSchema createDatabaseObject(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, DBECommandContext context, OracleDataSource parent, Object copyFrom)
    {
        String schemaName = EnterNameDialog.chooseName(workbenchWindow.getShell(), OracleMessages.edit_oracle_schema_manager_dialog_title);
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
            new AbstractDatabasePersistAction(OracleMessages.edit_oracle_schema_manager_action_create_schema, "CREATE SCHEMA " + command.getObject().getName()) //$NON-NLS-2$
        };
    }

    @Override
    protected IDatabasePersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command)
    {
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(OracleMessages.edit_oracle_schema_manager_action_drop_schema, "DROP SCHEMA " + command.getObject().getName()) //$NON-NLS-2$
        };
    }

    @Override
    public void renameObject(DBECommandContext commandContext, OracleSchema schema, String newName) throws DBException
    {
        throw new DBException("Direct database rename is not yet implemented in Oracle. You should use export/import functions for that.");
    }

}

