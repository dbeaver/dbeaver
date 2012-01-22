/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.wmi.service.WMIObjectAttribute;

import java.util.Collection;

/**
 * Class association
 */
public class WMIClassReference extends WMIClassElement<WMIObjectAttribute> implements DBSTableForeignKey
{
    private WMIClass refClass;

    protected WMIClassReference(WMIClass wmiClass, WMIObjectAttribute attribute, WMIClass refClass)
    {
        super(wmiClass, attribute);
        this.refClass = refClass;
    }

    public DBSTable getTable()
    {
        return getParentObject();
    }

    public DBSEntityConstraintType getConstraintType()
    {
        return DBSEntityConstraintType.ASSOCIATION;
    }

    public Collection<? extends DBSTableConstraintColumn> getColumns(DBRProgressMonitor monitor)
    {
        return null;
    }

    public DBSTableConstraintColumn getColumn(DBRProgressMonitor monitor, DBSTableColumn tableColumn)
    {
        return null;
    }

    public DBSEntity getAssociatedEntity()
    {
        return refClass;
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

    public String getFullQualifiedName()
    {
        return getName();
    }
}
