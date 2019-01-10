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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.Log;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * MySQLParameter
 */
public class MySQLParameter implements DBSObject
{
    private static final Log log = Log.getLog(MySQLParameter.class);

    private final MySQLDataSource dataSource;
    private final String name;
    private Object value;
    private String description;

    public MySQLParameter(MySQLDataSource dataSource, String name, Object value)
    {
        this.dataSource = dataSource;
        this.name = name;
        this.value = value;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, order = 2)
    public Object getValue()
    {
        return value;
    }

//    @Property(name = "Description", viewable = true, order = 100)
    @Nullable
    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public DBSObject getParentObject()
    {
        return getDataSource();
    }

    @NotNull
    @Override
    public MySQLDataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }
}
