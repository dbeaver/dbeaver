/*
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.ext.oracle.OracleMessages;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataType;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.dialogs.struct.CreateEntityDialog;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * OracleDataTypeManager
 */
public class OracleDataTypeManager extends SQLObjectEditor<OracleDataType, OracleSchema> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, OracleDataType> getObjectsCache(OracleDataType object)
    {
        return object.getSchema().dataTypeCache;
    }

    @Override
    protected OracleDataType createDatabaseObject(IWorkbenchWindow workbenchWindow, DBECommandContext context, OracleSchema parent, Object copyFrom)
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
    protected DBEPersistAction[] makeObjectCreateActions(ObjectCreateCommand objectCreateCommand)
    {
        return createOrReplaceProcedureQuery(objectCreateCommand.getObject());
    }

    @Override
    protected DBEPersistAction[] makeObjectDeleteActions(ObjectDeleteCommand objectDeleteCommand)
    {
        final OracleDataType object = objectDeleteCommand.getObject();
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction("Drop type",
                "DROP TYPE " + object.getFullQualifiedName()) //$NON-NLS-1$
        };
    }

    @Override
    protected DBEPersistAction[] makeObjectModifyActions(ObjectChangeCommand objectChangeCommand)
    {
        return createOrReplaceProcedureQuery(objectChangeCommand.getObject());
    }

    @Override
    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    private DBEPersistAction[] createOrReplaceProcedureQuery(OracleDataType dataType)
    {
        List<DBEPersistAction> actions = new ArrayList<DBEPersistAction>();
        String header = OracleUtils.normalizeSourceName(dataType, false);
        if (!CommonUtils.isEmpty(header)) {
            actions.add(
                new SQLDatabasePersistAction(
                    "Create type header",
                    "CREATE OR REPLACE " + header)); //$NON-NLS-1$
        }
        String body = OracleUtils.normalizeSourceName(dataType, true);
        if (!CommonUtils.isEmpty(body)) {
            actions.add(
                new SQLDatabasePersistAction(
                    "Create type body",
                    "CREATE OR REPLACE " + body)); //$NON-NLS-1$
        }
        OracleUtils.addSchemaChangeActions(actions, dataType);
        return actions.toArray(new DBEPersistAction[actions.size()]);
    }

}

