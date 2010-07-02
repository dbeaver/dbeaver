package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;

import java.util.ArrayList;
import java.util.List;

/**
 * GenericConstraint
 */
public class GenericPrimaryKey extends GenericConstraint
{
    private List<GenericConstraintColumn> columns;

    protected GenericPrimaryKey(GenericTable table, String name, String remarks, DBSConstraintType constraintType)
    {
        super(table, name, remarks, constraintType);
    }

    /**
     * Copy constructor
     * @param constraint
     */
    GenericPrimaryKey(GenericPrimaryKey constraint)
    {
        super(constraint.getTable(), constraint.getName(), constraint.getDescription(), constraint.getConstraintType());
        if (constraint.columns != null) {
            this.columns = new ArrayList<GenericConstraintColumn>(constraint.columns.size());
            for (GenericConstraintColumn sourceColumn : constraint.columns) {
                this.columns.add(new GenericConstraintColumn(this, sourceColumn));
            }
        }
    }

    public List<GenericConstraintColumn> getColumns(DBRProgressMonitor monitor)
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