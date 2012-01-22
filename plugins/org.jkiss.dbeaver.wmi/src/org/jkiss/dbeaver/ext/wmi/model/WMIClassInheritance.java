/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.util.Collection;

/**
 * Inheritance
 */
public class WMIClassInheritance implements DBSTableForeignKey
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

    public String getFullQualifiedName()
    {
        return getName();
    }

    public DBSEntityConstraintType getConstraintType()
    {
        return DBSEntityConstraintType.INHERITANCE;
    }

    public DBSTable getTable()
    {
        return subClass;
    }

    public Collection<? extends DBSTableConstraintColumn> getColumns(DBRProgressMonitor monitor)
    {
        return null;
    }

    public DBSTableConstraintColumn getColumn(DBRProgressMonitor monitor, DBSTableColumn tableColumn)
    {
        return null;
    }

    public DBSTableConstraint getReferencedConstraint()
    {
        return null;
    }

    public DBSConstraintModifyRule getDeleteRule()
    {
        return DBSConstraintModifyRule.RESTRICT;
    }

    public DBSConstraintModifyRule getUpdateRule()
    {
        return DBSConstraintModifyRule.RESTRICT;
    }
}
