/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.model.struct.DBSTableConstraint;

import java.util.Collection;

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

    @Property(id = "owner", name = "Owner", viewable = true, order = 2)
    public TABLE getTable()
    {
        return table;
    }

    @Override
    public Collection<? extends DBSEntityAttributeRef> getAttributeReferences(DBRProgressMonitor monitor)
    {
        return getColumns(monitor);
    }

    @Property(name = "Name", viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    //    @Property(name = "Description", viewable = true, order = 100)
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public DBSEntityConstraintType getConstraintType()
    {
        return constraintType;
    }

    public TABLE getParentObject()
    {
        return table;
    }

    public boolean isPersisted()
    {
        return true;
    }

    public String toString()
    {
        return getName() == null ? "<NONE>" : getName();
    }

}