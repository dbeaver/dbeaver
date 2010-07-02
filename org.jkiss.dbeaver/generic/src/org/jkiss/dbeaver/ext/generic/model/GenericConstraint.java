package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCConstraint;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;

/**
 * GenericConstraint
 */
public abstract class GenericConstraint extends JDBCConstraint<GenericDataSource, GenericTable>
{
    protected GenericConstraint(GenericTable table, String name, String remarks, DBSConstraintType constraintType)
    {
        super(table, name, remarks, constraintType);
    }

    /**
     * Copy constructor
     * @param constraint
     */
    GenericConstraint(GenericConstraint constraint)
    {
        super(constraint.getTable(), constraint.getName(), constraint.getDescription(), constraint.getConstraintType());
    }

}
