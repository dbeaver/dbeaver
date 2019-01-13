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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * PostgreSQL informational object
 */
public abstract class PostgreInformation implements DBSObject, PostgreObject {

    private PostgreDatabase database;

    protected PostgreInformation(PostgreDatabase database)
    {
        this.database = database;
    }

    @Override
    public DBSObject getParentObject()
    {
        return database;
    }

    @NotNull
    @Override
    public PostgreDatabase getDatabase() {
        return database;
    }


    @NotNull
    @Override
    public PostgreDataSource getDataSource()
    {
        return database.getDataSource();
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    @Override
    public String toString()
    {
        return getName();
    }
}
