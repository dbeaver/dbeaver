package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.anno.Property;
import org.jkiss.dbeaver.model.struct.DBSConstraint;
import org.jkiss.dbeaver.model.struct.DBSConstraintColumn;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * GenericConstraint
 */
public class GenericConstraint implements DBSConstraint
{
    private DBSConstraintType constraintType;
    private GenericTable table;
    private String name;
    private String remarks;
    private List<GenericConstraintColumn> columns;

    protected GenericConstraint(DBSConstraintType constraintType, GenericTable table, String name, String remarks)
    {
        this.constraintType = constraintType;
        this.table = table;
        this.name = name;
        this.remarks = remarks;
    }

    public DBSConstraintType getConstraintType()
    {
        return constraintType;
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

    public List<GenericConstraintColumn> getColumns()
    {
        return columns;
    }

    void addColumn(GenericConstraintColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<GenericConstraintColumn>();
        }
        this.columns.add(column);
    }
}
