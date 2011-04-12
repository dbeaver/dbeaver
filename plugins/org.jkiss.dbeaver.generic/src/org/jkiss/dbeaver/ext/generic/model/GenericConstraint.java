/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.DBUtils;
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
    protected GenericConstraint(GenericConstraint constraint)
    {
        super(constraint.getTable(), constraint.getName(), constraint.getDescription(), constraint.getConstraintType());
    }

    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getCatalog(),
            getTable().getSchema(),
            getTable(),
            this);
    }

}
