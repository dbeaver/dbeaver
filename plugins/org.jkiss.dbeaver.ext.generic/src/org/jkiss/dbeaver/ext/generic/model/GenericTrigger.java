/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;

import java.util.Map;

/**
 * GenericTrigger
 */
public abstract class GenericTrigger<OWNER extends DBSObject> implements DBSTrigger, GenericScriptObject
{
    @NotNull
    private final OWNER container;
    private String name;
    private String description;
    protected String source;

    public GenericTrigger(@NotNull OWNER container, String name, String description) {
        this.container = container;
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
    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 100)
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

    @NotNull
    public OWNER getContainer() {
        return container;
    }

    @Override
    public OWNER getParentObject()
    {
        return container;
    }

    @NotNull
    @Override
    public GenericDataSource getDataSource()
    {
        return (GenericDataSource) container.getDataSource();
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException
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
