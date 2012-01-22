/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

/**
 * GenericTableConstraint
 */
public abstract class GenericTableConstraint extends JDBCTableConstraint<GenericTable>
{
    protected GenericTableConstraint(GenericTable table, String name, String remarks, DBSEntityConstraintType constraintType, boolean persisted)
    {
        super(table, name, remarks, constraintType, persisted);
    }

    /**
     * Copy constructor
     * @param constraint source
     */
    protected GenericTableConstraint(GenericTableConstraint constraint)
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
