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
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;

/**
 * Object persist action implementation
 */
public class SQLDatabasePersistAction implements DBEPersistAction {

    private final String title;
    private final String script;
    private final ActionType type;
    private final boolean complex;

    public SQLDatabasePersistAction(String title, String script)
    {
        this(title, script, ActionType.NORMAL, false);
    }

    public SQLDatabasePersistAction(String title, String script, boolean complex)
    {
        this(title, script, ActionType.NORMAL, complex);
    }

    public SQLDatabasePersistAction(String title, String script, ActionType type)
    {
        this(title, script, type, false);
    }

    public SQLDatabasePersistAction(String title, String script, ActionType type, boolean complex)
    {
        this.title = title;
        this.script = script;
        this.type = type;
        this.complex = complex;
    }

    public SQLDatabasePersistAction(String script)
    {
        this("", script, ActionType.NORMAL);
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    @Override
    public String getScript()
    {
        return script;
    }

    @Override
    public void beforeExecute(DBCSession session) throws DBCException {
        // do nothing
    }

    @Override
    public void afterExecute(DBCSession session, Throwable error)
        throws DBCException
    {
        // do nothing
    }

    @Override
    public ActionType getType()
    {
        return type;
    }

    @Override
    public boolean isComplex() {
        return complex;
    }
}
