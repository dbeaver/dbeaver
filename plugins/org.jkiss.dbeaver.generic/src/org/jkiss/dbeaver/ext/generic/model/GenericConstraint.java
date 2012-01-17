/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

/**
 * GenericConstraint
 */
public abstract class GenericConstraint extends JDBCConstraint<GenericTable>
{
    protected GenericConstraint(GenericTable table, String name, String remarks, DBSEntityConstraintType constraintType, boolean persisted)
    {
        super(table, name, remarks, constraintType, persisted);
    }

    /**
     * Copy constructor
     * @param constraint source
     */
    protected GenericConstraint(GenericConstraint constraint)
    {
        super(constraint.getTable(), constraint.getName(), constraint.getDescription(), constraint.getConstraintType(), constraint.isPersisted());
    }

    public GenericDataSource getDataSource()
    {
        return getTable().getDataSource();
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
