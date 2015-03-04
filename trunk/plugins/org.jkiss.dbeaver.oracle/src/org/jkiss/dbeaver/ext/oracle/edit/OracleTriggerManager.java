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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.ext.oracle.OracleMessages;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableBase;
import org.jkiss.dbeaver.ext.oracle.model.OracleTrigger;
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
 * OracleTriggerManager
 */
public class OracleTriggerManager extends SQLObjectEditor<OracleTrigger, OracleTableBase> {

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
            throw new DBException("Trigger name cannot be empty");
        }
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, OracleTrigger> getObjectsCache(OracleTrigger object)
    {
        return object.getSchema().triggerCache;
    }

    @Override
    protected OracleTrigger createDatabaseObject(IWorkbenchWindow workbenchWindow, DBECommandContext context, OracleTableBase parent, Object copyFrom)
    {
        CreateEntityDialog dialog = new CreateEntityDialog(workbenchWindow.getShell(), parent.getDataSource(), OracleMessages.edit_oracle_trigger_manager_dialog_title);
        if (dialog.open() != IDialogConstants.OK_ID) {
            return null;
        }
        OracleTrigger newTrigger = new OracleTrigger(parent.getContainer(), parent, dialog.getEntityName());
        newTrigger.setSourceDeclaration("TRIGGER " + dialog.getEntityName() + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
            "BEGIN\n" + //$NON-NLS-1$
            "END;"); //$NON-NLS-1$
        return newTrigger;
    }

    @Override
    protected DBEPersistAction[] makeObjectCreateActions(ObjectCreateCommand command)
    {
        return createOrReplaceViewQuery(command.getObject());
    }

    @Override
    protected DBEPersistAction[] makeObjectModifyActions(ObjectChangeCommand command)
    {
        return createOrReplaceViewQuery(command.getObject());
    }

    @Override
    protected DBEPersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command)
    {
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction("Drop trigger", "DROP TRIGGER " + command.getObject().getFullQualifiedName()) //$NON-NLS-2$
        };
    }

    private DBEPersistAction[] createOrReplaceViewQuery(OracleTrigger trigger)
    {
        String source = OracleUtils.normalizeSourceName(trigger, false);
        if (source == null) {
            return null;
        }
        List<DBEPersistAction> actions = new ArrayList<DBEPersistAction>();
        actions.add(new SQLDatabasePersistAction("Create trigger", "CREATE OR REPLACE " + source)); //$NON-NLS-2$
        OracleUtils.addSchemaChangeActions(actions, trigger);
        return actions.toArray(new DBEPersistAction[actions.size()]);
    }

}

