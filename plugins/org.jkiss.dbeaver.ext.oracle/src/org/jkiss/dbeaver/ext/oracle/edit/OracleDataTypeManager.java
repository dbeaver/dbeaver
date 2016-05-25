/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.oracle.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.oracle.OracleMessages;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataType;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.dialogs.struct.CreateEntityDialog;
import org.jkiss.utils.CommonUtils;

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
    protected OracleDataType createDatabaseObject(DBECommandContext context, OracleSchema parent, Object copyFrom)
    {
        CreateEntityDialog dialog = new CreateEntityDialog(DBeaverUI.getActiveWorkbenchShell(), parent.getDataSource(), OracleMessages.edit_oracle_data_type_manager_dialog_title);
        if (dialog.open() != IDialogConstants.OK_ID) {
            return null;
        }
        OracleDataType dataType = new OracleDataType(
            parent,
            dialog.getEntityName(),
            false);
        dataType.setObjectDefinitionText("TYPE " + dataType.getName() + " AS OBJECT\n" + //$NON-NLS-1$ //$NON-NLS-2$
            "(\n" + //$NON-NLS-1$
            ")"); //$NON-NLS-1$
        return dataType;
    }

    @Override
    protected void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand objectCreateCommand)
    {
        createOrReplaceProcedureQuery(actions, objectCreateCommand.getObject());
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand objectDeleteCommand)
    {
        final OracleDataType object = objectDeleteCommand.getObject();
        actions.add(
            new SQLDatabasePersistAction("Drop type",
                "DROP TYPE " + object.getFullQualifiedName()) //$NON-NLS-1$
        );
    }

    @Override
    protected void addObjectModifyActions(List<DBEPersistAction> actionList, ObjectChangeCommand objectChangeCommand)
    {
        createOrReplaceProcedureQuery(actionList, objectChangeCommand.getObject());
    }

    @Override
    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    private void createOrReplaceProcedureQuery(List<DBEPersistAction> actionList, OracleDataType dataType)
    {
        String header = OracleUtils.normalizeSourceName(dataType, false);
        if (!CommonUtils.isEmpty(header)) {
            actionList.add(
                new SQLDatabasePersistAction(
                    "Create type header",
                    "CREATE OR REPLACE " + header)); //$NON-NLS-1$
        }
        String body = OracleUtils.normalizeSourceName(dataType, true);
        if (!CommonUtils.isEmpty(body)) {
            actionList.add(
                new SQLDatabasePersistAction(
                    "Create type body",
                    "CREATE OR REPLACE " + body)); //$NON-NLS-1$
        }
        OracleUtils.addSchemaChangeActions(actionList, dataType);
    }

}

