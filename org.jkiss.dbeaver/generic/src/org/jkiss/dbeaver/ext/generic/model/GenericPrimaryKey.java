package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.struct.DBSPrimaryKey;

import java.util.ArrayList;
import java.util.List;

/**
 * GenericPrimaryKey
 */
public class GenericPrimaryKey extends GenericConstraint implements DBSPrimaryKey<GenericDataSource, GenericTable>
{
    private List<GenericConstraintColumn> columns;

    public GenericPrimaryKey(GenericTable table, String name, String remarks)
    {
        super(table, name, remarks);
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
