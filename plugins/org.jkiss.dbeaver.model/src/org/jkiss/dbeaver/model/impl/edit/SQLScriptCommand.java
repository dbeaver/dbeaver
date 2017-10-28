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

import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Map;

/**
 * Script command
 */
public class SQLScriptCommand<OBJECT_TYPE extends DBSObject> extends DBECommandAbstract<OBJECT_TYPE> {

    private String script;

    public SQLScriptCommand(OBJECT_TYPE object, String title, String script)
    {
        super(object, title);
        this.script = script;
    }

    @Override
    public void updateModel()
    {
    }

    @Override
    public DBEPersistAction[] getPersistActions(Map<String, Object> options)
    {
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction(
                getTitle(),
                script)
        };
    }

}