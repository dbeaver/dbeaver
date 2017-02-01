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

/**
 * Direct persist action implementation
 */
public abstract class DirectDatabasePersistAction implements DBEPersistAction {

    private final String title;
    private final ActionType type;

    public DirectDatabasePersistAction(String title)
    {
        this(title, ActionType.NORMAL);
    }

    public DirectDatabasePersistAction(String title, ActionType type)
    {
        this.title = title;
        this.type = type;
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    @Override
    public String getScript()
    {
        return null;
    }

    @Override
    public ActionType getType()
    {
        return type;
    }

    @Override
    public boolean isComplex() {
        return false;
    }
}
