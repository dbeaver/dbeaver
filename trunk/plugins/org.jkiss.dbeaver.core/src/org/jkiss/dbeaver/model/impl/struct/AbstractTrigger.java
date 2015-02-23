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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSActionTiming;
import org.jkiss.dbeaver.model.struct.rdb.DBSManipulationType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;

/**
 * AbstractProcedure
 */
public abstract class AbstractTrigger implements DBSTrigger
{
    protected String name;
    private DBSActionTiming actionTiming;
    private DBSManipulationType manipulationType;
    private String description;

    protected AbstractTrigger()
    {
    }

    protected AbstractTrigger(String name, String description)
    {
        this.name = name;
        this.description = description;
    }

    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    protected void setName(String tableName)
    {
        this.name = tableName;
    }

    @Property(viewable = true, order = 2)
    public DBSActionTiming getActionTiming()
    {
        return actionTiming;
    }

    public void setActionTiming(DBSActionTiming actionTiming)
    {
        this.actionTiming = actionTiming;
    }

    @Property(viewable = true, order = 3)
    public DBSManipulationType getManipulationType()
    {
        return manipulationType;
    }

    public void setManipulationType(DBSManipulationType manipulationType)
    {
        this.manipulationType = manipulationType;
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 100)
    public String getDescription()
    {
        return description;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    protected void setDescription(String description)
    {
        this.description = description;
    }

}
