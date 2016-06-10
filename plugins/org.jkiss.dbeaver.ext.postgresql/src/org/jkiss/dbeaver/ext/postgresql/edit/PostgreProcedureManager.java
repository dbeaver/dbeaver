/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedure;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.ui.dialogs.struct.CreateProcedureDialog;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * PostgreProcedureManager
 */
public class PostgreProcedureManager extends SQLObjectEditor<PostgreProcedure, PostgreSchema> {

    @Nullable
    @Override
    public DBSObjectCache<PostgreSchema, PostgreProcedure> getObjectsCache(PostgreProcedure object)
    {
        return object.getContainer().proceduresCache;
    }

    @Override
    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(ObjectChangeCommand command)
        throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Procedure name cannot be empty");
        }
    }

    @Override
    protected PostgreProcedure createDatabaseObject(DBECommandContext context, PostgreSchema parent, Object copyFrom)
    {
        CreateProcedureDialog dialog = new CreateProcedureDialog(DBeaverUI.getActiveWorkbenchShell(), parent.getDataSource());
        if (dialog.open() != IDialogConstants.OK_ID) {
            return null;
        }
        PostgreProcedure newProcedure = new PostgreProcedure(parent);
        newProcedure.setName(dialog.getProcedureName());
        return newProcedure;
    }

    @Override
    protected void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand command)
    {
        if (command.getProperties().size() > 1 || command.getProperty("description") == null) {
            createOrReplaceProcedureQuery(actions, command.getObject());
        }
    }

    @Override
    protected void addObjectModifyActions(List<DBEPersistAction> actionList, ObjectChangeCommand command)
    {
        if (command.getProperties().size() > 1 || command.getProperty("description") == null) {
            createOrReplaceProcedureQuery(actionList, command.getObject());
        }
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command)
    {
        String objectType = command.getObject().getProcedureTypeName();
        actions.add(
            new SQLDatabasePersistAction("Drop procedure", "DROP " + objectType + " " + command.getObject().getFullQualifiedSignature()) //$NON-NLS-2$
        );
    }

    private void createOrReplaceProcedureQuery(List<DBEPersistAction> actions, PostgreProcedure procedure)
    {
        actions.add(
            new SQLDatabasePersistAction("Create procedure", procedure.getBody()));
    }

    @Override
    protected void addObjectExtraActions(List<DBEPersistAction> actions, NestedObjectCommand<PostgreProcedure, PropertyHandler> command) {
        if (command.getProperty("description") != null) {
            actions.add(new SQLDatabasePersistAction(
                "Comment procedure",
                "COMMENT ON " + command.getObject().getProcedureTypeName() + " " + command.getObject().getFullQualifiedSignature() +
                    " IS '" + SQLUtils.escapeString(command.getObject().getDescription()) + "'"));
        }
    }

}

