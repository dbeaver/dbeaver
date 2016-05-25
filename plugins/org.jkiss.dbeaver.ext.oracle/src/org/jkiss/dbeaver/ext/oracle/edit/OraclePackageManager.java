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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.oracle.OracleMessages;
import org.jkiss.dbeaver.ext.oracle.model.OraclePackage;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.dialogs.struct.CreateEntityDialog;
import org.jkiss.utils.CommonUtils;

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
    protected void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand objectCreateCommand)
    {
        createOrReplaceProcedureQuery(actions, objectCreateCommand.getObject());
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand objectDeleteCommand)
    {
        final OraclePackage object = objectDeleteCommand.getObject();
        actions.add(
            new SQLDatabasePersistAction("Drop package",
                "DROP PACKAGE " + object.getFullQualifiedName()) //$NON-NLS-1$
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

    private void createOrReplaceProcedureQuery(List<DBEPersistAction> actionList, OraclePackage pack)
    {
        try {
            String header = pack.getObjectDefinitionText(VoidProgressMonitor.INSTANCE);
            if (!CommonUtils.isEmpty(header)) {
                actionList.add(
                    new SQLDatabasePersistAction(
                        "Create package header",
                        header)); //$NON-NLS-1$
            }
            String body = pack.getObjectBodyDefinitionText(VoidProgressMonitor.INSTANCE);
            if (!CommonUtils.isEmpty(body)) {
                actionList.add(
                    new SQLDatabasePersistAction(
                        "Create package body",
                        body)); //$NON-NLS-1$
            } else {
                actionList.add(
                    new SQLDatabasePersistAction(
                        "Drop package header",
                        "DROP PACKAGE BODY " + pack.getFullQualifiedName(), DBEPersistAction.ActionType.OPTIONAL) //$NON-NLS-1$
                    );
            }
        } catch (DBException e) {
            log.warn(e);
        }
        OracleUtils.addSchemaChangeActions(actionList, pack);
    }

}

