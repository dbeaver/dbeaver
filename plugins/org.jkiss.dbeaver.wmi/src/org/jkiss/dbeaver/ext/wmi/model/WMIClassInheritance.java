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

    public boolean isPersisted()
    {
        return true;
    }

    public String getName()
    {
        return subClass.getName() + " inherits " + superClass.getName();
    }

    public String getDescription()
    {
        return null;
    }

    public WMIClass getParentObject()
    {
        return subClass;
    }

    public DBPDataSource getDataSource()
    {
        return subClass.getDataSource();
    }

    public WMIClass getAssociatedEntity()
    {
        return superClass;
    }

    public DBSEntityConstraintType getConstraintType()
    {
        return DBSEntityConstraintType.INHERITANCE;
    }

    public DBSTableConstraint getReferencedConstraint()
    {
        return null;
    }

}
