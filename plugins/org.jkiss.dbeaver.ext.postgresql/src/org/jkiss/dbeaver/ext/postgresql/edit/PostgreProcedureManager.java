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
import org.jkiss.dbeaver.ui.dialogs.struct.CreateProcedureDialog;
import org.jkiss.utils.CommonUtils;

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
        if (CommonUtils.isEmpty(command.getObject().getBody())) {
            throw new DBException("Procedure body cannot be empty");
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
    protected DBEPersistAction[] makeObjectCreateActions(ObjectCreateCommand command)
    {
        return createOrReplaceProcedureQuery(command.getObject());
    }

    @Override
    protected DBEPersistAction[] makeObjectModifyActions(ObjectChangeCommand command)
    {
        return createOrReplaceProcedureQuery(command.getObject());
    }

    @Override
    protected DBEPersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command)
    {
        String objectType = command.getObject().isAggregate() ? "AGGREGATE" : "FUNCTION";
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction("Drop procedure", "DROP " + objectType + " " + command.getObject().getFullQualifiedSignature()) //$NON-NLS-2$
        };
    }

    private DBEPersistAction[] createOrReplaceProcedureQuery(PostgreProcedure procedure)
    {
        String objectType = procedure.isAggregate() ? "AGGREGATE" : "FUNCTION";
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction("Drop procedure", "DROP " + objectType + " IF EXISTS " + procedure.getFullQualifiedSignature()), //$NON-NLS-2$ //$NON-NLS-3$
            new SQLDatabasePersistAction("Create procedure", procedure.getBody()) //$NON-NLS-2$
        };
    }

/*
    public ITabDescriptor[] getTabDescriptors(IWorkbenchWindow workbenchWindow, final IDatabaseEditor activeEditor, final PostgreProcedure object)
    {
        if (object.getContainer().isSystem()) {
            return null;
        }
        return new ITabDescriptor[] {
            new PropertyTabDescriptor(
                PropertiesContributor.CATEGORY_INFO,
                "procedure.body", //$NON-NLS-1$
                PostgreMessages.edit_procedure_manager_body,
                DBIcon.SOURCES.getImage(),
                new SectionDescriptor("default", PostgreMessages.edit_procedure_manager_body) {
                    public ISection getSectionClass()
                    {
                        return new PostgreProcedureBodySection(activeEditor);
                    }
                })
        };
    }

*/

}

