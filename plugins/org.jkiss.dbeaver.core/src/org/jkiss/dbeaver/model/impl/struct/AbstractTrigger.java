/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSActionTiming;
import org.jkiss.dbeaver.model.struct.DBSManipulationType;
import org.jkiss.dbeaver.model.struct.DBSTrigger;

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

    @Property(name = "Trigger Description", viewable = true, order = 100)
    public String getDescription()
    {
        return description;
    }

    public boolean isPersisted()
    {
        return true;
    }

    protected void setDescription(String description)
    {
        this.description = description;
    }

}