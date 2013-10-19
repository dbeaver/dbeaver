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
package org.jkiss.dbeaver.ext.db2.manager;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCObjectEditor;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * DB2 Base class for Managers that only handle DROP statements
 * 
 * @author Denis Forveille
 */
public abstract class DB2AbstractDropOnlyManager<OBJECT_TYPE extends DBSObject & DBPSaveableObject, CONTAINER_TYPE extends DBSObject>
    extends JDBCObjectEditor<OBJECT_TYPE, CONTAINER_TYPE> {

    @Override
    public long getMakerOptions()
    {
        return 0;
    }

    @Override
    public boolean canCreateObject(CONTAINER_TYPE parent)
    {
        return false;
    }

    @Override
    public boolean canEditObject(OBJECT_TYPE object)
    {
        return false;
    }

    protected abstract String buildDropStatement(OBJECT_TYPE object);

    @Override
    protected IDatabasePersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command)
    {
        IDatabasePersistAction action = new AbstractDatabasePersistAction("Drop", buildDropStatement(command.getObject()));
        return new IDatabasePersistAction[] { action };
    }

    @Override
    protected OBJECT_TYPE createDatabaseObject(IWorkbenchWindow workbenchWindow, DBECommandContext context, CONTAINER_TYPE owner,
        Object copyFrom)
    {
        return null;
    }

    @Override
    protected IDatabasePersistAction[] makeObjectCreateActions(ObjectCreateCommand command)
    {
        return null;
    }

}
