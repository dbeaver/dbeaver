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
package org.jkiss.dbeaver.model.virtual;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Virtual constraint
 */
public class DBVEntityConstraint implements DBSEntityConstraint, DBSEntityReferrer
{
    @NotNull
    private final DBVEntity entity;
    private final List<DBVEntityConstraintColumn> attributes = new ArrayList<>();
    private DBSEntityConstraintType type;
    private String name;
    private boolean useAllColumns = false;

    public DBVEntityConstraint(@NotNull DBVEntity entity, DBSEntityConstraintType type, String name)
    {
        this.entity = entity;
        this.type = type;
        this.name = (name == null ? type.getName() : name);
    }

    public DBVEntityConstraint(@NotNull DBVEntity entity, DBVEntityConstraint copy) {
        this.entity = entity;
        this.type = copy.type;
        this.name = copy.name;
        for (DBVEntityConstraintColumn col : copy.attributes) {
            this.attributes.add(new DBVEntityConstraintColumn(this, col));
        }
        this.useAllColumns = copy.useAllColumns;
    }

    @Override
    public List<DBVEntityConstraintColumn> getAttributeReferences(@Nullable DBRProgressMonitor monitor)
    {
        return attributes;
    }

    public List<DBVEntityConstraintColumn> getAttributes() {
        return attributes;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @NotNull
    @Override
    public DBVEntity getParentObject()
    {
        return entity;
    }

    @NotNull
    public DBVEntity getEntity() {
        return entity;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource()
    {
        return entity.getDataSource();
    }

    @NotNull
    @Override
    public DBSEntityConstraintType getConstraintType()
    {
        return type;
    }

    @NotNull
    @Override
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    public boolean isUseAllColumns() {
        return useAllColumns;
    }

    public void setUseAllColumns(boolean useAllColumns) {
        this.useAllColumns = useAllColumns;
    }

    public boolean hasAttributes()
    {
        return useAllColumns || !attributes.isEmpty();
    }

    public void setAttributes(Collection<DBSEntityAttribute> realAttributes)
    {
        attributes.clear();
        for (DBSEntityAttribute attr : realAttributes) {
            attributes.add(new DBVEntityConstraintColumn(this, attr.getName()));
        }
    }

    public void addAttribute(String name)
    {
        attributes.add(new DBVEntityConstraintColumn(this, name));
    }

    @Nullable
    private DBSAttributeEnumerable getEnumAttr() {
        if (attributes.size() == 1) {
            DBSEntityAttribute attribute = attributes.get(0).getAttribute();
            if (attribute instanceof DBSAttributeEnumerable) {
                return (DBSAttributeEnumerable) attribute;
            }
        }
        return null;
    }

}
