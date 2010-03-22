package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.struct.DBSConstraint;
import org.jkiss.dbeaver.model.struct.DBSConstraintColumn;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.anno.Property;
import org.jkiss.dbeaver.DBException;

import java.util.Collection;

/**
 * GenericConstraint
 */
public abstract class GenericConstraint implements DBSConstraint<GenericDataSource, GenericTable>
{
    private GenericTable table;
    private String name;
    private String remarks;

    protected GenericConstraint(GenericTable table, String name, String remarks)
    {
        this.table = table;
        this.name = name;
        this.remarks = remarks;
    }

    @Property(name = "Owner", viewable = true, order = 2)
    public GenericTable getTable()
    {
        return table;
    }

    @Property(name = "Name", viewable = false, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(name = "Description", viewable = true, order = 100)
    public String getDescription()
    {
        return remarks;
    }

    public GenericTable getParentObject()
    {
        return table;
    }

    public GenericDataSource getDataSource()
    {
        return table.getDataSource();
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
}
