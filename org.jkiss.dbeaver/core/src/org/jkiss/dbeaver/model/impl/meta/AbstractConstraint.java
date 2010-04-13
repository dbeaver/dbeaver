/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.meta;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.anno.Property;
import org.jkiss.dbeaver.model.struct.*;

import java.util.Collection;

/**
 * GenericConstraint
 */
public abstract class AbstractConstraint<
    DATASOURCE extends DBPDataSource,
    TABLE extends DBSTable>
    implements DBSConstraint
{
    private TABLE table;
    private String name;
    protected String description;

    protected AbstractConstraint(TABLE table, String name, String description)
    {
        this.table = table;
        this.name = name;
        this.description = description;
    }

    @Property(name = "Owner", viewable = true, order = 2)
    public TABLE getTable()
    {
        return table;
    }

    public DBSConstraintColumn getColumn(DBSTableColumn tableColumn)
    {
        Collection<? extends DBSConstraintColumn> columns = getColumns();
        for (DBSConstraintColumn constraintColumn : columns) {
            if (constraintColumn.getTableColumn() == tableColumn) {
                return constraintColumn;
            }
        }
        return null;
    }

    @Property(name = "Name", viewable = false, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(name = "Description", viewable = true, order = 100)
    public String getDescription()
    {
        return description;
    }

    protected void setDescription(String description)
    {
        this.description = description;
    }

    public DBSObject getParentObject()
    {
        return table;
    }

    public DATASOURCE getDataSource()
    {
        return (DATASOURCE) table.getDataSource();
    }

    public boolean refreshObject()
        throws DBException
    {
        return false;
    }

    public String toString()
    {
        return getName() == null ? "<NONE>" : getName();
    }

}