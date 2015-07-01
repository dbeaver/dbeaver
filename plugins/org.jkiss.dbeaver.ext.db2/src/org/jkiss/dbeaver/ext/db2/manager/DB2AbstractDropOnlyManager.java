/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.db2.manager;

import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * DB2 Base class for Managers that only handle DROP statements
 * 
 * @author Denis Forveille
 */
public abstract class DB2AbstractDropOnlyManager<OBJECT_TYPE extends DBSObject & DBPSaveableObject, CONTAINER_TYPE extends DBSObject>
    extends SQLObjectEditor<OBJECT_TYPE, CONTAINER_TYPE> {

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
    protected DBEPersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command)
    {
        DBEPersistAction action = new SQLDatabasePersistAction("Drop", buildDropStatement(command.getObject()));
        return new DBEPersistAction[] { action };
    }

    @Override
    protected OBJECT_TYPE createDatabaseObject(DBECommandContext context, CONTAINER_TYPE owner,
                                               Object copyFrom)
    {
        return null;
    }

    @Override
    protected DBEPersistAction[] makeObjectCreateActions(ObjectCreateCommand command)
    {
        return null;
    }

}
