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
package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;

/**
 * AbstractTable
 */
public abstract class AbstractTable<
    DATASOURCE extends DBPDataSource,
    CONTAINER extends DBSObject>
    implements DBSTable
{
    private CONTAINER container;
    private String tableName;

    protected AbstractTable(CONTAINER container)
    {
        this.container = container;
        this.tableName = "";
    }

    // Copy constructor
    protected AbstractTable(CONTAINER container, DBSEntity source)
    {
        this(container);
        this.tableName = source.getName();
    }

    protected AbstractTable(CONTAINER container, String tableName)
    {
        this(container);
        this.tableName = tableName;
    }

    public CONTAINER getContainer()
    {
        return container;
    }

    @NotNull
    @Override
    public DBSEntityType getEntityType()
    {
        return isView() ? DBSEntityType.VIEW : DBSEntityType.TABLE;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, order = 1)
    public String getName()
    {
        return tableName;
    }

    public void setName(String tableName)
    {
        this.tableName = tableName;
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public DATASOURCE getDataSource()
    {
        return (DATASOURCE) container.getDataSource();
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    @Override
    public CONTAINER getParentObject()
    {
        return container;
    }

    public String toString()
    {
        return getFullyQualifiedName(DBPEvaluationContext.UI);
    }

}
