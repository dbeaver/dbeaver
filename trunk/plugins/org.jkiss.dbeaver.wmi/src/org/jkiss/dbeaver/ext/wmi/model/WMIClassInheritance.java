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
package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

/**
 * Inheritance
 */
public class WMIClassInheritance implements DBSEntityAssociation
{

    private WMIClass superClass;
    private WMIClass subClass;

    public WMIClassInheritance(WMIClass superClass, WMIClass subClass)
    {
        this.superClass = superClass;
        this.subClass = subClass;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    @Override
    public String getName()
    {
        return subClass.getName() + " inherits " + superClass.getName();
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public WMIClass getParentObject()
    {
        return subClass;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource()
    {
        return subClass.getDataSource();
    }

    @Override
    public WMIClass getAssociatedEntity()
    {
        return superClass;
    }

    @NotNull
    @Override
    public DBSEntityConstraintType getConstraintType()
    {
        return DBSEntityConstraintType.INHERITANCE;
    }

    @NotNull
    @Override
    public DBSEntityConstraint getReferencedConstraint()
    {
        return null;
    }

}
