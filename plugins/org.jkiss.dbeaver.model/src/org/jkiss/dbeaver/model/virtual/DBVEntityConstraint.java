/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.virtual;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Virtual constraint
 */
public class DBVEntityConstraint implements DBSEntityConstraint, DBSEntityReferrer
{
    private final DBVEntity entity;
    private final List<DBVEntityConstraintColumn> attributes = new ArrayList<>();
    private DBSEntityConstraintType type;
    private String name;

    public DBVEntityConstraint(DBVEntity entity, DBSEntityConstraintType type, String name)
    {
        this.entity = entity;
        this.type = type;
        this.name = (name == null ? type.getName() : name);
    }

    @Override
    public List<DBVEntityConstraintColumn> getAttributeReferences(@Nullable DBRProgressMonitor monitor)
    {
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

    public boolean hasAttributes()
    {
        return !attributes.isEmpty();
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

    void copyFrom(DBVEntityConstraint constraint)
    {
        this.attributes.clear();
        for (DBVEntityConstraintColumn col : constraint.attributes) {
            this.attributes.add(new DBVEntityConstraintColumn(this, col.getAttributeName()));
        }
    }
}
