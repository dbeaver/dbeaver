/*
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.db2.model.DB2Routine;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCObjectEditor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.dialogs.struct.CreateProcedureDialog;

/**
 * DB2ProcedureManager
 */
public class DB2ProcedureManager extends JDBCObjectEditor<DB2Routine, DB2Schema> {

    @Override
    public DBSObjectCache<? extends DBSObject, DB2Routine> getObjectsCache(DB2Routine object)
    {
        return object.getSchema().getProcedureCache();
    }

    @Override
    protected DB2Routine createDatabaseObject(IWorkbenchWindow workbenchWindow,
                                              DBECommandContext context,
                                              DB2Schema parent,
                                              Object copyFrom)
    {
        CreateProcedureDialog dialog = new CreateProcedureDialog(workbenchWindow.getShell(), parent.getDataSource());
        if (dialog.open() != IDialogConstants.OK_ID) {
            return null;
        }
        // return new DB2RoutineBase(parent, dialog.getProcedureName(),
        // dialog.getProcedureType());
        return null;
    }

    @Override
    protected IDatabasePersistAction[] makeObjectCreateActions(ObjectCreateCommand objectCreateCommand)
    {
        // return createOrReplaceProcedureQuery(objectCreateCommand.getObject());
        return null;
    }

    @Override
    protected IDatabasePersistAction[] makeObjectDeleteActions(ObjectDeleteCommand objectDeleteCommand)
    {
        // final DB2RoutineBase object = objectDeleteCommand.getObject();
        // return new IDatabasePersistAction[] { new
        // AbstractDatabasePersistAction("Drop procedure",
        //               "DROP " + object.getProcedureType().name() + " " + object.getFullQualifiedName()) //$NON-NLS-1$ //$NON-NLS-2$
        // };
        return null;
    }

    @Override
    protected IDatabasePersistAction[] makeObjectModifyActions(ObjectChangeCommand objectChangeCommand)
    {
        // return createOrReplaceProcedureQuery(objectChangeCommand.getObject());
        return null;
    }

    @Override
    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    private IDatabasePersistAction[] createOrReplaceProcedureQuery(DB2Routine procedure)
    {
        // String source = DB2Utils.normalizeSourceName(procedure, false);
        // if (source == null) {
        // return null;
        // }
        // List<IDatabasePersistAction> actions = new
        // ArrayList<IDatabasePersistAction>();
        //      actions.add(new AbstractDatabasePersistAction("Create procedure", "CREATE OR REPLACE " + source)); //$NON-NLS-2$
        // DB2Utils.addSchemaChangeActions(actions, procedure);
        // return actions.toArray(new IDatabasePersistAction[actions.size()]);
        return null;
    }

}
