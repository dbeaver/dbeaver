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
import org.jkiss.dbeaver.ext.oracle.OracleMessages;
import org.jkiss.dbeaver.ext.oracle.model.OraclePackage;
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
 * OraclePackageManager
 */
public class OraclePackageManager extends SQLObjectEditor<OraclePackage, OracleSchema> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, OraclePackage> getObjectsCache(OraclePackage object)
    {
        return object.getSchema().packageCache;
    }

    @Override
    protected OraclePackage createDatabaseObject(DBECommandContext context, OracleSchema parent, Object copyFrom)
    {
        CreateEntityDialog dialog = new CreateEntityDialog(DBeaverUI.getActiveWorkbenchShell(), parent.getDataSource(), OracleMessages.edit_oracle_package_manager_dialog_title);
        if (dialog.open() != IDialogConstants.OK_ID) {
            return null;
        }
        return new OraclePackage(
            parent,
            dialog.getEntityName());
    }

    @Override
    protected DBEPersistAction[] makeObjectCreateActions(ObjectCreateCommand objectCreateCommand)
    {
        return createOrReplaceProcedureQuery(objectCreateCommand.getObject());
    }

    @Override
    protected DBEPersistAction[] makeObjectDeleteActions(ObjectDeleteCommand objectDeleteCommand)
    {
        final OraclePackage object = objectDeleteCommand.getObject();
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction("Drop package",
                "DROP PACKAGE " + object.getFullQualifiedName()) //$NON-NLS-1$
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

    private DBEPersistAction[] createOrReplaceProcedureQuery(OraclePackage pack)
    {
        List<DBEPersistAction> actions = new ArrayList<DBEPersistAction>();
        String header = OracleUtils.normalizeSourceName(pack, false);
        if (!CommonUtils.isEmpty(header)) {
            actions.add(
                new SQLDatabasePersistAction(
                    "Create package header",
                    "CREATE OR REPLACE " + header)); //$NON-NLS-1$
        }
        String body = OracleUtils.normalizeSourceName(pack, true);
        if (!CommonUtils.isEmpty(body)) {
            actions.add(
                new SQLDatabasePersistAction(
                    "Create package body",
                    "CREATE OR REPLACE " + body)); //$NON-NLS-1$
        } else {
            actions.add(
                new SQLDatabasePersistAction(
                    "Drop package header",
                    "DROP PACKAGE BODY " + pack.getFullQualifiedName(), DBEPersistAction.ActionType.OPTIONAL) //$NON-NLS-1$
                );
        }
        OracleUtils.addSchemaChangeActions(actions, pack);
        return actions.toArray(new DBEPersistAction[actions.size()]);
    }

}

