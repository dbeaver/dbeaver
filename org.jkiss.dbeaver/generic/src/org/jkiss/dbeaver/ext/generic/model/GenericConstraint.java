package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCConstraint;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;

import java.util.ArrayList;
import java.util.List;

/**
 * GenericConstraint
 */
public class GenericConstraint extends JDBCConstraint<GenericDataSource, GenericTable>
{
    private DBSConstraintType constraintType;
    private List<GenericConstraintColumn> columns;

    protected GenericConstraint(DBSConstraintType constraintType, GenericTable table, String name, String remarks)
    {
        super(table, name, remarks);
        this.constraintType = constraintType;
    }

    /**
     * Copy constructor
     * @param constraint
     */
    GenericConstraint(GenericConstraint constraint)
    {
        super(constraint.getTable(), constraint.getName(), constraint.getDescription());
        this.constraintType = constraint.constraintType;
        if (constraint.columns != null) {
            this.columns = new ArrayList<GenericConstraintColumn>(constraint.columns.size());
            for (GenericConstraintColumn sourceColumn : constraint.columns) {
                this.columns.add(new GenericConstraintColumn(this, sourceColumn));
            }
        }
    }

    public DBSConstraintType getConstraintType()
    {
        return constraintType;
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
