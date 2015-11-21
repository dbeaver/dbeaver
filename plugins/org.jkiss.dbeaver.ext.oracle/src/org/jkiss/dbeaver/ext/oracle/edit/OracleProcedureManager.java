/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
    protected OracleProcedureStandalone createDatabaseObject(DBECommandContext context, OracleSchema parent, Object copyFrom)
    {
        CreateProcedureDialog dialog = new CreateProcedureDialog(DBeaverUI.getActiveWorkbenchShell(), parent.getDataSource());
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
        List<DBEPersistAction> actions = new ArrayList<>();
        actions.add(new SQLDatabasePersistAction("Create procedure", source)); //$NON-NLS-2$
        OracleUtils.addSchemaChangeActions(actions, procedure);
        return actions.toArray(new DBEPersistAction[actions.size()]);
    }

}
