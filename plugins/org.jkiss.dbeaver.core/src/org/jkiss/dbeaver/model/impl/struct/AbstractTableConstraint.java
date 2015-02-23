/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;

/**
 * GenericConstraint
 */
public abstract class AbstractTableConstraint<TABLE extends DBSTable> implements DBSTableConstraint
{
    private final TABLE table;
    private String name;
    protected String description;
    protected DBSEntityConstraintType constraintType;

    protected AbstractTableConstraint(TABLE table, String name, String description, DBSEntityConstraintType constraintType)
    {
        this.table = table;
        this.name = name;
        this.description = description;
        this.constraintType = constraintType;
    }

    @Property(id = "owner", viewable = true, order = 2)
    public TABLE getTable()
    {
        return table;
    }

    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    //    @Property(name = "Description", viewable = true, order = 100)
    @Nullable
    @Override
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    @NotNull
    @Override
    public DBSEntityConstraintType getConstraintType()
    {
        return constraintType;
    }

    @Override
    public TABLE getParentObject()
    {
        return table;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    public String toString()
    {
        return getName() == null ? "<NONE>" : getName();
    }

}
