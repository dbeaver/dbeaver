/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.impl.struct.AbstractTriggerColumn;
import org.jkiss.dbeaver.model.meta.Property;

/**
 * OracleTriggerColumn
 */
public class OracleTriggerColumn extends AbstractTriggerColumn
{
    private OracleTrigger trigger;
    private OracleTableColumn tableColumn;
    private int ordinalPosition;

    public OracleTriggerColumn(
        OracleTrigger trigger,
        OracleTableColumn tableColumn,
        int ordinalPosition)
    {
        this.trigger = trigger;
        this.tableColumn = tableColumn;
        this.ordinalPosition = ordinalPosition;
    }

    OracleTriggerColumn(OracleTrigger toIndex, OracleTriggerColumn source)
    {
        this.trigger = toIndex;
        this.tableColumn = source.tableColumn;
        this.ordinalPosition = source.ordinalPosition;
    }

    public OracleTrigger getTrigger()
    {
        return trigger;
    }

    //@Property(name = "Name", viewable = true, order = 1)
    public String getName()
    {
        return tableColumn.getName();
    }

    @Property(id = "name", name = "Column", viewable = true, order = 1)
    public OracleTableColumn getTableColumn()
    {
        return tableColumn;
    }

    @Property(name = "Position", viewable = false, order = 2)
    public int getOrdinalPosition()
    {
        return ordinalPosition;
    }

    public String getDescription()
    {
        return tableColumn.getDescription();
    }

    public OracleTrigger getParentObject()
    {
        return trigger;
    }

    public OracleDataSource getDataSource()
    {
        return trigger.getDataSource();
    }

}
