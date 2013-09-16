/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
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
import org.jkiss.dbeaver.ext.db2.DB2Messages;
import org.jkiss.dbeaver.ext.db2.model.DB2DataType;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCObjectEditor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.dialogs.struct.CreateEntityDialog;

/**
 * DB2 Data Type Manager
 *
 * @author Denis Forveille
 */
public class DB2DataTypeManager extends JDBCObjectEditor<DB2DataType, DB2Schema> {

    private static final String SQL_DROP_TYPE = "DROP TYPE %s RESTRICT";

    @Override
    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    public DBSObjectCache<? extends DBSObject, DB2DataType> getObjectsCache(DB2DataType object)
    {
        // return object.getSchema().getDataTypeCache();
        return null;
    }

    @Override
    protected DB2DataType createDatabaseObject(IWorkbenchWindow workbenchWindow,
                                               DBECommandContext context,
                                               DB2Schema parent,
                                               Object copyFrom)
    {
        CreateEntityDialog dialog = new CreateEntityDialog(workbenchWindow.getShell(),
            parent.getDataSource(),
            DB2Messages.edit_db2_data_type_manager_dialog_title);
        if (dialog.open() != IDialogConstants.OK_ID) {
            return null;
        }
        // DB2DataType dataType = new DB2DataType(
        // parent,
        // dialog.getEntityName(),
        // false);
        //        dataType.setSourceDeclaration("TYPE " + dataType.getName() + " AS OBJECT\n" + //$NON-NLS-1$ //$NON-NLS-2$
        //            "(\n" + //$NON-NLS-1$
        //            ")"); //$NON-NLS-1$
        // return dataType;
        return null;
    }

    @Override
    protected IDatabasePersistAction[] makeObjectCreateActions(ObjectCreateCommand objectCreateCommand)
    {
        return createOrReplaceProcedureQuery(objectCreateCommand.getObject());
    }

    @Override
    protected IDatabasePersistAction[] makeObjectDeleteActions(ObjectDeleteCommand objectDeleteCommand)
    {
        String typeName = objectDeleteCommand.getObject().getFullQualifiedName();
        IDatabasePersistAction action = new AbstractDatabasePersistAction("Drop type", String.format(SQL_DROP_TYPE, typeName));
        return new IDatabasePersistAction[]{action};
    }

    @Override
    protected IDatabasePersistAction[] makeObjectModifyActions(ObjectChangeCommand objectChangeCommand)
    {
        return createOrReplaceProcedureQuery(objectChangeCommand.getObject());
    }

    private IDatabasePersistAction[] createOrReplaceProcedureQuery(DB2DataType dataType)
    {
        // List<IDatabasePersistAction> actions = new
        // ArrayList<IDatabasePersistAction>();
        // String header = DB2Utils.normalizeSourceName(dataType, false);
        // if (!CommonUtils.isEmpty(header)) {
        // actions.add(
        // new AbstractDatabasePersistAction(
        // "Create type header",
        //                    "CREATE OR REPLACE " + header)); //$NON-NLS-1$
        // }
        // String body = DB2Utils.normalizeSourceName(dataType, true);
        // if (!CommonUtils.isEmpty(body)) {
        // actions.add(
        // new AbstractDatabasePersistAction(
        // "Create type body",
        //                    "CREATE OR REPLACE " + body)); //$NON-NLS-1$
        // }
        // // DB2Utils.addSchemaChangeActions(actions, dataType);
        // return actions.toArray(new IDatabasePersistAction[actions.size()]);
        return null;
    }

}
