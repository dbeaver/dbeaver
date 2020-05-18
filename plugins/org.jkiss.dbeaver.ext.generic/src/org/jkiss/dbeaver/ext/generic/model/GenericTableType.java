/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Table type
 */
public class GenericTableType implements DBSObject {

    private GenericDataSource dataSource;
    private String name;

    public GenericTableType(GenericDataSource dataSource, String name)
    {
        this.dataSource = dataSource;
        this.name = name;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public DBSObject getParentObject()
    {
        return dataSource.getContainer();
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource()
    {
        return dataSource;
    }

    @NotNull
    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }
}
