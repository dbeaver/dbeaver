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
import org.jkiss.dbeaver.ext.oracle.model.OracleProcedureStandalone;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.dialogs.struct.CreateProcedureDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * OracleProcedureManager
 */
public class OracleProcedureManager extends SQLObjectEditor<OracleProcedureStandalone, OracleSchema> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, OracleProcedureStandalone> getObjectsCache(OracleProcedureStandalone object)
    {
        return object.getSchema().proceduresCache;
    }

    @Override
    protected OracleProcedureStandalone createDatabaseObject(IWorkbenchWindow workbenchWindow, DBECommandContext context, OracleSchema parent, Object copyFrom)
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
    protected DBEPersistAction[] makeObjectCreateActions(ObjectCreateCommand objectCreateCommand)
    {
        return createOrReplaceProcedureQuery(objectCreateCommand.getObject());
    }

    @Override
    protected DBEPersistAction[] makeObjectDeleteActions(ObjectDeleteCommand objectDeleteCommand)
    {
        final OracleProcedureStandalone object = objectDeleteCommand.getObject();
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction("Drop procedure",
                "DROP " + object.getProcedureType().name() + " " + object.getFullQualifiedName()) //$NON-NLS-1$ //$NON-NLS-2$
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

    private DBEPersistAction[] createOrReplaceProcedureQuery(OracleProcedureStandalone procedure)
    {
        String source = OracleUtils.normalizeSourceName(procedure, false);
        if (source == null) {
            return null;
        }
        List<DBEPersistAction> actions = new ArrayList<DBEPersistAction>();
        actions.add(new SQLDatabasePersistAction("Create procedure", "CREATE OR REPLACE " + source)); //$NON-NLS-2$
        OracleUtils.addSchemaChangeActions(actions, procedure);
        return actions.toArray(new DBEPersistAction[actions.size()]);
    }

}
