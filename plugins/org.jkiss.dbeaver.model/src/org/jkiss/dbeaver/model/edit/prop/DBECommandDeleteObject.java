/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.model.edit.prop;

import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;

import java.util.Map;

/**
 * Delete object command
 */
public abstract class DBECommandDeleteObject<OBJECT_TYPE extends DBPObject> extends DBECommandAbstract<OBJECT_TYPE> {

    //public static final String PROP_COMPOSITE_COMMAND = ".composite";

    public DBECommandDeleteObject(OBJECT_TYPE object, String title)
    {
        super(object, title);
    }

    @Override
    public DBECommand<?> merge(DBECommand<?> prevCommand, Map<Object, Object> userParams)
    {
        if (prevCommand != null && prevCommand.getObject() == getObject()) {
            return null;
        }
        return this;
    }

}