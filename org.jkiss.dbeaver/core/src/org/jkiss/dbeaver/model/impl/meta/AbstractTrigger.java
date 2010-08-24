/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.meta;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.anno.Property;
import org.jkiss.dbeaver.model.struct.DBSTrigger;
import org.jkiss.dbeaver.model.struct.DBSActionTiming;
import org.jkiss.dbeaver.model.struct.DBSManipulationType;

/**
 * AbstractProcedure
 */
public abstract class AbstractTrigger implements DBSTrigger
{
    private String name;
    private DBSActionTiming actionTiming;
    private DBSManipulationType manipulationType;
    private int ordinalPosition;
    private String description;

    protected AbstractTrigger()
    {
    }

    protected AbstractTrigger(String name, String description)
    {
        this.name = name;
        this.description = description;
    }

    @Property(name = "Trigger Name", viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    protected void setName(String tableName)
    {
        this.name = tableName;
    }

    @Property(name = "Timing", viewable = true, order = 2)
    public DBSActionTiming getActionTiming()
    {
        return actionTiming;
    }

    public void setActionTiming(DBSActionTiming actionTiming)
    {
        this.actionTiming = actionTiming;
    }

    @Property(name = "Type", viewable = true, order = 3)
    public DBSManipulationType getManipulationType()
    {
        return manipulationType;
    }

    public void setManipulationType(DBSManipulationType manipulationType)
    {
        this.manipulationType = manipulationType;
    }

    @Property(name = "Ordinal Position", viewable = true, order = 99)
    public int getOrdinalPosition()
    {
        return ordinalPosition;
    }

    public void setOrdinalPosition(int ordinalPosition)
    {
        this.ordinalPosition = ordinalPosition;
    }

    @Property(name = "Trigger Description", viewable = true, order = 100)
    public String getDescription()
    {
        return description;
    }

    protected void setDescription(String description)
    {
        this.description = description;
    }

    public boolean refreshEntity(DBRProgressMonitor monitor)
        throws DBException
    {
        return false;
    }
}