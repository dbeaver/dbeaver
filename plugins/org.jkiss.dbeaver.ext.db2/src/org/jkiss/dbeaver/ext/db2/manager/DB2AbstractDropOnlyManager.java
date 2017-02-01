/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.db2.manager;

import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;

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
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command)
    {
        DBEPersistAction action = new SQLDatabasePersistAction("Drop", buildDropStatement(command.getObject()));
        actions.add(action);
    }

    @Override
    protected OBJECT_TYPE createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, CONTAINER_TYPE owner,
                                               Object copyFrom)
    {
        return null;
    }

    @Override
    protected void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand command)
    {
    }

}
