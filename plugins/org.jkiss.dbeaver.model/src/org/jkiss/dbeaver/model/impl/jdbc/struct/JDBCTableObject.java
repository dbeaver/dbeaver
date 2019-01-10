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
package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * JDBC abstract table object
 */
public abstract class JDBCTableObject<TABLE extends JDBCTable> implements DBSObject, DBPSaveableObject
{
    private final TABLE table;
    private String name;
    private boolean persisted;

    protected JDBCTableObject(TABLE table, String name, boolean persisted) {
        this.table = table;
        this.name = name;
        this.persisted = persisted;
    }

    protected JDBCTableObject(JDBCTableObject<TABLE> source)
    {
        this.table = source.table;
        this.name = source.name;
        this.persisted = source.persisted;
    }

    @Override
    public TABLE getParentObject()
    {
        return table;
    }

    @NotNull
    @Override
    public String getName()
    {
        return name;
    }

    public void setName(String indexName)
    {
        this.name = indexName;
    }

    @Property(viewable = true, order = 2)
    public TABLE getTable()
    {
        return table;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
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
