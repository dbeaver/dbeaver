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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;

/**
 * GenericProcedure
 */
public class GenericTrigger implements DBSTrigger, GenericScriptObject
{
    @NotNull
    private final GenericStructContainer container;
    @Nullable
    private final GenericTable table;
    private String name;
    private String description;
    protected String source;

    public GenericTrigger(@NotNull GenericStructContainer container, @Nullable GenericTable table, String name, String description) {
        this.container = container;
        this.table = table;
        this.name = name;
        this.description = description;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, order = 1)
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 100)
    public String getDescription()
    {
        return description;
    }

    protected void setDescription(String description)
    {
        this.description = description;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 4)
    public GenericTable getTable()
    {
        return table;
    }

    @NotNull
    public GenericStructContainer getContainer() {
        return container;
    }

    @Override
    public DBSObject getParentObject()
    {
        return table == null ? container : table;
    }

    @NotNull
    @Override
    public GenericDataSource getDataSource()
    {
        return container.getDataSource();
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor) throws DBException
    {
        if (source == null) {
            source = getDataSource().getMetaModel().getTriggerDDL(monitor, this);
        }
        return source;
    }

    public void setSource(String sourceText) throws DBException
    {
        source = sourceText;
    }

}
