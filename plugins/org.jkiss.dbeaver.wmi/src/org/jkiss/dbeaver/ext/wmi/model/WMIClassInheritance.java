/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSTableConstraint;

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

    @Override
    public DBSEntityConstraintType getConstraintType()
    {
        return DBSEntityConstraintType.INHERITANCE;
    }

    @Override
    public DBSTableConstraint getReferencedConstraint()
    {
        return null;
    }

}
