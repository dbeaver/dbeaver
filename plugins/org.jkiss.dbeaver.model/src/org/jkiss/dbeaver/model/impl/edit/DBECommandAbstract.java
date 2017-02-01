/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.model.impl.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.edit.DBECommand;

import java.util.Map;

/**
 * Abstract object command
 */
public class DBECommandAbstract<OBJECT_TYPE extends DBPObject> implements DBECommand<OBJECT_TYPE> {
    private final OBJECT_TYPE object;
    private final String title;

    public DBECommandAbstract(OBJECT_TYPE object, String title)
    {
        this.object = object;
        this.title = title;
    }

    @Override
    public OBJECT_TYPE getObject()
    {
        return object;
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    @Override
    public boolean isUndoable()
    {
        return true;
    }

    @Override
    public void validateCommand() throws DBException
    {
        // do nothing by default
    }

    @Override
    public void updateModel()
    {
    }

    @Override
    public DBECommand<?> merge(DBECommand<?> prevCommand, Map<Object, Object> userParams)
    {
        return this;
    }

    @Override
    public DBEPersistAction[] getPersistActions()
    {
        return null;
    }

}
