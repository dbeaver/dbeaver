/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
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

    @NotNull
    @Override
    public GenericDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Override
    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getCatalog(),
            getTable().getSchema(),
            getTable(),
            this);
    }

}
