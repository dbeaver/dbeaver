/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.dbeaver.Log;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Abstract DB2 schema object
 * 
 * @author Denis Forveille
 */
public abstract class DB2GlobalObject implements DBSObject, DBPSaveableObject {
    protected Log log = Log.getLog(DB2GlobalObject.class);

    private final DB2DataSource dataSource;
    private boolean persisted;

    // -----------------------
    // Constructors
    // -----------------------

    protected DB2GlobalObject(DB2DataSource dataSource, boolean persisted)
    {
        this.dataSource = dataSource;
        this.persisted = persisted;
    }

    // By default : no Description
    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    // -----------------------
    // Standard Getters/Setters
    // -----------------------

    @Override
    public DBSObject getParentObject()
    {
        return dataSource.getContainer();
    }

    @NotNull
    @Override
    public DB2DataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    public boolean isPersisted()
    {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted)
    {
        this.persisted = persisted;
    }

}
